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

package org.opends.quicksetup;

import org.opends.quicksetup.event.ButtonActionListener;
import org.opends.quicksetup.event.ButtonEvent;
import org.opends.quicksetup.event.ProgressUpdateEvent;
import org.opends.quicksetup.event.ProgressUpdateListener;
import org.opends.quicksetup.i18n.ResourceProvider;
import org.opends.quicksetup.installer.FieldName;
import org.opends.quicksetup.ui.QuickSetupDialog;
import org.opends.quicksetup.ui.UIFactory;
import org.opends.quicksetup.util.BackgroundTask;
import org.opends.quicksetup.util.ProgressMessageFormatter;
import org.opends.quicksetup.util.Utils;
import org.opends.quicksetup.util.HtmlProgressMessageFormatter;

import javax.swing.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
 * installer.  Until this class is not finished the WebStart Installer will be
 * on the ProgressStep.DOWNLOADING step.
 *
 */
public class QuickSetup implements ButtonActionListener, ProgressUpdateListener
{

  static private final Logger LOG =
          Logger.getLogger(QuickSetup.class.getName());

  private Application application;

  private CurrentInstallStatus installStatus;

  private WizardStep currentStep;

  private QuickSetupDialog dialog;

  private StringBuffer progressDetails = new StringBuffer();

  private ProgressDescriptor lastDescriptor;

  private ProgressDescriptor lastDisplayedDescriptor;

