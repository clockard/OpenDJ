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

package org.opends.quicksetup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;

import javax.swing.SwingUtilities;

import org.opends.quicksetup.event.ButtonActionListener;
import org.opends.quicksetup.event.ButtonEvent;
import org.opends.quicksetup.event.ProgressUpdateEvent;
import org.opends.quicksetup.event.ProgressUpdateListener;
import org.opends.quicksetup.i18n.ResourceProvider;
import org.opends.quicksetup.installer.DataOptions;
import org.opends.quicksetup.installer.FieldName;
import org.opends.quicksetup.installer.InstallProgressDescriptor;
import org.opends.quicksetup.installer.InstallProgressStep;
import org.opends.quicksetup.installer.Installer;
import org.opends.quicksetup.installer.UserInstallData;
import org.opends.quicksetup.installer.UserInstallDataException;
import org.opends.quicksetup.installer.offline.OfflineInstaller;
import org.opends.quicksetup.installer.webstart.WebStartDownloader;
import org.opends.quicksetup.installer.webstart.WebStartInstaller;
import org.opends.quicksetup.ui.QuickSetupDialog;
import org.opends.quicksetup.ui.UIFactory;
import org.opends.quicksetup.uninstaller.Uninstaller;
import org.opends.quicksetup.util.BackgroundTask;
import org.opends.quicksetup.util.Utils;

/**
 * This class is responsible for doing the following:
 *
 * Check whether we are installing or uninstalling and which type of
 * installation we are running.
 *
 * Performs all the checks and validation of the data provided by the user
 * during the setup.
 *
 * It will launch also the installation once the user clicks on 'Finish' if we
 * are installing the product.
 *
 * If we are running a web start installation it will start the background
 * downloading of the jar files that are required to perform the installation
 * (OpenDS.jar, je.jar, etc.).  The global idea is to force the user to
 * download just one jar file (quicksetup.jar) to launch the Web Start
 * installer.  Then QuickSetup will call WebStartDownloader to download the jar
 * files.  Until this class is not finished the WebStart Installer will be on
 * the InstallProgressStep.DOWNLOADING step.
 *
 */
class QuickSetup implements ButtonActionListener, ProgressUpdateListener
{
  // Contains the data provided by the user
  private UserInstallData userData;

  private Installer installer;

  private Uninstaller uninstaller;

  private WebStartDownloader jnlpDownloader;

  private CurrentInstallStatus installStatus;

  private Step currentStep = Step.WELCOME;

  private QuickSetupDialog dialog;

  private StringBuffer progressDetails = new StringBuffer();

  private InstallProgressDescriptor lastDescriptor;

  private InstallProgressDescriptor lastDisplayedDescriptor;

  private InstallProgressDescriptor descriptorToDisplay;

  // Constants used to do checks
  private static final int MIN_DIRECTORY_MANAGER_PWD = 1;

  private static final int MIN_PORT_VALUE = 1;

  private static final int MAX_PORT_VALUE = 65535;

  private static final int MIN_NUMBER_ENTRIES = 1;

  private static final int MAX_NUMBER_ENTRIES = 10000000;

  // Update period of the dialogs.
  private static final int UPDATE_PERIOD = 500;

  /**
   * This method creates the install/uninstall dialogs and to check the current
   * install status. This method must be called outside the event thread because
   * it can perform long operations which can make the user think that the UI is
   * blocked.
   *
   * @param args for the moment this parameter is not used but we keep it in
   * order to (in case of need) pass parameters through the command line.
   */
  public void initialize(String[] args)
  {
    if (isWebStart())
    {
      jnlpDownloader = new WebStartDownloader();
      jnlpDownloader.start(false);
    }
    installStatus = new CurrentInstallStatus();
    initLookAndFeel();
    if (Utils.isUninstall())
    {
      setCurrentStep(Step.CONFIRM_UNINSTALL);
    } else
    {
      setCurrentStep(Step.WELCOME);
    }
  }

