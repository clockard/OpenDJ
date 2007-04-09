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
import org.opends.quicksetup.util.Utils;
import org.opends.quicksetup.util.ZipExtractor;
import org.opends.quicksetup.util.FileManager;
import org.opends.quicksetup.util.ServerController;
import org.opends.quicksetup.ui.QuickSetupDialog;
import org.opends.quicksetup.ui.QuickSetupStepPanel;
import org.opends.server.tools.BackUpDB;
import org.opends.server.tools.LDIFDiff;

import java.awt.event.WindowEvent;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;

import static org.opends.quicksetup.Installation.*;

/**
 * QuickSetup application of ugrading the bits of an installation of
 * OpenDS.
 */
public class Upgrader extends Application implements CliApplication {

  /** Steps in the Upgrade wizard. */
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
  }

  /** Steps during the upgrade process. */
  enum UpgradeProgressStep implements ProgressStep {

    NOT_STARTED("summary-upgrade-not-started"),

    INITIALIZING("summary-upgrade-initializing"),

    STOPPING_SERVER("summary-stopping"),

    BACKING_UP_DATABASES("summary-upgrade-backing-up-db"),

    BACKING_UP_FILESYSTEM("summary-upgrade-backing-up-files"),

    CALCULATING_SCHEMA_CUSTOMIZATIONS(
            "summary-upgrade-calculating-schema-customization"),

    CALCULATING_CONFIGURATION_CUSTOMIZATIONS(
            "summary-upgrade-calculating-config-customization"),

    UPGRADING_COMPONENTS("summary-upgrade-upgrading-components"),

    APPLYING_SCHEMA_CUSTOMIZATIONS(
            "summary-upgrade-applying-schema-customization"),

    APPLYING_CONFIGURATION_CUSTOMIZATIONS(
            "summary-upgrade-applying-config-customization"),

    VERIFYING("summary-upgrade-verifying"),

    RECORDING_HISTORY("summary-upgrade-history"),

    CLEANUP("summary-upgrade-cleanup"),

    FINISHED_WITH_ERRORS("summary-upgrade-finished-with-errors"),

    FINISHED("summary-upgrade-finished-successfully");

    private String summaryMsgKey;

    private UpgradeProgressStep(String summaryMsgKey) {
      this.summaryMsgKey = summaryMsgKey;
    }

    /**
     * Return a key for access a summary message.
     * @return String representing key for access summary in resource bundle
     */
    public String getSummaryMesssageKey() {
      return summaryMsgKey;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLast() {
      return this == FINISHED ||
              this == FINISHED_WITH_ERRORS;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isError() {
      return this == FINISHED_WITH_ERRORS;
    }
  }

  static private final Logger LOG = Logger.getLogger(Upgrader.class.getName());

  // Root files that will be ignored during backup
  static private final String[] ROOT_FILES_TO_IGNORE_DURING_BACKUP = {
    CHANGELOG_PATH_RELATIVE, // changelogDb
    DATABASES_PATH_RELATIVE, // db
    LOGS_PATH_RELATIVE, // logs
    LOCKS_PATH_RELATIVE, // locks
    HISTORY_PATH_RELATIVE // history
  };

  private ProgressStep currentProgressStep = UpgradeProgressStep.NOT_STARTED;

  /** Assigned if an exception occurs during run(). */
  private ApplicationException runException = null;

  /** Helps with CLI specific tasks. */
  private UpgraderCliHelper cliHelper = null;

  /** Directory where we keep files temporarily. */
  private File stagingDirectory = null;

  /** Directory where backup is kept in case the upgrade needs reversion. */
  private File backupDirectory = null;

  /** ID that uniquely identifieds this invocation of the Upgrader in the
   * historical logs.
   */
  private Long historicalOperationId;

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
   * {@inheritDoc}
   */
  protected String getInstallationPath() {
    return Utils.getInstallPathFromClasspath();
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
    return 100 * ((UpgradeProgressStep)step).ordinal() /
            EnumSet.allOf(UpgradeWizardStep.class).size();
  }

  /**
   * {@inheritDoc}
   */
  public String getSummary(ProgressStep step) {
    return getMsg(((UpgradeProgressStep)step).getSummaryMesssageKey());
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
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public Step getNextWizardStep(WizardStep step) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public WizardStep getPreviousWizardStep(WizardStep step) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public void quitClicked(WizardStep step, QuickSetup qs) {
  }

  /**
   * {@inheritDoc}
   */
  protected void updateUserData(WizardStep cStep, QuickSetup qs)
          throws UserDataException
  {
  }

  /**
   * {@inheritDoc}
   */
  public void previousClicked(WizardStep cStep, QuickSetup qs) {
  }

  /**
   * {@inheritDoc}
   */
  public void finishClicked(final WizardStep cStep, final QuickSetup qs) {
  }

  /**
   * {@inheritDoc}
   */
  public void nextClicked(WizardStep cStep, QuickSetup qs) {
  }

  /**
   * {@inheritDoc}
   */
  public void closeClicked(WizardStep cStep, QuickSetup qs) {
  }

  /**
   * {@inheritDoc}
   */
  public void cancelClicked(WizardStep cStep, QuickSetup qs) {
  }

  /**
   * {@inheritDoc}
   */
  public void run() {
    // Reset exception just in case this application is rerun
    // for some reason
    runException = null;
    Integer fromVersion = null;
    Integer toVersion = null;

    try {
      try {
        setCurrentProgressStep(UpgradeProgressStep.INITIALIZING);
        initialize();
        fromVersion = getStagedInstallation().getSvnRev();
        toVersion = getInstallation().getSvnRev();
        this.historicalOperationId =
                writeInitialHistoricalRecord(fromVersion, toVersion);
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "error initializing upgrader", e);
        throw e;
      }

      if (getInstallation().getStatus().isServerRunning()) {
        try {
          setCurrentProgressStep(UpgradeProgressStep.STOPPING_SERVER);
          new ServerController(this).stopServer();
        } catch (ApplicationException e) {
          LOG.log(Level.INFO, "error stopping server", e);
          throw e;
        }
      }

      try {
        setCurrentProgressStep(UpgradeProgressStep.BACKING_UP_DATABASES);
        backupDatabases();
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "error backing up databases", e);
        throw e;
      }

      try {
        setCurrentProgressStep(UpgradeProgressStep.BACKING_UP_FILESYSTEM);
        backupFilesytem();
      } catch (ApplicationException e) {
        LOG.log(Level.INFO, "error backing up files", e);
        throw e;
      }

        try {
          setCurrentProgressStep(
              UpgradeProgressStep.CALCULATING_SCHEMA_CUSTOMIZATIONS);
          calculateSchemaCustomizations();
        } catch (ApplicationException e) {
          LOG.log(Level.INFO, "error calculating schema customizations", e);
          throw e;
        }

        try {
          setCurrentProgressStep(
              UpgradeProgressStep.CALCULATING_CONFIGURATION_CUSTOMIZATIONS);
          calculateConfigCustomizations();
        } catch (ApplicationException e) {
          LOG.log(Level.INFO,
                  "error calculating config customizations", e);
          throw e;
        }

      try {
        setCurrentProgressStep(
            UpgradeProgressStep.UPGRADING_COMPONENTS);
        upgradeComponents();
      } catch (ApplicationException e) {
        LOG.log(Level.INFO,
                "error upgrading components", e);
        throw e;
      }

//      setCurrentProgressStep(
//              UpgradeProgressStep.APPLYING_SCHEMA_CUSTOMIZATIONS);
//      sleepFor1();
//      setCurrentProgressStep(
//              UpgradeProgressStep.APPLYING_CONFIGURATION_CUSTOMIZATIONS);
//      sleepFor1();
//      setCurrentProgressStep(UpgradeProgressStep.VERIFYING);
//      sleepFor1();



    } catch (ApplicationException ae) {
      this.runException = ae;
    } catch (Throwable t) {
      this.runException =
              new ApplicationException(ApplicationException.Type.BUG,
                      t.getLocalizedMessage(),
                      t);
    } finally {
      try {
        setCurrentProgressStep(UpgradeProgressStep.CLEANUP);
        cleanup();

        // Write a record in the log file indicating success/failure
        setCurrentProgressStep(UpgradeProgressStep.RECORDING_HISTORY);
        HistoricalRecord.Status status;
        String note = null;
        if (runException == null) {
          status = HistoricalRecord.Status.SUCCESS;
        } else {
          status = HistoricalRecord.Status.FAILURE;
          note = runException.getLocalizedMessage();
        }
        writeHistoricalRecord(historicalOperationId,
                fromVersion,
                toVersion,
                status,
                note);

      } catch (ApplicationException e) {
        System.err.print("error cleaning up after upgrade: " +
                e.getLocalizedMessage());
      }
    }

    // Decide final status based on presense of error
    if (runException == null) {
      setCurrentProgressStep(UpgradeProgressStep.FINISHED);
    } else {
      setCurrentProgressStep(UpgradeProgressStep.FINISHED_WITH_ERRORS);
    }

  }

  private Long writeInitialHistoricalRecord(
          Integer fromVersion,
          Integer toVersion)
          throws ApplicationException
  {
    Long id;
    try {
      HistoricalLog log =
            new HistoricalLog(getInstallation().getHistoryLogFile());
      id = log.append(fromVersion, toVersion,
              HistoricalRecord.Status.STARTED, null);
    } catch (IOException e) {
      throw ApplicationException.createFileSystemException(
              "error logging operation", e);
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

      // FOR TESTING
      List<HistoricalRecord> records = log.getRecords();
      for(HistoricalRecord record : records) {
        System.out.println(record);
      }

    } catch (IOException e) {
      throw ApplicationException.createFileSystemException(
              "error logging operation", e);
    }
  }

  private void upgradeComponents() throws ApplicationException {
    try {
      File stageDir = getStageDirectory();
      File root = getInstallation().getRootDirectory();
      FileManager fm = new FileManager(this);
      for (String fileName : stageDir.list()) {
        File f = new File(stageDir, fileName);
        fm.copyRecursively(f, root, new UpgradeFileFilter(stageDir));
      }
    } catch (IOException e) {
      throw ApplicationException.createFileSystemException(
              "I/0 error upgrading components", e);
    }
  }

  private void calculateConfigCustomizations() throws ApplicationException {
    try {
    if (getInstallation().getCurrentConfiguration().hasBeenModified()) {

      try {
        List<String> args = new ArrayList<String>();

        args.add("-s"); // source LDIF
        args.add(getInstallation().getCurrentConfigurationFile().
                getCanonicalPath());

        args.add("-t"); // target LDIF
        args.add(getInstallation().getBaseConfigurationFile().
                getCanonicalPath());

        args.add("-o"); // output LDIF
        args.add(getCustomConfigDiffFile().
                getCanonicalPath());

        // TODO i18n
        notifyListeners("Diff'ing configuration with base configuration...");

        int ret = LDIFDiff.mainDiff(args.toArray(new String[]{}), false);
        if (ret != 0) {
          StringBuffer msg = new StringBuffer()
                  .append("'ldif-diff' tool returned error code ")
                  .append(ret)
                  .append(" when invoked with args :")
                  .append(Utils.listToString(args, " "));
          throw ApplicationException.createFileSystemException(
                  msg.toString(), null);
        } else {
          notifyListeners(formatter.getFormattedDone());
        }
      } catch (Exception e) {
        throw ApplicationException.createFileSystemException(
                "error determining configuration customizations", e);
      }
    } else {
      // TODO i18n
      notifyListeners("No configuration customizations to migrate" +
                      formatter.getLineBreak());
    }
    } catch (IOException e) {
      // TODO i18n
      throw ApplicationException.createFileSystemException(
              "could not determine configuration modifications", e);
    }
  }

  private void calculateSchemaCustomizations() throws ApplicationException {
    if (getInstallation().getStatus().schemaHasBeenModified()) {

      // TODO i18n
      notifyListeners(
              "Schema contains customizations and needs to be migrated");

      try {
        List<String> args = new ArrayList<String>();

        args.add("-s"); // source LDIF
        args.add(getInstallation().getSchemaConcatFile().
                getCanonicalPath());

        args.add("-t"); // target LDIF
        args.add(getInstallation().getBaseSchemaFile().
                getCanonicalPath());

        args.add("-o"); // output LDIF
        args.add(getCustomSchemaDiffFile().
                getCanonicalPath());

        // TODO i18n
        notifyListeners("Diff'ing schema with base schema...");

        int ret = LDIFDiff.mainDiff(args.toArray(new String[]{}), false);
        if (ret != 0) {
          StringBuffer sb = new StringBuffer()
                  .append("'ldif-diff' tool returned error code ")
                  .append(ret)
                  .append(" when invoked with args: ")
                  .append(Utils.listToString(args, " "));
          throw ApplicationException.createFileSystemException(sb.toString(),
                  null);
        } else {
          notifyListeners(formatter.getFormattedDone());
        }

      } catch (Exception e) {
        throw ApplicationException.createFileSystemException(
                "error determining schema customizations", e);
      }
    } else {
      // TODO i18n
      notifyListeners("No schema customizations to migrate" +
          formatter.getLineBreak());
    }
  }

  private void backupFilesytem() throws ApplicationException {
    try {
      File filesBackupDirectory = getFilesBackupDirectory();
      FileManager fm = new FileManager(this);
      File root = getInstallation().getRootDirectory();
      for (String fileName : root.list()) {
        File f = new File(root, fileName);
        fm.copyRecursively(f, filesBackupDirectory,
                new UpgradeFileFilter(root));
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
    int ret = BackUpDB.mainBackUpDB(args.toArray(new String[0]));
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
      FileManager fm = new FileManager(this);
      fm.deleteRecursively(stagingDir);
    } catch (IOException e) {
      // TODO i18n
      throw ApplicationException.createFileSystemException(
              "error attempting to clean up tmp directory " +
              stagingDir != null ? stagingDir.getName() : "null",
              e);
    }
  }

  private void initialize() throws ApplicationException {
    try {
      expandZipFile();
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
    Integer currentVersion;
    Integer newVersion;

    try {
      currentVersion = getInstallation().getSvnRev();
    } catch (QuickSetupException e) {
      LOG.log(Level.INFO, "error", e);
      throw ApplicationException.createFileSystemException(
              "could not determine current version number", e);
    }

    try {
      newVersion = getStagedInstallation().getSvnRev();
    } catch (Exception e) {
      LOG.log(Level.INFO, "error", e);
      throw ApplicationException.createFileSystemException(
              "could not determine upgrade version number", e);
    }

    UpgradeOracle uo = new UpgradeOracle(currentVersion, newVersion);
    if (!uo.isSupported()) {
      throw new ApplicationException(ApplicationException.Type.APPLICATION,
              uo.getSummaryMessage(), null);
    }

  }

  private Installation getStagedInstallation()
          throws IOException, ApplicationException
  {
    return new Installation(getStageDirectory());
  }

  private void expandZipFile()
          throws ApplicationException, IOException, QuickSetupException
  {
    File installPackage = getUpgradeUserData().getInstallPackage();
    FileInputStream fis = new FileInputStream(installPackage);
    ZipExtractor extractor = new ZipExtractor(fis,
            1, 10, // TODO figure out these values
            getStageDirectory().getCanonicalPath(),
            Utils.getNumberZipEntries(),
            installPackage.getName(), this);
    extractor.extract();
  }

  /**
   * Delays for a time FOR TESTING ONLY.
   */
  private void sleepFor1() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {

    }
  }

  /**
   * {@inheritDoc}
   */
  public UserData createUserData(String[] args, CurrentInstallStatus cis)
          throws UserDataException
  {
    return getCliHelper().createUserData(args, cis);
  }

  /**
   * {@inheritDoc}
   */
  public ApplicationException getException() {
    return runException;
  }

  private void setCurrentProgressStep(UpgradeProgressStep step) {
    this.currentProgressStep = step;
    String msg = getMsg(step.getSummaryMesssageKey());
    notifyListeners(getFormattedProgress(msg) + getLineBreak());
  }

  private UpgraderCliHelper getCliHelper() {
    if (cliHelper == null) {
      cliHelper = new UpgraderCliHelper();
    }
    return cliHelper;
  }

  private File getTempDirectory() {
    return new File(System.getProperty("java.io.tmpdir"));
  }

  private File getStageDirectory()
          throws ApplicationException, IOException
  {
    if (stagingDirectory == null) {
      File tmpDir = getTempDirectory();
      stagingDirectory =
              new File(tmpDir, "opends-upgrade-tmp-" +
                      System.currentTimeMillis());
      if (stagingDirectory.exists()) {
        FileManager fm = new FileManager(this);
        fm.deleteRecursively(stagingDirectory);
      }
      stagingDirectory.mkdirs();
    }
    return stagingDirectory;
  }

  private UpgradeUserData getUpgradeUserData() {
    return (UpgradeUserData)getUserData();
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
