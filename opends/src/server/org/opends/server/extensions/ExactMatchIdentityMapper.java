/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE
 * or https://OpenDS.dev.java.net/OpenDS.LICENSE.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opends/resource/legal-notices/OpenDS.LICENSE.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.extensions;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;

import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.server.ExactMatchIdentityMapperCfg;
import org.opends.server.admin.std.server.IdentityMapperCfg;
import org.opends.server.api.IdentityMapper;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.InitializationException;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchFilter;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;

import static org.opends.server.messages.ExtensionsMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class provides an implementation of a Directory Server identity mapper
 * that looks for the exact value provided as the ID string to appear in an
 * attribute of a user's entry.  This mapper may be configured to look in one or
 * more attributes using zero or more search bases.  In order for the mapping to
 * be established properly, exactly one entry must have an attribute that
 * exactly matches (according to the equality matching rule associated with that
 * attribute) the ID value.
 */
public class ExactMatchIdentityMapper
       extends IdentityMapper<ExactMatchIdentityMapperCfg>
       implements ConfigurationChangeListener<
                       ExactMatchIdentityMapperCfg>
{
  // The set of attribute types to use when performing lookups.
  private AttributeType[] attributeTypes;

  // The DN of the configuration entry for this identity mapper.
  private DN configEntryDN;

  // The current configuration for this identity mapper.
  private ExactMatchIdentityMapperCfg currentConfig;

  // The set of attributes to return in search result entries.
  private LinkedHashSet<String> requestedAttributes;



  /**
   * Creates a new instance of this exact match identity mapper.  All
   * initialization should be performed in the {@code initializeIdentityMapper}
   * method.
   */
  public ExactMatchIdentityMapper()
  {
    super();

    // Don't do any initialization here.
  }



  /**
   * {@inheritDoc}
   */
  public void initializeIdentityMapper(
                   ExactMatchIdentityMapperCfg configuration)
         throws ConfigException, InitializationException
  {
    configuration.addExactMatchChangeListener(this);

    currentConfig = configuration;
    configEntryDN = currentConfig.dn();


    // Get the attribute types to use for the searches.
    SortedSet<String> attrNames = currentConfig.getMatchAttribute();
    attributeTypes = new AttributeType[attrNames.size()];
    int i=0;
    for (String name : attrNames)
    {
      AttributeType type = DirectoryServer.getAttributeType(toLowerCase(name),
                                                            false);
      if (type == null)
      {
        int    msgID   = MSGID_EXACTMAP_UNKNOWN_ATTR;
        String message = getMessage(msgID, String.valueOf(configEntryDN),
                                    name);
        throw new ConfigException(msgID, message);
      }

      attributeTypes[i++] = type;
    }


    // Create the attribute list to include in search requests.  We want to
    // include all user and operational attributes.
    requestedAttributes = new LinkedHashSet<String>(2);
    requestedAttributes.add("*");
    requestedAttributes.add("+");
  }



  /**
   * Performs any finalization that may be necessary for this identity mapper.
   */
  public void finalizeIdentityMapper()
  {
    currentConfig.removeExactMatchChangeListener(this);
  }



  /**
   * Retrieves the user entry that was mapped to the provided identification
   * string.
   *
   * @param  id  The identification string that is to be mapped to a user.
   *
   * @return  The user entry that was mapped to the provided identification, or
   *          <CODE>null</CODE> if no users were found that could be mapped to
   *          the provided ID.
   *
   * @throws  DirectoryException  If a problem occurs while attempting to map
   *                              the given ID to a user entry, or if there are
   *                              multiple user entries that could map to the
   *                              provided ID.
   */
  public Entry getEntryForID(String id)
         throws DirectoryException
  {
    ExactMatchIdentityMapperCfg config = currentConfig;
    AttributeType[] attributeTypes = this.attributeTypes;


    // Construct the search filter to use to make the determination.
    SearchFilter filter;
    if (attributeTypes.length == 1)
    {
      AttributeValue value = new AttributeValue(attributeTypes[0], id);
      filter = SearchFilter.createEqualityFilter(attributeTypes[0], value);
    }
    else
    {
      ArrayList<SearchFilter> filterComps =
           new ArrayList<SearchFilter>(attributeTypes.length);
      for (AttributeType t : attributeTypes)
      {
        AttributeValue value = new AttributeValue(t, id);
        filterComps.add(SearchFilter.createEqualityFilter(t, value));
      }

      filter = SearchFilter.createORFilter(filterComps);
    }


    // Iterate through the set of search bases and process an internal search
    // to find any matching entries.  Since we'll only allow a single match,
    // then use size and time limits to constrain costly searches resulting from
    // non-unique or inefficient criteria.
    Collection<DN> baseDNs = config.getMatchBaseDN();
    if ((baseDNs == null) || baseDNs.isEmpty())
    {
      baseDNs = DirectoryServer.getPublicNamingContexts().keySet();
    }

    SearchResultEntry matchingEntry = null;
    InternalClientConnection conn =
         InternalClientConnection.getRootConnection();
    for (DN baseDN : baseDNs)
    {
      InternalSearchOperation internalSearch =
           conn.processSearch(baseDN, SearchScope.WHOLE_SUBTREE,
                              DereferencePolicy.NEVER_DEREF_ALIASES, 1, 10,
                              false, filter, requestedAttributes);

      switch (internalSearch.getResultCode())
      {
        case SUCCESS:
          // This is fine.  No action needed.
          break;

        case NO_SUCH_OBJECT:
          // The search base doesn't exist.  Not an ideal situation, but we'll
          // ignore it.
          break;

        case SIZE_LIMIT_EXCEEDED:
          // Multiple entries matched the filter.  This is not acceptable.
          int    msgID   = MSGID_EXACTMAP_MULTIPLE_MATCHING_ENTRIES;
          String message = getMessage(msgID, String.valueOf(id));
          throw new DirectoryException(ResultCode.CONSTRAINT_VIOLATION, message,
                                       msgID);

        case TIME_LIMIT_EXCEEDED:
        case ADMIN_LIMIT_EXCEEDED:
          // The search criteria was too inefficient.
          msgID   = MSGID_EXACTMAP_INEFFICIENT_SEARCH;
          message = getMessage(msgID, String.valueOf(id),
                         String.valueOf(internalSearch.getErrorMessage()));
          throw new DirectoryException(internalSearch.getResultCode(), message,
                                       msgID);

        default:
          // Just pass on the failure that was returned for this search.
          msgID   = MSGID_EXACTMAP_SEARCH_FAILED;
          message = getMessage(msgID, String.valueOf(id),
                         String.valueOf(internalSearch.getErrorMessage()));
          throw new DirectoryException(internalSearch.getResultCode(), message,
                                       msgID);
      }

      LinkedList<SearchResultEntry> searchEntries =
           internalSearch.getSearchEntries();
      if ((searchEntries != null) && (! searchEntries.isEmpty()))
      {
        if (matchingEntry == null)
        {
          Iterator<SearchResultEntry> iterator = searchEntries.iterator();
          matchingEntry = iterator.next();
          if (iterator.hasNext())
          {
            int    msgID   = MSGID_EXACTMAP_MULTIPLE_MATCHING_ENTRIES;
            String message = getMessage(msgID, String.valueOf(id));
            throw new DirectoryException(ResultCode.CONSTRAINT_VIOLATION,
                                         message, msgID);
          }
        }
        else
        {
          int    msgID   = MSGID_EXACTMAP_MULTIPLE_MATCHING_ENTRIES;
          String message = getMessage(msgID, String.valueOf(id));
          throw new DirectoryException(ResultCode.CONSTRAINT_VIOLATION, message,
                                       msgID);
        }
      }
    }


    if (matchingEntry == null)
    {
      return null;
    }
    else
    {
      return matchingEntry;
    }
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public boolean isConfigurationAcceptable(IdentityMapperCfg configuration,
                                           List<String> unacceptableReasons)
  {
    ExactMatchIdentityMapperCfg config =
         (ExactMatchIdentityMapperCfg) configuration;
    return isConfigurationChangeAcceptable(config, unacceptableReasons);
  }



  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationChangeAcceptable(
                      ExactMatchIdentityMapperCfg configuration,
                      List<String> unacceptableReasons)
  {
    boolean configAcceptable = true;

    // Make sure that the set of attribute types is acceptable.
    SortedSet<String> attributeNames = configuration.getMatchAttribute();
    for (String name : attributeNames)
    {
      AttributeType t = DirectoryServer.getAttributeType(toLowerCase(name),
                                                         false);
      if (t == null)
      {
        int msgID = MSGID_EXACTMAP_UNKNOWN_ATTR;
        unacceptableReasons.add(getMessage(msgID, configuration.dn(), name));
        configAcceptable = false;
      }
    }


    return configAcceptable;
  }



  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationChange(
              ExactMatchIdentityMapperCfg configuration)
  {
    ResultCode        resultCode          = ResultCode.SUCCESS;
    boolean           adminActionRequired = false;
    ArrayList<String> messages            = new ArrayList<String>();


    // Get the attribute types to use for the searches.
    SortedSet<String> attrNames = configuration.getMatchAttribute();
    AttributeType[] newAttributeTypes = new AttributeType[attrNames.size()];
    int i=0;
    for (String name : attrNames)
    {
      AttributeType type = DirectoryServer.getAttributeType(toLowerCase(name),
                                                            false);
      if (type == null)
      {
        if (resultCode == ResultCode.SUCCESS)
        {
          resultCode = ResultCode.NO_SUCH_ATTRIBUTE;
        }

        int msgID = MSGID_EXACTMAP_UNKNOWN_ATTR;
        messages.add(getMessage(msgID, String.valueOf(configEntryDN), name));
      }

      newAttributeTypes[i++] = type;
    }


    if (resultCode == ResultCode.SUCCESS)
    {
      attributeTypes = newAttributeTypes;
      currentConfig  = configuration;
    }


   return new ConfigChangeResult(resultCode, adminActionRequired, messages);
  }
}

