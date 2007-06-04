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

package org.opends.quicksetup.util;

import org.opends.quicksetup.*;
import org.opends.server.loggers.debug.TextDebugLogPublisher;
import org.opends.server.loggers.debug.DebugLogger;
import org.opends.server.loggers.TextErrorLogPublisher;
import org.opends.server.loggers.TextWriter;
import org.opends.server.loggers.ErrorLogger;
import org.opends.server.loggers.TextAccessLogPublisher;
import org.opends.server.loggers.AccessLogger;
import org.opends.server.types.DN;
import org.opends.server.types.Modification;
import org.opends.server.types.ResultCode;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.ByteStringFactory;
import org.opends.server.types.ByteString;
import org.opends.server.types.InitializationException;
import org.opends.server.types.Attribute;
import org.opends.server.types.RawAttribute;
import org.opends.server.api.DebugLogPublisher;
import org.opends.server.api.ErrorLogPublisher;
import org.opends.server.api.AccessLogPublisher;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.opends.server.util.ModifyChangeRecordEntry;
import org.opends.server.util.ChangeRecordEntry;
import org.opends.server.util.AddChangeRecordEntry;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.ldap.LDAPAttribute;
import org.opends.server.core.ModifyOperation;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.AddOperation;
import org.opends.server.core.DeleteOperation;
import org.opends.server.config.ConfigException;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

/**
 * Class used to manipulate an OpenDS server in the same JVM process as
 * the client class.
 *
 * NOTE:  this class imports classes from the server packages.  You should
 * refer to the class using a fully qualified package name and insure that
 * that this class does not appear in any import statements.
 */
public class InProcessServerController {

  static private final Logger LOG =
          Logger.getLogger(InProcessServerController.class.getName());

  /**
   * Indicates that the server has already been started once and that a
   * restart should happen instead.
   */
  static private boolean serverHasBeenStarted = false;

  /**
   * Pushes messages published by the server loggers into OperationOutput.
   */
  static private abstract class ServerControllerTextWriter
          implements TextWriter
  {

    private int bytesWritten = 0;
    private OperationOutput output = null;

    abstract void storeRecord(String record, OperationOutput output);

    ServerControllerTextWriter() {
      // do nothing
    }

    ServerControllerTextWriter(OperationOutput output) {
      setOutput(output);
    }

    public void setOutput(OperationOutput ouput) {
      this.output = ouput;
    }

    public void writeRecord(String record) {
      if (record != null) {
        bytesWritten += bytesWritten;
        if (output != null) {
          storeRecord(record, output);
        }
      }
    }

    public void flush() {
      // do nothing;
    }

    public void shutdown() {
      // do nothing;
    }

    public long getBytesWritten() {
      return bytesWritten;
    }
  }

  static private ServerControllerTextWriter debugWriter =
          new ServerControllerTextWriter() {
    void storeRecord(String record,
                     OperationOutput output) {
      LOG.log(Level.INFO, "server start (debug log): " +
              record);
      output.addDebugMessage(record);
    }};

  static private ServerControllerTextWriter errorWriter =
          new ServerControllerTextWriter() {
    void storeRecord(String record,
                     OperationOutput output) {
      LOG.log(Level.INFO, "server start (error log): " +
              record);
      output.addErrorMessage(record);
    }
  };

  static private ServerControllerTextWriter accessWriter =
          new ServerControllerTextWriter() {
    void storeRecord(String record,
                     OperationOutput output) {
      LOG.log(Level.INFO, "server start (access log): " +
              record);
      output.addAccessMessage(record);
    }
  };

  private Installation installation;

