/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2006-2010 Sun Microsystems, Inc.
 *      Portions Copyright 2011-2014 ForgeRock AS
 */
package org.opends.server.backends;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.opends.messages.Message;
import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.server.RootDSEBackendCfg;
import org.opends.server.api.Backend;
import org.opends.server.api.ClientConnection;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.core.AddOperation;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.ModifyOperation;
import org.opends.server.core.ModifyDNOperation;
import org.opends.server.core.SearchOperation;
import org.opends.server.core.WorkflowTopologyNode;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.*;
import org.opends.server.util.LDIFWriter;
import org.opends.server.util.Validator;

import static org.opends.messages.BackendMessages.*;
import static org.opends.messages.ConfigMessages.*;
import static org.opends.server.config.ConfigConstants.*;
import static org.opends.server.loggers.ErrorLogger.*;
import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;

/**
 * This class defines a backend to hold the Directory Server root DSE.  It is a
 * kind of meta-backend in that it will dynamically generate the root DSE entry
 * (although there will be some caching) for base-level searches, and will
 * simply redirect to other backends for operations in other scopes.
 * <BR><BR>
 * This should not be treated like a regular backend when it comes to
 * initializing the server configuration.  It should only be initialized after
 * all other backends are configured.  As such, it should have a special entry
 * in the configuration rather than being placed under the cn=Backends branch
 * with the other backends.
 */
