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
 *      Copyright 2006-2009 Sun Microsystems, Inc.
 */

package org.opends.quicksetup.installer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.ldap.InitialLdapContext;

import org.opends.quicksetup.Application;
import org.opends.quicksetup.ApplicationException;
import org.opends.quicksetup.Installation;
import org.opends.quicksetup.ReturnCode;
import org.opends.quicksetup.util.OutputReader;
import org.opends.quicksetup.util.Utils;

import static org.opends.quicksetup.util.Utils.*;

import org.opends.server.admin.DefaultBehaviorException;
import org.opends.server.admin.ManagedObjectNotFoundException;
import org.opends.server.admin.client.ManagementContext;
import org.opends.server.admin.client.ldap.LDAPManagementContext;
import org.opends.server.admin.client.ldap.JNDIDirContextAdaptor;
import org.opends.server.admin.std.client.*;
import org.opends.server.admin.std.meta.*;
import org.opends.server.backends.task.TaskState;
import org.opends.messages.CoreMessages;
import org.opends.messages.JebMessages;
import org.opends.messages.ReplicationMessages;
import org.opends.messages.Message;
import static org.opends.messages.QuickSetupMessages.*;

import org.opends.server.tools.ConfigureDS;
import org.opends.server.tools.ConfigureWindowsService;
import org.opends.server.tools.JavaPropertiesTool;
import org.opends.server.types.DN;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.ExistingFileBehavior;
import org.opends.server.types.LDIFExportConfig;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFWriter;
import org.opends.server.util.SetupUtils;
import org.opends.server.util.StaticUtils;

/**
 * This is the only class that uses classes in org.opends.server (excluding the
 * case of DynamicConstants, SetupUtils, OperatingSystem and CertificateManager
 * which are already included in quicksetup.jar).
 *
 * Important note: do not include references to this class until OpenDS.jar has
 * been loaded. These classes must be loaded during Runtime.
 * The code is written in a way that when we execute the code that uses these
 * classes the required jar files are already loaded. However these jar files
 * are not necessarily loaded when we create this class.
 */
public class InstallerHelper {
  private static final Logger LOG = Logger.getLogger(
      InstallerHelper.class.getName());

  private static final int MAX_ID_VALUE = Short.MAX_VALUE;
  private static final String DOMAIN_BASE_NAME = "domain ";

  private static final String INITIAL_CLIENT_HEAP_ARG = "-Xms8m";

  private static final String SERVER_HEAP_ARGS = "-Xms128m -Xmx256m";

  private static final long SERVER_MAX_HEAP_BYTES = 256 * 1024 * 1024;

  /**
   * Invokes the method ConfigureDS.configMain with the provided parameters.
   * @param args the arguments to be passed to ConfigureDS.configMain.
   * @return the return code of the ConfigureDS.configMain method.
   * @throws ApplicationException if something goes wrong.
   * @see org.opends.server.tools.ConfigureDS#configMain(String[]).
   */
  public int invokeConfigureServer(String[] args) throws ApplicationException {
    return ConfigureDS.configMain(args, System.out, System.err);
  }

