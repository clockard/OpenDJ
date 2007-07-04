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

package org.opends.admin.ads;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NoPermissionException;
import javax.naming.NotContextException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

/**
 * Class used to update and read the contents of the Administration Data.
 */
public class ADSContext
{
  private static final Logger LOG =
    Logger.getLogger(ADSContext.class.getName());
  /**
   * Enumeration containing the different server properties that are stored in
   * the ADS.
   */
  public enum ServerProperty
  {
    /**
     * The ID used to identify the server.
     */
    ID("id"),
    /**
     * The host name of the server.
     */
    HOST_NAME("hostname"),
    /**
     * The LDAP port of the server.
     */
    LDAP_PORT("ldapport"),
    /**
     * The JMX port of the server.
     */
    JMX_PORT("jmxport"),
    /**
     * The JMX secure port of the server.
     */
    JMXS_PORT("jmxsport"),
    /**
     * The LDAPS port of the server.
     */
    LDAPS_PORT("ldapsport"),
    /**
     * The certificate used by the server.
     */
    CERTIFICATE("certificate"),
    /**
     * The path where the server is installed.
     */
    INSTANCE_PATH("instancepath"),
    /**
     * The description of the server.
     */
    DESCRIPTION("description"),
    /**
     * The OS of the machine where the server is installed.
     */
    HOST_OS("os"),
    /**
     * Whether LDAP is enabled or not.
     */
    LDAP_ENABLED("ldapEnabled"),
    /**
     * Whether LDAPS is enabled or not.
     */
    LDAPS_ENABLED("ldapsEnabled"),
    /**
     * Whether StartTLS is enabled or not.
     */
    STARTTLS_ENABLED("startTLSEnabled"),
    /**
     * Whether JMX is enabled or not.
     */
    JMX_ENABLED("jmxEnabled"),
    /**
     * Whether JMX is enabled or not.
     */
    JMXS_ENABLED("jmxsEnabled"),
    /**
     * The location of the server.
     */
    LOCATION("location"),
    /**
     * The groups to which this server belongs.
     */
    GROUPS("memberofgroups");

    private String attrName;
    /**
     * Private constructor.
     * @param n the name of the attribute.
     */
    private ServerProperty(String n)
    {
      attrName = n;
    }

    /**
     * Returns the attribute name.
     * @return the attribute name.
     */
    public String getAttributeName()
    {
      return attrName;
    }
  };

  /**
   * The list of server properties that are multivalued.
   */
  private final static Set<ServerProperty> MULTIVALUED_SERVER_PROPERTIES =
    new HashSet<ServerProperty>();
  static
  {
    MULTIVALUED_SERVER_PROPERTIES.add(ServerProperty.GROUPS);
  }

  /**
   * Enumeration containing the different server group properties that are
   * stored in the ADS.
   */
  public enum ServerGroupProperty
  {
    /**
     * The UID of the server group.
     */
    UID("cn"),
    /**
     * The description of the server group.
     */
    DESCRIPTION("description"),
    /**
     * The members of the server group.
     */
    MEMBERS("uniqueMember");

    private String attrName;

    /**
     * Private constructor.
     * @param n the attribute name.
     */
    private ServerGroupProperty(String n)
    {
      attrName = n;
    }

    /**
     * Returns the attribute name.
     * @return the attribute name.
     */
    public String getAttributeName()
    {
      return attrName;
    }
  };

  /**
   * The list of server group properties that are multivalued.
   */
  private final static
  Set<ServerGroupProperty> MULTIVALUED_SERVER_GROUP_PROPERTIES =
    new HashSet<ServerGroupProperty>();
  static
  {
    MULTIVALUED_SERVER_GROUP_PROPERTIES.add(ServerGroupProperty.MEMBERS);
  }

  /**
   * The enumeration containing the different Administrator properties.
   */
  public enum AdministratorProperty
  {
    /**
     * The UID of the administrator.
     */
    UID,
    /**
     * The password of the administrator.
     */
    PASSWORD,
    /**
     * The description of the administrator.
     */
    DESCRIPTION,
    /**
     * The DN of the administrator.
     */
    ADMINISTRATOR_DN
  };

  // The context used to retrieve information
  InitialLdapContext dirContext;


  /**
   * Constructor of the ADSContext.
   * @param dirContext the DirContext that must be used to retrieve information.
   */
  public ADSContext(InitialLdapContext dirContext)
  {
    this.dirContext = dirContext;
  }


  /**
   * Returns the DirContext used to retrieve information by this ADSContext.
   * @return the DirContext used to retrieve information by this ADSContext.
   */
  public InitialLdapContext getDirContext()
  {
    return dirContext;
  }

  /**
   * Method called to register a server in the ADS.
   * @param serverProperties the properties of the server.
   * @throws ADSContextException if the server could not be registered.
   */
  public void registerServer(Map<ServerProperty, Object> serverProperties)
  throws ADSContextException
  {
    LdapName dn = makeDNFromServerProperties(serverProperties);
    BasicAttributes attrs = makeAttrsFromServerProperties(serverProperties);
    try
    {
      DirContext ctx = dirContext.createSubcontext(dn, attrs);
      ctx.close();
    }
    catch (NameAlreadyBoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ALREADY_REGISTERED);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }


  /**
   * Method called to udpate the properties of a server in the ADS.
   * @param serverProperties the new properties of the server.
   * @throws ADSContextException if the server could not be registered.
   */
  public void updateServer(Map<ServerProperty, Object> serverProperties)
  throws ADSContextException
  {
    LdapName dn = makeDNFromServerProperties(serverProperties);
    BasicAttributes attrs = makeAttrsFromServerProperties(serverProperties);
    try
    {
      dirContext.modifyAttributes(dn, InitialLdapContext.REPLACE_ATTRIBUTE,
          attrs);
    }
    catch (NameNotFoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.NOT_YET_REGISTERED);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Method called to unregister a server in the ADS.
   * @param serverProperties the properties of the server.
   * @throws ADSContextException if the server could not be unregistered.
   */
  public void unregisterServer(Map<ServerProperty, Object> serverProperties)
  throws ADSContextException
  {
    LdapName dn = makeDNFromServerProperties(serverProperties);
    try
    {
      dirContext.destroySubcontext(dn);
    }
    catch (NameNotFoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.NOT_YET_REGISTERED);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Returns whether a given server is already registered or not.
   * @param serverProperties the server properties.
   * @return <CODE>true</CODE> if the server was registered and
   * <CODE>false</CODE> otherwise.
   * @throws ADSContextException if something went wrong.
   */
  public boolean isServerAlreadyRegistered(
      Map<ServerProperty, Object> serverProperties)
  throws ADSContextException
  {
    LdapName dn = makeDNFromServerProperties(serverProperties);
    return isExistingEntry(dn);
  }

  /**
   * Returns whether a given administrator is already registered or not.
   * @param adminProperties the administrator properties.
   * @return <CODE>true</CODE> if the administrator was registered and
   * <CODE>false</CODE> otherwise.
   * @throws ADSContextException if something went wrong.
   */
  public boolean isAdministratorAlreadyRegistered(
      Map<AdministratorProperty, Object> adminProperties)
  throws ADSContextException
  {
    LdapName dn = makeDNFromAdministratorProperties(adminProperties);
    return isExistingEntry(dn);
  }

  /**
   * A convenience method that takes some server properties as parameter and
   * if there is no server registered associated with those properties,
   * registers it and if it is already registered, updates it.
   * @param serverProperties the server properties.
   * @throws ADSContextException if something goes wrong.
   */
  public void registerOrUpdateServer(
      Map<ServerProperty, Object> serverProperties) throws ADSContextException
  {
    try
    {
      registerServer(serverProperties);
    }
    catch(ADSContextException x)
    {
      if (x.getError() == ADSContextException.ErrorType.ALREADY_REGISTERED)
      {
        updateServer(serverProperties);
      }
      else
      {
        throw x;
      }
    }
  }

  /**
   * Returns the member list of a group of server.
   *
   * @param serverGroupId
   *          The group name.
   * @return the member list of a group of server.
   * @throws ADSContextException
   *           if something goes wrong.
   */
  public Set<String> getServerGroupMemberList(
      String serverGroupId) throws ADSContextException
  {
    LdapName dn = nameFromDN("cn=" + Rdn.escapeValue(serverGroupId) + ","
        + getServerGroupContainerDN());

    Set<String> result = new HashSet<String>() ;
    try
    {
      SearchControls sc = new SearchControls();
      sc.setSearchScope(SearchControls.OBJECT_SCOPE);
      NamingEnumeration<SearchResult> srs = getDirContext().search(dn,
          "(objectclass=*)", sc);

      if (!srs.hasMore())
      {
        return result;
      }
      Attributes attrs = srs.next().getAttributes();
      NamingEnumeration ne = attrs.getAll();
      while (ne.hasMore())
      {
        Attribute attr = (Attribute)ne.next();
        String attrID = attr.getID();

        if (!attrID.toLowerCase().equals(
            ServerGroupProperty.MEMBERS.getAttributeName().toLowerCase()))
        {
          continue;
        }

        // We have the members list
        NamingEnumeration ae = attr.getAll();
        while (ae.hasMore())
        {
          result.add((String)ae.next());
        }
        break;
      }
    }
    catch (NameNotFoundException x)
    {
      result = new HashSet<String>();
    }
    catch (NoPermissionException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ACCESS_PERMISSION);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
    return result;
  }

  /**
   * Returns a set containing the servers that are registered in the
   * ADS.
   *
   * @return a set containing the servers that are registered in the
   *         ADS.
   * @throws ADSContextException
   *           if something goes wrong.
   */
  public Set<Map<ServerProperty,Object>> readServerRegistry()
  throws ADSContextException
  {
    Set<Map<ServerProperty,Object>> result =
      new HashSet<Map<ServerProperty,Object>>();
    try
    {
      NamingEnumeration ne;
      SearchControls sc = new SearchControls();

      sc.setSearchScope(SearchControls.ONELEVEL_SCOPE);
      ne = dirContext.search(getServerContainerDN(), "(objectclass=*)", sc);
      while (ne.hasMore())
      {
        SearchResult sr = (SearchResult)ne.next();
        Map<ServerProperty,Object> properties =
          makePropertiesFromServerAttrs(sr.getAttributes());
        result.add(properties);
      }
    }
    catch (NameNotFoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.BROKEN_INSTALL);
    }
    catch (NoPermissionException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ACCESS_PERMISSION);
    }
    catch(NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }

    return result;
  }