  /**
   * This method displays the setup dialog. This method must be called from the
   * event thread.
   */
  public void display()
  {
    getDialog().packAndShow();
  }

  /**
   * ButtonActionListener implementation. It assumes that we are called in the
   * event thread.
   *
   * @param ev the ButtonEvent we receive.
   */
  public void buttonActionPerformed(ButtonEvent ev)
  {
    switch (ev.getButtonName())
    {
    case NEXT:
      nextClicked();
      break;

    case CLOSE:
      closeClicked();
      break;

    case FINISH:
      finishClicked();
      break;

    case QUIT:
      quitClicked();
      break;

    case PREVIOUS:
      previousClicked();
      break;

    default:
      throw new IllegalArgumentException("Unknown button name: "
          + ev.getButtonName());
    }
  }

  /**
   * ProgressUpdateListener implementation. Here we take the ProgressUpdateEvent
   * and create a ProgressDescriptor that will be used to update the progress
   * dialog.
   *
   * @param ev the ProgressUpdateEvent we receive.
   *
   * @see #runDisplayUpdater()
   */
  public void progressUpdate(ProgressUpdateEvent ev)
  {
    synchronized (this)
    {
      InstallProgressDescriptor desc = createProgressDescriptor(ev);
      boolean isLastDescriptor =
          desc.getProgressStep() == InstallProgressStep.FINISHED_SUCCESSFULLY
              || desc.getProgressStep() ==
                InstallProgressStep.FINISHED_WITH_ERROR;
      if (isLastDescriptor)
      {
        lastDescriptor = desc;
      }

      descriptorToDisplay = desc;
    }
  }

  /**
   * This method is used to update the progress dialog.
   *
   * We are receiving notifications from the installer (this class is a
   * ProgressListener). However if we lots of notifications updating the
   * progress panel every time we get a progress update can result of a lot of
   * flickering. So the idea here is to have a minimal time between 2 updates of
   * the progress dialog (specified by UPDATE_PERIOD).
   *
   * @see #progressUpdate(ProgressUpdateEvent ev)
   *
   */
  private void runDisplayUpdater()
  {
    boolean doPool = true;
    while (doPool)
    {
      try
      {
        Thread.sleep(UPDATE_PERIOD);
      } catch (Exception ex)
      {
      }
      synchronized (this)
      {
        final InstallProgressDescriptor desc = descriptorToDisplay;
        if (desc != null)
        {
          if (desc != lastDisplayedDescriptor)
          {
            lastDisplayedDescriptor = desc;

            SwingUtilities.invokeLater(new Runnable()
            {
              public void run()
              {
                getDialog().displayProgress(desc);
              }
            });
          }
          doPool = desc != lastDescriptor;
        }
      }
    }
  }

  /**
   * Method called when user clicks 'Next' button of the wizard.
   *
   */
  private void nextClicked()
  {
    final Step cStep = getCurrentStep();
    switch (cStep)
    {
    case PROGRESS:
      throw new IllegalStateException(
          "Cannot click on next from progress step");

    case REVIEW:
      throw new IllegalStateException("Cannot click on next from review step");

    default:
      BackgroundTask worker = new BackgroundTask()
      {
        public Object processBackgroundTask() throws UserInstallDataException
        {
          try
          {
            updateUserData(cStep);
          } catch (RuntimeException re)
          {
            throw new UserInstallDataException(getCurrentStep(),
                getExceptionMsg("bug-msg", re));
          }
          return null;
        }

        public void backgroundTaskCompleted(Object returnValue,
            Throwable throwable)
        {
          getDialog().workerFinished();
          UserInstallDataException ude = (UserInstallDataException)throwable;
          if (ude != null)
          {
            displayError(ude.getLocalizedMessage(), getMsg("error-title"));
          } else
          {
            setCurrentStep(nextStep(cStep));
          }
        }
      };
      getDialog().workerStarted();
      worker.startBackgroundTask();
    }
  }