  /**
   * Invokes the import-ldif command-line with the provided parameters.
   * @param application the application that is launching this.
   * @param args the arguments to be passed to import-ldif.
   * @return the return code of the import-ldif call.
   * @throws IOException if the process could not be launched.
   * @throws InterruptedException if the process was interrupted.
   */
  public int invokeImportLDIF(final Application application, String[] args)
  throws IOException, InterruptedException
  {
    File installPath = new File(application.getInstallationPath());
    ArrayList<String> argList = new ArrayList<String>();
    File binPath;
    if (Utils.isWindows())
    {
      binPath =
        new File(installPath, Installation.WINDOWS_BINARIES_PATH_RELATIVE);
    } else
    {
      binPath =
        new File(installPath, Installation.UNIX_BINARIES_PATH_RELATIVE);
    }
    File importPath;
    if (Utils.isWindows())
    {
      importPath = new File(binPath, Installation.WINDOWS_IMPORT_LDIF);
    } else
    {
      importPath = new File(binPath, Installation.UNIX_IMPORT_LDIF);
    }
    argList.add(Utils.getScriptPath(importPath.getAbsolutePath()));
    for (String arg : args)
    {
      argList.add(arg);
    }
    String[] allArgs = new String[argList.size()];
    argList.toArray(allArgs);
    LOG.log(Level.INFO, "import-ldif arg list: "+argList);
    ProcessBuilder pb = new ProcessBuilder(allArgs);
    Map<String, String> env = pb.environment();
    env.remove(SetupUtils.OPENDS_JAVA_HOME);
    env.remove(SetupUtils.OPENDS_JAVA_ARGS);
    pb.directory(installPath);
    Process process = null;
    try
    {
      process = pb.start();
      final BufferedReader err =
        new BufferedReader(new InputStreamReader(process.getErrorStream()));
      new OutputReader(err)
      {
        public void processLine(String line)
        {
          LOG.log(Level.WARNING, "import-ldif error log: "+line);
          application.notifyListeners(Message.raw(line));
          application.notifyListeners(application.getLineBreak());
        }
      };
      BufferedReader out =
        new BufferedReader(new InputStreamReader(process.getInputStream()));
      new OutputReader(out)
      {
        public void processLine(String line)
        {
          LOG.log(Level.INFO, "import-ldif out log: "+line);
          application.notifyListeners(Message.raw(line));
          application.notifyListeners(application.getLineBreak());
        }
      };
      return process.waitFor();
    }
    finally
    {
      if (process != null)
      {
        try
        {
          process.getErrorStream().close();
        }
        catch (Throwable t)
        {
          LOG.log(Level.WARNING, "Error closing error stream: "+t, t);
        }
        try
        {
          process.getOutputStream().close();
        }
        catch (Throwable t)
        {
          LOG.log(Level.WARNING, "Error closing output stream: "+t, t);
        }
      }
    }
  }

  /**
   * Returns the Message ID that corresponds to a successfully started server.
   * @return the Message ID that corresponds to a successfully started server.
   */
  public String getStartedId()
  {
    return String.valueOf(CoreMessages.NOTE_DIRECTORY_SERVER_STARTED.getId());
  }

  /**
   * This methods enables this server as a Windows service.
   * @throws ApplicationException if something goes wrong.
   */
  public void enableWindowsService() throws ApplicationException {
    int code = ConfigureWindowsService.enableService(System.out, System.err);

    Message errorMessage = INFO_ERROR_ENABLING_WINDOWS_SERVICE.get();

    switch (code) {
      case
        ConfigureWindowsService.SERVICE_ENABLE_SUCCESS:
        break;
      case
        ConfigureWindowsService.SERVICE_ALREADY_ENABLED:
        break;
      default:
        throw new ApplicationException(
            ReturnCode.WINDOWS_SERVICE_ERROR,
                errorMessage, null);
    }
  }

  /**
   * This method disables this server as a Windows service.
   * @throws ApplicationException if something goes worong.
   */
  public void disableWindowsService() throws ApplicationException
  {
    int code = ConfigureWindowsService.disableService(System.out, System.err);
    if (code == ConfigureWindowsService.SERVICE_DISABLE_ERROR) {
      throw new ApplicationException(
          // TODO: fix this message's format string
          ReturnCode.WINDOWS_SERVICE_ERROR,
              INFO_ERROR_DISABLING_WINDOWS_SERVICE.get(""), null);
    }
  }

  /**
   * Creates a template LDIF file with an entry that has as dn the provided
   * baseDn.
   * @param baseDn the dn of the entry that will be created in the LDIF file.
   * @return the File object pointing to the created temporary file.
   * @throws ApplicationException if something goes wrong.
   */
  public File createBaseEntryTempFile(String baseDn)
          throws ApplicationException {
    File ldifFile;
    try
    {
      ldifFile = File.createTempFile("opends-base-entry", ".ldif");
      ldifFile.deleteOnExit();
    } catch (IOException ioe)
    {
      Message failedMsg =
              getThrowableMsg(INFO_ERROR_CREATING_TEMP_FILE.get(), ioe);
      throw new ApplicationException(
          ReturnCode.FILE_SYSTEM_ACCESS_ERROR,
          failedMsg, ioe);
    }

    try
    {
      LDIFExportConfig exportConfig = new LDIFExportConfig(
          ldifFile.getAbsolutePath(), ExistingFileBehavior.OVERWRITE);

      LDIFWriter writer = new LDIFWriter(exportConfig);

      DN dn = DN.decode(baseDn);
      Entry entry = StaticUtils.createEntry(dn);

      writer.writeEntry(entry);
      writer.close();
    } catch (DirectoryException de) {
      throw new ApplicationException(
          ReturnCode.CONFIGURATION_ERROR,
              getThrowableMsg(INFO_ERROR_IMPORTING_LDIF.get(), de), de);
    } catch (LDIFException le) {
      throw new ApplicationException(
          ReturnCode.CONFIGURATION_ERROR,
              getThrowableMsg(INFO_ERROR_IMPORTING_LDIF.get(), le), le);
    } catch (IOException ioe) {
      throw new ApplicationException(
          ReturnCode.CONFIGURATION_ERROR,
              getThrowableMsg(INFO_ERROR_IMPORTING_LDIF.get(), ioe), ioe);
    } catch (Throwable t) {
      throw new ApplicationException(
          ReturnCode.BUG, getThrowableMsg(
              INFO_BUG_MSG.get(), t), t);
    }
    return ldifFile;
  }

