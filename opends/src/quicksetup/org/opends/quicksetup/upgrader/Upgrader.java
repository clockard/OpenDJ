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

package org.opends.quicksetup.upgrader;

import org.opends.quicksetup.*;
import org.opends.quicksetup.upgrader.ui.WelcomePanel;
import org.opends.quicksetup.upgrader.ui.ChooseVersionPanel;
import org.opends.quicksetup.upgrader.ui.UpgraderReviewPanel;
import org.opends.quicksetup.util.Utils;
import org.opends.quicksetup.util.FileManager;
import org.opends.quicksetup.util.ServerController;
import org.opends.quicksetup.util.ZipExtractor;
import org.opends.quicksetup.util.OperationOutput;
import org.opends.quicksetup.ui.*;

import java.awt.event.WindowEvent;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.InetSocketAddress;

import static org.opends.quicksetup.Installation.*;

import javax.swing.*;

/**
 * QuickSetup application of ugrading the bits of an installation of
 * OpenDS.
 */
public class Upgrader extends GuiApplication implements CliApplication {

  /**
   * Steps in the Upgrade wizard.
   */
  enum UpgradeWizardStep implements WizardStep {

    WELCOME("welcome-step"),

    CHOOSE_VERSION("step-upgrade-choose-version"),

    REVIEW("review-step"),

    PROGRESS("progress-step");

    private String msgKey;

    private UpgradeWizardStep(String msgKey) {
      this.msgKey = msgKey;
    }

    /**
     * {@inheritDoc}
     */
    public String getMessageKey() {
      return msgKey;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProgressStep() {
      return this == PROGRESS;
    }

  }

  /**
   * Steps during the upgrade process.
   */
  enum UpgradeProgressStep implements ProgressStep {

    NOT_STARTED("summary-upgrade-not-started", 0),

    DOWNLOADING("summary-upgrade-downloading", 10),

    EXTRACTING("summary-upgrade-extracting", 20),

    INITIALIZING("summary-upgrade-initializing", 30),

    CHECK_SERVER_HEALTH("summary-upgrade-check-server-health", 35),

    CALCULATING_SCHEMA_CUSTOMIZATIONS(
            "summary-upgrade-calculating-schema-customization", 40),

    CALCULATING_CONFIGURATION_CUSTOMIZATIONS(
            "summary-upgrade-calculating-config-customization", 45),

    BACKING_UP_DATABASES("summary-upgrade-backing-up-db", 50),

    BACKING_UP_FILESYSTEM("summary-upgrade-backing-up-files",55),

    UPGRADING_COMPONENTS("summary-upgrade-upgrading-components", 60),

    APPLYING_SCHEMA_CUSTOMIZATIONS(
            "summary-upgrade-applying-schema-customization", 70),

    APPLYING_CONFIGURATION_CUSTOMIZATIONS(
            "summary-upgrade-applying-config-customization", 75),

    VERIFYING("summary-upgrade-verifying", 80),

    RECORDING_HISTORY("summary-upgrade-history", 85),

    CLEANUP("summary-upgrade-cleanup", 90),

    ABORT("summary-upgrade-abort", 95),

    FINISHED_WITH_ERRORS("summary-upgrade-finished-with-errors", 100),

    FINISHED_WITH_WARNINGS("summary-upgrade-finished-with-warnings", 100),

    FINISHED("summary-upgrade-finished-successfully", 100);

    private String summaryMsgKey;
    private int progress;

    private UpgradeProgressStep(String summaryMsgKey, int progress) {
      this.summaryMsgKey = summaryMsgKey;
      this.progress = progress;
    }

    /**
     * Return a key for access a summary message.
     *
     * @return String representing key for access summary in resource bundle
     */
    public String getSummaryMesssageKey() {
      return summaryMsgKey;
    }