  /**
   * Method called when user clicks 'Finish' button of the wizard.
   *
   */
  private void finishClicked()
  {
    Step cStep = getCurrentStep();
    switch (cStep)
    {
    case REVIEW:
      updateUserDataForReviewPanel();
      launchInstallation();
      setCurrentStep(Step.PROGRESS);
      break;

    case CONFIRM_UNINSTALL:
      launchUninstallation();
      setCurrentStep(Step.PROGRESS);
      break;

    default:
      throw new IllegalStateException(
          "Cannot click on finish when we are not in the Review window");
    }
  }

  /**
   * Method called when user clicks 'Previous' button of the wizard.
   *
   */
  private void previousClicked()
  {
    Step cStep = getCurrentStep();
    switch (cStep)
    {
    case WELCOME:
      throw new IllegalStateException(
          "Cannot click on previous from progress step");

    case PROGRESS:
      throw new IllegalStateException(
          "Cannot click on previous from progress step");

    default:
      setCurrentStep(previousStep(cStep));
    }
  }

  /**
   * Method called when user clicks 'Quit' button of the wizard.
   *
   */
  private void quitClicked()
  {
    Step cStep = getCurrentStep();
    switch (cStep)
    {
    case PROGRESS:
      throw new IllegalStateException(
          "Cannot click on quit from progress step");

    default:
      if (installStatus.isInstalled())
      {
        quit();
      } else if (displayConfirmation(getMsg("confirm-quit-quicksetup-msg"),
          getMsg("confirm-quit-quicksetup-title")))
      {
        quit();
      }
    }
  }

  /**
   * Method called when user clicks 'Close' button of the wizard.
   *
   */
  private void closeClicked()
  {
    Step cStep = getCurrentStep();
    switch (cStep)
    {
    case PROGRESS:
      if (Utils.isUninstall())
      {
        boolean finished = uninstaller.isFinished();
        if (finished
            || displayConfirmation(getMsg("confirm-close-uninstall-msg"),
                getMsg("confirm-close-uninstall-title")))
        {
          quit();
        }
      } else
      {
        boolean finished = installer.isFinished();
        if (finished
            || displayConfirmation(getMsg("confirm-close-quicksetup-msg"),
                getMsg("confirm-close-quicksetup-title")))
        {
          quit();
        }
      }
      break;

    default:
      throw new IllegalStateException(
          "Close only can be clicked on PROGRESS step");
    }
  }

  /**
   * Method called when we want to quit the setup (for instance when the user
   * clicks on 'Close' or 'Quit' buttons and has confirmed that (s)he wants to
   * quit the program.
   *
   */
  private void quit()
  {
    System.exit(0);
  }

  /**
   * These methods validate the data provided by the user in the panels and
   * update the UserData object according to that content.
   *
   * @param cStep
   *          the current step of the wizard
   *
   * @throws an
   *           UserInstallDataException if the data provided by the user is not
   *           valid.
   *
   */
  private void updateUserData(Step cStep) throws UserInstallDataException
  {
    switch (cStep)
    {
    case SERVER_SETTINGS:
      updateUserDataForServerSettingsPanel();
      break;

    case DATA_OPTIONS:
      updateUserDataForDataOptionsPanel();
      break;

    case REVIEW:
      updateUserDataForReviewPanel();
      break;
    }
  }