  /**
   * Deletes a backend on the server.
   * @param ctx the connection to the server.
   * @param backendName the name of the backend to be deleted.
   * @param serverDisplay the server display.
   * @throws ApplicationException if something goes wrong.
   */
  public void deleteBackend(InitialLdapContext ctx, String backendName,
      String serverDisplay)
  throws ApplicationException
  {
    try
    {
      ManagementContext mCtx = LDAPManagementContext.createFromContext(
          JNDIDirContextAdaptor.adapt(ctx));
      RootCfgClient root = mCtx.getRootConfiguration();
      root.removeBackend(backendName);
    }
    catch (Throwable t)
    {
      Message errorMessage = INFO_ERROR_CONFIGURING_REMOTE_GENERIC.get(
              serverDisplay, t.toString());
      throw new ApplicationException(
          ReturnCode.CONFIGURATION_ERROR, errorMessage,
          t);
    }
  }


  /**
   * Creates a local database backend on the server.
   * @param ctx the connection to the server.
   * @param backendName the name of the backend to be created.
   * @param baseDNs the list of base DNs to be defined on the server.
   * @param serverDisplay the server display.
   * @throws ApplicationException if something goes wrong.
   */
  public void createLocalDBBackend(InitialLdapContext ctx,
      String backendName,
      Set<String> baseDNs,
      String serverDisplay)
  throws ApplicationException
  {
    try
    {
      ManagementContext mCtx = LDAPManagementContext.createFromContext(
          JNDIDirContextAdaptor.adapt(ctx));
      RootCfgClient root = mCtx.getRootConfiguration();
      LocalDBBackendCfgDefn provider = LocalDBBackendCfgDefn.getInstance();
      LocalDBBackendCfgClient backend = root.createBackend(provider,
          backendName, null);
      backend.setEnabled(true);
      Set<DN> setBaseDNs = new HashSet<DN>();
      for (String baseDN : baseDNs)
      {
        setBaseDNs.add(DN.decode(baseDN));
      }
      backend.setBaseDN(setBaseDNs);
      backend.setBackendId(backendName);
      backend.setWritabilityMode(BackendCfgDefn.WritabilityMode.ENABLED);
      backend.commit();
    }
    catch (Throwable t)
    {
      Message errorMessage = INFO_ERROR_CONFIGURING_REMOTE_GENERIC.get(
              serverDisplay, t.toString());
      throw new ApplicationException(
          ReturnCode.CONFIGURATION_ERROR, errorMessage,
          t);
    }
  }

  /**
   * Sets the base DNs on a given backend.
   * @param ctx the connection to the server.
   * @param backendName the name of the backend where the base Dns must be
   * defined.
   * @param baseDNs the list of base DNs to be defined on the server.
   * @param serverDisplay the server display.
   * @throws ApplicationException if something goes wrong.
   */
  public void setBaseDns(InitialLdapContext ctx,
      String backendName,
      Set<String> baseDNs,
      String serverDisplay)
  throws ApplicationException
  {
    try
    {
      ManagementContext mCtx = LDAPManagementContext.createFromContext(
          JNDIDirContextAdaptor.adapt(ctx));
      RootCfgClient root = mCtx.getRootConfiguration();
      BackendCfgClient backend = root.getBackend(backendName);
      Set<DN> setBaseDNs = new HashSet<DN>();
      for (String baseDN : baseDNs)
      {
        setBaseDNs.add(DN.decode(baseDN));
      }
      backend.setBaseDN(setBaseDNs);
      backend.commit();
    }
    catch (Throwable t)
    {
      Message errorMessage = INFO_ERROR_CONFIGURING_REMOTE_GENERIC.get(
              serverDisplay, t.toString());
      throw new ApplicationException(
          ReturnCode.CONFIGURATION_ERROR, errorMessage,
          t);
    }
  }

