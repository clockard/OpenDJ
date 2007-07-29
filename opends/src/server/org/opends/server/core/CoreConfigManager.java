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
package org.opends.server.core;



import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.opends.server.admin.server.ConfigurationChangeListener;
import org.opends.server.admin.std.server.GlobalCfg;
import org.opends.server.admin.std.server.RootCfg;
import org.opends.server.admin.server.ServerManagementContext;
import org.opends.server.api.IdentityMapper;
import org.opends.server.config.ConfigException;
import org.opends.server.types.AcceptRejectWarn;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.DN;
import org.opends.server.types.InitializationException;
import org.opends.server.types.ResultCode;
import org.opends.server.types.WritabilityMode;

import static org.opends.server.messages.ConfigMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.ServerConstants.*;



/**
 * This class defines a utility that will be used to manage the set of core
 * configuration attributes defined in the Directory Server.  These
 * configuration attributes appear in the "cn=config" configuration entry.
 */
public class CoreConfigManager
       implements ConfigurationChangeListener<GlobalCfg>
{
  /**
   * Creates a new instance of this core config manager.
   */
  public CoreConfigManager()
  {
    // No implementation is required.
  }



  /**
   * Initializes the Directory Server's core configuration.  This should only be
   * called at server startup.
   *
   * @throws  ConfigException  If a configuration problem causes the identity
   *                           mapper initialization process to fail.
   *
   * @throws  InitializationException  If a problem occurs while initializing
   *                                   the identity mappers that is not related
   *                                   to the server configuration.
   */
  public void initializeCoreConfig()
         throws ConfigException, InitializationException
  {
    // Get the root configuration object.
    ServerManagementContext managementContext =
         ServerManagementContext.getInstance();
    RootCfg rootConfiguration =
         managementContext.getRootConfiguration();


    // Get the global configuration and register with it as a change listener.
    GlobalCfg globalConfig = rootConfiguration.getGlobalConfiguration();
    globalConfig.addChangeListener(this);


    // If there are any STMP servers specified, then make sure that if the value
    // contains a colon that the portion after it is an integer between 1 and
    // 65535.
    Set<String> smtpServers = globalConfig.getSMTPServer();
    if (smtpServers != null)
    {
      for (String server : smtpServers)
      {
        int colonPos = server.indexOf(':');
        if ((colonPos == 0) || (colonPos == (server.length()-1)))
        {
          int    msgID   = MSGID_CONFIG_CORE_INVALID_SMTP_SERVER;
          String message = getMessage(msgID, server);
          throw new ConfigException(msgID, message);
        }
        else if (colonPos > 0)
        {
          try
          {
            int port = Integer.parseInt(server.substring(colonPos+1));
            if ((port < 1) || (port > 65535))
            {
              int    msgID   = MSGID_CONFIG_CORE_INVALID_SMTP_SERVER;
              String message = getMessage(msgID, server);
              throw new ConfigException(msgID, message);
            }
          }
          catch (Exception e)
          {
            int    msgID   = MSGID_CONFIG_CORE_INVALID_SMTP_SERVER;
            String message = getMessage(msgID, server);
            throw new ConfigException(msgID, message, e);
          }
        }
      }
    }


    // Apply the configuration to the server.
    applyGlobalConfiguration(globalConfig);
  }



  /**
   * Applies the settings in the provided configuration to the Directory Server.
   *
   * @param  globalConfig  The configuration settings to be applied.
   */
  private static void applyGlobalConfiguration(GlobalCfg globalConfig)
  {
    DirectoryServer.setCheckSchema(globalConfig.isCheckSchema());

    DirectoryServer.setDefaultPasswordPolicyDN(
         globalConfig.getDefaultPasswordPolicy());

    DirectoryServer.setAddMissingRDNAttributes(
         globalConfig.isAddMissingRDNAttributes());

    DirectoryServer.setAllowAttributeNameExceptions(
         globalConfig.isAllowAttributeNameExceptions());

    switch (globalConfig.getInvalidAttributeSyntaxBehavior())
    {
      case ACCEPT:
        DirectoryServer.setSyntaxEnforcementPolicy(AcceptRejectWarn.ACCEPT);
        break;
      case WARN:
        DirectoryServer.setSyntaxEnforcementPolicy(AcceptRejectWarn.WARN);
        break;
      case REJECT:
      default:
        DirectoryServer.setSyntaxEnforcementPolicy(AcceptRejectWarn.REJECT);
        break;
    }

    DirectoryServer.setServerErrorResultCode(
         ResultCode.valueOf(globalConfig.getServerErrorResultCode()));

    switch (globalConfig.getSingleStructuralObjectclassBehavior())
    {
      case ACCEPT:
        DirectoryServer.setSingleStructuralObjectClassPolicy(
             AcceptRejectWarn.ACCEPT);
        break;
      case WARN:
        DirectoryServer.setSingleStructuralObjectClassPolicy(
             AcceptRejectWarn.WARN);
        break;
      case REJECT:
      default:
        DirectoryServer.setSingleStructuralObjectClassPolicy(
             AcceptRejectWarn.REJECT);
        break;
    }

    DirectoryServer.setNotifyAbandonedOperations(
         globalConfig.isNotifyAbandonedOperations());

    DirectoryServer.setSizeLimit(globalConfig.getSizeLimit());

    DirectoryServer.setTimeLimit((int) globalConfig.getTimeLimit());

    DirectoryServer.setProxiedAuthorizationIdentityMapperDN(
         globalConfig.getProxiedAuthorizationIdentityMapperDN());

    switch (globalConfig.getWritabilityMode())
    {
      case ENABLED:
        DirectoryServer.setWritabilityMode(WritabilityMode.ENABLED);
        break;
      case INTERNAL_ONLY:
        DirectoryServer.setWritabilityMode(WritabilityMode.INTERNAL_ONLY);
        break;
      case DISABLED:
      default:
        DirectoryServer.setWritabilityMode(WritabilityMode.DISABLED);
        break;
    }

    DirectoryServer.setRejectUnauthenticatedRequests(
         globalConfig.isRejectUnauthenticatedRequests());

    DirectoryServer.setBindWithDNRequiresPassword(
         globalConfig.isBindWithDNRequiresPassword());

    DirectoryServer.setLookthroughLimit(globalConfig.getLookthroughLimit());


    ArrayList<Properties> mailServerProperties = new ArrayList<Properties>();
    Set<String> smtpServers = globalConfig.getSMTPServer();
    if ((smtpServers != null) && (! smtpServers.isEmpty()))
    {
      for (String smtpServer : smtpServers)
      {
        int colonPos = smtpServer.indexOf(':');
        if (colonPos > 0)
        {
          String smtpHost = smtpServer.substring(0, colonPos);
          String smtpPort = smtpServer.substring(colonPos+1);

          Properties properties = new Properties();
          properties.setProperty(SMTP_PROPERTY_HOST, smtpHost);
          properties.setProperty(SMTP_PROPERTY_PORT, smtpPort);
          mailServerProperties.add(properties);
        }
        else
        {
          Properties properties = new Properties();
          properties.setProperty(SMTP_PROPERTY_HOST, smtpServer);
          mailServerProperties.add(properties);
        }
      }
    }
    DirectoryServer.setMailServerPropertySets(mailServerProperties);

    DirectoryServer.setAllowedTasks(globalConfig.getAllowedTask());
  }



  /**
   * {@inheritDoc}
   */
  public boolean isConfigurationChangeAcceptable(GlobalCfg configuration,
                      List<String> unacceptableReasons)
  {
    boolean configAcceptable = true;

    // Make sure that the default password policy DN is valid.
    DN policyDN = configuration.getDefaultPasswordPolicy();
    PasswordPolicy policy = DirectoryServer.getPasswordPolicy(policyDN);
    if (policy == null)
    {
      int    msgID   = MSGID_CONFIG_CORE_NO_SUCH_PWPOLICY;
      String message = getMessage(msgID, String.valueOf(policyDN));
      unacceptableReasons.add(message);

      configAcceptable = false;
    }

    // Make sure that the proxied auth identity mapper is valid.
    DN mapperDN = configuration.getProxiedAuthorizationIdentityMapperDN();
    IdentityMapper mapper = DirectoryServer.getIdentityMapper(mapperDN);
    if (mapper == null)
    {
      int    msgID   = MSGID_CONFIG_CORE_NO_PROXY_MAPPER_FOR_DN;
      String message = getMessage(msgID, String.valueOf(mapperDN),
                                  String.valueOf(configuration.dn()));
      unacceptableReasons.add(message);

      configAcceptable = false;
    }

    Set<String> smtpServers = configuration.getSMTPServer();
    if (smtpServers != null)
    {
      for (String server : smtpServers)
      {
        int colonPos = server.indexOf(':');
        if ((colonPos == 0) || (colonPos == (server.length()-1)))
        {
          int    msgID   = MSGID_CONFIG_CORE_INVALID_SMTP_SERVER;
          String message = getMessage(msgID, server);
          unacceptableReasons.add(message);
          configAcceptable = false;
        }
        else if (colonPos > 0)
        {
          try
          {
            int port = Integer.parseInt(server.substring(colonPos+1));
            if ((port < 1) || (port > 65535))
            {
              int    msgID   = MSGID_CONFIG_CORE_INVALID_SMTP_SERVER;
              String message = getMessage(msgID, server);
              unacceptableReasons.add(message);
              configAcceptable = false;
            }
          }
          catch (Exception e)
          {
            int    msgID   = MSGID_CONFIG_CORE_INVALID_SMTP_SERVER;
            String message = getMessage(msgID, server);
            unacceptableReasons.add(message);
            configAcceptable = false;
          }
        }
      }
    }

    return configAcceptable;
  }



  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationChange(GlobalCfg configuration)
  {
    ResultCode        resultCode          = ResultCode.SUCCESS;
    boolean           adminActionRequired = false;
    ArrayList<String> messages            = new ArrayList<String>();

    applyGlobalConfiguration(configuration);

    return new ConfigChangeResult(resultCode, adminActionRequired, messages);
  }
}