  /**
   * Validate the data provided by the user in the server settings panel and
   * update the UserData object according to that content.
   *
   * @throws an
   *           UserInstallDataException if the data provided by the user is not
   *           valid.
   *
   */
  private void updateUserDataForServerSettingsPanel()
      throws UserInstallDataException
  {
    ArrayList<String> errorMsgs = new ArrayList<String>();

    if (isWebStart())
    {
      // Check the server location
      String serverLocation = getFieldStringValue(FieldName.SERVER_LOCATION);

      if ((serverLocation == null) || ("".equals(serverLocation.trim())))
      {
        errorMsgs.add(getMsg("empty-server-location"));
        displayFieldInvalid(FieldName.SERVER_LOCATION, true);
      } else if (!Utils.parentDirectoryExists(serverLocation))
      {
        String[] arg =
          { serverLocation };
        errorMsgs.add(getMsg("parent-directory-does-not-exist", arg));
        displayFieldInvalid(FieldName.SERVER_LOCATION, true);
      } else if (Utils.fileExists(serverLocation))
      {
        String[] arg =
          { serverLocation };
        errorMsgs.add(getMsg("file-exists", arg));
        displayFieldInvalid(FieldName.SERVER_LOCATION, true);
      } else if (Utils.directoryExistsAndIsNotEmpty(serverLocation))
      {
        String[] arg =
          { serverLocation };
        errorMsgs.add(getMsg("directory-exists-not-empty", arg));
        displayFieldInvalid(FieldName.SERVER_LOCATION, true);
      } else if (!Utils.canWrite(serverLocation))
      {
        String[] arg =
          { serverLocation };
        errorMsgs.add(getMsg("directory-not-writable", arg));
        displayFieldInvalid(FieldName.SERVER_LOCATION, true);

      } else if (!Utils.hasEnoughSpace(serverLocation,
          getRequiredInstallSpace()))
      {
        long requiredInMb = getRequiredInstallSpace() / (1024 * 1024);
        String[] args =
          { serverLocation, String.valueOf(requiredInMb) };
        errorMsgs.add(getMsg("not-enough-disk-space", args));
        displayFieldInvalid(FieldName.SERVER_LOCATION, true);

      } else
      {
        getUserData().setServerLocation(serverLocation);
        displayFieldInvalid(FieldName.SERVER_LOCATION, false);
      }
    }

    // Check the port
    String sPort = getFieldStringValue(FieldName.SERVER_PORT);
    try
    {
      int port = Integer.parseInt(sPort);
      if ((port < MIN_PORT_VALUE) || (port > MAX_PORT_VALUE))
      {
        String[] args =
          { String.valueOf(MIN_PORT_VALUE), String.valueOf(MAX_PORT_VALUE) };
        errorMsgs.add(getMsg("invalid-port-value-range", args));
        displayFieldInvalid(FieldName.SERVER_PORT, true);
      } else if (!Utils.canUseAsPort(port))
      {
        if (Utils.isPriviledgedPort(port))
        {
          errorMsgs.add(getMsg("cannot-bind-priviledged-port", new String[]
            { String.valueOf(port) }));
        } else
        {
          errorMsgs.add(getMsg("cannot-bind-port", new String[]
            { String.valueOf(port) }));
        }
        displayFieldInvalid(FieldName.SERVER_PORT, true);

      } else
      {
        getUserData().setServerPort(port);
        displayFieldInvalid(FieldName.SERVER_PORT, false);
      }

    } catch (NumberFormatException nfe)
    {
      String[] args =
        { String.valueOf(MIN_PORT_VALUE), String.valueOf(MAX_PORT_VALUE) };
      errorMsgs.add(getMsg("invalid-port-value-range", args));
      displayFieldInvalid(FieldName.SERVER_PORT, true);
    }

    // Check the Directory Manager DN
    String dmDn = getFieldStringValue(FieldName.DIRECTORY_MANAGER_DN);

    if ((dmDn == null) || (dmDn.trim().length() == 0))
    {
      errorMsgs.add(getMsg("empty-directory-manager-dn"));
      displayFieldInvalid(FieldName.DIRECTORY_MANAGER_DN, true);
    } else if (!Utils.isDn(dmDn))
    {
      errorMsgs.add(getMsg("not-a-directory-manager-dn"));
      displayFieldInvalid(FieldName.DIRECTORY_MANAGER_DN, true);
    } else if (Utils.isConfigurationDn(dmDn))
    {
      errorMsgs.add(getMsg("directory-manager-dn-is-config-dn"));
      displayFieldInvalid(FieldName.DIRECTORY_MANAGER_DN, true);
    } else
    {
      getUserData().setDirectoryManagerDn(dmDn);
      displayFieldInvalid(FieldName.DIRECTORY_MANAGER_DN, false);
    }

    // Check the provided passwords
    String pwd1 = getFieldStringValue(FieldName.DIRECTORY_MANAGER_PWD);
    String pwd2 = getFieldStringValue(FieldName.DIRECTORY_MANAGER_PWD_CONFIRM);
    if (pwd1 == null)
    {
      pwd1 = "";
    }

    boolean pwdValid = true;
    if (!pwd1.equals(pwd2))
    {
      errorMsgs.add(getMsg("not-equal-pwd"));
      displayFieldInvalid(FieldName.DIRECTORY_MANAGER_PWD_CONFIRM, true);
      pwdValid = false;

    }
    if (pwd1.length() < MIN_DIRECTORY_MANAGER_PWD)
    {
      errorMsgs.add(getMsg(("pwd-too-short"), new String[]
        { String.valueOf(MIN_DIRECTORY_MANAGER_PWD) }));
      displayFieldInvalid(FieldName.DIRECTORY_MANAGER_PWD, true);
      if ((pwd2 == null) || (pwd2.length() < MIN_DIRECTORY_MANAGER_PWD))
      {
        displayFieldInvalid(FieldName.DIRECTORY_MANAGER_PWD_CONFIRM, true);
      }
      pwdValid = false;
    }

    if (pwdValid)
    {
      getUserData().setDirectoryManagerPwd(pwd1);
      displayFieldInvalid(FieldName.DIRECTORY_MANAGER_PWD, false);
      displayFieldInvalid(FieldName.DIRECTORY_MANAGER_PWD_CONFIRM, false);
    }

    if (errorMsgs.size() > 0)
    {
      throw new UserInstallDataException(Step.SERVER_SETTINGS,
          getStringFromCollection(errorMsgs));
    }
  }