  /**
   * Configures the replication on a given server.
   * @param remoteCtx the conection to the server where we want to configure
   * the replication.
   * @param replicationServers a Map where the key value is the base dn and
   * the value is the list of replication servers for that base dn (or domain).
   * @param replicationPort the replicationPort of the server that is being
   * configured (it might not exist and the user specified it in the setup).
   * @param useSecureReplication whether to encrypt connections with the
   * replication port or not.
   * @param serverDisplay the server display.
   * @param usedReplicationServerIds the list of replication server ids that
   * are already used.
   * @param usedServerIds the list of server ids (domain ids) that
   * are already used.
   * @throws ApplicationException if something goes wrong.
   * @return a ConfiguredReplication object describing what has been configured.
   */
  public ConfiguredReplication configureReplication(
      InitialLdapContext remoteCtx, Map<String,Set<String>> replicationServers,
      int replicationPort, boolean useSecureReplication, String serverDisplay,
      Set<Integer> usedReplicationServerIds, Set<Integer> usedServerIds)
  throws ApplicationException
  {
    boolean synchProviderCreated;
    boolean synchProviderEnabled;
    boolean replicationServerCreated;
    boolean secureReplicationEnabled;
    try
    {
      ManagementContext mCtx = LDAPManagementContext.createFromContext(
          JNDIDirContextAdaptor.adapt(remoteCtx));
      RootCfgClient root = mCtx.getRootConfiguration();

      /*
       * Configure Synchronization plugin.
       */
      ReplicationSynchronizationProviderCfgClient sync = null;
      try
      {
        sync = (ReplicationSynchronizationProviderCfgClient)
        root.getSynchronizationProvider("Multimaster Synchronization");
      }
      catch (ManagedObjectNotFoundException monfe)
      {
        // It does not exist.
      }
      if (sync == null)
      {
        ReplicationSynchronizationProviderCfgDefn provider =
          ReplicationSynchronizationProviderCfgDefn.getInstance();
        sync = root.createSynchronizationProvider(provider,
            "Multimaster Synchronization",
            new ArrayList<DefaultBehaviorException>());
        sync.setJavaClass(
            org.opends.server.replication.plugin.MultimasterReplication.class.
            getName());
        sync.setEnabled(Boolean.TRUE);
        synchProviderCreated = true;
        synchProviderEnabled = false;
      }
      else
      {
        synchProviderCreated = false;
        if (!sync.isEnabled())
        {
          sync.setEnabled(Boolean.TRUE);
          synchProviderEnabled = true;
        }
        else
        {
          synchProviderEnabled = false;
        }
      }
      sync.commit();

      /*
       * Configure the replication server.
       */
      ReplicationServerCfgClient replicationServer = null;

      if (!sync.hasReplicationServer())
      {
        if (useSecureReplication)
        {
         CryptoManagerCfgClient crypto = root.getCryptoManager();
         if (!crypto.isSSLEncryption())
         {
           crypto.setSSLEncryption(true);
           crypto.commit();
           secureReplicationEnabled = true;
         }
         else
         {
           // Only mark as true if we actually change the configuration
           secureReplicationEnabled = false;
         }
        }
        else
        {
          secureReplicationEnabled = false;
        }
        int id = getReplicationId(usedReplicationServerIds);
        usedReplicationServerIds.add(id);
        replicationServer = sync.createReplicationServer(
            ReplicationServerCfgDefn.getInstance(),
            new ArrayList<DefaultBehaviorException>());
        replicationServer.setReplicationServerId(id);
        replicationServer.setReplicationPort(replicationPort);
        replicationServerCreated = true;
      }
      else
      {
        secureReplicationEnabled = false;
        replicationServer = sync.getReplicationServer();
        usedReplicationServerIds.add(
            replicationServer.getReplicationServerId());
        replicationServerCreated = false;
      }

      Set<String> servers = replicationServer.getReplicationServer();
      if (servers == null)
      {
        servers = new HashSet<String>();
      }
      Set<String> oldServers = new HashSet<String>();
      oldServers.addAll(servers);
      for (Set<String> rs : replicationServers.values())
      {
        servers.addAll(rs);
      }

      replicationServer.setReplicationServer(servers);
      replicationServer.commit();

      Set<String> newReplicationServers = new HashSet<String>();
      newReplicationServers.addAll(servers);
      newReplicationServers.removeAll(oldServers);

      /*
       * Create the domains
       */
      String[] domainNames = sync.listReplicationDomains();
      if (domainNames == null)
      {
        domainNames = new String[]{};
      }
      Set<ConfiguredDomain> domainsConf = new HashSet<ConfiguredDomain>();
      ReplicationDomainCfgClient[] domains =
        new ReplicationDomainCfgClient[domainNames.length];
      for (int i=0; i<domains.length; i++)
      {
        domains[i] = sync.getReplicationDomain(domainNames[i]);
      }
      for (String dn : replicationServers.keySet())
      {
        ReplicationDomainCfgClient domain = null;
        boolean isCreated;
        String domainName = null;
        for (int i=0; i<domains.length && (domain == null); i++)
        {
          if (areDnsEqual(dn,
              domains[i].getBaseDN().toString()))
          {
            domain = domains[i];
            domainName = domainNames[i];
          }
        }
        if (domain == null)
        {
          int domainId = getReplicationId(usedServerIds);
          usedServerIds.add(domainId);
          domainName = getDomainName(domainNames, domainId, dn);
          domain = sync.createReplicationDomain(
              ReplicationDomainCfgDefn.getInstance(), domainName,
              new ArrayList<DefaultBehaviorException>());
          domain.setServerId(domainId);
          domain.setBaseDN(DN.decode(dn));
          isCreated = true;
        }
        else
        {
          isCreated = false;
        }
        oldServers = domain.getReplicationServer();
        if (oldServers == null)
        {
          oldServers = new TreeSet<String>();
        }
        servers = replicationServers.get(dn);
        domain.setReplicationServer(servers);
        usedServerIds.add(domain.getServerId());

        domain.commit();
        Set<String> addedServers = new TreeSet<String>();
        addedServers.addAll(servers);
        addedServers.removeAll(oldServers);
        ConfiguredDomain domainConf = new ConfiguredDomain(domainName,
            isCreated, addedServers);
        domainsConf.add(domainConf);
      }
      return new ConfiguredReplication(synchProviderCreated,
          synchProviderEnabled, replicationServerCreated,
          secureReplicationEnabled, newReplicationServers,
          domainsConf);
    }
    catch (Throwable t)
    {
      Message errorMessage = INFO_ERROR_CONFIGURING_REMOTE_GENERIC.get(
              serverDisplay, t.toString());
      throw new ApplicationException(
          ReturnCode.CONFIGURATION_ERROR, errorMessage,
          t);
    }
  }