public class RootDSEBackend
       extends Backend<RootDSEBackendCfg>
       implements ConfigurationChangeListener<RootDSEBackendCfg>
{
  /** The tracer object for the debug logger. */
  private static final DebugTracer TRACER = getTracer();

  /**
   * The set of standard "static" attributes that we will always include in the
   * root DSE entry and won't change while the server is running.
   */
  private ArrayList<Attribute> staticDSEAttributes;

  /**
   * The set of user-defined attributes that will be included in the root DSE
   * entry.
   */
  private ArrayList<Attribute> userDefinedAttributes;

  /**
   * Indicates whether the attributes of the root DSE should always be treated
   * as user attributes even if they are defined as operational in the schema.
   */
  private boolean showAllAttributes;

  /**
   * The set of subordinate base DNs and their associated backends that will be
   * used for non-base searches.
   */
  private ConcurrentHashMap<DN,Backend> subordinateBaseDNs;

  /** The set of objectclasses that will be used in the root DSE entry. */
  private HashMap<ObjectClass,String> dseObjectClasses;

  /** The current configuration state. */
  private RootDSEBackendCfg currentConfig;

  /** The DN of the configuration entry for this backend. */
  private DN configEntryDN;

  /** The DN for the root DSE. */
  private DN rootDSEDN;

  /** The set of base DNs for this backend. */
  private DN[] baseDNs;



  /**
   * Creates a new backend with the provided information.  All backend
   * implementations must implement a default constructor that use
   * <CODE>super()</CODE> to invoke this constructor.
   */
  public RootDSEBackend()
  {
    super();

    // Perform all initialization in initializeBackend.
  }

  /** {@inheritDoc} */
  @Override
  public void configureBackend(RootDSEBackendCfg config) throws ConfigException
  {
    Validator.ensureNotNull(config);
    currentConfig = config;
    configEntryDN = config.dn();
  }

  /** {@inheritDoc} */
  @Override
  public void initializeBackend()
         throws ConfigException, InitializationException
  {
    ConfigEntry configEntry =
         DirectoryServer.getConfigEntry(configEntryDN);

    // Make sure that a configuration entry was provided.  If not, then we will
    // not be able to complete initialization.
    if (configEntry == null)
    {
      Message message = ERR_ROOTDSE_CONFIG_ENTRY_NULL.get();
      throw new ConfigException(message);
    }

    // Get the set of user-defined attributes for the configuration entry.  Any
    // attributes that we don't recognize will be included directly in the root
    // DSE.
    userDefinedAttributes = new ArrayList<Attribute>();
    for (List<Attribute> attrs :
         configEntry.getEntry().getUserAttributes().values())
    {
      for (Attribute a : attrs)
      {
        if (! isDSEConfigAttribute(a))
        {
          userDefinedAttributes.add(a);
        }
      }
    }
    for (List<Attribute> attrs :
         configEntry.getEntry().getOperationalAttributes().values())
    {
      for (Attribute a : attrs)
      {
        if (! isDSEConfigAttribute(a))
        {
          userDefinedAttributes.add(a);
        }
      }
    }


    // Create the set of base DNs that we will handle.  In this case, it's just
    // the root DSE.
    rootDSEDN    = DN.nullDN();
    this.baseDNs = new DN[] { rootDSEDN };


    // Create the set of subordinate base DNs.  If this is specified in the
    // configuration, then use that set.  Otherwise, use the set of non-private
    // backends defined in the server.
    try
    {
      Set<DN> subDNs = currentConfig.getSubordinateBaseDN();
      if (subDNs.isEmpty())
      {
        // This is fine -- we'll just use the set of user-defined suffixes.
        subordinateBaseDNs = null;
      }
      else
      {
        subordinateBaseDNs = new ConcurrentHashMap<DN,Backend>();
        for (DN baseDN : subDNs)
        {
          Backend backend = DirectoryServer.getBackend(baseDN);
          if (backend == null)
          {
            Message message = WARN_ROOTDSE_NO_BACKEND_FOR_SUBORDINATE_BASE.get(
                String.valueOf(baseDN));
            logError(message);
          }
          else
          {
            subordinateBaseDNs.put(baseDN, backend);
          }
        }
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      Message message = WARN_ROOTDSE_SUBORDINATE_BASE_EXCEPTION.get(
          stackTraceToSingleLineString(e));
      throw new InitializationException(message, e);
    }


    // Determine whether all root DSE attributes should be treated as user
    // attributes.
    showAllAttributes = currentConfig.isShowAllAttributes();


    // Construct the set of "static" attributes that will always be present in
    // the root DSE.
    staticDSEAttributes = new ArrayList<Attribute>();

    staticDSEAttributes.add(Attributes.create(ATTR_VENDOR_NAME,
                                            SERVER_VENDOR_NAME));

    staticDSEAttributes.add(Attributes.create(ATTR_VENDOR_VERSION,
                                 DirectoryServer.getVersionString()));



    // Construct the set of objectclasses to include in the root DSE entry.
    dseObjectClasses = new HashMap<ObjectClass,String>(2);
    ObjectClass topOC = DirectoryServer.getObjectClass(OC_TOP);
    if (topOC == null)
    {
      topOC = DirectoryServer.getDefaultObjectClass(OC_TOP);
    }
    dseObjectClasses.put(topOC, OC_TOP);

    ObjectClass rootDSEOC =
         DirectoryServer.getObjectClass(OC_ROOT_DSE);
    if (rootDSEOC == null)
    {
      rootDSEOC = DirectoryServer.getDefaultObjectClass(OC_ROOT_DSE);
    }
    dseObjectClasses.put(rootDSEOC, OC_ROOT_DSE);


    // Set the backend ID for this backend. The identifier needs to be
    // specific enough to avoid conflict with user backend identifiers.
    setBackendID("__root.dse__");


    // Register as a change listener.
    currentConfig.addChangeListener(this);
  }

  /** {@inheritDoc} */
  @Override
  public void finalizeBackend()
  {
    super.finalizeBackend();
    currentConfig.removeChangeListener(this);
  }



  /**
   * Indicates whether the provided attribute is one that is used in the
   * configuration of this backend.
   *
   * @param  attribute  The attribute for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the provided attribute is one that is used in
   *          the configuration of this backend, <CODE>false</CODE> if not.
   */
  private boolean isDSEConfigAttribute(Attribute attribute)
  {
    AttributeType attrType = attribute.getAttributeType();
    return attrType.hasName(ATTR_ROOT_DSE_SUBORDINATE_BASE_DN.toLowerCase())
        || attrType.hasName(ATTR_ROOTDSE_SHOW_ALL_ATTRIBUTES.toLowerCase())
        || attrType.hasName(ATTR_COMMON_NAME);
  }

  /** {@inheritDoc} */
  @Override
  public DN[] getBaseDNs()
  {
    return baseDNs;
  }

  /** {@inheritDoc} */
  @Override
  public synchronized long getEntryCount()
  {
    // There is always just a single entry in this backend.
    return 1;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isLocal()
  {
    // For the purposes of this method, this is a local backend.
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isIndexed(AttributeType attributeType, IndexType indexType)
  {
    // All searches in this backend will always be considered indexed.
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public ConditionResult hasSubordinates(DN entryDN)
         throws DirectoryException
  {
    long ret = numSubordinates(entryDN, false);
    if(ret < 0)
    {
      return ConditionResult.UNDEFINED;
    }
    else if(ret == 0)
    {
      return ConditionResult.FALSE;
    }
    else
    {
      return ConditionResult.TRUE;
    }
  }

  /** {@inheritDoc} */
  @Override
  public long numSubordinates(DN entryDN, boolean subtree)
         throws DirectoryException
  {
    if (entryDN == null || ! entryDN.isNullDN())
    {
      return -1;
    }

    long count = 0;

    Map<DN,Backend> baseMap;
    if (subordinateBaseDNs == null)
    {
      baseMap = DirectoryServer.getPublicNamingContexts();
    }
    else
    {
      baseMap = subordinateBaseDNs;
    }

    for (Map.Entry<DN,Backend> entry : baseMap.entrySet())
    {
      DN subBase = entry.getKey();
      Backend b = entry.getValue();
      Entry subBaseEntry = b.getEntry(subBase);
      if (subBaseEntry != null)
      {
        if(subtree)
        {
          long subCount = b.numSubordinates(subBase, true);
          if(subCount < 0)
          {
            return -1;
          }

          count += subCount;
        }
        count ++;
      }
    }

    return count;
  }

  /** {@inheritDoc} */
  @Override
  public Entry getEntry(DN entryDN)
         throws DirectoryException
  {
    // If the requested entry was the root DSE, then create and return it.
    if (entryDN == null || entryDN.isNullDN())
    {
      return getRootDSE();
    }


    // This method should never be used to get anything other than the root DSE.
    // If we got here, then that appears to be the case, so log a message.
    Message message =
        WARN_ROOTDSE_GET_ENTRY_NONROOT.get(String.valueOf(entryDN));
    logError(message);


    // Go ahead and check the subordinate backends to see if we can find the
    // entry there.  Note that in order to avoid potential loop conditions, this
    // will only work if the set of subordinate bases has been explicitly
    // specified.
    if (subordinateBaseDNs != null)
    {
      for (Backend b : subordinateBaseDNs.values())
      {
        if (b.handlesEntry(entryDN))
        {
          return b.getEntry(entryDN);
        }
      }
    }


    // If we've gotten here, then we couldn't find the entry so return null.
    return null;
  }



  /**
   * Retrieves the root DSE entry for the Directory Server.
   *
   * @return The root DSE entry for the Directory Server.
   */
  public Entry getRootDSE()
  {
    return getRootDSE(null);
  }



  /**
   * Retrieves the root DSE entry for the Directory Server.
   *
   * @param connection
   *          The client connection, or {@code null} if there is no associated
   *          client connection.
   * @return The root DSE entry for the Directory Server.
   */
  public Entry getRootDSE(ClientConnection connection)
  {
    HashMap<AttributeType,List<Attribute>> dseUserAttrs =
         new HashMap<AttributeType,List<Attribute>>();

    HashMap<AttributeType,List<Attribute>> dseOperationalAttrs =
         new HashMap<AttributeType,List<Attribute>>();


    // Add the "namingContexts" attribute.
    final Collection<DN> namingContexts;
    if (connection == null)
    {
      namingContexts = DirectoryServer.getPublicNamingContexts().keySet();
    }
    else
    {
      namingContexts = new LinkedList<DN>();
      for (WorkflowTopologyNode node : connection.getNetworkGroup()
          .getNamingContexts().getPublicNamingContexts())
      {
        namingContexts.add(node.getBaseDN());
      }
    }

    Attribute publicNamingContextAttr = createDNAttribute(ATTR_NAMING_CONTEXTS,
        ATTR_NAMING_CONTEXTS_LC, namingContexts);
    addAttribute(publicNamingContextAttr, dseUserAttrs, dseOperationalAttrs);


    // Add the "ds-private-naming-contexts" attribute.
    Attribute privateNamingContextAttr = createDNAttribute(
        ATTR_PRIVATE_NAMING_CONTEXTS, ATTR_PRIVATE_NAMING_CONTEXTS,
        DirectoryServer.getPrivateNamingContexts().keySet());
    addAttribute(privateNamingContextAttr, dseUserAttrs, dseOperationalAttrs);

    // Add the "supportedControl" attribute.
    Attribute supportedControlAttr = createAttribute(ATTR_SUPPORTED_CONTROL,
        ATTR_SUPPORTED_CONTROL_LC, DirectoryServer.getSupportedControls());
    addAttribute(supportedControlAttr, dseUserAttrs, dseOperationalAttrs);

    // Add the "supportedExtension" attribute.
    Attribute supportedExtensionAttr = createAttribute(
        ATTR_SUPPORTED_EXTENSION, ATTR_SUPPORTED_EXTENSION_LC, DirectoryServer
            .getSupportedExtensions().keySet());
    addAttribute(supportedExtensionAttr, dseUserAttrs, dseOperationalAttrs);

    // Add the "supportedFeature" attribute.
    Attribute supportedFeatureAttr = createAttribute(ATTR_SUPPORTED_FEATURE,
        ATTR_SUPPORTED_FEATURE_LC, DirectoryServer.getSupportedFeatures());
    addAttribute(supportedFeatureAttr, dseUserAttrs, dseOperationalAttrs);


    // Add the "supportedSASLMechanisms" attribute.
    Attribute supportedSASLMechAttr = createAttribute(
        ATTR_SUPPORTED_SASL_MECHANISMS, ATTR_SUPPORTED_SASL_MECHANISMS_LC,
        DirectoryServer.getSupportedSASLMechanisms().keySet());
    addAttribute(supportedSASLMechAttr, dseUserAttrs, dseOperationalAttrs);


    // Add the "supportedLDAPVersions" attribute.
    TreeSet<String> versionStrings = new TreeSet<String>();
    for (Integer ldapVersion : DirectoryServer.getSupportedLDAPVersions())
    {
      versionStrings.add(ldapVersion.toString());
    }
    Attribute supportedLDAPVersionAttr =
         createAttribute(ATTR_SUPPORTED_LDAP_VERSION,
                         ATTR_SUPPORTED_LDAP_VERSION_LC,
                         versionStrings);
    addAttribute(supportedLDAPVersionAttr, dseUserAttrs, dseOperationalAttrs);


    // Add the "supportedAuthPasswordSchemes" attribute.
    Set<String> authPWSchemes =
         DirectoryServer.getAuthPasswordStorageSchemes().keySet();
    if (!authPWSchemes.isEmpty())
    {
      Attribute supportedAuthPWSchemesAttr =
           createAttribute(ATTR_SUPPORTED_AUTH_PW_SCHEMES,
                           ATTR_SUPPORTED_AUTH_PW_SCHEMES_LC, authPWSchemes);
      ArrayList<Attribute> supportedAuthPWSchemesAttrs =
           new ArrayList<Attribute>(1);
      supportedAuthPWSchemesAttrs.add(supportedAuthPWSchemesAttr);
      if (showAllAttributes
          || !supportedSASLMechAttr.getAttributeType().isOperational())
      {
        dseUserAttrs.put(supportedAuthPWSchemesAttr.getAttributeType(),
                         supportedAuthPWSchemesAttrs);
      }
      else
      {
        dseOperationalAttrs.put(supportedAuthPWSchemesAttr.getAttributeType(),
                                supportedAuthPWSchemesAttrs);
      }
    }


    // Obtain TLS protocol and cipher support.
    Collection<String> supportedTlsProtocols;
    Collection<String> supportedTlsCiphers;
    if (connection != null)
    {
      // Only return the list of enabled protocols / ciphers for the connection
      // handler to which the client is connected.
      supportedTlsProtocols = connection.getConnectionHandler()
          .getEnabledSSLProtocols();
      supportedTlsCiphers = connection.getConnectionHandler()
          .getEnabledSSLCipherSuites();
    }
    else
    {
      try
      {
        final SSLContext context = SSLContext.getDefault();
        final SSLParameters parameters = context.getSupportedSSLParameters();
        supportedTlsProtocols = Arrays.asList(parameters.getProtocols());
        supportedTlsCiphers = Arrays.asList(parameters.getCipherSuites());
      }
      catch (Exception e)
      {
        // A default SSL context should always be available.
        supportedTlsProtocols = Collections.emptyList();
        supportedTlsCiphers = Collections.emptyList();
      }
    }

    // Add the "supportedTLSProtocols" attribute.
    Attribute supportedTLSProtocolsAttr = createAttribute(
        ATTR_SUPPORTED_TLS_PROTOCOLS, ATTR_SUPPORTED_TLS_PROTOCOLS_LC,
        supportedTlsProtocols);
    addAttribute(supportedTLSProtocolsAttr, dseUserAttrs, dseOperationalAttrs);

    // Add the "supportedTLSCiphers" attribute.
    Attribute supportedTLSCiphersAttr = createAttribute(
        ATTR_SUPPORTED_TLS_CIPHERS, ATTR_SUPPORTED_TLS_CIPHERS_LC,
        supportedTlsCiphers);
    addAttribute(supportedTLSCiphersAttr, dseUserAttrs, dseOperationalAttrs);

    // Add all the standard "static" attributes.
    for (Attribute a : staticDSEAttributes)
    {
      AttributeType type = a.getAttributeType();

      if (type.isOperational() && !showAllAttributes)
      {
        List<Attribute> attrs = dseOperationalAttrs.get(type);
        if (attrs == null)
        {
          attrs = new ArrayList<Attribute>();
          dseOperationalAttrs.put(type, attrs);
        }
        attrs.add(a);
      }
      else
      {
        List<Attribute> attrs = dseUserAttrs.get(type);
        if (attrs == null)
        {
          attrs = new ArrayList<Attribute>();
          dseUserAttrs.put(type, attrs);
        }
        attrs.add(a);
      }
    }


    // Add all the user-defined attributes.
    for (Attribute a : userDefinedAttributes)
    {
      AttributeType type = a.getAttributeType();

      if (type.isOperational() && !showAllAttributes)
      {
        List<Attribute> attrs = dseOperationalAttrs.get(type);
        if (attrs == null)
        {
          attrs = new ArrayList<Attribute>();
          dseOperationalAttrs.put(type, attrs);
        }
        attrs.add(a);
      }
      else
      {
        List<Attribute> attrs = dseUserAttrs.get(type);
        if (attrs == null)
        {
          attrs = new ArrayList<Attribute>();
          dseUserAttrs.put(type, attrs);
        }
        attrs.add(a);
      }
    }


    // Construct and return the entry.
    Entry e = new Entry(rootDSEDN, dseObjectClasses, dseUserAttrs,
                        dseOperationalAttrs);
    e.processVirtualAttributes();
    return e;
  }



  private void addAttribute(Attribute publicNamingContextAttr,
      HashMap<AttributeType, List<Attribute>> userAttrs,
      HashMap<AttributeType, List<Attribute>> operationalAttrs)
  {
    if (!publicNamingContextAttr.isEmpty())
    {
      List<Attribute> privateNamingContextAttrs = new ArrayList<Attribute>(1);
      privateNamingContextAttrs.add(publicNamingContextAttr);
      final AttributeType attrType = publicNamingContextAttr.getAttributeType();
      if (showAllAttributes || !attrType.isOperational())
      {
        userAttrs.put(attrType, privateNamingContextAttrs);
      }
      else
      {
        operationalAttrs.put(attrType, privateNamingContextAttrs);
      }
    }
  }

  /**
   * Determines the workflow nodes which handle subordinate naming contexts.
   * A workflow node is handling a subordinate naming context if the workflow
   * base DN is in the list of the RootDSE subordinate naming contexts.
   *
   * @param   nodes
   *          The list from which we search the workflow nodes which
   *          are handling subordinate naming contexts
   *
   * @return  The list of workflow nodes that are handling subordinate
   *          naming contexts
   */
  public Iterable<WorkflowTopologyNode> getSubordinateNamingContexts(
      Iterable<WorkflowTopologyNode> nodes)
  {
    // If the list of subordinate naming contexts is null
    // then return the whole list of workflow nodes.
    if (subordinateBaseDNs == null)
    {
      return nodes;
    }

    // The returned list of subordinate naming contexts
    List<WorkflowTopologyNode> subNC = new ArrayList<WorkflowTopologyNode>();

    // Determine which workflow node is handling a subordinate naming context.
    for (WorkflowTopologyNode node : nodes)
    {
      DN dn = node.getBaseDN();
      if (subordinateBaseDNs.containsKey(dn))
      {
        subNC.add(node);
      }
    }

    return subNC;
  }


  /**
   * Creates an attribute for the root DSE meant to hold a set of DNs.
   *
   * @param  name       The name for the attribute.
   * @param  lowerName  The name for the attribute formatted in all lowercase
   *                    characters.
   * @param  values     The set of DN values to use for the attribute.
   *
   * @return  The constructed attribute.
   */
  private Attribute createDNAttribute(String name, String lowerName,
                                      Collection<DN> values)
  {
    AttributeType type = DirectoryServer.getAttributeType(lowerName);
    if (type == null)
    {
      type = DirectoryServer.getDefaultAttributeType(name);
    }

    AttributeBuilder builder = new AttributeBuilder(type, name);
    for (DN dn : values) {
      builder.add(
          AttributeValues.create(type, dn.toString()));
    }

    return builder.toAttribute();
  }



  /**
   * Creates an attribute for the root DSE with the following
   * criteria.
   *
   * @param name
   *          The name for the attribute.
   * @param lowerName
   *          The name for the attribute formatted in all lowercase
   *          characters.
   * @param values
   *          The set of values to use for the attribute.
   * @return The constructed attribute.
   */
  private Attribute createAttribute(String name, String lowerName,
                                    Collection<String> values)
  {
    AttributeType type = DirectoryServer.getAttributeType(lowerName);
    if (type == null)
    {
      type = DirectoryServer.getDefaultAttributeType(name);
    }

    AttributeBuilder builder = new AttributeBuilder(type, name);
    builder.setInitialCapacity(values.size());
    for (String s : values) {
      builder.add(AttributeValues.create(type, s));
    }

    return builder.toAttribute();
  }

  /** {@inheritDoc} */
  @Override
  public boolean entryExists(DN entryDN)
         throws DirectoryException
  {
    // If the specified DN was the null DN, then it exists.
    if (entryDN.isNullDN())
    {
      return true;
    }


    // If it was not the null DN, then iterate through the associated
    // subordinate backends to make the determination.
    Map<DN,Backend> baseMap;
    if (subordinateBaseDNs == null)
    {
      baseMap = DirectoryServer.getPublicNamingContexts();
    }
    else
    {
      baseMap = subordinateBaseDNs;
    }

    for (Map.Entry<DN,Backend> entry : baseMap.entrySet())
    {
      DN baseDN = entry.getKey();
      if (entryDN.isDescendantOf(baseDN))
      {
        Backend b = entry.getValue();
        if (b.entryExists(entryDN))
        {
          return true;
        }
      }
    }

    return false;
  }

  /** {@inheritDoc} */
  @Override
  public void addEntry(Entry entry, AddOperation addOperation)
         throws DirectoryException
  {
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM,
        ERR_BACKEND_ADD_NOT_SUPPORTED.get(String.valueOf(entry.getDN()), getBackendID()));
  }

  /** {@inheritDoc} */
  @Override
  public void deleteEntry(DN entryDN, DeleteOperation deleteOperation)
         throws DirectoryException
  {
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM,
        ERR_BACKEND_DELETE_NOT_SUPPORTED.get(String.valueOf(entryDN), getBackendID()));
  }

  /** {@inheritDoc} */
  @Override
  public void replaceEntry(Entry oldEntry, Entry newEntry,
      ModifyOperation modifyOperation) throws DirectoryException
  {
    Message message = ERR_ROOTDSE_MODIFY_NOT_SUPPORTED.get(
        String.valueOf(newEntry.getDN()), String.valueOf(configEntryDN));
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message);
  }

  /** {@inheritDoc} */
  @Override
  public void renameEntry(DN currentDN, Entry entry,
                                   ModifyDNOperation modifyDNOperation)
         throws DirectoryException
  {
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM,
        ERR_BACKEND_MODIFY_DN_NOT_SUPPORTED.get(String.valueOf(currentDN), getBackendID()));
  }

  /** {@inheritDoc} */
  @Override
  public void search(SearchOperation searchOperation)
         throws DirectoryException, CanceledOperationException {
    DN baseDN = searchOperation.getBaseDN();
    if (! baseDN.isNullDN())
    {
      Message message = ERR_ROOTDSE_INVALID_SEARCH_BASE.
          get(searchOperation.getConnectionID(),
              searchOperation.getOperationID(), String.valueOf(baseDN));
      throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message);
    }


    SearchFilter filter = searchOperation.getFilter();
    switch (searchOperation.getScope())
    {
      case BASE_OBJECT:
        Entry dseEntry = getRootDSE(searchOperation.getClientConnection());
        if (filter.matchesEntry(dseEntry))
        {
          searchOperation.returnEntry(dseEntry, null);
        }
        break;


      case SINGLE_LEVEL:
        Map<DN,Backend> baseMap;
        if (subordinateBaseDNs == null)
        {
          baseMap = DirectoryServer.getPublicNamingContexts();
        }
        else
        {
          baseMap = subordinateBaseDNs;
        }

        for (Map.Entry<DN,Backend> entry : baseMap.entrySet())
        {
          DN subBase = entry.getKey();
          searchOperation.checkIfCanceled(false);

          Backend b = entry.getValue();
          Entry subBaseEntry = b.getEntry(subBase);
          if (subBaseEntry != null && filter.matchesEntry(subBaseEntry))
          {
            searchOperation.returnEntry(subBaseEntry, null);
          }
        }
        break;


      case WHOLE_SUBTREE:
      case SUBORDINATE_SUBTREE:
        if (subordinateBaseDNs == null)
        {
          baseMap = DirectoryServer.getPublicNamingContexts();
        }
        else
        {
          baseMap = subordinateBaseDNs;
        }

        try
        {
          for (Map.Entry<DN,Backend> entry : baseMap.entrySet())
          {
            searchOperation.checkIfCanceled(false);

            DN subBase = entry.getKey();
            searchOperation.setBaseDN(subBase);

            Backend b = entry.getValue();
            try
            {
              b.search(searchOperation);
            }
            catch (DirectoryException de)
            {
              // If it's a "no such object" exception, then the base entry for
              // the backend doesn't exist.  This isn't an error, so ignore it.
              // We'll propogate all other errors, though.
              if (de.getResultCode() != ResultCode.NO_SUCH_OBJECT)
              {
                throw de;
              }
            }
          }
        }
        catch (DirectoryException de)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, de);
          }

          throw de;
        }
        catch (Exception e)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }

          Message message = ERR_ROOTDSE_UNEXPECTED_SEARCH_FAILURE.
              get(searchOperation.getConnectionID(),
                  searchOperation.getOperationID(),
                  stackTraceToSingleLineString(e));
          throw new DirectoryException(
                         DirectoryServer.getServerErrorResultCode(), message,
                         e);
        }
        finally
        {
          searchOperation.setBaseDN(rootDSEDN);
        }
        break;

      default:
        Message message = ERR_ROOTDSE_INVALID_SEARCH_SCOPE.
            get(searchOperation.getConnectionID(),
                searchOperation.getOperationID(),
                String.valueOf(searchOperation.getScope()));
        throw new DirectoryException(ResultCode.PROTOCOL_ERROR, message);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getSupportedControls()
  {
    return Collections.emptySet();
  }

  /** {@inheritDoc} */
  @Override
  public Set<String> getSupportedFeatures()
  {
    return Collections.emptySet();
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsLDIFExport()
  {
    // We will only export the DSE entry itself.
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public void exportLDIF(LDIFExportConfig exportConfig)
         throws DirectoryException
  {
    // Create the LDIF writer.
    LDIFWriter ldifWriter;
    try
    {
      ldifWriter = new LDIFWriter(exportConfig);
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      Message message = ERR_ROOTDSE_UNABLE_TO_CREATE_LDIF_WRITER.get(
          stackTraceToSingleLineString(e));
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message);
    }


    // Write the root DSE entry itself to it.  Make sure to close the LDIF
    // writer when we're done.
    try
    {
      ldifWriter.writeEntry(getRootDSE());
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      Message message =
          ERR_ROOTDSE_UNABLE_TO_EXPORT_DSE.get(stackTraceToSingleLineString(e));
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message);
    }
    finally
    {
      close(ldifWriter);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsLDIFImport()
  {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public LDIFImportResult importLDIF(LDIFImportConfig importConfig)
         throws DirectoryException
  {
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM,
        ERR_BACKEND_IMPORT_AND_EXPORT_NOT_SUPPORTED.get(getBackendID()));
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsBackup()
  {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsBackup(BackupConfig backupConfig,
                                StringBuilder unsupportedReason)
  {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public void createBackup(BackupConfig backupConfig)
         throws DirectoryException
  {
    Message message = ERR_ROOTDSE_BACKUP_AND_RESTORE_NOT_SUPPORTED.get();
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message);
  }

  /** {@inheritDoc} */
  @Override
  public void removeBackup(BackupDirectory backupDirectory,
                           String backupID)
         throws DirectoryException
  {
    Message message = ERR_ROOTDSE_BACKUP_AND_RESTORE_NOT_SUPPORTED.get();
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message);
  }

  /** {@inheritDoc} */
  @Override
  public boolean supportsRestore()
  {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public void restoreBackup(RestoreConfig restoreConfig)
         throws DirectoryException
  {
    Message message = ERR_ROOTDSE_BACKUP_AND_RESTORE_NOT_SUPPORTED.get();
    throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConfigurationAcceptable(RootDSEBackendCfg config,
                                           List<Message> unacceptableReasons)
  {
    return isConfigurationChangeAcceptable(config, unacceptableReasons);
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConfigurationChangeAcceptable(
       RootDSEBackendCfg cfg,
       List<Message> unacceptableReasons)
  {
    boolean configIsAcceptable = true;


    try
    {
      Set<DN> subDNs = cfg.getSubordinateBaseDN();
      if (subDNs.isEmpty())
      {
        // This is fine -- we'll just use the set of user-defined suffixes.
      }
      else
      {
        for (DN baseDN : subDNs)
        {
          Backend backend = DirectoryServer.getBackend(baseDN);
          if (backend == null)
          {
            Message message = WARN_ROOTDSE_NO_BACKEND_FOR_SUBORDINATE_BASE.get(
                    String.valueOf(baseDN));
            unacceptableReasons.add(message);
            configIsAcceptable = false;
          }
        }
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      Message message = WARN_ROOTDSE_SUBORDINATE_BASE_EXCEPTION.get(
              stackTraceToSingleLineString(e));
      unacceptableReasons.add(message);
      configIsAcceptable = false;
    }


    return configIsAcceptable;
  }

  /** {@inheritDoc} */
  @Override
  public ConfigChangeResult applyConfigurationChange(RootDSEBackendCfg cfg)
  {
    ResultCode         resultCode          = ResultCode.SUCCESS;
    boolean            adminActionRequired = false;
    ArrayList<Message> messages            = new ArrayList<Message>();


    // Check to see if we should apply a new set of base DNs.
    ConcurrentHashMap<DN,Backend> subBases;
    try
    {
      Set<DN> subDNs = cfg.getSubordinateBaseDN();
      if (subDNs.isEmpty())
      {
        // This is fine -- we'll just use the set of user-defined suffixes.
        subBases = null;
      }
      else
      {
        subBases = new ConcurrentHashMap<DN,Backend>();
        for (DN baseDN : subDNs)
        {
          Backend backend = DirectoryServer.getBackend(baseDN);
          if (backend == null)
          {
            // This is not fine.  We can't use a suffix that doesn't exist.
            Message message = WARN_ROOTDSE_NO_BACKEND_FOR_SUBORDINATE_BASE.get(
                    String.valueOf(baseDN));
            messages.add(message);

            if (resultCode == ResultCode.SUCCESS)
            {
              resultCode = DirectoryServer.getServerErrorResultCode();
            }
          }
          else
          {
            subBases.put(baseDN, backend);
          }
        }
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      Message message = WARN_ROOTDSE_SUBORDINATE_BASE_EXCEPTION.get(
              stackTraceToSingleLineString(e));
      messages.add(message);

      if (resultCode == ResultCode.SUCCESS)
      {
        resultCode = DirectoryServer.getServerErrorResultCode();
      }

      subBases = null;
    }


    boolean newShowAll = cfg.isShowAllAttributes();


    // Check to see if there is a new set of user-defined attributes.
    ArrayList<Attribute> userAttrs = new ArrayList<Attribute>();
    try
    {
      ConfigEntry configEntry = DirectoryServer.getConfigEntry(configEntryDN);

      for (List<Attribute> attrs :
           configEntry.getEntry().getUserAttributes().values())
      {
        for (Attribute a : attrs)
        {
          if (! isDSEConfigAttribute(a))
          {
            userAttrs.add(a);
          }
        }
      }
      for (List<Attribute> attrs :
           configEntry.getEntry().getOperationalAttributes().values())
      {
        for (Attribute a : attrs)
        {
          if (! isDSEConfigAttribute(a))
          {
            userAttrs.add(a);
          }
        }
      }
    }
    catch (ConfigException e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      messages.add(ERR_CONFIG_BACKEND_ERROR_INTERACTING_WITH_BACKEND_ENTRY.get(
              String.valueOf(configEntryDN),
              stackTraceToSingleLineString(e)));
      resultCode = DirectoryServer.getServerErrorResultCode();
    }


    if (resultCode == ResultCode.SUCCESS)
    {
      subordinateBaseDNs = subBases;

      if (subordinateBaseDNs == null)
      {
        messages.add(INFO_ROOTDSE_USING_SUFFIXES_AS_BASE_DNS.get());
      }
      else
      {
        StringBuilder basesStr = new StringBuilder();
        for (DN dn : subordinateBaseDNs.keySet())
        {
          if (basesStr.length() > 0)
          {
            basesStr.append(", ");
          }
          else
          {
            basesStr.append("{ ");
          }

          basesStr.append(dn);
        }

        basesStr.append(" }");

        Message message = INFO_ROOTDSE_USING_NEW_SUBORDINATE_BASE_DNS.get(
                basesStr.toString());
        messages.add(message);
      }


      if (showAllAttributes != newShowAll)
      {
        showAllAttributes = newShowAll;
        Message message = INFO_ROOTDSE_UPDATED_SHOW_ALL_ATTRS.get(
                ATTR_ROOTDSE_SHOW_ALL_ATTRIBUTES,
                String.valueOf(showAllAttributes));
        messages.add(message);
      }


      userDefinedAttributes = userAttrs;
      messages.add(INFO_ROOTDSE_USING_NEW_USER_ATTRS.get());
    }


    return new ConfigChangeResult(resultCode, adminActionRequired, messages);
  }

  /** {@inheritDoc} */
  @Override
  public void preloadEntryCache() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Operation not supported.");
  }
}