  /**
   * Validate the data provided by the user in the data options panel and update
   * the UserData object according to that content.
   *
   * @throws an
   *           UserInstallDataException if the data provided by the user is not
   *           valid.
   *
   */
  private void updateUserDataForDataOptionsPanel()
      throws UserInstallDataException
  {
    ArrayList<String> errorMsgs = new ArrayList<String>();

    DataOptions dataOptions = null;

    // Check the base dn
    boolean validBaseDn = false;
    String baseDn = getFieldStringValue(FieldName.DIRECTORY_BASE_DN);
    if ((baseDn == null) || (baseDn.trim().length() == 0))
    {
      errorMsgs.add(getMsg("empty-base-dn"));
      displayFieldInvalid(FieldName.DIRECTORY_BASE_DN, true);
    } else if (!Utils.isDn(baseDn))
    {
      errorMsgs.add(getMsg("not-a-base-dn"));
      displayFieldInvalid(FieldName.DIRECTORY_BASE_DN, true);
    } else if (Utils.isConfigurationDn(baseDn))
    {
      errorMsgs.add(getMsg("base-dn-is-configuration-dn"));
      displayFieldInvalid(FieldName.DIRECTORY_BASE_DN, true);
    } else
    {
      displayFieldInvalid(FieldName.DIRECTORY_BASE_DN, false);
      validBaseDn = true;
    }

    // Check the data options
    DataOptions.Type type =
        (DataOptions.Type) getFieldValue(FieldName.DATA_OPTIONS);

    switch (type)
    {
    case IMPORT_FROM_LDIF_FILE:
      String ldifPath = getFieldStringValue(FieldName.LDIF_PATH);
      if ((ldifPath == null) || (ldifPath.trim().equals("")))
      {
        errorMsgs.add(getMsg("no-ldif-path"));
        displayFieldInvalid(FieldName.LDIF_PATH, true);
      } else if (!Utils.fileExists(ldifPath))
      {
        errorMsgs.add(getMsg("ldif-file-does-not-exist"));
        displayFieldInvalid(FieldName.LDIF_PATH, true);
      } else if (validBaseDn)
      {
        dataOptions = new DataOptions(type, baseDn, ldifPath);
        displayFieldInvalid(FieldName.LDIF_PATH, false);
      }
      break;

    case IMPORT_AUTOMATICALLY_GENERATED_DATA:
      // variable used to know if everything went ok during these
      // checks
      int startErrors = errorMsgs.size();

      // Check the number of entries
      String nEntries = getFieldStringValue(FieldName.NUMBER_ENTRIES);
      if ((nEntries == null) || (nEntries.trim().equals("")))
      {
        errorMsgs.add(getMsg("no-number-entries"));
        displayFieldInvalid(FieldName.NUMBER_ENTRIES, true);
      } else
      {
        boolean nEntriesValid = false;
        try
        {
          int n = Integer.parseInt(nEntries);

          nEntriesValid = n >= MIN_NUMBER_ENTRIES && n <= MAX_NUMBER_ENTRIES;
        } catch (NumberFormatException nfe)
        {
        }

        if (!nEntriesValid)
        {
          String[] args =
                { String.valueOf(MIN_NUMBER_ENTRIES),
                    String.valueOf(MAX_NUMBER_ENTRIES) };
          errorMsgs.add(getMsg("invalid-number-entries-range", args));
          displayFieldInvalid(FieldName.NUMBER_ENTRIES, true);
        } else
        {
          displayFieldInvalid(FieldName.NUMBER_ENTRIES, false);
        }
      }
      if (startErrors == errorMsgs.size() && validBaseDn)
      {
        // No validation errors
        dataOptions = new DataOptions(type, baseDn, new Integer(nEntries));
      }
      break;

    default:
      displayFieldInvalid(FieldName.LDIF_PATH, false);
      displayFieldInvalid(FieldName.NUMBER_ENTRIES, false);
      if (validBaseDn)
      {
        dataOptions = new DataOptions(type, baseDn);
      }
    }

    if (dataOptions != null)
    {
      getUserData().setDataOptions(dataOptions);
    }

    if (errorMsgs.size() > 0)
    {
      throw new UserInstallDataException(Step.DATA_OPTIONS,
          getStringFromCollection(errorMsgs));
    }
  }