  /**
   * Creates a new instance that will operate on <code>application</code>'s
   * installation.
   * @param installation representing the server instance to control
   * @throws IllegalStateException if the the version of the OpenDS code
   * running in this JVM is not the same version as the code whose bits
   * are stored in <code>installation</code>.
   */
  public InProcessServerController(Installation installation)
          throws IllegalStateException
  {

    // Attempting to use DirectoryServer with a configuration file
    // for a different version of the server can cause problems for
    // the server at startup.
    BuildInformation installBi = null;
    BuildInformation currentBi = null;
    try {
      installBi = installation.getBuildInformation();
      currentBi = BuildInformation.getCurrent();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to verify the build version of " +
              "the " + installation + " matches the currently executing " +
              "version.");
    }

    if (!currentBi.equals(installBi)) {
      throw new IllegalStateException("The build version of the " +
              "installation " + installation + " is " + installBi +
              " and does not match the currently executing version " +
              currentBi);
    }

    this.installation=installation;
  }

  /**
   * Starts the directory server within this process.
   * @param disableConnectionHandlers boolean that when true starts the
   * the server mode that is otherwise up and running but will not accept any
   * connections from external clients (i.e., does not create or initialize the
   * connection handlers). This could be useful, for example, in an upgrade mode
   * where it might be helpful to start the server but don't want it to appear
   * externally as if the server is online without connection handlers
   * listening.
   * @return OperationOutput object containing output from the start server
   * command invocation.
   * @throws org.opends.server.config.ConfigException
   *         If there is a problem with the Directory Server
   *         configuration that prevents a critical component
   *         from being instantiated.
   *
   * @throws org.opends.server.types.InitializationException
   *         If some other problem occurs while
   *         attempting to initialize and start the
   *         Directory Server.
   */
  public OperationOutput startServer(boolean disableConnectionHandlers)
          throws InitializationException, ConfigException
  {
    LOG.log(Level.INFO, "Starting in process server with connection handlers " +
            (disableConnectionHandlers ? "disabled" : "enabled"));
    System.setProperty(
            "org.opends.server.DisableConnectionHandlers",
            disableConnectionHandlers ? "true" : "false");
    return startServer();
  }

  /**
   * Disables the server's connection handlers upon startup.  The server
   * when started is otherwise up and running but will not accept any
   * connections from external clients (i.e., does not create or initialize the
   * connection handlers). This could be useful, for example, in an upgrade mode
   * where it might be helpful to start the server but don't want it to appear
   * externally as if the server is online without connection handlers
   * listening.
   * @param disable boolean that when true disables connection handlers when
   * the server is started.
   */
  static public void disableConnectionHandlers(boolean disable) {
    System.setProperty(
            "org.opends.server.DisableConnectionHandlers",
            disable ? "true" : "false");
  }

  /**
   * Stops a server that had been running 'in process'.
   */
  public void stopServer() {
    LOG.log(Level.INFO, "Shutting down in process server");
    StandardOutputSuppressor.suppress();
    try {
      DirectoryServer.shutDown(getClass().getName(),
              "quicksetup requests shutdown");

      // Note:  this should not be necessary in the future when a
      // the shutdown method will not return until everything is
      // cleaned up.

      // Connection handlers are stopped and started asynchonously.
      // Give the connection handlers time to let go of any resources
      // before continuing.
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        // do nothing;
      }

    } finally {
      StandardOutputSuppressor.unsuppress();
    }
  }

  /**
   * Starts the OpenDS server in this process.
   * @return OperationOutput with the results of the operation.
   * @throws org.opends.server.config.ConfigException
   *  If there is a problem with the Directory Server
   *  configuration that prevents a critical component
   *  from being instantiated.
   *
   * @throws org.opends.server.types.InitializationException
   *  If some other problem occurs while
   *  attempting to initialize and start the
   *  Directory Server.
   */
  public synchronized OperationOutput startServer()
          throws
          InitializationException,
          ConfigException {
    OperationOutput output = new OperationOutput();
    setOutputForWriters(output);

    // Properties systemProperties = System.getProperties();
    // systemProperties.list(System.out);

    StandardOutputSuppressor.suppress();
    try {

      // The server's startServer method should not be called directly
      // more than once since it leave the server corrupted.  Restart
      // is the correct choice in this case.
      if (!serverHasBeenStarted) {
        DirectoryServer directoryServer = DirectoryServer.getInstance();

        // Bootstrap and start the Directory Server.
        LOG.log(Level.FINER, "Bootstrapping directory server");
        directoryServer.bootstrapServer();

        LOG.log(Level.FINER, "Initializing configuration");
        String configClass = "org.opends.server.extensions.ConfigFileHandler";
        String configPath = Utils.getPath(
                installation.getCurrentConfigurationFile());

        directoryServer.initializeConfiguration(configClass, configPath);

      } else {
        LOG.log(Level.FINER, "Reinitializing the server");
        DirectoryServer.reinitialize();
      }

      // Must be done following bootstrap
      registerListenersForOuput();

      LOG.log(Level.FINER, "Invoking server start");

      // It is important to get a new instance after calling reinitalize()
      DirectoryServer directoryServer = DirectoryServer.getInstance();

      directoryServer.startServer();
      serverHasBeenStarted = true;

      // Note:  this should not be necessary in the future.  This
      // seems necessary currenty for the case in which shutdown
      // is called immediately afterward as is done by the upgrader.

      // Connection handlers are stopped and started asynchonously.
      // Give the connection handlers time to initialize before
      // continuing.

      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        // do nothing;
      }

    } finally {
      StandardOutputSuppressor.unsuppress();
      setOutputForWriters(null);
    }
    return output;
  }

  /**
   * Applies modifications contained in an LDIF file to the server.
   *
   * @param ldifFile LDIF file to apply
   * @throws IOException if there is an IO Error
   * @throws LDIFException if there is an LDIF error
   * @throws ApplicationException if there is an application specific error
   */
  public void modify(File ldifFile)
          throws IOException, LDIFException,
          ApplicationException {
    try {
      InternalClientConnection cc =
              InternalClientConnection.getRootConnection();
      LDIFImportConfig importCfg =
              new LDIFImportConfig(
                      Utils.getPath(ldifFile));
      LDIFReader ldifReader =
              new LDIFReader(importCfg);
      ChangeRecordEntry cre;
      while (null != (cre = ldifReader.readChangeRecord(false))) {
        ByteString dnByteString =
                ByteStringFactory.create(
                        cre.getDN().toString());
        ResultCode rc;
        switch(cre.getChangeOperationType()) {
        case MODIFY:
          LOG.log(Level.INFO, "proparing to modify " + dnByteString);
          ModifyChangeRecordEntry mcre =
                  (ModifyChangeRecordEntry) cre;
          ModifyOperation op =
                  cc.processModify(dnByteString, mcre.getModifications());
          rc = op.getResultCode();
          if (rc.equals(ResultCode.
                  SUCCESS)) {
            LOG.log(Level.INFO, "processed server modification " +
                            modListToString(op.getModifications()));
          } else if (rc.equals(
                  ResultCode.
                          ATTRIBUTE_OR_VALUE_EXISTS)) {
            // ignore this error
            LOG.log(Level.INFO, "ignoring attribute that already exists: " +
                    modListToString(op.getModifications()));
          } else if (rc.equals(ResultCode.NO_SUCH_ATTRIBUTE)) {
            // This can happen if for instance the old configuration was
            // changed so that the value of an attribute matches the default
            // value of the attribute in the new configuration.
            // Just log it and move on.
            LOG.log(Level.INFO, "Ignoring attribute not found: " +
                    modListToString(op.getModifications()));
          } else {
            // report the error to the user
            StringBuilder error = op.getErrorMessage();
            throw new ApplicationException(
                    ApplicationException.Type.IMPORT_ERROR,
                    "error processing modification of '" +
                            dnByteString + "': " +
                            error != null ? error.toString() : "",
                    null);
          }
          break;
        case ADD:
          LOG.log(Level.INFO, "proparing to add " + dnByteString);
          AddChangeRecordEntry acre = (AddChangeRecordEntry) cre;
          List<Attribute> attrs = acre.getAttributes();
          ArrayList<RawAttribute> rawAttrs =
                  new ArrayList<RawAttribute>(attrs.size());
          for (Attribute a : attrs) {
            rawAttrs.add(new LDAPAttribute(a));
          }
          AddOperation addOp = cc.processAdd(dnByteString, rawAttrs);
          rc = addOp.getResultCode();
          if (rc.equals(ResultCode.SUCCESS)) {
            LOG.log(Level.INFO, "processed server add " + addOp.getEntryDN());
          } else {
            // report the error to the user
            StringBuilder error = addOp.getErrorMessage();
            throw new ApplicationException(
                    ApplicationException.Type.IMPORT_ERROR,
                    "error processing add of '" +
                            dnByteString + "': " +
                            error != null ? error.toString() : "",
                    null);
          }
          break;
        case DELETE:
          LOG.log(Level.INFO, "proparing to delete " + dnByteString);
          DeleteOperation delOp = cc.processDelete(dnByteString);
          rc = delOp.getResultCode();
          if (rc.equals(ResultCode.SUCCESS)) {
            LOG.log(Level.INFO, "processed server delete " +
                    delOp.getEntryDN());
          } else {
            // report the error to the user
            StringBuilder error = delOp.getErrorMessage();
            throw new ApplicationException(
                    ApplicationException.Type.IMPORT_ERROR,
                    "error processing delete of '" +
                            dnByteString + "': " +
                            error != null ? error.toString() : "",
                    null);
          }
          break;
        default:
          throw new ApplicationException(
                  ApplicationException.Type.IMPORT_ERROR,
                  "unexpected change record type " + cre.getClass(),
                  null);
        }
      }
    } catch (Throwable t) {
      throw new ApplicationException(ApplicationException.Type.BUG,
              t.getMessage(), t);
    }
  }

  private String modListToString(
          List<Modification> modifications) {
    StringBuilder modsMsg = new StringBuilder();
    for (int i = 0; i < modifications.size(); i++) {
      modsMsg.append(modifications.get(i).toString());
      if (i < modifications.size() - 1) {
        modsMsg.append(" ");
      }
    }
    return modsMsg.toString();
  }

  static private void setOutputForWriters(OperationOutput output) {
    debugWriter.setOutput(output);
    errorWriter.setOutput(output);
    accessWriter.setOutput(output);
  }

  static private void registerListenersForOuput() {
    try {
      DebugLogPublisher startupDebugPublisher =
              TextDebugLogPublisher.getStartupTextDebugPublisher(debugWriter);
      DebugLogger.addDebugLogPublisher(
              DN.decode("cn=QuickSetup,cn=Loggers,cn=config"),
              startupDebugPublisher);

      ErrorLogPublisher startupErrorPublisher =
              TextErrorLogPublisher.getStartupTextErrorPublisher(errorWriter);
      ErrorLogger.addErrorLogPublisher(
              DN.decode("cn=QuickSetup,cn=Loggers,cn=config"),
              startupErrorPublisher);

      AccessLogPublisher startupAccessPublisher =
              TextAccessLogPublisher.getStartupTextAccessPublisher(
                      accessWriter, true);
      AccessLogger.addAccessLogPublisher(
              DN.decode("cn=QuickSetup,cn=Loggers,cn=config"),
              startupAccessPublisher);

    } catch (Exception e) {
      LOG.log(Level.INFO, "Error installing test log publishers: " +
              e.toString());
    }
  }

  static private void unregisterListenersForOutput() {
    DebugLogger.removeDebugLogPublisher(DN.NULL_DN);
    ErrorLogger.removeErrorLogPublisher(DN.NULL_DN);
    AccessLogger.removeAccessLogPublisher(DN.NULL_DN);
  }

}