    /**
     * Gets the amount of progress to show in the progress meter for this step.
     * @return int representing progress
     */
    public int getProgress() {
      return this.progress;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLast() {
      return this == FINISHED ||
              this == FINISHED_WITH_ERRORS ||
              this == FINISHED_WITH_WARNINGS;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isError() {
      return this == FINISHED_WITH_ERRORS;
    }
  }

  static private final Logger LOG = Logger.getLogger(Upgrader.class.getName());

  /**
   * Passed in from the shell script if the root is known at the time
   * of invocation.
   */
  static private final String SYS_PROP_INSTALL_ROOT =
          "org.opends.quicksetup.upgrader.Root";


  /**
   * If set to true, an error is introduced during the
   * upgrade process for testing.
   */
  static private final String SYS_PROP_CREATE_ERROR =
          "org.opends.upgrader.Upgrader.CreateError";

  /**
   * If set to true, if the upgrader encounters an error
   * during upgrade, the abort method that backs out
   * changes is made a no-op leaving the server in the
   * erroneous state.
   */
  static private final String SYS_PROP_NO_ABORT =
          "org.opends.upgrader.Upgrader.NoAbort";


  // Root files that will be ignored during backup
  static private final String[] ROOT_FILES_TO_IGNORE_DURING_BACKUP = {
          CHANGELOG_PATH_RELATIVE, // changelogDb
          DATABASES_PATH_RELATIVE, // db
          LOGS_PATH_RELATIVE, // logs
          LOCKS_PATH_RELATIVE, // locks
          HISTORY_PATH_RELATIVE, // history
          TMP_PATH_RELATIVE // tmp
  };

  private ProgressStep currentProgressStep = UpgradeProgressStep.NOT_STARTED;

  /**
   * Assigned if an exception occurs during run().
   */
  private ApplicationException runError = null;

  /**
   * Assigned if a non-fatal error happened during the upgrade that the
   * user needs to be warned about during run().
   */
  private ApplicationException runWarning = null;

  /**
   * Helps with CLI specific tasks.
   */
  private UpgraderCliHelper cliHelper = null;

  /**
   * Directory where backup is kept in case the upgrade needs reversion.
   */
  private File backupDirectory = null;

  /**
   * ID that uniquely identifieds this invocation of the Upgrader in the
   * historical logs.
   */
  private Long historicalOperationId;

  /**
   * SVN rev number of the current build.
   */
  private Integer currentVersion = null;

  /**
   * New OpenDS bits.
   */
  private Installation stagedInstallation = null;

  /**
   * SVN rev number of the build in the stage directory.
   */
  private Integer stagedVersion = null;

  private RemoteBuildManager remoteBuildManager = null;

  /** Set to true if the user decides to close the window while running. */
  private boolean abort = false;

  /**
   * Creates a default instance.
   */
  public Upgrader() {

    // Initialize the logs if necessary
    try {
      if (!QuickSetupLog.isInitialized())
        QuickSetupLog.initLogFileHandler(
                File.createTempFile(
                        UpgradeLauncher.LOG_FILE_PREFIX,
                        UpgradeLauncher.LOG_FILE_SUFFIX));
    } catch (IOException e) {
      System.err.println("Failed to initialize log");
    }

    // Get started on downloading the web start jars
    if (Utils.isWebStart()) {
      initLoader();
    }

    final String instanceRootFromSystem =
            System.getProperty(SYS_PROP_INSTALL_ROOT);
    if (instanceRootFromSystem != null) {
      setInstallation(new Installation(instanceRootFromSystem));
    }

  }

  /**
   * {@inheritDoc}
   */
  public String getFrameTitle() {
    return getMsg("frame-upgrade-title");
  }

  /**
   * {@inheritDoc}
   */
  public WizardStep getFirstWizardStep() {
    return UpgradeWizardStep.WELCOME;
  }

  /**
   * {@inheritDoc}
   */
  protected void setWizardDialogState(QuickSetupDialog dlg,
                                      UserData userData,
                                      WizardStep step) {
    // TODO
  }

  /**
   * Gets a remote build manager that this class can use to find
   * out about and download builds for upgrading.
   *
   * @return RemoteBuildManager to use for builds
   */
  public RemoteBuildManager getRemoteBuildManager() {
    if (remoteBuildManager == null) {
      try {
        String listUrlString =
                System.getProperty("org.opends.quicksetup.upgrader.BuildList");
        if (listUrlString == null) {
          listUrlString = "http://www.opends.org/upgrade-builds";
        }
        URL buildRepo = new URL(listUrlString);

        // See if system properties dictate use of a proxy
        Proxy proxy = null;
        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        if (proxyHost != null && proxyPort != null) {
          try {
            SocketAddress addr =
                    new InetSocketAddress(proxyHost, new Integer(proxyPort));
            proxy = new Proxy(Proxy.Type.HTTP, addr);
          } catch (NumberFormatException nfe) {
            LOG.log(Level.INFO, "Illegal proxy port number " + proxyPort);
          }
        }

        remoteBuildManager = new RemoteBuildManager(this, buildRepo, proxy);
      } catch (MalformedURLException e) {
        LOG.log(Level.INFO, "", e);
      }
    }
    return remoteBuildManager;
  }

  /**
   * {@inheritDoc}
   */
  public int getExtraDialogHeight() {
    return UIFactory.EXTRA_DIALOG_HEIGHT;
  }

  /**
   * {@inheritDoc}
   */
  protected String getInstallationPath() {
    // The upgrader runs from the bits extracted by BuildExtractor
    // in the staging directory.  So 'stagePath' below will point
    // to the staging directory [installroot]/tmp/upgrade.  However
    // we still want the Installation to point at the build being
    // upgraded so the install path reported in [installroot].

    String installationPath = null;
    String path = Utils.getInstallPathFromClasspath();
    if (path != null) {
      File f = new File(path);
      if (f.getParentFile() != null &&
              f.getParentFile().getParentFile() != null &&
              new File(f.getParentFile().getParentFile(),
                      Installation.LOCKS_PATH_RELATIVE).exists()) {
        installationPath = Utils.getPath(f.getParentFile().getParentFile());
      } else {
        installationPath = path;
      }
    }
    return installationPath;
  }

  /**
   * {@inheritDoc}
   */
  public ProgressStep getCurrentProgressStep() {
    return currentProgressStep;
  }

  /**
   * {@inheritDoc}
   */
  public Integer getRatio(ProgressStep step) {
    return ((UpgradeProgressStep) step).getProgress();
  }

  /**
   * {@inheritDoc}
   */
  public String getSummary(ProgressStep step) {
    String txt = null;
    if (step == UpgradeProgressStep.FINISHED) {
      txt = getFinalSuccessMessage();
    } else if (step == UpgradeProgressStep.FINISHED_WITH_ERRORS) {
      txt = getFinalErrorMessage();
    } else if (step == UpgradeProgressStep.FINISHED_WITH_WARNINGS) {
      txt = getFinalWarningMessage();
    }
    else {
      txt = getMsg(((UpgradeProgressStep) step).getSummaryMesssageKey());
    }
    return txt;
  }

  /**
   * {@inheritDoc}
   */
  public void windowClosing(QuickSetupDialog dlg, WindowEvent evt) {
    // TODO
  }

  /**
   * {@inheritDoc}
   */
  public ButtonName getInitialFocusButtonName() {
    // TODO
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Set<? extends WizardStep> getWizardSteps() {
    return Collections.unmodifiableSet(EnumSet.allOf(UpgradeWizardStep.class));
  }

  /**
   * {@inheritDoc}
   */
  public QuickSetupStepPanel createWizardStepPanel(WizardStep step) {
    QuickSetupStepPanel pnl = null;
    if (UpgradeWizardStep.WELCOME.equals(step)) {
      pnl = new WelcomePanel(this);
    } else if (UpgradeWizardStep.CHOOSE_VERSION.equals(step)) {
      pnl = new ChooseVersionPanel(this);
    } else if (UpgradeWizardStep.REVIEW.equals(step)) {
      pnl = new UpgraderReviewPanel(this);
    } else if (UpgradeWizardStep.PROGRESS.equals(step)) {
      pnl = new ProgressPanel(this);
    }
    return pnl;
  }

  /**
   * {@inheritDoc}
   */
  public WizardStep getNextWizardStep(WizardStep step) {
    WizardStep next = null;
    if (UpgradeWizardStep.WELCOME.equals(step)) {
      next = UpgradeWizardStep.CHOOSE_VERSION;
    } else if (UpgradeWizardStep.CHOOSE_VERSION.equals(step)) {
      next = UpgradeWizardStep.REVIEW;
    } else if (UpgradeWizardStep.REVIEW.equals(step)) {
      next = UpgradeWizardStep.PROGRESS;
    }
    return next;
  }

  /**
   * {@inheritDoc}
   */
  public WizardStep getPreviousWizardStep(WizardStep step) {
    WizardStep prev = null;
    if (UpgradeWizardStep.PROGRESS.equals(step)) {
      prev = UpgradeWizardStep.REVIEW;
    } else if (UpgradeWizardStep.REVIEW.equals(step)) {
      prev = UpgradeWizardStep.CHOOSE_VERSION;
    } else if (UpgradeWizardStep.CHOOSE_VERSION.equals(step)) {
      prev = UpgradeWizardStep.WELCOME;
    }
    return prev;
  }

  /**
   * {@inheritDoc}
   */
  public boolean canQuit(WizardStep step) {
    return UpgradeWizardStep.WELCOME == step ||
            UpgradeWizardStep.CHOOSE_VERSION == step ||
            UpgradeWizardStep.REVIEW == step;
  }

  /**
   * {@inheritDoc}
   */
  public boolean canClose(WizardStep step) {
    return step == UpgradeWizardStep.PROGRESS;
  }

  /**
   * {@inheritDoc}
   */
  public String getFinishButtonToolTipKey() {
    return "finish-button-upgrade-tooltip";
  }

  /**
   * {@inheritDoc}
   */
  public String getQuitButtonToolTipKey() {
    return "quit-button-upgrade-tooltip";
  }

  /**
   * {@inheritDoc}
   */
  public void closeClicked(WizardStep cStep, final QuickSetup qs) {
    if (cStep == UpgradeWizardStep.PROGRESS) {
      if (isFinished()) {
        qs.quit();
      } else if (qs.displayConfirmation(getMsg("confirm-close-upgrade-msg"),
              getMsg("confirm-close-upgrade-title"))) {
        abort = true;
        JButton btnClose = qs.getDialog().getButtonsPanel().
                getButton(ButtonName.CLOSE);
        btnClose.setEnabled(false);
        new Thread(new Runnable() {
          public void run() {
            while (!isFinished()) {
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
                // do nothing
              }
            }
            qs.quit();
          }
        }).start();
      }
    } else {
      throw new IllegalStateException(
              "Close only can be clicked on PROGRESS step");
    }
  }

  /**
   * {@inheritDoc}
   */
  public boolean isFinished() {
    return getCurrentProgressStep() ==
            UpgradeProgressStep.FINISHED
            || getCurrentProgressStep() ==
            UpgradeProgressStep.FINISHED_WITH_ERRORS
            || getCurrentProgressStep() ==
            UpgradeProgressStep.FINISHED_WITH_WARNINGS;
  }

  /**
   * {@inheritDoc}
   */
  public void quitClicked(WizardStep cStep, final QuickSetup qs) {
    if (cStep == UpgradeWizardStep.PROGRESS) {
      throw new IllegalStateException(
              "Cannot click on quit from progress step");
    } else if (isFinished()) {
      qs.quit();
    } else if (qs.displayConfirmation(getMsg("confirm-quit-upgrade-msg"),
            getMsg("confirm-quit-upgrade-title"))) {
      qs.quit();
    }
  }

  /**
   * {@inheritDoc}
   */
  public void updateUserData(WizardStep cStep, QuickSetup qs)
          throws UserDataException {
    List<String> errorMsgs = new ArrayList<String>();
    UpgradeUserData uud = getUpgradeUserData();
    if (cStep == UpgradeWizardStep.WELCOME) {

      // User can only select the installation to upgrade
      // in the webstart version of this tool.  Otherwise
      // the fields are readonly.
      if (Utils.isWebStart()) {
        String serverLocationString =
                qs.getFieldStringValue(FieldName.SERVER_LOCATION);
        if ((serverLocationString == null) ||
                ("".equals(serverLocationString.trim()))) {
          errorMsgs.add(getMsg("empty-server-location"));
          qs.displayFieldInvalid(FieldName.SERVER_LOCATION, true);
        } else {
          try {
            File serverLocation = new File(serverLocationString);
            Installation.validateRootDirectory(serverLocation);

            // If we get here the value is acceptable and not null

            Installation currentInstallation = getInstallation();
            if (currentInstallation == null ||
                !serverLocation.equals(getInstallation().getRootDirectory())) {
              LOG.log(Level.INFO,
                      "user changed server root from " +
                      (currentInstallation == null ?
                              "'null'" :
                              currentInstallation.getRootDirectory()) +
                      " to " + serverLocation);
              Installation installation = new Installation(serverLocation);
              setInstallation(installation);
            }

            uud.setServerLocation(serverLocationString);

          } catch (IllegalArgumentException iae) {
            LOG.log(Level.INFO,
                    "illegal OpenDS installation directory selected", iae);
            errorMsgs.add(getMsg("error-invalid-server-location",
                    serverLocationString));
            qs.displayFieldInvalid(FieldName.SERVER_LOCATION, true);
          }
        }
      } else {
        // do nothing; all fields are read-only
      }

    } else if (cStep == UpgradeWizardStep.CHOOSE_VERSION) {
      Build buildToDownload = null;
      File buildFile = null;
      Boolean downloadFirst =
              (Boolean) qs.getFieldValue(FieldName.UPGRADE_DOWNLOAD);
      if (downloadFirst) {
        buildToDownload =
                (Build) qs.getFieldValue(FieldName.UPGRADE_BUILD_TO_DOWNLOAD);
      } else {
        buildFile = (File) qs.getFieldValue(FieldName.UPGRADE_FILE);
        if (buildFile == null) {
          errorMsgs.add("You must specify a path to an OpenDS build file");
        } else if (!buildFile.exists()) {
          errorMsgs.add("File " + Utils.getPath(buildFile) +
                  " does not exist.");
          qs.displayFieldInvalid(FieldName.UPGRADE_FILE, true);
        }
      }
      uud.setBuildToDownload(buildToDownload);
      uud.setInstallPackage(buildFile);
    } else if (cStep == UpgradeWizardStep.REVIEW) {
      Boolean startServer =
              (Boolean) qs.getFieldValue(FieldName.SERVER_START);
      uud.setStartServer(startServer);
    }

    if (errorMsgs.size() > 0) {
      throw new UserDataException(Step.SERVER_SETTINGS,
              Utils.getStringFromCollection(errorMsgs, "\n"));
    }

  }

  /**
   * {@inheritDoc}
   */
  public void previousClicked(WizardStep cStep, QuickSetup qs) {
  }

  /**
   * {@inheritDoc}
   */
  public boolean finishClicked(final WizardStep cStep, final QuickSetup qs) {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  public void nextClicked(WizardStep cStep, QuickSetup qs) {
  }

  /**
   * {@inheritDoc}
   */
  public boolean canFinish(WizardStep step) {
    boolean cf = UpgradeWizardStep.REVIEW.equals(step);
    return cf;
  }

  /**
   * {@inheritDoc}
   */
  public boolean canGoBack(WizardStep step) {
    return super.canGoBack(step) && !step.equals(UpgradeWizardStep.PROGRESS);
  }

  /**
   * {@inheritDoc}
   */
  public void run() {
    // Reset exception just in case this application is rerun
    // for some reason
    runError = null;

    try {

      if (Utils.isWebStart()) {
        try {
          LOG.log(Level.INFO, "waiting for Java Web Start jar download");
          waitForLoader(15); // TODO: ratio
        } catch (ApplicationException e) {
          LOG.log(Level.SEVERE, "Error downloading WebStart jars", e);
          throw e;
        }
      }

      checkAbort();

      File buildZip;
      Build buildToDownload =
              getUpgradeUserData().getInstallPackageToDownload();
      if (buildToDownload != null) {
        try {
          LOG.log(Level.INFO, "build to download " + buildToDownload);
          setCurrentProgressStep(UpgradeProgressStep.DOWNLOADING);
          buildZip = new File(getStageDirectory(), "OpenDS.zip");
          if (buildZip.exists()) {
            LOG.log(Level.INFO, "build file " + buildZip.getName() +
                    " already exists");
            if (!buildZip.delete()) {
              LOG.log(Level.WARNING, "removal of existing build file failed");
              throw ApplicationException.createFileSystemException(
                      "Could not delete existing build file " +
                              Utils.getPath(buildZip), null);
            }
          }
          LOG.log(Level.FINE, "Preparing to download " +
                  buildToDownload.getUrl() +
                  " to " + Utils.getPath(buildZip));
          try {
            getRemoteBuildManager().download(buildToDownload, buildZip);
          } catch (IOException e) {
            throw new ApplicationException(
                    ApplicationException.Type.APPLICATION,
                    "Failed to download build package .zip " +
                            "file from " + buildToDownload.getUrl(), e);
          }
          LOG.log(Level.INFO, "download finished");
          notifyListeners(formatter.getFormattedDone() +
                  formatter.getLineBreak());
        } catch (ApplicationException e) {
          LOG.log(Level.INFO, "Error downloading build file", e);
          throw e;
        }
      } else {
        buildZip = getUpgradeUserData().getInstallPackage();
        LOG.log(Level.INFO, "will use local build " + buildZip);
      }

      checkAbort();

      if (buildZip != null) {
        LOG.log(Level.INFO, "existing local build file " + buildZip.getName());
        try {
          LOG.log(Level.INFO, "extracting local build file " + buildZip);
          setCurrentProgressStep(UpgradeProgressStep.EXTRACTING);
          ZipExtractor extractor = new ZipExtractor(buildZip);
          extractor.extract(getStageDirectory());
          notifyListeners(formatter.getFormattedDone() +
                  formatter.getLineBreak());
          LOG.log(Level.INFO, "extraction finished");
        } catch (ApplicationException e) {
          LOG.log(Level.INFO, "Error extracting build file", e);
          throw e;
        }
      }

      checkAbort();

      try {
        LOG.log(Level.INFO, "initializing upgrade");
        setCurrentProgressStep(UpgradeProgressStep.INITIALIZING);
        initialize();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "initialization finished");
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "Error initializing upgrader", e);
        throw e;
      }

      checkAbort();

      try {
        LOG.log(Level.INFO, "checking server health");
        setCurrentProgressStep(UpgradeProgressStep.CHECK_SERVER_HEALTH);
        checkServerHealth();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "server health check finished");
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "Server failed initial health check", e);
        throw e;
      }