  /**
   * Configures the replication on a given server.
   * @param remoteCtx the conection to the server where we want to configure
   * the replication.
   * @param replConf the object describing what was configured.
   * @param serverDisplay the server display.
   * @throws ApplicationException if something goes wrong.
   */
  public void unconfigureReplication(
      InitialLdapContext remoteCtx, ConfiguredReplication replConf,
      String serverDisplay)
  throws ApplicationException
  {
    try
    {
      ManagementContext mCtx = LDAPManagementContext.createFromContext(
          JNDIDirContextAdaptor.adapt(remoteCtx));
      RootCfgClient root = mCtx.getRootConfiguration();

      /*
       * Unconfigure Synchronization plugin.
       */
      if (replConf.isSynchProviderCreated())
      {
        try
        {
          root.removeSynchronizationProvider("Multimaster Synchronization");
        }
        catch (ManagedObjectNotFoundException monfe)
        {
          // It does not exist.
        }
      }
      else
      {
        try
        {
          ReplicationSynchronizationProviderCfgClient sync =
            (ReplicationSynchronizationProviderCfgClient)
            root.getSynchronizationProvider("Multimaster Synchronization");
          if (replConf.isSynchProviderEnabled())
          {
            sync.setEnabled(Boolean.FALSE);
          }
          if (replConf.isReplicationServerCreated())
          {
            sync.removeReplicationServer();
          }
          else if (sync.hasReplicationServer())
          {
            ReplicationServerCfgClient replicationServer =
              sync.getReplicationServer();
            Set<String> replServers = replicationServer.getReplicationServer();
            if (replServers != null)
            {
              replServers.removeAll(replConf.getNewReplicationServers());
              replicationServer.setReplicationServer(replServers);
              replicationServer.commit();
            }
          }
          for (ConfiguredDomain domain : replConf.getDomainsConf())
          {
            if (domain.isCreated())
            {
              sync.removeReplicationDomain(domain.getDomainName());
            }
            else
            {
              try
              {
                ReplicationDomainCfgClient d =
                  sync.getReplicationDomain(domain.getDomainName());
                Set<String> replServers = d.getReplicationServer();
                if (replServers != null)
                {
                  replServers.removeAll(domain.getAddedReplicationServers());
                  d.setReplicationServer(replServers);
                  d.commit();
                }
              }
              catch (ManagedObjectNotFoundException monfe)
              {
                // It does not exist.
              }
            }
          }
          sync.commit();
        }
        catch (ManagedObjectNotFoundException monfe)
        {
          // It does not exist.
        }
      }
      if (replConf.isSecureReplicationEnabled())
      {
        CryptoManagerCfgClient crypto = root.getCryptoManager();
        if (crypto.isSSLEncryption())
        {
          crypto.setSSLEncryption(false);
          crypto.commit();
        }
      }
    }
    catch (Throwable t)
    {
      Message errorMessage = INFO_ERROR_CONFIGURING_REMOTE_GENERIC.get(
              serverDisplay, t.toString());
      throw new ApplicationException(
          ReturnCode.CONFIGURATION_ERROR, errorMessage,
          t);
    }
  }