  /**
   * Creates a Server Group in the ADS.
   * @param serverGroupProperties the properties of the server group to be
   * created.
   * @throws ADSContextException if somethings goes wrong.
   */
  public void createServerGroup(
      Map<ServerGroupProperty, Object> serverGroupProperties)
  throws ADSContextException
  {
    LdapName dn = makeDNFromServerGroupProperties(serverGroupProperties);
    BasicAttributes attrs = makeAttrsFromServerGroupProperties(
        serverGroupProperties);
    // Add the objectclass attribute value
    Attribute oc = new BasicAttribute("objectclass");
    oc.add("top");
    oc.add("groupOfUniqueNames");
    attrs.put(oc);
    try
    {
      DirContext ctx = dirContext.createSubcontext(dn, attrs);
      ctx.close();
    }
    catch (NameAlreadyBoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ALREADY_REGISTERED);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.BROKEN_INSTALL, x);
    }
  }

  /**
   * Updates the properties of a Server Group in the ADS.
   * @param serverGroupProperties the new properties of the server group to be
   * updated.
   * @param groupID The group name.
   * @throws ADSContextException if somethings goes wrong.
   */
  public void updateServerGroup(String groupID,
      Map<ServerGroupProperty, Object> serverGroupProperties)
  throws ADSContextException
  {
    LdapName dn = nameFromDN("cn=" + Rdn.escapeValue(groupID) + "," +
        getServerGroupContainerDN());
    try
    {
      // Entry renaming ?
      if (serverGroupProperties.containsKey(ServerGroupProperty.UID))
      {
        String newGroupId = serverGroupProperties
            .get(ServerGroupProperty.UID).toString();
        if (!newGroupId.equals(groupID))
        {
          // Rename to entry
          LdapName newDN = nameFromDN("cn=" + Rdn.escapeValue(newGroupId)
              + "," + getServerGroupContainerDN());
          dirContext.rename(dn, newDN);
          dn = newDN ;
        }

        // In any case, we remove the "cn" attribute.
        serverGroupProperties.remove(ServerGroupProperty.UID);
      }
      if (serverGroupProperties.isEmpty())
      {
        return ;
      }

      BasicAttributes attrs =
        makeAttrsFromServerGroupProperties(serverGroupProperties);
      // attribute modification
      dirContext.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE, attrs);
    }
    catch (NameNotFoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.NOT_YET_REGISTERED);
    }
    catch (NameAlreadyBoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ALREADY_REGISTERED);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Updates the properties of a Server Group in the ADS.
   * @param serverGroupProperties the new properties of the server group to be
   * updated.
   * @param groupID The group name.
   * @throws ADSContextException if somethings goes wrong.
   */
  public void removeServerGroupProp(String groupID,
      Set<ServerGroupProperty> serverGroupProperties)
  throws ADSContextException
  {

    LdapName dn = nameFromDN("cn=" + Rdn.escapeValue(groupID) + "," +
        getServerGroupContainerDN());
    BasicAttributes attrs =
      makeAttrsFromServerGroupProperties(serverGroupProperties);
    try
    {
      dirContext.modifyAttributes(dn, DirContext.REMOVE_ATTRIBUTE, attrs);
    }
    catch (NameAlreadyBoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ALREADY_REGISTERED);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Deletes a Server Group in the ADS.
   * @param serverGroupProperties the properties of the server group to be
   * deleted.
   * @throws ADSContextException if somethings goes wrong.
   */
  public void deleteServerGroup(
      Map<ServerGroupProperty, Object> serverGroupProperties)
  throws ADSContextException
  {
    LdapName dn = makeDNFromServerGroupProperties(serverGroupProperties);
    try
    {
      dirContext.destroySubcontext(dn);
    }
    catch(NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Returns a set containing the server groups that are defined in the ADS.
   * @return a set containing the server groups that are defined in the ADS.
   * @throws ADSContextException if something goes wrong.
   */
  public Set<Map<ServerGroupProperty, Object>> readServerGroupRegistry()
  throws ADSContextException
  {
    Set<Map<ServerGroupProperty, Object>> result =
      new HashSet<Map<ServerGroupProperty, Object>>();
    try
    {
      NamingEnumeration ne;
      SearchControls sc = new SearchControls();

      sc.setSearchScope(SearchControls.ONELEVEL_SCOPE);
      ne = dirContext.search(getServerGroupContainerDN(), "(objectclass=*)",
          sc);
      while (ne.hasMore())
      {
        SearchResult sr = (SearchResult)ne.next();
        Map<ServerGroupProperty, Object> properties =
          makePropertiesFromServerGroupAttrs(sr.getAttributes());
        result.add(properties);
      }
    }
    catch (NameNotFoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.BROKEN_INSTALL);
    }
    catch (NoPermissionException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ACCESS_PERMISSION);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
    return result;
  }


  /**
   * Returns a set containing the administrators that are defined in the ADS.
   * @return a set containing the administrators that are defined in the ADS.
   * @throws ADSContextException if something goes wrong.
   */
  public Set<Map<AdministratorProperty, Object>> readAdministratorRegistry()
  throws ADSContextException
  {
    Set<Map<AdministratorProperty, Object>> result =
      new HashSet<Map<AdministratorProperty, Object>>();
    try {
      NamingEnumeration ne;
      SearchControls sc = new SearchControls();

      sc.setSearchScope(SearchControls.ONELEVEL_SCOPE);
      ne = dirContext.search(getAdministratorContainerDN(), "(objectclass=*)",
          sc);
      while (ne.hasMore())
      {
        SearchResult sr = (SearchResult)ne.next();

        Map<AdministratorProperty, Object> properties =
          makePropertiesFromAdministratorAttrs(
              getRdn(sr.getName()), sr.getAttributes());

        result.add(properties);
      }
    }
    catch (NameNotFoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.BROKEN_INSTALL);
    }
    catch (NoPermissionException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ACCESS_PERMISSION);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }

    return result;
  }

  /**
   * Creates the Administration Data in the server.
   * The call to this method assumes that OpenDS.jar has already been loaded.
   * So this should not be called by the Java Web Start before being sure that
   * this jar is loaded.
   * @param backendName the backend name which will handle admin information.
   * @throws ADSContextException if something goes wrong.
   */
  public void createAdminData(String backendName) throws ADSContextException
  {
    // Add the administration suffix
    createAdministrationSuffix(backendName);

    // Create the DIT below the administration suffix
    createTopContainerEntry();
    createAdministratorContainerEntry();
    createContainerEntry(getServerContainerDN());
    createContainerEntry(getServerGroupContainerDN());
  }

  /**
   * Removes the administration data.
   * @throws ADSContextException if something goes wrong.
   */
  public void removeAdminData() throws ADSContextException
  {
    removeAdministrationSuffix();
  }


  /**
   * Returns <CODE>true</CODE> if the server contains Administration Data and
   * <CODE>false</CODE> otherwise.
   * @return <CODE>true</CODE> if the server contains Administration Data and
   * <CODE>false</CODE> otherwise.
   * @throws ADSContextException if something goes wrong.
   */
  public boolean hasAdminData() throws ADSContextException
  {
    return isExistingEntry(nameFromDN(getAdministrationSuffixDN()));
  }

  /**
   * Returns the DN of the administrator for a given UID.
   * @param uid the UID to be used to generate the DN.
   * @return the DN of the administrator for the given UID:
   */
  public static String getAdministratorDN(String uid)
  {
    return "cn=" + Rdn.escapeValue(uid) + "," + getAdministratorContainerDN();
  }

  /**
   * Creates an Administrator in the ADS.
   * @param adminProperties the properties of the administrator to be created.
   * @throws ADSContextException if something goes wrong.
   */
  public void createAdministrator(
      Map<AdministratorProperty, Object> adminProperties)
  throws ADSContextException {
    LdapName dnCentralAdmin =
      makeDNFromAdministratorProperties(adminProperties);
    BasicAttributes attrs = makeAttrsFromAdministratorProperties(
        adminProperties);

    try
    {
      DirContext ctx = dirContext.createSubcontext(dnCentralAdmin, attrs);
      ctx.close();
    }
    catch (NameAlreadyBoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ALREADY_REGISTERED);
    }
    catch (NoPermissionException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ACCESS_PERMISSION);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Deletes the administrator in the ADS.
   * @param adminProperties the properties of the administrator to be deleted.
   * @throws ADSContextException if something goes wrong.
   */
  public void deleteAdministrator(
      Map<AdministratorProperty, Object> adminProperties)
  throws ADSContextException {

    LdapName dnCentralAdmin =
      makeDNFromAdministratorProperties(adminProperties);

    try
    {
      dirContext.destroySubcontext(dnCentralAdmin);
    }
    catch (NameNotFoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.NOT_YET_REGISTERED);
    }
    catch (NotContextException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.NOT_YET_REGISTERED);
    }
    catch (NoPermissionException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ACCESS_PERMISSION);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Updates and administrator registered in the ADS.
   * @param adminProperties the new properties of the administrator.
   * @throws ADSContextException if something goes wrong.
   */
  public void updateAdministrator(
      Map<AdministratorProperty, Object> adminProperties)
  throws ADSContextException
  {

    LdapName dnCentralAdmin =
      makeDNFromAdministratorProperties(adminProperties);
    BasicAttributes attrs = makeAttrsFromAdministratorProperties(
        adminProperties);

    try
    {
      dirContext.modifyAttributes(dnCentralAdmin, DirContext.REPLACE_ATTRIBUTE,
          attrs);
    }
    catch (NameNotFoundException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.NOT_YET_REGISTERED);
    }
    catch (NoPermissionException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ACCESS_PERMISSION);
    }
    catch (NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Returns the DN of the suffix that contains the administration data.
   * @return the DN of the suffix that contains the administration data.
   */
  public static String getAdministrationSuffixDN()
  {
    return "cn=admin data";
  }

  /**
   * This method returns the DN of the entry that corresponds to the given host
   * name and installation path.
   * @param hostname the host name.
   * @param ipath the installation path.
   * @return the DN of the entry that corresponds to the given host name and
   * installation path.
   * @throws ADSContextException if something goes wrong.
   */
  private static LdapName makeDNFromHostnameAndPath(String hostname,
      String ipath) throws ADSContextException
  {
    String cnValue = Rdn.escapeValue(hostname + "@" + ipath);
    return nameFromDN("cn=" + cnValue + "," + getServerContainerDN());
  }

  /**
   * This method returns the DN of the entry that corresponds to the given host
   * name port representation.
   * @param hostnameport the host name and port.
   * @return the DN of the entry that corresponds to the given host name and
   * port.
   * @throws ADSContextException if something goes wrong.
   */
  private static LdapName makeDNFromHostnamePort(String hostnamePort)
  throws ADSContextException
  {
    String cnValue = Rdn.escapeValue(hostnamePort);
    return nameFromDN("cn=" + cnValue + "," + getServerContainerDN());
  }


  /**
   * This method returns the DN of the entry that corresponds to the given
   * server group properties.
   * @param serverGroupProperties the server group properties
   * @return the DN of the entry that corresponds to the given server group
   * properties.
   * @throws ADSContextException if something goes wrong.
   */
  private static LdapName makeDNFromServerGroupProperties(
      Map<ServerGroupProperty, Object> serverGroupProperties)
  throws ADSContextException
  {
    String serverGroupId = (String)serverGroupProperties.get(
        ServerGroupProperty.UID);
    if (serverGroupId == null)
    {
      throw new ADSContextException(ADSContextException.ErrorType.MISSING_NAME);
    }
    return nameFromDN("cn=" + Rdn.escapeValue(serverGroupId) + "," +
          getServerGroupContainerDN());
  }

  /**
   * This method returns the DN of the entry that corresponds to the given
   * server properties.
   * @param serverProperties the server properties.
   * @return the DN of the entry that corresponds to the given server
   * properties.
   * @throws ADSContextException if something goes wrong.
   */
  private static LdapName makeDNFromServerProperties(
      Map<ServerProperty, Object> serverProperties) throws ADSContextException
  {
    String hostname = getHostname(serverProperties);
    try
    {
      String ipath = getInstallPath(serverProperties);
      return makeDNFromHostnameAndPath(hostname, ipath);
    }
    catch (ADSContextException ace)
    {
      ServerDescriptor s = ServerDescriptor.createStandalone(serverProperties);
      return makeDNFromHostnamePort(s.getHostPort(true));
    }
  }

  /**
   * This method returns the DN of the entry that corresponds to the given
   * administrator properties.
   * @param adminProperties the administrator properties.
   * @return the DN of the entry that corresponds to the given administrator
   * properties.
   * @throws ADSContextException if something goes wrong.
   */
  private static LdapName makeDNFromAdministratorProperties(
      Map<AdministratorProperty, Object> adminProperties)
  throws ADSContextException
  {
    String adminUid = getAdministratorUID(adminProperties);

    String dnCentralAdmin = getAdministratorDN(adminUid);

    return nameFromDN(dnCentralAdmin);
  }

  /**
   * Returns the attributes for some administrator properties.
   * @param adminProperties the administrator properties.
   * @return the attributes for the given administrator properties.
   * @throws ADSContextException if something goes wrong.
   */
  private static BasicAttributes makeAttrsFromAdministratorProperties(
      Map<AdministratorProperty, Object> adminProperties)
  throws ADSContextException
  {
    BasicAttributes attrs = new BasicAttributes();
    String adminPassword = getAdministratorPassword(adminProperties);
    Attribute oc = new BasicAttribute("objectclass");
    oc.add("top");
    oc.add("person");
    attrs.put(oc);
    attrs.put("sn", "admin");
    attrs.put("userPassword", adminPassword);
    Attribute privilege = new BasicAttribute("ds-privilege-name");
    privilege.add("bypass-acl");
    privilege.add("modify-acl");
    privilege.add("config-read");
    privilege.add("config-write");
    privilege.add("ldif-import");
    privilege.add("ldif-export");
    privilege.add("backend-backup");
    privilege.add("backend-restore");
    privilege.add("server-shutdown");
    privilege.add("server-restart");
    privilege.add("disconnect-client");
    privilege.add("cancel-request");
    privilege.add("password-reset");
    privilege.add("update-schema");
    privilege.add("privilege-change");
    privilege.add("unindexed-search");
    attrs.put(privilege);
    return attrs;
  }

  /**
   * Returns the attributes for some server properties.
   * @param serverProperties the server properties.
   * @return the attributes for the given server properties.
   * @throws ADSContextException if something goes wrong.
   */
  private static BasicAttributes makeAttrsFromServerProperties(
      Map<ServerProperty, Object> serverProperties)
  {
    BasicAttributes result = new BasicAttributes();

    // Transform 'properties' into 'attributes'
    for (ServerProperty prop: serverProperties.keySet())
    {
      Attribute attr = makeAttrFromServerProperty(prop,
          serverProperties.get(prop));
      if (attr != null)
      {
        result.put(attr);
      }
    }
    // Add the objectclass attribute value
    // TODO: use another structural objectclass
    Attribute oc = new BasicAttribute("objectclass");
    oc.add("top");
    oc.add("ds-cfg-branch");
    oc.add("extensibleobject");
    result.put(oc);
    return result;
  }

  /**
   * Returns the attribute for a given server property.
   * @param property the server property.
   * @return the attribute for a given server property.
   * @throws ADSContextException if something goes wrong.
   */
  private static Attribute makeAttrFromServerProperty(ServerProperty property,
      Object value)
  {
    Attribute result;

    switch(property)
    {
    case GROUPS:
      result = new BasicAttribute(ServerProperty.GROUPS.getAttributeName());
      Iterator groupIterator = ((Set)value).iterator();
      while (groupIterator.hasNext())
      {
        result.add(groupIterator.next());
      }
      break;
    default:
      result = new BasicAttribute(property.getAttributeName(), value);
    }
    return result;
  }

  /**
   * Returns the attributes for some server group properties.
   * @param serverProperties the server group properties.
   * @return the attributes for the given server group properties.
   * @throws ADSContextException if something goes wrong.
   */
  private static BasicAttributes makeAttrsFromServerGroupProperties(
      Map<ServerGroupProperty, Object> serverGroupProperties)
  {
    BasicAttributes result = new BasicAttributes();

    // Transform 'properties' into 'attributes'
    for (ServerGroupProperty prop: serverGroupProperties.keySet())
    {
      Attribute attr = makeAttrFromServerGroupProperty(prop,
          serverGroupProperties.get(prop));
      if (attr != null)
      {
        result.put(attr);
      }
    }
    return result;
  }

  /**
   * Returns the attributes for some server group properties.
   * @param serverProperties the server group properties.
   * @return the attributes for the given server group properties.
   * @throws ADSContextException if something goes wrong.
   */
  private static BasicAttributes makeAttrsFromServerGroupProperties(
      Set<ServerGroupProperty> serverGroupProperties)
  {
    BasicAttributes result = new BasicAttributes();

    // Transform 'properties' into 'attributes'
    for (ServerGroupProperty prop: serverGroupProperties)
    {
      Attribute attr = makeAttrFromServerGroupProperty(prop,null);
      if (attr != null)
      {
        result.put(attr);
      }
    }
    return result;
  }

  /**
   * Returns the attribute for a given server group property.
   * @param property the server group property.
   * @return the attribute for a given server group property.
   * @throws ADSContextException if something goes wrong.
   */
  private static Attribute makeAttrFromServerGroupProperty(
      ServerGroupProperty property, Object value)
  {
    Attribute result;

    switch(property)
    {
    case MEMBERS:
      result = new BasicAttribute(
          ServerGroupProperty.MEMBERS.getAttributeName());
      Iterator memberIterator = ((Set)value).iterator();
      while (memberIterator.hasNext())
      {
        result.add(memberIterator.next());
      }
      break;
    default:
      result = new BasicAttribute(property.getAttributeName(), value);
    }

    return result;
  }

  /**
   * Returns the properties of a server group for some LDAP attributes.
   * @param attrs the LDAP attributes.
   * @return the properties of a server group for some LDAP attributes.
   * @throws ADSContextException if something goes wrong.
   */
  private Map<ServerGroupProperty, Object> makePropertiesFromServerGroupAttrs(
      Attributes attrs) throws ADSContextException
  {
    HashMap<ServerGroupProperty, Object> result =
      new HashMap<ServerGroupProperty, Object>();
    try
    {
      for (ServerGroupProperty prop : ServerGroupProperty.values())
      {
        Attribute attr = attrs.get(prop.getAttributeName());
        if (attr == null)
        {
          continue ;
        }
        Object value = null;

        if (attr.size() >= 1 &&
            MULTIVALUED_SERVER_GROUP_PROPERTIES.contains(prop))
        {

          Set<String> set = new HashSet<String>();
          NamingEnumeration ae = attr.getAll();
          while (ae.hasMore())
          {
            set.add((String)ae.next());
          }
          value = set;
        }
        else
        {
          value = attr.get(0);
        }

        result.put(prop, value);
      }
    }
    catch(NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
    return result;
  }

  /**
   * Returns the properties of a server for some LDAP attributes.
   * @param attrs the LDAP attributes.
   * @return the properties of a server for some LDAP attributes.
   * @throws ADSContextException if something goes wrong.
   */
  private Map<ServerProperty, Object> makePropertiesFromServerAttrs(
      Attributes attrs) throws ADSContextException
  {
    HashMap<ServerProperty, Object> result =
      new HashMap<ServerProperty, Object>();
    try
    {
      NamingEnumeration ne = attrs.getAll();
      while (ne.hasMore())
      {
        Attribute attr = (Attribute)ne.next();
        String attrID = attr.getID();
        Object value = null;

        if (attrID.endsWith(";binary"))
        {
          attrID = attrID.substring(0, attrID.lastIndexOf(";binary"));
        }

        ServerProperty prop = null;
        ServerProperty[] props = ServerProperty.values();
        for (int i=0; i<props.length && (prop == null); i++)
        {
          String v = props[i].getAttributeName();
          if (attrID.equalsIgnoreCase(v))
          {
            prop = props[i];
          }
        }
        if (prop == null)
        {
          // Do not handle it
        }
        else
        {

          if (attr.size() >= 1 && MULTIVALUED_SERVER_PROPERTIES.contains(prop))
          {
            Set<String> set = new HashSet<String>();
            NamingEnumeration ae = attr.getAll();
            while (ae.hasMore())
            {
              set.add((String)ae.next());
            }
            value = set;
          }
          else
          {
            value = attr.get(0);
          }

          result.put(prop, value);
        }
      }
    }
    catch(NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
    return result;
  }


  /**
   * Returns the properties of an administrator for some rdn and LDAP
   * attributes.
   * @param rdn the RDN.
   * @param attrs the LDAP attributes.
   * @return the properties of an administrator for the given rdn and LDAP
   * attributes.
   * @throws ADSContextException if something goes wrong.
   */
  private Map<AdministratorProperty, Object>
  makePropertiesFromAdministratorAttrs(String rdn, Attributes attrs)
  throws ADSContextException
  {
    Map<AdministratorProperty, Object> result =
      new HashMap<AdministratorProperty, Object>();
    LdapName nameObj;
    nameObj = nameFromDN(rdn);
    String dn = nameObj + "," + getAdministratorContainerDN();
    result.put(AdministratorProperty.ADMINISTRATOR_DN, dn);

    try
    {
      NamingEnumeration<? extends Attribute> ne = attrs.getAll();
      while (ne.hasMore()) {
        Attribute attr = ne.next();
        String attrID = attr.getID();
        Object value = null;

        if (attrID.equalsIgnoreCase("cn"))
        {
          value = attr.get(0);
          result.put(AdministratorProperty.UID, value);
        }
        else if (attrID.equalsIgnoreCase("userpassword"))
        {
          value = attr.get(0);
          result.put(AdministratorProperty.PASSWORD, value);
        }
        else if (attrID.equalsIgnoreCase("description"))
        {
          value = attr.get(0);
          result.put(AdministratorProperty.DESCRIPTION, value);
        }
      }
    }
    catch(NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }

    return result;
  }

  /**
   * Returns the parent entry of the server entries.
   * @return the parent entry of the server entries.
   */
  private static String getServerContainerDN()
  {
    return "cn=Servers," + getAdministrationSuffixDN();
  }

  /**
   * Returns the parent entry of the administrator entries.
   * @return the parent entry of the administrator entries.
   */
  public static String getAdministratorContainerDN()
  {
    return "cn=Administrators," + getAdministrationSuffixDN();
  }

  /**
   * Returns the parent entry of the server group entries.
   * @return the parent entry of the server group entries.
   */
  private static String getServerGroupContainerDN()
  {
    return "cn=Server Groups," + getAdministrationSuffixDN();
  }

  /**
   * Returns the host name for the given properties.
   * @param serverProperties the server properties.
   * @return the host name for the given properties.
   * @throws ADSContextException if the host name could not be found or its
   * value is not valid.
   */
  private static String getHostname(
      Map<ServerProperty, Object> serverProperties) throws ADSContextException
  {
    String result = (String)serverProperties.get(ServerProperty.HOST_NAME);
    if (result == null)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.MISSING_HOSTNAME);
    }
    else if (result.length() == 0)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.NOVALID_HOSTNAME);
    }
    return result;
  }

  /**
   * Returns the install path for the given properties.
   * @param serverProperties the server properties.
   * @return the install path for the given properties.
   * @throws ADSContextException if the install path could not be found or its
   * value is not valid.
   */
  private static String getInstallPath(
      Map<ServerProperty, Object> serverProperties) throws ADSContextException
  {
    String result = (String)serverProperties.get(ServerProperty.INSTANCE_PATH);
    if (result == null)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.MISSING_IPATH);
    }
    else if (result.length() == 0)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.NOVALID_IPATH);
    }
    return result;
  }


  /**
   * Returns the Administrator UID for the given properties.
   * @param adminProperties the server properties.
   * @return the Administrator UID for the given properties.
   * @throws ADSContextException if the administrator UID could not be found.
   */
  private static String getAdministratorUID(
      Map<AdministratorProperty, Object> adminProperties)
  throws ADSContextException {
    String result = (String)adminProperties.get(
        AdministratorProperty.UID);
    if (result == null)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.MISSING_ADMIN_UID);
    }
    return result;
  }

  /**
   * Returns the Administrator password for the given properties.
   * @param adminProperties the server properties.
   * @return the Administrator password for the given properties.
   * @throws ADSContextException if the administrator password could not be
   * found.
   */
  private static String getAdministratorPassword(
      Map<AdministratorProperty, Object> adminProperties)
  throws ADSContextException {
    String result = (String)adminProperties.get(
        AdministratorProperty.PASSWORD);
    if (result == null)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.MISSING_ADMIN_PASSWORD);
    }
    return result;
  }


  //
  // LDAP utilities
  //
  /**
   * Returns the LdapName object for the given dn.
   * @return the LdapName object for the given dn.
   * @throws ADSContextException if a valid LdapName could not be retrieved
   * for the given dn.
   */
  private static LdapName nameFromDN(String dn) throws ADSContextException
  {
    LdapName result = null;
    try
    {
      result = new LdapName(dn);
    }
    catch (InvalidNameException x)
    {
      LOG.log(Level.SEVERE, "Error parsing dn "+dn, x);
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
    return result;
  }

  /**
   * Returns the String rdn for the given search result name.
   * @return the String rdn for the given search result name.
   * @throws ADSContextException if a valid String rdn could not be retrieved
   * for the given result name.
   */
  private static String getRdn(String rdnName) throws ADSContextException
  {
    CompositeName nameObj;
    String rdn;
    //
    // Transform the JNDI name into a RDN string
    //
    try {
      nameObj = new CompositeName(rdnName);
      rdn = nameObj.get(0);
    }
    catch (InvalidNameException x)
    {
      LOG.log(Level.SEVERE, "Error parsing rdn "+rdnName, x);
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
    return rdn;
  }

  /**
   * Tells whether an entry with the provided DN exists.
   * @return <CODE>true</CODE> if the entry exists and <CODE>false</CODE> if
   * it does not.
   * @throws ADSContextException if an error occurred while checking if the
   * entry exists or not.
   */
  private boolean isExistingEntry(LdapName dn) throws ADSContextException
  {
    boolean result;

    try
    {
      SearchControls sc = new SearchControls();

      sc.setSearchScope(SearchControls.OBJECT_SCOPE);
      result = getDirContext().search(dn, "(objectclass=*)", sc).hasMore();
    }
    catch (NameNotFoundException x)
    {
      result = false;
    }
    catch (NoPermissionException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ACCESS_PERMISSION);
    }
    catch(javax.naming.NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }

    return result;
  }

  /**
   * Creates a container entry with the given dn.
   * @param dn the entry of the new entry to be created.
   * @throws ADSContextException if the entry could not be created.
   */
  private void createContainerEntry(String dn) throws ADSContextException
  {
    BasicAttributes attrs = new BasicAttributes();
    Attribute oc = new BasicAttribute("objectclass");
    oc.add("top");
    oc.add("ds-cfg-branch");
    attrs.put(oc);
    createEntry(dn, attrs);
  }

  /**
   * Creates the administrator container entry.
   * @throws ADSContextException if the entry could not be created.
   */
  private void createAdministratorContainerEntry() throws ADSContextException
  {
    BasicAttributes attrs = new BasicAttributes();

    Attribute oc = new BasicAttribute("objectclass");
    oc.add("groupofurls");
    attrs.put(oc);
    attrs.put("memberURL", "ldap:///" + getAdministratorContainerDN() +
        "??one?(objectclass=*)");
    attrs.put("description", "Group of identities which have full access.");
    createEntry(getAdministratorContainerDN(), attrs);
  }


  /**
   * Creates the top container entry.
   * @throws ADSContextException if the entry could not be created.
   */
  private void createTopContainerEntry() throws ADSContextException
  {
    BasicAttributes attrs = new BasicAttributes();

    Attribute oc = new BasicAttribute("objectclass");
    oc.add("top");
    oc.add("ds-cfg-branch");
    attrs.put(oc);
    createEntry(getAdministrationSuffixDN(), attrs);
  }


  /**
   * Creates an entry with the provided dn and attributes.
   * @param dn the dn of the entry.
   * @param attrs the attributes of the entry.
   * @throws ADSContextException if the entry could not be created.
   */
  private void createEntry(String dn, Attributes attrs)
  throws ADSContextException {
    try
    {
      DirContext ctx = getDirContext().createSubcontext(nameFromDN(dn), attrs);
      ctx.close();
    }
    catch(NamingException x)
    {
      throw new ADSContextException(
          ADSContextException.ErrorType.ERROR_UNEXPECTED, x);
    }
  }

  /**
   * Creates the Administration Suffix.
   * @param backendName the backend name to be used for the Administration
   * Suffix.  If this value is null the default backendName for the
   * Administration Suffix will be used.
   * @throws ADSContextException if something goes wrong.
   */
  public void createAdministrationSuffix(String backendName)
  throws ADSContextException
  {
    ADSContextHelper helper = new ADSContextHelper();
    String ben = backendName ;
    if (backendName == null)
    {
      ben = getBackendName() ;
    }
    helper.createAdministrationSuffix(getDirContext(), ben,
        getDbName(), getImportTemp());
  }

  /**
   * Removes the administration suffix.
   * @throws ADSContextException
   */
  private void removeAdministrationSuffix() throws ADSContextException
  {
    ADSContextHelper helper = new ADSContextHelper();
    helper.removeAdministrationSuffix(getDirContext(), getBackendName());
  }

  private static String getBackendName()
  {
    return "adminRoot";
  }

  private static String getDbName()
  {
    return "adminDb";
  }

  private static String getImportTemp()
  {
    return "importAdminTemp";
  }
}
