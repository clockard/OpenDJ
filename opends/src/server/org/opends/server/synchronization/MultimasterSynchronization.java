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
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */
package org.opends.server.synchronization;

import java.util.HashMap;
import java.util.Map;

import org.opends.server.api.ConfigAddListener;
import org.opends.server.api.ConfigChangeListener;
import org.opends.server.api.ConfigDeleteListener;
import org.opends.server.api.SynchronizationProvider;
import org.opends.server.changelog.Changelog;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.core.AddOperation;
import org.opends.server.types.DN;
import org.opends.server.core.DeleteOperation;
import org.opends.server.core.DirectoryException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.types.Entry;
import org.opends.server.core.ModifyDNOperation;
import org.opends.server.core.ModifyOperation;
import org.opends.server.core.Operation;
import org.opends.server.types.ConfigChangeResult;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SynchronizationProviderResult;

import static org.opends.server.synchronization.SynchMessages.*;

/**
 * This class is used to load the Synchronization code inside the JVM
 * and to trigger initialization of the synchronization.
 */
public class MultimasterSynchronization extends SynchronizationProvider
       implements ConfigAddListener, ConfigDeleteListener, ConfigChangeListener
{
  static String CHANGELOG_DN = "cn=Changelog Server, cn=config";
  static String CHANGELOG_SERVER_ATTR = "ds-cfg-changelog-server";
  static String SERVER_ID_ATTR = "ds-cfg-server-id";
  static String CHANGELOG_PORT_ATTR = "ds-cfg-changelog-port";

  private Changelog changelog = null;
  private static Map<DN, SynchronizationDomain> domains =
    new HashMap<DN, SynchronizationDomain>() ;

  /**
   * {@inheritDoc}
   */
  public void initializeSynchronizationProvider(ConfigEntry configEntry)
  throws ConfigException
  {
    DN configEntryDn = null;

    SynchMessages.registerMessages();

    configEntry.registerAddListener(this);
    configEntry.registerDeleteListener(this);

    /*
     * Read changelog server the changelog configuration entry
     */
    try
    {
      configEntryDn = DN.decode(CHANGELOG_DN);
      ConfigEntry config = DirectoryServer.getConfigEntry(configEntryDn);
      /*
       * If there is no such entry, this process must not be a changelog server
       */
      if (config != null)
      {
        changelog = new Changelog(config);
      }
    } catch (DirectoryException e)
    {
      /* never happens */
      throw new ConfigException(MSGID_SYNC_INVALID_DN,
      "Invalid Changelog configuration DN");
    }

    /*
     * Parse the list of entries below configEntry,
     * create one synchronization domain for each child
     */
    for (ConfigEntry domainEntry : configEntry.getChildren().values())
    {
      SynchronizationDomain domain = new SynchronizationDomain(domainEntry);
      domains.put(domain.getBaseDN(), domain);
      domain.start();
    }
  }

  /**
   * Indicates whether the configuration entry that will result from a proposed
   * modification is acceptable to this change listener.
   *
   * @param  configEntry         The configuration entry that will result from
   *                             the requested update.
   * @param  unacceptableReason  A buffer to which this method can append a
   *                             human-readable message explaining why the
   *                             proposed change is not acceptable.
   *
   * @return  <CODE>true</CODE> if the proposed entry contains an acceptable
   *          configuration, or <CODE>false</CODE> if it does not.
   */
  public boolean configChangeIsAcceptable(ConfigEntry configEntry,
                                          StringBuilder unacceptableReason)
  {
    return false; // TODO :NYI
  }

  /**
   * Attempts to apply a new configuration to this Directory Server component
   * based on the provided changed entry.
   *
   * @param  configEntry  The configuration entry that containing the updated
   *                      configuration for this component.
   *
   * @return  Information about the result of processing the configuration
   *          change.
   */
  public ConfigChangeResult applyConfigurationChange(ConfigEntry configEntry)
  {
    // TODO implement this method
    return new ConfigChangeResult(ResultCode.SUCCESS, false);
  }

  /**
   * {@inheritDoc}
   */
  public boolean configAddIsAcceptable(ConfigEntry configEntry,
      StringBuilder unacceptableReason)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationAdd(ConfigEntry configEntry)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean configDeleteIsAcceptable(ConfigEntry configEntry,
      StringBuilder unacceptableReason)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public ConfigChangeResult applyConfigurationDelete(ConfigEntry configEntry)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void doPostOperation(AddOperation addOperation)
  {
    DN dn = addOperation.getEntryDN();
    genericPostOperation(addOperation, dn);
  }


  /**
   * {@inheritDoc}
   */
  public void doPostOperation(DeleteOperation deleteOperation)
  {
    DN dn = deleteOperation.getEntryDN();
    genericPostOperation(deleteOperation, dn);
  }

  /**
   * {@inheritDoc}
   */
  public void doPostOperation(ModifyDNOperation modifyDNOperation)
  {
    DN dn = modifyDNOperation.getEntryDN();
    genericPostOperation(modifyDNOperation, dn);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doPostOperation(ModifyOperation modifyOperation)
  {
    DN dn = modifyOperation.getEntryDN();
    genericPostOperation(modifyOperation, dn);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SynchronizationProviderResult handleConflictResolution(
                                                ModifyOperation modifyOperation)
  {
    SynchronizationDomain domain = findDomain(modifyOperation.getEntryDN());
    if (domain == null)
      return new SynchronizationProviderResult(true);

    return domain.handleConflictResolution(modifyOperation);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public SynchronizationProviderResult
      doPreOperation(ModifyOperation modifyOperation)
  {
    SynchronizationDomain domain = findDomain(modifyOperation.getEntryDN());
    if (domain == null)
      return new SynchronizationProviderResult(true);

    Historical historicalInformation = (Historical)
                            modifyOperation.getAttachment(HISTORICAL);
    if (historicalInformation == null)
    {
      Entry entry = modifyOperation.getModifiedEntry();
      historicalInformation = Historical.load(entry);
      modifyOperation.setAttachment(HISTORICAL, historicalInformation);
    }

    historicalInformation.generateState(modifyOperation);

    return new SynchronizationProviderResult(true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SynchronizationProviderResult doPreOperation(AddOperation addOperation)
  {
    SynchronizationDomain domain = findDomain(addOperation.getEntryDN());
    if (domain == null)
      return new SynchronizationProviderResult(true);

    domain.setChangeNumber(addOperation);
    return new SynchronizationProviderResult(true);
  }

  /**
   * Pre-operation processing.
   * Called after operation has been processed by the core server
   * but before being committed to the backend
   * Generate the Change number and update the historical information
   *
   * @param deleteOperation the current operation
   * @return code indicating if operation must be processed
   */
  @Override
  public SynchronizationProviderResult
                  doPreOperation(DeleteOperation deleteOperation)
  {
    SynchronizationDomain domain = findDomain(deleteOperation.getEntryDN());
    if (domain == null)
      return new SynchronizationProviderResult(true);

    domain.setChangeNumber(deleteOperation);

    return new SynchronizationProviderResult(true);
  }

  /**
   * Pre-operation processing.
   * Called after operation has been processed by the core server
   * but before being committed to the backend
   * Generate the Change number and update the historical information
   *
   * @param modifyDNOperation the current operation
   * @return code indicating if operation must be processed
   */
  @Override
  public SynchronizationProviderResult
                  doPreOperation(ModifyDNOperation modifyDNOperation)
  {

    SynchronizationDomain domain = findDomain(modifyDNOperation.getEntryDN());
    if (domain == null)
      return new SynchronizationProviderResult(true);

    domain.setChangeNumber(modifyDNOperation);
    return new SynchronizationProviderResult(true);
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public void finalizeSynchronizationProvider()
  {
    // shutdown all the Synchronization domains
    for (SynchronizationDomain domain : domains.values())
    {
      domain.shutdown();
    }

    // shutdown the Changelog Service if necessary
    if (changelog != null)
      Changelog.shutdown();
  }

  /**
   * Finds the Synchronization domain for a given DN.
   *
   * @param dn The DN for which the domain must be returned.
   * @return The Synchronization domain for this DN.
   */
  private static SynchronizationDomain findDomain(DN dn)
  {
    SynchronizationDomain domain = null;
    DN temp = dn;
    do
    {
      domain = domains.get(temp);
      temp = temp.getParent();
      if (temp == null)
      {
        break;
      }
    } while (domain == null);

    if ((domain!= null) && (domain.getServerStateDN().equals(dn)))
      return null;

    return domain;
  }

  /**
   * Generic code for all the postOperation entry point.
   *
   * @param operation The Operation for which the post-operation is called.
   * @param dn The Dn for which the post-operation is called.
   */
  private void genericPostOperation(Operation operation, DN dn)
  {
    SynchronizationDomain domain = findDomain(dn);
    if (domain == null)
      return;

    domain.synchronize(operation);

    return;
  }

  /**
   * Get the ServerState associated to the SynchronizationDomain
   * with a given DN.
   *
   * @param baseDn The DN of the Synchronization Domain for which the
   *               ServerState must be returned.
   * @return the ServerState associated to the SynchronizationDomain
   *         with the DN in parameter.
   */
  public static ServerState getServerState(DN baseDn)
  {
    SynchronizationDomain domain = findDomain(baseDn);
    return domain.getServerState();
  }

}