  /**
   * For the given state provided by a Task tells if the task is done or not.
   * @param sState the String representing the task state.
   * @return <CODE>true</CODE> if the task is done and <CODE>false</CODE>
   * otherwise.
   */
  public boolean isDone(String sState)
  {
    TaskState state = TaskState.fromString(sState);
    return TaskState.isDone(state);
  }

  /**
   * For the given state provided by a Task tells if the task is successful or
   * not.
   * @param sState the String representing the task state.
   * @return <CODE>true</CODE> if the task is successful and <CODE>false</CODE>
   * otherwise.
   */
  public boolean isSuccessful(String sState)
  {
    TaskState state = TaskState.fromString(sState);
    return TaskState.isSuccessful(state);
  }

  /**
   * For the given state provided by a Task tells if the task is complete with
   * errors or not.
   * @param sState the String representing the task state.
   * @return <CODE>true</CODE> if the task is complete with errors and
   * <CODE>false</CODE> otherwise.
   */
  public boolean isCompletedWithErrors(String sState)
  {
    TaskState state = TaskState.fromString(sState);
    return state == TaskState.COMPLETED_WITH_ERRORS;
  }

  /**
   * For the given state provided by a Task tells if the task is stopped by
   * error or not.
   * @param sState the String representing the task state.
   * @return <CODE>true</CODE> if the task is stopped by error and
   * <CODE>false</CODE> otherwise.
   */
  public boolean isStoppedByError(String sState)
  {
    TaskState state = TaskState.fromString(sState);
    return state == TaskState.STOPPED_BY_ERROR;
  }

  /**
   * Tells whether the provided log message corresponds to a peers not found
   * error during the initialization of a replica or not.
   * @param logMsg the log message.
   * @return <CODE>true</CODE> if the log message corresponds to a peers not
   * found error during initialization and <CODE>false</CODE> otherwise.
   */
  public boolean isPeersNotFoundError(String logMsg)
  {
    return logMsg.indexOf(
        "="+ReplicationMessages.
                ERR_NO_REACHABLE_PEER_IN_THE_DOMAIN.getId()) != -1;
  }

  /**
   * Returns the ID to be used for a new replication server or domain.
   * @param usedIds the list of already used ids.
   * @return the ID to be used for a new replication server or domain.
   */
  public static int getReplicationId(Set<Integer> usedIds)
  {
    Random r = new Random();
    int id = 0;
    while ((id == 0) || usedIds.contains(id))
    {
      id = r.nextInt(MAX_ID_VALUE);
    }
    return id;
  }

  /**
   * Returns the name to be used for a new replication domain.
   * @param existingDomains the existing domains names.
   * @param newDomainId the new domain replication id.
   * @param baseDN the base DN of the domain.
   * @return the name to be used for a new replication domain.
   */
  public static String getDomainName(String[] existingDomains, int newDomainId,
      String baseDN)
  {
    String domainName = baseDN + " ("+DOMAIN_BASE_NAME+newDomainId+")";
    boolean nameExists = true;
    int j = 0;
    while (nameExists)
    {
      boolean found = false;
      for (int i=0; i<existingDomains.length && !found; i++)
      {
        found = existingDomains[i].equalsIgnoreCase(domainName);
      }
      if (found)
      {
        domainName = DOMAIN_BASE_NAME+newDomainId+"-"+j;
      }
      else
      {
        nameExists = false;
      }
      j++;
    }
    return domainName;
  }