      checkAbort();

      boolean schemaCustomizationPresent = false;
      try {
        LOG.log(Level.INFO, "checking for schema customizations");
        setCurrentProgressStep(
                UpgradeProgressStep.CALCULATING_SCHEMA_CUSTOMIZATIONS);
        schemaCustomizationPresent = calculateSchemaCustomizations();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "check for schema customizations finished");
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "Error calculating schema customizations", e);
        throw e;
      }

      checkAbort();

      boolean configCustimizationPresent = false;
      try {
        LOG.log(Level.INFO, "checking for config customizations");
        setCurrentProgressStep(
                UpgradeProgressStep.CALCULATING_CONFIGURATION_CUSTOMIZATIONS);
        configCustimizationPresent = calculateConfigCustomizations();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "check for config customizations finished");
      } catch (ApplicationException e) {
        LOG.log(Level.INFO,
                "Error calculating config customizations", e);
        throw e;
      }

      checkAbort();

      try {
        LOG.log(Level.INFO, "backing up databases");
        setCurrentProgressStep(UpgradeProgressStep.BACKING_UP_DATABASES);
        backupDatabases();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "database backup finished");
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "Error backing up databases", e);
        throw e;
      }

      checkAbort();

      try {
        LOG.log(Level.INFO, "backing up filesystem");
        setCurrentProgressStep(UpgradeProgressStep.BACKING_UP_FILESYSTEM);
        backupFilesytem();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "filesystem backup finished");
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "Error backing up files", e);
        throw e;
      }

      checkAbort();

      try {
        LOG.log(Level.INFO, "upgrading components");
        setCurrentProgressStep(
                UpgradeProgressStep.UPGRADING_COMPONENTS);
        upgradeComponents();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "componnet upgrade finished");
      } catch (ApplicationException e) {
        LOG.log(Level.INFO,
                "Error upgrading components", e);
        throw e;
      }

      checkAbort();

      //********************************************
      //*  The two steps following this step require
      //*  the server to be started 'in process'.
      // *******************************************
      LOG.log(Level.INFO, "schema customization " +
              (schemaCustomizationPresent ? "":"not ") + "present");
      LOG.log(Level.INFO, "config customization " +
              (configCustimizationPresent ? "":"not ") + "present");
      if (schemaCustomizationPresent || configCustimizationPresent) {
        try {
          LOG.log(Level.INFO, "starting server");
          startServerWithoutConnectionHandlers();
          LOG.log(Level.INFO, "start server finished");
        } catch (ApplicationException e) {
          LOG.log(Level.INFO,
                  "Error starting server in process in order to apply custom" +
                          "schema and/or configuration", e);
          throw e;
        }

        checkAbort();

        if (schemaCustomizationPresent) {
          try {
            LOG.log(Level.INFO, "applying schema customizatoin");
            setCurrentProgressStep(
                    UpgradeProgressStep.APPLYING_SCHEMA_CUSTOMIZATIONS);
            applySchemaCustomizations();
            notifyListeners(formatter.getFormattedDone() +
                    formatter.getLineBreak());
            LOG.log(Level.INFO, "custom schema application finished");
          } catch (ApplicationException e) {
            LOG.log(Level.INFO,
                    "Error applying schema customizations", e);
            throw e;
          }
        }

        checkAbort();

        if (configCustimizationPresent) {
          try {
            LOG.log(Level.INFO, "applying config customizatoin");
            setCurrentProgressStep(
                    UpgradeProgressStep.APPLYING_CONFIGURATION_CUSTOMIZATIONS);
            applyConfigurationCustomizations();
            notifyListeners(formatter.getFormattedDone() +
                    formatter.getLineBreak());
            LOG.log(Level.INFO, "custom config application finished");
          } catch (ApplicationException e) {
            LOG.log(Level.INFO,
                    "Error applying configuration customizations", e);
            throw e;
          }
        }

        checkAbort();

        try {
          LOG.log(Level.INFO, "stopping server");
          getServerController().stopServerInProcess();
          LOG.log(Level.INFO, "server stopped");
        } catch (Throwable t) {
          LOG.log(Level.INFO, "Error stopping server", t);
          throw new ApplicationException(ApplicationException.Type.BUG,
                  "Error stopping server in process", t);
        }
      }

      checkAbort();

      // This allows you to test whether or not he upgrader can successfully
      // abort an upgrade once changes have been made to the installation
      // path's filesystem.
      if ("true".equals(
              System.getProperty(SYS_PROP_CREATE_ERROR))) {
        LOG.log(Level.WARNING, "creating artificial error");
        throw new ApplicationException(
                null, "ARTIFICIAL ERROR FOR TESTING ABORT PROCESS", null);
      }

      try {
        LOG.log(Level.INFO, "verifying upgrade");
        setCurrentProgressStep(UpgradeProgressStep.VERIFYING);
        verifyUpgrade();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "upgrade verification complete");
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "Error verifying upgrade", e);
        throw e;
      }

      // Leave the server in the state requested by the user via the
      // checkbox on the review panel.  The upgrade has already been
      // verified at this point to in the unlikely event of an error,
      // we call this a warning instead of an error.
      try {
        ServerController control = getServerController();
        boolean serverRunning = getInstallation().getStatus().isServerRunning();
        boolean userRequestsStart = getUserData().getStartServer();
        if (userRequestsStart && !serverRunning) {
          try {
            LOG.log(Level.INFO, "starting server");
            control.startServer();
          } catch (ApplicationException e) {
            LOG.log(Level.INFO, "error starting server");
            this.runWarning = e;
          }
        } else if (!userRequestsStart && serverRunning) {
          try {
            LOG.log(Level.INFO, "stopping server");
            control.stopServer();
          } catch (ApplicationException e) {
            LOG.log(Level.INFO, "error stopping server");
            this.runWarning = e;
          }
        }
      } catch (IOException ioe) {
        LOG.log(Level.INFO, "error determining if server running");
        this.runWarning = new ApplicationException(
                ApplicationException.Type.TOOL_ERROR,
                "Error determining whether or not server running", ioe);
      }

    } catch (ApplicationException ae) {
      this.runError = ae;
    } catch (Throwable t) {
      this.runError =
              new ApplicationException(ApplicationException.Type.BUG,
                      "Unexpected error: " + t.getLocalizedMessage(),
                      t);
    } finally {
      try {
        HistoricalRecord.Status status;
        String note = null;
        if (runError == null) {
          status = HistoricalRecord.Status.SUCCESS;
        } else {
          status = HistoricalRecord.Status.FAILURE;
          note = runError.getLocalizedMessage();

          // Abort the upgrade and put things back like we found it
          LOG.log(Level.INFO, "canceling upgrade");
          ProgressStep lastProgressStep = getCurrentProgressStep();
          setCurrentProgressStep(UpgradeProgressStep.ABORT);
          LOG.log(Level.INFO, "abort");
          abort(lastProgressStep);
          notifyListeners(formatter.getFormattedDone() +
                  formatter.getLineBreak());
          LOG.log(Level.INFO, "cancelation complete");
        }

        LOG.log(Level.INFO, "cleaning up after upgrade");
        setCurrentProgressStep(UpgradeProgressStep.CLEANUP);
        cleanup();
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "clean up complete");


        // Write a record in the log file indicating success/failure
        LOG.log(Level.INFO, "recording upgrade history");
        setCurrentProgressStep(UpgradeProgressStep.RECORDING_HISTORY);
        writeHistoricalRecord(historicalOperationId,
                getCurrentVersion(),
                getStagedVersion(),
                status,
                note);
        notifyListeners(formatter.getFormattedDone() +
                formatter.getLineBreak());
        LOG.log(Level.INFO, "history recorded");
        notifyListeners("See '" +
                Utils.getPath(getInstallation().getHistoryLogFile()) +
                " for upgrade history" + formatter.getLineBreak());
      } catch (ApplicationException e) {
        System.err.print("Error cleaning up after upgrade: " +
                e.getLocalizedMessage());
      }

      // Decide final status based on presense of error

      // WARNING: change this code at your own risk!  The ordering
      // of these statements is important.  There are differences
      // in how the CLI and GUI application's processes exit.
      // Changing the ordering here may result in messages being
      // skipped because the process has already exited by the time
      // processing messages has finished.  Need to resolve these
      // issues.
      if (runError != null) {
        LOG.log(Level.INFO, "upgrade completed with errors", runError);
        if (!Utils.isCli()) {
          notifyListenersOfLog();
          this.currentProgressStep = UpgradeProgressStep.FINISHED_WITH_ERRORS;
          notifyListeners(formatter.getFormattedError(runError, true));
        } else {
          notifyListeners(formatter.getFormattedError(runError, true) +
                          formatter.getLineBreak());
          notifyListeners(formatter.getLineBreak());
          setCurrentProgressStep(UpgradeProgressStep.FINISHED_WITH_ERRORS);
          notifyListeners(formatter.getLineBreak());
        }
      } else if (runWarning != null) {
        LOG.log(Level.INFO, "upgrade completed with warnings");
        String warningText = runWarning.getLocalizedMessage();
        if (!Utils.isCli()) {
          notifyListenersOfLog();
          this.currentProgressStep = UpgradeProgressStep.FINISHED_WITH_WARNINGS;
          notifyListeners(formatter.getFormattedWarning(warningText, true));
        } else {
          notifyListeners(formatter.getFormattedWarning(warningText, true) +
                          formatter.getLineBreak());
          notifyListeners(formatter.getLineBreak());
          setCurrentProgressStep(UpgradeProgressStep.FINISHED_WITH_WARNINGS);
          notifyListeners(formatter.getLineBreak());
        }
      } else {
        LOG.log(Level.INFO, "upgrade completed successfully");
        if (!Utils.isCli()) {
          notifyListenersOfLog();
          this.currentProgressStep = UpgradeProgressStep.FINISHED;
          notifyListeners(null);
        } else {
          notifyListeners(null);
          this.currentProgressStep = UpgradeProgressStep.FINISHED;
        }
      }
    }

  }

  private void checkAbort() throws ApplicationException {
    if (abort) throw new ApplicationException(
            ApplicationException.Type.APPLICATION,
            "upgrade canceled by user", null);
  }

  /**
   * Stops and starts the server checking for serious errors.  Also
   * has the side effect of having the server write schema.current
   * if it has never done so.
   */
  private void checkServerHealth() throws ApplicationException {
    Installation installation = getInstallation();
    ServerController control = new ServerController(installation);
    try {
      if (installation.getStatus().isServerRunning()) {
        control.stopServer();
      }
      OperationOutput op = control.startServer();
      List<String> errors = op.getErrors();
      if (errors != null) {
        throw new ApplicationException(
                ApplicationException.Type.APPLICATION,
                "The server currently starts with errors which must" +
                        "be resolved before an upgrade can occur: " +
                        Utils.listToString(errors, " "),
                null);
      }
      control.stopServer();
    } catch (Exception e) {
      throw new ApplicationException(ApplicationException.Type.APPLICATION,
              "Server health check failed.  Please resolve the following " +
                      "before running the upgrade " +
                      "tool: " + e.getLocalizedMessage(), e);
    }
  }

  /**
   * Abort this upgrade and repair the installation.
   *
   * @param lastStep ProgressStep indicating how much work we will have to
   *                 do to get the installation back like we left it
   * @throws ApplicationException of something goes wrong
   */
  private void abort(ProgressStep lastStep) throws ApplicationException {

    // This can be used to bypass the aborted upgrade cleanup
    // process so that an autopsy can be performed on the
    // crippled server.
    if ("true".equals(System.getProperty(SYS_PROP_NO_ABORT))) {
      return;
    }

    UpgradeProgressStep lastUpgradeStep = (UpgradeProgressStep) lastStep;
    EnumSet<UpgradeProgressStep> stepsStarted =
            EnumSet.range(UpgradeProgressStep.NOT_STARTED, lastUpgradeStep);

    if (stepsStarted.contains(UpgradeProgressStep.BACKING_UP_FILESYSTEM)) {

      // Files were copied from the stage directory to the current
      // directory.  Repair things by overwriting file in the
      // root with those that were copied to the backup directory
      // during backupFiles()

      File root = getInstallation().getRootDirectory();
      File backupDirectory;
      try {
        backupDirectory = getFilesBackupDirectory();
        FileManager fm = new FileManager();
        boolean restoreError = false;
        for (String fileName : backupDirectory.list()) {
          File f = new File(backupDirectory, fileName);

          // Do our best to restore the filesystem like
          // we found it.  Just report potential problems
          // to the user.
          try {
            fm.move(f, root, null);
          } catch (Throwable t) {
            restoreError = true;
            notifyListeners("The following could not be restored after the" +
                    "failed upgrade attempt.  You should restore this " +
                    "file/directory manually: '" + f + "' to '" + root + "'");
          }
        }
        if (!restoreError) {
          fm.deleteRecursively(backupDirectory);
        }

        // Restart the server after putting the files
        // back like we found them.
        getServerController().stopServer();
        getServerController().startServer();

      } catch (IOException e) {
        LOG.log(Level.INFO, "Error getting backup directory", e);
      }
    }


  }

  private void verifyUpgrade() throws ApplicationException {
    ServerController sc = new ServerController(getInstallation());
    OperationOutput op = sc.startServer();
    if (op.getErrors() != null) {
      throw new ApplicationException(ApplicationException.Type.APPLICATION,
              "Upgraded server failed verification test by signaling " +
                      "errors during startup: " +
                      Utils.listToString(op.getErrors(), " "), null);
    }
  }

  private void applyConfigurationCustomizations() throws ApplicationException {
    try {
      File configDiff = getCustomConfigDiffFile();
      if (configDiff.exists()) {
        applyCustomizationLdifFile(configDiff);
      }
    } catch (IOException e) {
      String msg = "IO Error applying configuration customization: " +
              e.getLocalizedMessage();
      LOG.log(Level.INFO, msg, e);
      throw new ApplicationException(ApplicationException.Type.IMPORT_ERROR,
              msg, e);
    } catch (org.opends.server.util.LDIFException e) {
      String msg = "LDIF error applying configuration customization: " +
              e.getLocalizedMessage();
      LOG.log(Level.INFO, msg, e);
      throw new ApplicationException(ApplicationException.Type.IMPORT_ERROR,
              msg, e);
    }
  }

  private void applySchemaCustomizations() throws ApplicationException {
    try {
      File schemaDiff = getCustomSchemaDiffFile();
      if (schemaDiff.exists()) {
        applyCustomizationLdifFile(schemaDiff);
      }
    } catch (IOException e) {
      String msg = "IO Error applying schema customization: " +
              e.getLocalizedMessage();
      LOG.log(Level.INFO, msg, e);
      throw new ApplicationException(ApplicationException.Type.IMPORT_ERROR,
              msg, e);
    } catch (org.opends.server.util.LDIFException e) {
      String msg = "LDIF error applying schema customization: " +
              e.getLocalizedMessage();
      LOG.log(Level.INFO, msg, e);
      throw new ApplicationException(ApplicationException.Type.IMPORT_ERROR,
              msg, e);
    }
  }


  /**
   * Applies configuration or schema customizations.
   * NOTE: Assumes that the server is running in process.
   *
   * @param ldifFile LDIF file to apply
   * @throws IOException
   * @throws org.opends.server.util.LDIFException
   *
   * @throws ApplicationException
   */
  private void applyCustomizationLdifFile(File ldifFile)
          throws IOException, org.opends.server.util.LDIFException,
          ApplicationException {
    try {
      org.opends.server.protocols.internal.InternalClientConnection cc =
              org.opends.server.protocols.internal.
                      InternalClientConnection.getRootConnection();
      org.opends.server.types.LDIFImportConfig importCfg =
              new org.opends.server.types.LDIFImportConfig(
                      Utils.getPath(ldifFile));
      org.opends.server.util.LDIFReader ldifReader =
              new org.opends.server.util.LDIFReader(importCfg);
      org.opends.server.util.ChangeRecordEntry cre;
      while (null != (cre = ldifReader.readChangeRecord(false))) {
        if (cre instanceof org.opends.server.util.ModifyChangeRecordEntry) {
          org.opends.server.util.ModifyChangeRecordEntry mcre =
                  (org.opends.server.util.ModifyChangeRecordEntry) cre;
          org.opends.server.types.ByteString dnByteString =
                  org.opends.server.types.ByteStringFactory.create(
                          mcre.getDN().toString());
          org.opends.server.core.ModifyOperation op =
                  cc.processModify(dnByteString, mcre.getModifications());
          org.opends.server.types.ResultCode rc = op.getResultCode();
          if (rc.equals(
                  org.opends.server.types.ResultCode.
                          OBJECTCLASS_VIOLATION)) {
            // try again without schema checking
            org.opends.server.core.DirectoryServer.setCheckSchema(false);
            op = cc.processModify(dnByteString, mcre.getModifications());
            rc = op.getResultCode();
          }
          if (rc.equals(org.opends.server.types.ResultCode.
                  SUCCESS)) {
            LOG.log(Level.INFO, "processed server modification " +
                    (org.opends.server.core.DirectoryServer.checkSchema() ?
                            ":" : "(schema checking off):" +
                            modListToString(op.getModifications())));
            if (!org.opends.server.core.DirectoryServer.checkSchema()) {
              org.opends.server.core.DirectoryServer.setCheckSchema(true);
            }
          } else if (rc.equals(
                  org.opends.server.types.ResultCode.
                          ATTRIBUTE_OR_VALUE_EXISTS)) {
            // ignore this error
            LOG.log(Level.INFO, "ignoring attribute that already exists: " +
                    modListToString(op.getModifications()));
          } else {
            // report the error to the user
            StringBuilder error = op.getErrorMessage();
            if (error != null) {
              throw new ApplicationException(
                      ApplicationException.Type.IMPORT_ERROR,
                      "error processing custom configuration "
                              + error.toString(),
                      null);
            }
          }
        } else {
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
          List<org.opends.server.types.Modification> modifications) {
    StringBuilder modsMsg = new StringBuilder();
    for (int i = 0; i < modifications.size(); i++) {
      modsMsg.append(modifications.get(i).toString());
      if (i < modifications.size() - 1) {
        modsMsg.append(" ");
      }
    }
    return modsMsg.toString();
  }

  private Long writeInitialHistoricalRecord(
          Integer fromVersion,
          Integer toVersion)
          throws ApplicationException {
    Long id;
    try {
      HistoricalLog log =
              new HistoricalLog(getInstallation().getHistoryLogFile());
      id = log.append(fromVersion, toVersion,
              HistoricalRecord.Status.STARTED, null);
    } catch (IOException e) {
      String msg = "IO Error logging operation: " + e.getLocalizedMessage();
      throw ApplicationException.createFileSystemException(
              msg, e);
    }
    return id;
  }

  private void writeHistoricalRecord(
          Long id,
          Integer from,
          Integer to,
          HistoricalRecord.Status status,
          String note)
          throws ApplicationException {
    try {
      HistoricalLog log =
              new HistoricalLog(getInstallation().getHistoryLogFile());
      log.append(id, from, to, status, note);
    } catch (IOException e) {
      String msg = "Error logging operation: " + e.getLocalizedMessage();
      throw ApplicationException.createFileSystemException(msg, e);
    }
  }

  private void upgradeComponents() throws ApplicationException {
    try {
      File stageDir = getStageDirectory();
      File root = getInstallation().getRootDirectory();
      FileManager fm = new FileManager();
      for (String fileName : stageDir.list()) {
        File f = new File(stageDir, fileName);
        fm.copyRecursively(f, root,
                new UpgradeFileFilter(stageDir),
                /*overwrite=*/true);
      }
    } catch (IOException e) {
      throw ApplicationException.createFileSystemException(
              "I/0 error upgrading components: " + e.getLocalizedMessage(), e);
    }
  }

  private boolean calculateConfigCustomizations() throws ApplicationException {
    boolean isCustom = false;
    try {
      if (getInstallation().getCurrentConfiguration().hasBeenModified()) {
        isCustom = true;
        LOG.log(Level.INFO, "Configuration contains customizations that will " +
                "be migrated");
        try {
          ldifDiff(getInstallation().getBaseConfigurationFile(),
                  getInstallation().getCurrentConfigurationFile(),
                  getCustomConfigDiffFile());
        } catch (Exception e) {
          throw ApplicationException.createFileSystemException(
                  "Error determining configuration customizations: "
                          + e.getLocalizedMessage(), e);
        }
      } else {
        LOG.log(Level.INFO, "No configuration customizations to migrate");
      }
    } catch (IOException e) {
      // TODO i18n
      throw ApplicationException.createFileSystemException(
              "Could not determine configuration modifications: " +
                      e.getLocalizedMessage(), e);
    }
    return isCustom;
  }

  private void ldifDiff(File source, File target, File output)
          throws ApplicationException {
    List<String> args = new ArrayList<String>();

    args.add("-s"); // source LDIF
    args.add(Utils.getPath(source));

    args.add("-t"); // target LDIF
    args.add(Utils.getPath(target));

    args.add("-o"); // output LDIF
    args.add(Utils.getPath(output));

    args.add("-O"); // overwrite
    args.add("-S"); // single-value changes

    // TODO i18n
    LOG.log(Level.INFO, "Diff'ing " +
            Utils.getPath(source) + " with " +
            Utils.getPath(target));

    int ret = org.opends.server.tools.LDIFDiff.mainDiff(
            args.toArray(new String[]{}), false);
    if (ret != 0) {
      StringBuffer sb = new StringBuffer()
              .append("'ldif-diff' tool returned error code ")
              .append(ret)
              .append(" when invoked with args: ")
              .append(Utils.listToString(args, " "));
      throw ApplicationException.createFileSystemException(sb.toString(),
              null);
    }
  }

  private boolean calculateSchemaCustomizations() throws ApplicationException {
    boolean isCustom = false;
    if (getInstallation().getStatus().schemaHasBeenModified()) {
      isCustom = true;
      LOG.log(Level.INFO, "Schema contains customizations that will " +
              "be migrated");
      try {
        ldifDiff(getInstallation().getBaseSchemaFile(),
                getInstallation().getSchemaConcatFile(),
                getCustomSchemaDiffFile());
      } catch (Exception e) {
        throw ApplicationException.createFileSystemException(
                "Error determining schema customizations: " +
                        e.getLocalizedMessage(), e);
      }
    } else {
      LOG.log(Level.INFO, "No schema customizations to migrate");
    }
    return isCustom;
  }

  private void backupFilesytem() throws ApplicationException {
    try {
      File filesBackupDirectory = getFilesBackupDirectory();
      FileManager fm = new FileManager();
      File root = getInstallation().getRootDirectory();
      FileFilter filter = new UpgradeFileFilter(root);
      for (String fileName : root.list()) {
        File f = new File(root, fileName);
        //fm.copyRecursively(f, filesBackupDirectory,
        fm.move(f, filesBackupDirectory, filter);
      }
    } catch (Exception e) {
      throw new ApplicationException(
              ApplicationException.Type.FILE_SYSTEM_ERROR,
              e.getLocalizedMessage(),
              e);
    }
  }

  private void backupDatabases() throws ApplicationException {
    List<String> args = new ArrayList<String>();
    args.add("--configClass");
    args.add("org.opends.server.extensions.ConfigFileHandler");
    args.add("--configFile");
    args.add(getInstallation().getCurrentConfigurationFile().getPath());
    args.add("-a"); // backup all
    args.add("-d"); // backup to directory
    try {
      args.add(getUpgradeBackupDirectory().getCanonicalPath());
    } catch (IOException e) {
      // TODO i18n
      throw new ApplicationException(
              ApplicationException.Type.FILE_SYSTEM_ERROR,
              "error backup up databases", e);
    }
    int ret = org.opends.server.tools.BackUpDB.mainBackUpDB(
            args.toArray(new String[0]));
    if (ret != 0) {
      StringBuffer sb = new StringBuffer()
              .append("'backup utility returned error code ")
              .append(ret)
              .append(" when invoked with args: ")
              .append(Utils.listToString(args, " "));
      throw new ApplicationException(
              ApplicationException.Type.FILE_SYSTEM_ERROR,
              sb.toString(), null);

    }
  }

  private void cleanup() throws ApplicationException {
    deleteStagingDirectory();
  }

  private void deleteStagingDirectory() throws ApplicationException {
    File stagingDir = null;
    try {
      stagingDir = getStageDirectory();
      FileManager fm = new FileManager();

      // Doing this seems to work better than just plain
      // old delete.  Note that on Windows there are file
      // locking issues to we mark files for deletion after
      // this JVM exits
      if (stagingDir.exists()) {
        fm.deleteRecursively(stagingDir, null, /*onExit=*/true);
      }

    } catch (IOException e) {
      // TODO i18n
      throw ApplicationException.createFileSystemException(
              "Error attempting to clean up tmp directory " +
                      stagingDir != null ? stagingDir.getName() : "null" +
                      ": " + e.getLocalizedMessage(),
              e);
    }
  }

  private void initialize() throws ApplicationException {
    try {
      Integer fromVersion = getCurrentVersion();
      Integer toVersion = getStagedVersion();
      this.historicalOperationId =
              writeInitialHistoricalRecord(fromVersion, toVersion);

      insureUpgradability();

    } catch (Exception e) {
      throw new ApplicationException(
              ApplicationException.Type.FILE_SYSTEM_ERROR,
              e.getMessage(), e);
    }
  }

  /**
   * Given the current information, determines whether or not
   * an upgrade from the current version to the next version
   * is possible.  Upgrading may not be possible due to 'flag
   * day' types of changes to the codebase.
   */
  private void insureUpgradability() throws ApplicationException {
    BuildInformation currentVersion;
    BuildInformation newVersion;

    try {
      currentVersion = getInstallation().getBuildInformation();
    } catch (ApplicationException e) {
      LOG.log(Level.INFO, "error", e);
      throw ApplicationException.createFileSystemException(
              "Could not determine current build information: " +
                      e.getLocalizedMessage(), e);
    }

    try {
      newVersion = getStagedInstallation().getBuildInformation();
    } catch (Exception e) {
      LOG.log(Level.INFO, "error", e);
      throw ApplicationException.createFileSystemException(
              "Could not determine upgrade build information: " +
                      e.getLocalizedMessage(), e);
    }

    UpgradeOracle uo = new UpgradeOracle(currentVersion, newVersion);
    if (!uo.isSupported()) {
      throw new ApplicationException(ApplicationException.Type.APPLICATION,
              uo.getSummaryMessage(), null);
    }

  }

  private Installation getStagedInstallation()
          throws IOException, ApplicationException {
    if (stagedInstallation == null) {
      File stageDir = getStageDirectory();
      try {
        Installation.validateRootDirectory(stageDir);
        stagedInstallation = new Installation(getStageDirectory());
      } catch (IllegalArgumentException e) {
        throw ApplicationException.createFileSystemException(
                "Directory '" + getStageDirectory() +
                        "' does not contain a staged installation of OpenDS" +
                        " as was expected.  Verify that the new installation" +
                        " package (.zip) is an OpenDS installation file", null);
      }
    }
    return stagedInstallation;
  }

  /**
   * {@inheritDoc}
   */
  public UserData createUserData() {
    UpgradeUserData uud = new UpgradeUserData();
    String instanceRootFromSystem = System.getProperty(SYS_PROP_INSTALL_ROOT);
    if (instanceRootFromSystem != null) {
      uud.setServerLocation(instanceRootFromSystem);
    }
    return uud;
  }

  /**
   * {@inheritDoc}
   */
  public UserData createUserData(String[] args, CurrentInstallStatus cis)
          throws UserDataException {
    return getCliHelper().createUserData(args, cis);
  }

  /**
   * {@inheritDoc}
   */
  public ApplicationException getException() {
    return runError;
  }

  private void setCurrentProgressStep(UpgradeProgressStep step) {
    this.currentProgressStep = step;
    int progress = step.getProgress();
    String msg = getSummary(step);
    notifyListeners(progress, msg, getFormattedProgress(msg));
  }

  private UpgraderCliHelper getCliHelper() {
    if (cliHelper == null) {
      cliHelper = new UpgraderCliHelper();
    }
    return cliHelper;
  }

  private String getFinalSuccessMessage() {
    String txt;
    String installPath = Utils.getPath(getInstallation().getRootDirectory());
    String newVersion = null;
    try {
      BuildInformation bi = getInstallation().getBuildInformation();
      if (bi != null) {
        newVersion = bi.toString();
      } else {
        newVersion = getMsg("upgrade-build-id-unknown");
      }
    } catch (ApplicationException e) {
      newVersion = getMsg("upgrade-build-id-unknown");
    }
    String[] args = {
            formatter.getFormattedText(installPath),
            newVersion};
    if (Utils.isCli()) {
      txt = getMsg("summary-upgrade-finished-successfully-cli", args);
    } else {
      txt = getFormattedSuccess(
              getMsg("summary-upgrade-finished-successfully",
                      args));
    }
    return txt;
  }

  private String getFinalErrorMessage() {
    String txt;
    if (Utils.isCli()) {
      txt = getMsg("summary-upgrade-finished-with-errors-cli");
    } else {
      txt = getFormattedError(
              getMsg("summary-upgrade-finished-with-errors"));
    }
    return txt;
  }

  private String getFinalWarningMessage() {
    String txt;
    if (Utils.isCli()) {
      txt = getMsg("summary-upgrade-finished-with-warnings-cli");
    } else {
      txt = getFormattedWarning(
              getMsg("summary-upgrade-finished-with-warnings"));
    }
    return txt;
  }

  private File getStageDirectory()
          throws ApplicationException, IOException {
    return getInstallation().getTemporaryUpgradeDirectory();
  }

  private UpgradeUserData getUpgradeUserData() {
    return (UpgradeUserData) getUserData();
  }

  private File getFilesBackupDirectory() throws IOException {
    File files = new File(getUpgradeBackupDirectory(), "files");
    if (!files.exists()) {
      if (!files.mkdirs()) {
        throw new IOException("error creating files backup directory");
      }
    }
    return files;
  }

  private File getUpgradeBackupDirectory() throws IOException {
    if (backupDirectory == null) {
      backupDirectory = getInstallation().createHistoryBackupDirectory();
    }
    return backupDirectory;
  }

  private File getCustomConfigDiffFile() throws IOException {
    return new File(getUpgradeBackupDirectory(), "config.custom.diff");
  }

  private File getCustomSchemaDiffFile() throws IOException {
    return new File(getUpgradeBackupDirectory(), "schema.custom.diff");
  }

  private Integer getCurrentVersion() {
    if (this.currentVersion == null) {
      try {
        currentVersion = getInstallation().getSvnRev();
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "error trying to determine current version", e);
      }
    }
    return currentVersion;
  }

  private Integer getStagedVersion() {
    if (stagedVersion == null) {
      try {
        stagedVersion = getStagedInstallation().getSvnRev();
      } catch (Exception e) {
        LOG.log(Level.INFO, "error", e);
      }
    }
    return stagedVersion;
  }

  /**
   * Filter defining files we want to manage in the upgrade
   * process.
   */
  private class UpgradeFileFilter implements FileFilter {

    Set<File> filesToIgnore;

    public UpgradeFileFilter(File root) throws IOException {
      this.filesToIgnore = new HashSet<File>();
      for (String rootFileNamesToIgnore : ROOT_FILES_TO_IGNORE_DURING_BACKUP) {
        filesToIgnore.add(new File(root, rootFileNamesToIgnore));
      }

      // Definitely want to not back this up since it would create
      // infinite recursion.  This may not be necessary if we are
      // ignoring the entire history directory but its added here for
      // safe measure.
      filesToIgnore.add(getUpgradeBackupDirectory());
    }

    public boolean accept(File file) {
      boolean accept = true;
      for (File ignoreFile : filesToIgnore) {
        if (ignoreFile.equals(file) ||
                Utils.isParentOf(ignoreFile, file)) {
          accept = false;
          break;
        }
      }
      return accept;
    }
  }

}
