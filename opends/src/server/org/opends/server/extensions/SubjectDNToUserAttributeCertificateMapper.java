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
 *      Portions Copyright 2007 Sun Microsystems, Inc.
 */
package org.opends.server.extensions;



import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opends.server.api.CertificateMapper;
import org.opends.server.api.ConfigurableComponent;
import org.opends.server.config.ConfigAttribute;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.config.DNConfigAttribute;
import org.opends.server.config.StringConfigAttribute;
import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.InitializationException;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchFilter;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchScope;

import static org.opends.server.config.ConfigConstants.*;
import static org.opends.server.loggers.debug.DebugLogger.debugCought;
import static org.opends.server.loggers.debug.DebugLogger.debugEnabled;
import org.opends.server.types.DebugLogLevel;
import static org.opends.server.messages.ExtensionsMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class implements a very simple Directory Server certificate mapper that
 * will map a certificate to a user only if that user's entry contains an
 * attribute with the subject of the client certificate.  There must be exactly
 * one matching user entry for the mapping to be successful.
 */
public class SubjectDNToUserAttributeCertificateMapper
       extends CertificateMapper
       implements ConfigurableComponent
{



  // The attribute type that will be used to map the certificate's subject.
  private AttributeType subjectAttributeType;

  // The DN of the configuration entry for this certificate mapper.
  private DN configEntryDN;

  // The set of base DNs below which the search will be performed.
  private DN[] baseDNs;



  /**
   * Creates a new instance of this certificate mapper.  Note that all actual
   * initialization should be done in the
   * <CODE>initializeCertificateMapper</CODE> method.
   */
  public SubjectDNToUserAttributeCertificateMapper()
  {
    super();

  }



  /**
   * {@inheritDoc}
   */
  public void initializeCertificateMapper(ConfigEntry configEntry)
         throws ConfigException, InitializationException
  {
    this.configEntryDN = configEntry.getDN();

    // Get the attribute type that will be used to hold the certificate subject.
    int msgID = MSGID_SDTUACM_DESCRIPTION_SUBJECT_ATTR;
    StringConfigAttribute attrStub =
         new StringConfigAttribute(ATTR_CERTIFICATE_SUBJECT_ATTR,
                                   getMessage(msgID), true, false, false);
    try
    {
      StringConfigAttribute attrAttr =
           (StringConfigAttribute) configEntry.getConfigAttribute(attrStub);
      if (attrAttr == null)
      {
        msgID = MSGID_SDTUACM_NO_SUBJECT_ATTR;
        String message = getMessage(msgID, String.valueOf(configEntryDN),
                                    ATTR_CERTIFICATE_SUBJECT_ATTR);
        throw new ConfigException(msgID, message);
      }
      else
      {
        String attrName  = attrAttr.pendingValue();
        String lowerName = toLowerCase(attrName);
        subjectAttributeType =
             DirectoryServer.getAttributeType(lowerName, false);
        if (subjectAttributeType == null)
        {
          msgID = MSGID_SDTUACM_NO_SUCH_ATTR;
          String message = getMessage(msgID, String.valueOf(configEntryDN),
                                      attrName);
          throw new ConfigException(msgID, message);
        }
      }
    }
    catch (ConfigException ce)
    {
      throw ce;
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      msgID = MSGID_SDTUACM_CANNOT_GET_SUBJECT_ATTR;
      String message = getMessage(msgID, String.valueOf(configEntryDN),
                                  stackTraceToSingleLineString(e));
      throw new InitializationException(msgID, message, e);
    }


    // Get the set of base DNs below which to perform the searches.
    baseDNs = null;
    msgID = MSGID_SDTUACM_DESCRIPTION_BASE_DN;
    DNConfigAttribute baseStub =
         new DNConfigAttribute(ATTR_CERTIFICATE_SUBJECT_BASEDN,
                               getMessage(msgID), false, true, false);
    try
    {
      DNConfigAttribute baseAttr =
           (DNConfigAttribute) configEntry.getConfigAttribute(baseStub);
      if (baseAttr != null)
      {
        List<DN> dnList = baseAttr.activeValues();
        baseDNs = new DN[dnList.size()];
        dnList.toArray(baseDNs);
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      msgID = MSGID_SDTUACM_CANNOT_GET_BASE_DN;
      String message = getMessage(msgID, String.valueOf(configEntryDN),
                                  stackTraceToSingleLineString(e));
      throw new InitializationException(msgID, message, e);
    }

    DirectoryServer.registerConfigurableComponent(this);
  }



  /**
   * {@inheritDoc}
   */
  public void finalizeCertificateMapper()
  {
    DirectoryServer.deregisterConfigurableComponent(this);
  }



  /**
   * {@inheritDoc}
   */
  public Entry mapCertificateToUser(Certificate[] certificateChain)
         throws DirectoryException
  {
    // Make sure that a peer certificate was provided.
    if ((certificateChain == null) || (certificateChain.length == 0))
    {
      int    msgID   = MSGID_SDTUACM_NO_PEER_CERTIFICATE;
      String message = getMessage(msgID);
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID);
    }


    // Get the first certificate in the chain.  It must be an X.509 certificate.
    X509Certificate peerCertificate;
    try
    {
      peerCertificate = (X509Certificate) certificateChain[0];
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      int    msgID   = MSGID_SDTUACM_PEER_CERT_NOT_X509;
      String message =
           getMessage(msgID, String.valueOf(certificateChain[0].getType()));
      throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                   msgID);
    }


    // Get the subject from the peer certificate and use it to create a search
    // filter.
    X500Principal peerPrincipal = peerCertificate.getSubjectX500Principal();
    String peerName = peerPrincipal.getName(X500Principal.RFC2253);
    AttributeValue value = new AttributeValue(subjectAttributeType, peerName);
    SearchFilter filter =
         SearchFilter.createEqualityFilter(subjectAttributeType, value);


    // If we have an explicit set of base DNs, then use it.  Otherwise, use the
    // set of public naming contexts in the server.
    DN[] bases = baseDNs;
    if (bases == null)
    {
      bases = new DN[0];
      bases = DirectoryServer.getPublicNamingContexts().keySet().toArray(bases);
    }


    // For each base DN, issue an internal search in an attempt to map the
    // certificate.
    Entry userEntry = null;
    InternalClientConnection conn =
         InternalClientConnection.getRootConnection();
    for (DN baseDN : bases)
    {
      InternalSearchOperation searchOperation =
           conn.processSearch(baseDN, SearchScope.WHOLE_SUBTREE, filter);
      for (SearchResultEntry entry : searchOperation.getSearchEntries())
      {
        if (userEntry == null)
        {
          userEntry = entry;
        }
        else
        {
          int    msgID   = MSGID_SDTUACM_MULTIPLE_MATCHING_ENTRIES;
          String message = getMessage(msgID, peerName,
                                      String.valueOf(userEntry.getDN()),
                                      String.valueOf(entry.getDN()));
          throw new DirectoryException(ResultCode.INVALID_CREDENTIALS, message,
                                       msgID);
        }
      }
    }


    // If we've gotten here, then we either found exactly one user entry or we
    // didn't find any.  Either way, return the entry or null to the caller.
    return userEntry;
  }



  /**
   * Retrieves the DN of the configuration entry with which this
   * component is associated.
   *
   * @return  The DN of the configuration entry with which this
   *          component is associated.
   */
  public DN getConfigurableComponentEntryDN()
  {
    return configEntryDN;
  }



  /**
   * Retrieves the set of configuration attributes that are associated
   * with this configurable component.
   *
   * @return  The set of configuration attributes that are associated
   *          with this configurable component.
   */
  public List<ConfigAttribute> getConfigurationAttributes()
  {
    LinkedList<ConfigAttribute> attrList = new LinkedList<ConfigAttribute>();

    int msgID = MSGID_SDTUACM_DESCRIPTION_SUBJECT_ATTR;
    attrList.add(new StringConfigAttribute(ATTR_CERTIFICATE_SUBJECT_ATTR,
                          getMessage(msgID), true, false, false,
                          subjectAttributeType.getNameOrOID()));

    LinkedList<DN> dnList = new LinkedList<DN>();
    if (baseDNs != null)
    {
      for (DN baseDN : baseDNs)
      {
        dnList.add(baseDN);
      }
    }

    msgID = MSGID_SDTUACM_DESCRIPTION_BASE_DN;
    attrList.add(new DNConfigAttribute(ATTR_CERTIFICATE_SUBJECT_BASEDN,
                                       getMessage(msgID), false, true, false,
                                       dnList));

    return attrList;
  }



  /**
   * Indicates whether the provided configuration entry has an
   * acceptable configuration for this component.  If it does not,
   * then detailed information about the problem(s) should be added to
   * the provided list.
   *
   * @param  configEntry          The configuration entry for which to
   *                              make the determination.
   * @param  unacceptableReasons  A list that can be used to hold
   *                              messages about why the provided
   *                              entry does not have an acceptable
   *                              configuration.
   *
   * @return  <CODE>true</CODE> if the provided entry has an
   *          acceptable configuration for this component, or
   *          <CODE>false</CODE> if not.
   */
  public boolean hasAcceptableConfiguration(ConfigEntry configEntry,
                                            List<String> unacceptableReasons)
  {
    DN configEntryDN = configEntry.getDN();
    boolean configAcceptable = true;


    // Get the attribute type that will be used to hold the certificate subject.
    int msgID = MSGID_SDTUACM_DESCRIPTION_SUBJECT_ATTR;
    StringConfigAttribute attrStub =
         new StringConfigAttribute(ATTR_CERTIFICATE_SUBJECT_ATTR,
                                   getMessage(msgID), true, false, false);
    try
    {
      StringConfigAttribute attrAttr =
           (StringConfigAttribute) configEntry.getConfigAttribute(attrStub);
      if (attrAttr == null)
      {
        msgID = MSGID_SDTUACM_NO_SUBJECT_ATTR;
        String message = getMessage(msgID, String.valueOf(configEntryDN),
                                    ATTR_CERTIFICATE_SUBJECT_ATTR);
        unacceptableReasons.add(message);
        configAcceptable = false;
      }
      else
      {
        String attrName  = attrAttr.pendingValue();
        String lowerName = toLowerCase(attrName);
        AttributeType attrType =
             DirectoryServer.getAttributeType(lowerName, false);
        if (attrType == null)
        {
          msgID = MSGID_SDTUACM_NO_SUCH_ATTR;
          String message = getMessage(msgID, String.valueOf(configEntryDN),
                                      attrName);
          unacceptableReasons.add(message);
          configAcceptable = false;
        }
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      msgID = MSGID_SDTUACM_CANNOT_GET_SUBJECT_ATTR;
      String message = getMessage(msgID, String.valueOf(configEntryDN),
                                  stackTraceToSingleLineString(e));
      unacceptableReasons.add(message);
      configAcceptable = false;
    }


    // Get the set of base DNs below which to perform the searches.
    msgID = MSGID_SDTUACM_DESCRIPTION_BASE_DN;
    DNConfigAttribute baseStub =
         new DNConfigAttribute(ATTR_CERTIFICATE_SUBJECT_BASEDN,
                               getMessage(msgID), false, true, false);
    try
    {
      DNConfigAttribute baseAttr =
           (DNConfigAttribute) configEntry.getConfigAttribute(baseStub);
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      msgID = MSGID_SDTUACM_CANNOT_GET_BASE_DN;
      String message = getMessage(msgID, String.valueOf(configEntryDN),
                                  stackTraceToSingleLineString(e));
      unacceptableReasons.add(message);
      configAcceptable = false;
    }


    return configAcceptable;
  }



  /**
   * Makes a best-effort attempt to apply the configuration contained
   * in the provided entry.  Information about the result of this
   * processing should be added to the provided message list.
   * Information should always be added to this list if a
   * configuration change could not be applied.  If detailed results
   * are requested, then information about the changes applied
   * successfully (and optionally about parameters that were not
   * changed) should also be included.
   *
   * @param  configEntry      The entry containing the new
   *                          configuration to apply for this
   *                          component.
   * @param  detailedResults  Indicates whether detailed information
   *                          about the processing should be added to
   *                          the list.
   *
   * @return  Information about the result of the configuration
   *          update.
   */
  public ConfigChangeResult applyNewConfiguration(ConfigEntry configEntry,
                                                  boolean detailedResults)
  {
    DN                configEntryDN       = configEntry.getDN();
    ResultCode        resultCode          = ResultCode.SUCCESS;
    ArrayList<String> messages            = new ArrayList<String>();
    boolean           adminActionRequired = false;


    // Get the attribute type that will be used to hold the certificate subject.
    AttributeType newAttributeType = null;
    int msgID = MSGID_SDTUACM_DESCRIPTION_SUBJECT_ATTR;
    StringConfigAttribute attrStub =
         new StringConfigAttribute(ATTR_CERTIFICATE_SUBJECT_ATTR,
                                   getMessage(msgID), true, false, false);
    try
    {
      StringConfigAttribute attrAttr =
           (StringConfigAttribute) configEntry.getConfigAttribute(attrStub);
      if (attrAttr == null)
      {
        if (resultCode == ResultCode.SUCCESS)
        {
          resultCode = ResultCode.OBJECTCLASS_VIOLATION;
        }

        msgID = MSGID_SDTUACM_NO_SUBJECT_ATTR;
        messages.add(getMessage(msgID, String.valueOf(configEntryDN),
                                ATTR_CERTIFICATE_SUBJECT_ATTR));
      }
      else
      {
        String attrName  = attrAttr.pendingValue();
        String lowerName = toLowerCase(attrName);
        newAttributeType = DirectoryServer.getAttributeType(lowerName, false);
        if (subjectAttributeType == null)
        {
          if (resultCode == ResultCode.SUCCESS)
          {
            resultCode = ResultCode.NO_SUCH_ATTRIBUTE;
          }

          msgID = MSGID_SDTUACM_NO_SUCH_ATTR;
          messages.add(getMessage(msgID, String.valueOf(configEntryDN),
                                  attrName));
        }
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      if (resultCode == ResultCode.SUCCESS)
      {
        resultCode = ResultCode.OBJECTCLASS_VIOLATION;
      }

      msgID = MSGID_SDTUACM_CANNOT_GET_SUBJECT_ATTR;
      messages.add(getMessage(msgID, String.valueOf(configEntryDN),
                              stackTraceToSingleLineString(e)));
    }


    // Get the set of base DNs below which to perform the searches.
    DN[] newBaseDNs = null;
    msgID = MSGID_SDTUACM_DESCRIPTION_BASE_DN;
    DNConfigAttribute baseStub =
         new DNConfigAttribute(ATTR_CERTIFICATE_SUBJECT_BASEDN,
                               getMessage(msgID), false, true, false);
    try
    {
      DNConfigAttribute baseAttr =
           (DNConfigAttribute) configEntry.getConfigAttribute(baseStub);
      if (baseAttr != null)
      {
        List<DN> dnList = baseAttr.activeValues();
        newBaseDNs = new DN[dnList.size()];
        dnList.toArray(newBaseDNs);
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      if (resultCode == ResultCode.SUCCESS)
      {
        resultCode = ResultCode.OBJECTCLASS_VIOLATION;
      }

      msgID = MSGID_SDTUACM_CANNOT_GET_BASE_DN;
      messages.add(getMessage(msgID, String.valueOf(configEntryDN),
                              stackTraceToSingleLineString(e)));
    }


    if (resultCode == ResultCode.SUCCESS)
    {
      subjectAttributeType = newAttributeType;
      baseDNs              = newBaseDNs;
    }

    return new ConfigChangeResult(resultCode, adminActionRequired, messages);
  }
}