  /**
   * Writes the set-java-home file that is used by the scripts to set the
   * java home and the java arguments.
   * @param installPath the install path of the server.
   * @throws IOException if an error occurred writing the file.
   */
  public void writeSetOpenDSJavaHome(String installPath) throws IOException
  {
    String javaHome = System.getProperty("java.home");
    if ((javaHome == null) || (javaHome.length() == 0))
    {
      javaHome = System.getenv(SetupUtils.OPENDS_JAVA_HOME);
    }

    String configDir = Utils.getPath(Utils
        .getInstancePathFromClasspath(installPath),
        Installation.CONFIG_PATH_RELATIVE);
    String propertiesFile = Utils.getPath(
        configDir, Installation.DEFAULT_JAVA_PROPERTIES_FILE);
    boolean propertiesFileModified = false;
    FileInputStream fs = null;
    try
    {
      fs = new FileInputStream(propertiesFile);
      Properties properties = new Properties();
      properties.load(fs);
      propertiesFileModified = properties.keySet().size() > 0;
    }
    catch (Throwable t)
    {
    }
    finally
    {
      if (fs != null)
      {
        try
        {
          fs.close();
        }
        catch (Throwable t)
        {
        }
      }
    }
    BufferedWriter writer = new BufferedWriter(new FileWriter(propertiesFile,
        true));

    if (!propertiesFileModified)
    {
      writer.newLine();
      writer.write("overwrite-env-java-home=false");
      writer.newLine();
      writer.write("overwrite-env-java-args=false");
      writer.newLine();
      writer.newLine();
      writer.write("default.java-home="+javaHome);
      writer.newLine();
      writer.newLine();
      boolean supportsClient = supportsClient(javaHome, installPath);
      boolean supportsServer = supportsServer(javaHome, installPath);

      boolean supportsClientInitialHeap =
        supportsOption(INITIAL_CLIENT_HEAP_ARG, javaHome, installPath);
      boolean supportsServerInitialHeap = false;
      // If the current max memory is bigger than the max heap we want to set,
      // assume that the JVM ergonomics are going to be able to allocate enough
      // memory.
      if (Runtime.getRuntime().maxMemory() < SERVER_MAX_HEAP_BYTES)
      {
        supportsServerInitialHeap =
          supportsOption(SERVER_HEAP_ARGS, javaHome, installPath);
      }
      // Scripts to which we will pass -client argument
      String[] clientScripts =
      {
          "backup.online", "base64", "create-rc-script", "dsconfig",
          "dsreplication", "dsframework", "export-ldif.online",
          "import-ldif.online", "ldapcompare", "ldapdelete",
          "ldapmodify", "ldappasswordmodify", "ldapsearch", "list-backends",
          "manage-account", "manage-tasks", "restore.online", "stop-ds",
          "status", "control-panel", "uninstall", "setup"
      };

      // Scripts to which we will pass -server argument
      String[] serverScripts =
      {
          "backup.offline", "encode-password", "export-ldif.offline",
          "import-ldif.offline", "ldif-diff", "ldifmodify", "ldifsearch",
          "make-ldif", "rebuild-index", "restore.offline", "start-ds",
          "upgrade", "verify-index", "dbtest"
      };

      if (supportsServer || supportsServerInitialHeap)
      {
        for (int i=0; i<serverScripts.length; i++)
        {
          writer.newLine();
          String arg = "";
          if (supportsServer)
          {
            arg = "-server";
          }
          if (supportsServerInitialHeap)
          {
            if (arg.length() > 0)
            {
              arg += " ";
            }
            arg += SERVER_HEAP_ARGS;
          }
          writer.write(serverScripts[i]+".java-args="+arg);
        }
      }
      else
      {
        for (int i=0; i<serverScripts.length; i++)
        {
          writer.newLine();
          writer.write(serverScripts[i]+".java-args=");
        }
      }

      if (supportsClient || supportsClientInitialHeap)
      {
        for (int i=0; i<clientScripts.length; i++)
        {
          writer.newLine();
          String arg = "";
          if (supportsClient)
          {
            arg = "-client";
          }
          if (supportsClientInitialHeap)
          {
            if (arg.length() > 0)
            {
              arg += " ";
            }
            arg += INITIAL_CLIENT_HEAP_ARG;
          }
          writer.write(clientScripts[i]+".java-args="+arg);
        }
      }
      else
      {
        for (int i=0; i<clientScripts.length; i++)
        {
          writer.newLine();
          writer.write(clientScripts[i]+".java-args=");
        }
      }

      if (supportsClient || supportsServer || supportsClientInitialHeap)
      {
        writer.newLine();
        writer.newLine();
      }

      writer.close();
    }
    String destinationFile;
    String libDir = Utils.getPath(Utils.
            getInstancePathFromClasspath(installPath),
        Installation.LIBRARIES_PATH_RELATIVE);
    if (Utils.isWindows())
    {
      destinationFile = Utils.getPath(libDir,
          Installation.SET_JAVA_PROPERTIES_FILE_WINDOWS);
    }
    else
    {
      destinationFile = Utils.getPath(libDir,
          Installation.SET_JAVA_PROPERTIES_FILE_UNIX);
    }

    // Launch the script
    String[] args =
    {
        "--propertiesFile", propertiesFile,
        "--destinationFile", destinationFile,
        "--quiet"
    };

    int returnValue = JavaPropertiesTool.mainCLI(args);

    if ((returnValue !=
      JavaPropertiesTool.ErrorReturnCode.SUCCESSFUL.getReturnCode()) &&
      returnValue !=
        JavaPropertiesTool.ErrorReturnCode.SUCCESSFUL_NOP.getReturnCode())
    {
      LOG.log(Level.WARNING, "Error creating java home scripts, error code: "+
          returnValue);
      throw new IOException(
          ERR_ERROR_CREATING_JAVA_HOME_SCRIPTS.get(returnValue).toString());
    }
  }