  /**
   * Update the UserData object according to the content of the review panel.
   *
   */
  private void updateUserDataForReviewPanel()
  {
    Boolean b = (Boolean) getFieldValue(FieldName.SERVER_START);
    getUserData().setStartServer(b.booleanValue());
  }

  /**
   * Launch the installation of Open DS. Depending on whether we are running a
   * web start or not it will use on Installer object or other.
   *
   */
  private void launchInstallation()
  {
    if (isWebStart())
    {
      installer = new WebStartInstaller(getUserData(), jnlpDownloader);
    } else
    {
      installer = new OfflineInstaller(getUserData());
    }
    installer.addProgressUpdateListener(this);
    installer.start();
    Thread t = new Thread(new Runnable()
    {
      public void run()
      {
        runDisplayUpdater();
      }
    });
    t.start();
  }

  /**
   * Launch the uninstallation of Open DS.
   *
   */
  private void launchUninstallation()
  {
    uninstaller = new Uninstaller();
    // TO COMPLETE
  }

  /**
   * Provides the object representing the data provided by the user in the
   * install.
   *
   * @return the UserInstallData representing the data provided by the user in
   *         the Install wizard.
   */
  private UserInstallData getUserData()
  {
    if (userData == null)
    {
      userData = new UserInstallData();
    }
    return userData;
  }