  private ProgressDescriptor descriptorToDisplay;

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
    ProgressMessageFormatter formatter = new HtmlProgressMessageFormatter();
    try {
      installStatus = new CurrentInstallStatus();

      application = Application.create();
      application.setProgressMessageFormatter(formatter);
      application.setCurrentInstallStatus(installStatus);

      initLookAndFeel();

      /* In the calls to setCurrentStep the dialog will be created */
      setCurrentStep(application.getFirstWizardStep());

    } catch (ApplicationException e) {
      LOG.log(Level.INFO, "error", e);
      throw new RuntimeException("failed to create quicksetup application", e);
    }

  }

  /**
   * Gets the current installation status of the filesystem
   * bits this quick setup is managing.
   * @return CurrentInstallStatus indicating the install status
   */
  public CurrentInstallStatus getInstallStatus() {
    return installStatus;
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

    case CONTINUE_INSTALL:
      continueInstallClicked();
      break;

    case PREVIOUS:
      previousClicked();
      break;

    case CANCEL:
      cancelClicked();
      break;

    case LAUNCH_STATUS_PANEL:
      launchStatusPanelClicked();
      break;

    default:
      throw new IllegalArgumentException("Unknown button name: "
          + ev.getButtonName());
    }
  }

  /**
   * ProgressUpdateListener implementation. Here we take the
   * ProgressUpdateEvent and create an ProgressDescriptor that
   * will be used to update the progress dialog.
   *
   * @param ev the ProgressUpdateEvent we receive.
   *
   * @see #runDisplayUpdater()
   */
  public void progressUpdate(ProgressUpdateEvent ev)
  {
    synchronized (this)
    {
      ProgressDescriptor desc = createInstallProgressDescriptor(ev);
      boolean isLastDescriptor = desc.getProgressStep().isLast();
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
   * We are receiving notifications from the installer and uninstaller (this
   * class is a ProgressListener). However if we lots of notifications updating
   * the progress panel every time we get a progress update can result of a lot
   * of flickering. So the idea here is to have a minimal time between 2 updates
   * of the progress dialog (specified by UPDATE_PERIOD).
   *
   * @see #progressUpdate(ProgressUpdateEvent)
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
      synchronized (this) {
        final ProgressDescriptor desc = descriptorToDisplay;
        if (desc != null) {
          if (desc != lastDisplayedDescriptor) {
            lastDisplayedDescriptor = desc;

            SwingUtilities.invokeLater(new Runnable() {
              public void run() {
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
    final WizardStep cStep = getCurrentStep();
    application.nextClicked(cStep, this);
    BackgroundTask worker = new BackgroundTask() {
      public Object processBackgroundTask() throws UserDataException {
        try {
          application.updateUserData(cStep, QuickSetup.this);
        }
        catch (UserDataException uide) {
          throw uide;
        }
        catch (Throwable t) {
          throw new UserDataException(cStep,
                  getThrowableMsg("bug-msg", t));
        }
        return null;
      }

      public void backgroundTaskCompleted(Object returnValue,
                                          Throwable throwable) {
        getDialog().workerFinished();

        if (throwable != null) {
          UserDataException ude = (UserDataException) throwable;
          displayError(ude.getLocalizedMessage(), getMsg("error-title"));
        } else {
          setCurrentStep(application.getNextWizardStep(cStep));
        }
      }
    };
    getDialog().workerStarted();
    worker.startBackgroundTask();
  }

  /**
   * Method called when user clicks 'Finish' button of the wizard.
   *
   */
  private void finishClicked()
  {
    final WizardStep cStep = getCurrentStep();
    application.finishClicked(cStep, this);
  }

  /**
   * Method called when user clicks 'Previous' button of the wizard.
   *
   */
  private void previousClicked()
  {
    WizardStep cStep = getCurrentStep();
    application.previousClicked(cStep, this);
    setCurrentStep(application.getPreviousWizardStep(cStep));
  }

  /**
   * Method called when user clicks 'Quit' button of the wizard.
   *
   */
  private void quitClicked()
  {
    WizardStep cStep = getCurrentStep();
    application.quitClicked(cStep, this);
  }

  /**
   * Method called when user clicks 'Continue' button in the case where there
   * is something installed.
   */
  private void continueInstallClicked()
  {
    // TODO:  move this stuff to Installer?
    application.forceToDisplay();
    getDialog().forceToDisplay();
    setCurrentStep(Step.WELCOME);
  }

  /**
   * Method called when user clicks 'Close' button of the wizard.
   *
   */
  private void closeClicked()
  {
    WizardStep cStep = getCurrentStep();
    application.closeClicked(cStep, this);
  }

  /**
   * Method called when user clicks 'Cancel' button of the wizard.
   *
   */
  private void cancelClicked()
  {
    WizardStep cStep = getCurrentStep();
    application.cancelClicked(cStep, this);
  }

  private void launchStatusPanelClicked()
  {
    BackgroundTask worker = new BackgroundTask()
    {
      public Object processBackgroundTask() throws UserDataException {
        try
        {
          String rootDirectory;
          if (isWebStart()) {
            rootDirectory = application.getUserData().getServerLocation();
          } else {
            rootDirectory = Utils.getInstallPathFromClasspath();
          }
          Installation installation = new Installation(rootDirectory);
          String cmd = Utils.getPath(installation.getStatusPanelCommandFile());
          ProcessBuilder pb = new ProcessBuilder(cmd);
          Map<String, String> env = pb.environment();
          env.put("JAVA_HOME", System.getProperty("java.home"));
          /* Remove JAVA_BIN to be sure that we use the JVM running the
           * uninstaller JVM to stop the server.
           */
          env.remove("JAVA_BIN");
          Process process = pb.start();
          int returnValue = process.waitFor();

          if (returnValue != 0)
          {
            throw new Error(getMsg("could-not-launch-status-panel-msg"));
          }
        }
        catch (Throwable t)
        {
          // This looks like a bug
          t.printStackTrace();
          throw new Error(getMsg("could-not-launch-status-panel-msg"));
        }
        return null;
      }

      public void backgroundTaskCompleted(Object returnValue,
          Throwable throwable)
      {
        getDialog().workerFinished();

        if (throwable != null)
        {
          displayError(throwable.getMessage(), getMsg("error-title"));
        }
      }
    };
    getDialog().workerStarted();
    worker.startBackgroundTask();
  }

  /**
   * Method called when we want to quit the setup (for instance when the user
   * clicks on 'Close' or 'Quit' buttons and has confirmed that (s)he wants to
   * quit the program.
   *
   */
  public void quit()
  {
    System.exit(0);
  }

  /**
   * Launch the QuickSetup application Open DS.
   */
  public void launch()
  {
    application.addProgressUpdateListener(this);
    new Thread(application).start();
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
   * The following three methods are just commodity methods to get localized
   * messages.
   * @param key String key
   * @return String message
   */
  private String getMsg(String key)
  {
    return getI18n().getMsg(key);
  }

  /**
   * The following three methods are just commodity methods to get localized
   * messages.
   * @param key String key
   * @param args String[] args
   * @return String message
   */
  private String getMsg(String key, String[] args)
  {
    return getI18n().getMsg(key, args);
  }

  /**
   * The following three methods are just commodity methods to get localized
   * messages.
   * @param key String key
   * @param t Throwable throwable
   * @return String message
   */
  private String getThrowableMsg(String key, Throwable t)
  {
    return Utils.getThrowableMsg(getI18n(), key, null, t);
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
  private WizardStep getCurrentStep()
  {
    return currentStep;
  }

  /**
   * Set the current step. This will basically make the required calls in the
   * dialog to display the panel that corresponds to the step passed as
   * argument.
   *
   * @param step The step to be displayed.
   */
  public void setCurrentStep(WizardStep step)
  {
    if (step == null)
    {
      throw new NullPointerException("step is null");
    }
    currentStep = step;
    application.setDisplayedWizardStep(step, application.getUserData(),
        getDialog());
  }

  /**
   * Get the dialog that is displayed.
   *
   * @return the dialog.
   */
  public QuickSetupDialog getDialog()
  {
    if (dialog == null)
    {
      dialog = new QuickSetupDialog(application,
              installStatus);
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
  public void displayError(String msg, String title)
  {
    if (Utils.isCli()) {
      System.err.println(msg);
    } else {
      getDialog().displayError(msg, title);
    }
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
  public boolean displayConfirmation(String msg, String title)
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
  public String getFieldStringValue(FieldName fieldName)
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
  public Object getFieldValue(FieldName fieldName)
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
  public void displayFieldInvalid(FieldName fieldName, boolean invalid)
  {
    getDialog().displayFieldInvalid(fieldName, invalid);
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
   * A methods that creates an ProgressDescriptor based on the value of a
   * ProgressUpdateEvent.
   *
   * @param ev
   *          the ProgressUpdateEvent used to generate the
   *          ProgressDescriptor.
   * @return the ProgressDescriptor.
   */
  private ProgressDescriptor createInstallProgressDescriptor(
      ProgressUpdateEvent ev)
  {
    ProgressStep status = ev.getProgressStep();
    String newProgressLabel = ev.getCurrentPhaseSummary();
    String additionalDetails = ev.getNewLogs();
    Integer ratio = ev.getProgressRatio();

    if (additionalDetails != null)
    {
      progressDetails.append(additionalDetails);
    }

    return new ProgressDescriptor(status, ratio, newProgressLabel,
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