  /**
   * If the log message is of type "[03/Apr/2008:21:25:43 +0200] category=JEB
   * severity=NOTICE msgID=8847454 Processed 1 entries, imported 0, skipped 1,
   * rejected 0 and migrated 0 in 1 seconds (average rate 0.0/sec)" returns
   * the message part.  Returns <CODE>null</CODE> otherwise.
   * @param msg the message to be parsed.
   * @return the parsed import message.
   */
  public String getImportProgressMessage(String msg)
  {
    String parsedMsg = null;
    if (msg != null)
    {
      if ((msg.indexOf(
          "msgID="+JebMessages.NOTE_JEB_IMPORT_FINAL_STATUS.getId()) != -1) ||
          (msg.indexOf(
          "msgID="+JebMessages.NOTE_JEB_IMPORT_PROGRESS_REPORT.getId()) != -1))
      {
        int index = msg.indexOf("msg=");
        if (index != -1)
        {
          parsedMsg = msg.substring(index + 4);
        }
      }
    }
    return parsedMsg;
  }

  /**
   * Tells whether the provided java installation supports the server option
   * or not.
   * @param javaHome the java installation path.
   * @param installPath the install path of the server.
   * @return <CODE>true</CODE> if the provided java installation supports the
   * server option and <CODE>false</CODE> otherwise.
   */
  private boolean supportsServer(String javaHome, String installPath)
  {
    return supportsOption("-server", javaHome, installPath);
  }

  /**
   * Tells whether the provided java installation supports the client option
   * or not.
   * @param javaHome the java installation path.
   * @param installPath the install path of the server.
   * @return <CODE>true</CODE> if the provided java installation supports the
   * client option and <CODE>false</CODE> otherwise.
   */
  private boolean supportsClient(String javaHome, String installPath)
  {
    return supportsOption("-client", javaHome, installPath);
  }
}

/**
 * A class describing a replication domain.
 *
 */
class DomainEntry
{
  private String name;
  private int replicationId;
  private String baseDn;
  private Set<String> replicationServers;
  /**
   * The constructor of the domain entry.
   * @param name the name of the domain.
   * @param replicationId the replicationId of the domain.
   * @param baseDn the base dn of the domain.
   * @param replicationServers the list of replication servers for the domain.
   */
  public DomainEntry(String name, int replicationId, String baseDn,
      Set<String> replicationServers)
  {
    this.name = name;
    this.replicationId = replicationId;
    this.baseDn = baseDn;
    this.replicationServers = replicationServers;
  }
  /**
   * Returns the base dn of the domain.
   * @return the base dn of the domain.
   */
  public String getBaseDn()
  {
    return baseDn;
  }
  /**
   * Returns the name of the domain.
   * @return the name of the domain.
   */
  public String getName()
  {
    return name;
  }
  /**
   * Returns the replication Id of the domain.
   * @return the replication Id of the domain.
   */
  public int getReplicationId()
  {
    return replicationId;
  }
  /**
   * Returns the list of replication servers of the domain.
   * @return the list of replication servers of the domain.
   */
  public Set<String> getReplicationServers()
  {
    return replicationServers;
  }
}