  /**
   * Provides an object representing the default data/install parameters that
   * will be proposed to the user in the Installation wizard. This data includes
   * elements such as the default dn of the directory manager or the default
   * install location.
   *
   * @return the UserInstallData representing the default data/parameters that
   *         will be proposed to the user.
   */
  private UserInstallData getDefaultUserData()
  {
    UserInstallData defaultUserData = new UserInstallData();

    DataOptions defaultDataOptions = new DefaultDataOptions();

    defaultUserData.setServerLocation(Utils.getDefaultServerLocation());
    // See what we can propose as port
    int defaultPort = getDefaultPort();
    if (defaultPort != -1)
    {
      defaultUserData.setServerPort(defaultPort);
    }
    defaultUserData.setDirectoryManagerDn("cn=Directory Manager");

    defaultUserData.setDataOptions(defaultDataOptions);
    defaultUserData.setStartServer(true);

    return defaultUserData;
  }

  /**
   * The following three methods are just commodity methods to get localized
   * messages.
   */
  private String getMsg(String key)
  {
    return getI18n().getMsg(key);
  }

  private String getMsg(String key, String[] args)
  {
    return getI18n().getMsg(key, args);
  }

  private String getExceptionMsg(String key, Exception ex)
  {
    return Utils.getExceptionMsg(getI18n(), key, null, ex);
  }

  private ResourceProvider getI18n()
  {
    return ResourceProvider.getInstance();
  }

  /**
   * Get the current step.
   *
   * @return the currently displayed Step of the wizard.
   */
  private Step getCurrentStep()
  {
    return currentStep;
  }

  /**
   * Set the current step. This will basically make the required calls in the
   * dialog to display the panel that corresponds to the step passed as
   * argument.
   *
   * @param step.
   *          The step to be displayed.
   */
  private void setCurrentStep(Step step)
  {
    if (step == null)
    {
      throw new NullPointerException("step is null");
    }
    currentStep = step;
    getDialog().setDisplayedStep(step, getUserData());
  }

  /**
   * Gets the next step corresponding to the step passed as parameter.
   *
   * @param step,
   *          the step of which we want to get the new step.
   * @return the next step for the current step.
   * @throws IllegalArgumentException
   *           if the current step has not a next step.
   */
  private Step nextStep(Step step)
  {
    Iterator<Step> it = EnumSet.range(step, Step.PROGRESS).iterator();
    it.next();
    if (!it.hasNext())
    {
      throw new IllegalArgumentException("No next for step: " + step);
    }
    return it.next();
  }

  /**
   * Gets the previous step corresponding to the step passed as parameter.
   *
   * @param step,
   *          the step of which we want to get the previous step.
   * @return the next step for the current step.
   * @throws IllegalArgumentException
   *           if the current step has not a previous step.
   */
  private Step previousStep(Step step)
  {
    Step previous = null;
    for (Step s : Step.values())
    {
      if (s == step)
      {
        return previous;
      }
      previous = s;
    }
    throw new IllegalArgumentException("No previous for step: " + step);
  }

  /**
   * Get the dialog that is displayed.
   *
   * @return the dialog.
   */
  private QuickSetupDialog getDialog()
  {
    if (dialog == null)
    {
      dialog = new QuickSetupDialog(getDefaultUserData(), installStatus);
      dialog.addButtonActionListener(this);
    }
    return dialog;
  }

  /**
   * Displays an error message dialog.
   *
   * @param msg
   *          the error message.
   * @param title
   *          the title for the dialog.
   */
  private void displayError(String msg, String title)
  {
    getDialog().displayError(msg, title);
  }

  /**
   * Displays a confirmation message dialog.
   *
   * @param msg
   *          the confirmation message.
   * @param title
   *          the title of the dialog.
   * @return <CODE>true</CODE> if the user confirms the message, or
   * <CODE>false</CODE> if not.
   */
  private boolean displayConfirmation(String msg, String title)
  {
    return getDialog().displayConfirmation(msg, title);
  }

  /**
   * Gets the string value for a given field name.
   *
   * @param fieldName
   *          the field name object.
   * @return the string value for the field name.
   */
  private String getFieldStringValue(FieldName fieldName)
  {
    String sValue = null;

    Object value = getFieldValue(fieldName);
    if (value != null)
    {
      if (value instanceof String)
      {
        sValue = (String) value;
      } else
      {
        sValue = String.valueOf(value);
      }
    }
    return sValue;
  }

  /**
   * Gets the value for a given field name.
   *
   * @param fieldName
   *          the field name object.
   * @return the value for the field name.
   */
  private Object getFieldValue(FieldName fieldName)
  {
    return getDialog().getFieldValue(fieldName);
  }

  /**
   * Marks the fieldName as valid or invalid depending on the value of the
   * invalid parameter. With the current implementation this implies basically
   * using a red color in the label associated with the fieldName object. The
   * color/style used to mark the label invalid is specified in UIFactory.
   *
   * @param fieldName
   *          the field name object.
   * @param invalid
   *          whether to mark the field valid or invalid.
   */
  private void displayFieldInvalid(FieldName fieldName, boolean invalid)
  {
    getDialog().displayFieldInvalid(fieldName, invalid);
  }

  /**
   * This is a helper method that gets a String representation of the elements
   * in the Collection. The String will display the different elements separated
   * by a '\n' character.
   *
   * @param col
   *          the collection containing the String.
   * @return the String representation for the collection.
   */
  private String getStringFromCollection(Collection<String> col)
  {
    String msg = null;
    for (String m : col)
    {
      if (msg == null)
      {
        msg = m;
      } else
      {
        msg += "\n" + m;
      }
    }
    return msg;
  }

  /**
   * A method to initialize the look and feel.
   *
   */
  private void initLookAndFeel()
  {
    UIFactory.initialize();
  }

  /**
   * A methods that creates a InstallProgressDescriptor based on the value of a
   * ProgressUpdateEvent.
   *
   * @param ev
   *          the ProgressUpdateEvent used to generate the
   *          InstallProgressDescriptor.
   * @return the InstallProgressDescriptor.
   */
  private InstallProgressDescriptor createProgressDescriptor(
      ProgressUpdateEvent ev)
  {
    InstallProgressStep status = ev.getProgressStep();
    String newProgressLabel = ev.getCurrentPhaseSummary();
    String additionalDetails = ev.getNewLogs();
    Integer ratio = ev.getProgressRatio();

    if (additionalDetails != null)
    {
      progressDetails.append(additionalDetails);
    }

    return new InstallProgressDescriptor(status, ratio, newProgressLabel,
        progressDetails.toString());
  }

  /**
   * Indicates whether we are in a web start installation or not.
   *
   * @return <CODE>true</CODE> if we are in a web start installation and
   *         <CODE>false</CODE> if not.
   */
  private boolean isWebStart()
  {
    return Utils.isWebStart();
  }

  /**
   * Provides the port that will be proposed to the user in the second page of
   * the installation wizard. It will check whether we can use 389 and if not it
   * will return -1.
   *
   * @return the port 389 if it is available and we can use and -1 if not.
   */
  private int getDefaultPort()
  {
    int defaultPort = -1;
    if (Utils.canUseAsPort(389))
    {
      defaultPort = 389;
    }
    return defaultPort;
  }

  /**
   * Returns the number of free disk space in bytes required to install Open DS
   *
   * For the moment we just return 15 Megabytes. TODO we might want to have
   * something dynamic to calculate the required free disk space for the
   * installation.
   *
   * @return the number of free disk space required to install Open DS.
   */
  private long getRequiredInstallSpace()
  {
    return 15 * 1024 * 1024;
  }
}

/**
 * This class is just used to specify which are the default values that will be
 * proposed to the user in the Data Options panel of the Installation wizard.
 *
 */
class DefaultDataOptions extends DataOptions
{
  /**
   * Default constructor.
   *
   */
  public DefaultDataOptions()
  {
    super(Type.CREATE_BASE_ENTRY, "dc=example,dc=com");
  }

  /**
   * Get the number of entries that will be automatically generated.
   *
   * @return the number of entries that will be automatically generated.
   */
  public int getNumberEntries()
  {
    return 2000;
  }
}
