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

package org.opends.quicksetup;

import static org.opends.server.util.DynamicConstants.PRINTABLE_VERSION_STRING;

import org.opends.quicksetup.util.Utils;
import org.opends.quicksetup.i18n.ResourceProvider;

import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Responsible for providing initial evaluation of command line arguments
 * and determining whether to launch a CLI, GUI, or print a usage statement.
 */
public abstract class Launcher {

  static private final Logger LOG = Logger.getLogger(Launcher.class.getName());

  /** Arguments with which this launcher was invoked. */
  protected String[] args;

  /**
   * Creates a Launcher.
   * @param args String[] of argument passes from the command line
   */
  public Launcher(String[] args) {
    if (args == null) {
      throw new IllegalArgumentException("args cannot be null");
    }
    this.args = args;
  }

  /**
   * Indicates whether or not the launcher should print a usage
   * statement based on the content of the arguments passed into
   * the constructor.
   * @return boolean where true indicates usage should be printed
   */
  protected boolean shouldPrintUsage() {
    boolean printUsage = false;
    if ((args != null) && (args.length > 0)) {
      for (String arg : args) {
        if (arg.equalsIgnoreCase("-H") ||
          arg.equalsIgnoreCase("--help")) {
          printUsage = true;
        }
      }
    }
    return printUsage;
  }

  /**
   * Indicates whether or not the launcher should print a version
   * statement based on the content of the arguments passed into
   * the constructor.
   * @return boolean where true indicates version should be printed
   */
  protected boolean shouldPrintVersion() {
    boolean printVersion = false;
    if ((args != null) && (args.length > 0))
    {
      for (String arg : args)
      {
        if (arg.equalsIgnoreCase("-V") || arg.equalsIgnoreCase("--version"))
        {
          printVersion = true;
        }
      }
    }
    return printVersion;
  }

  /**
   * Indicates whether the launcher will launch a command line versus
   * a graphical application based on the contents of the arguments
   * passed into the constructor.
   *
   * @return boolean where true indicates that a CLI application
   *         should be launched
   */
  protected boolean isCli() {
    boolean isCli = false;
    for (String arg : args) {
      if (arg.equalsIgnoreCase("--cli") || arg.equalsIgnoreCase("-c")) {
        isCli = true;
        break;
      }
    }
    return isCli;
  }

  /**
   * Creates an internationaized message based on the input key and
   * properly formatted for the terminal.
   * @param key for the message in the bundle
   * @return String message properly formatted for the terminal
   */
  protected String getMsg(String key)
  {
    return org.opends.server.util.StaticUtils.wrapText(getI18n().getMsg(key),
        Utils.getCommandLineMaxLineWidth());
  }

  /**
   * Creates an internationaized message based on the input key and
   * properly formatted for the terminal.
   * @param key for the message in the bundle
   * @param args String... arguments for the message
   * @return String message properly formatted for the terminal
   */
  protected String getMsg(String key, String... args)
  {
    return org.opends.server.util.StaticUtils.wrapText(
            getI18n().getMsg(key, args),
        Utils.getCommandLineMaxLineWidth());
  }

  /**
   * Prints a usage message to the terminal.
   * @param i18nMsg localized user message that will be printed to the terminal.
   * @param toStdErr whether the message must be printed to the standard error
   * or the standard output.
   */
  protected void printUsage(String i18nMsg, boolean toStdErr) {
    if (toStdErr)
    {
      System.err.println(i18nMsg);
    }
    else
    {
      System.out.println(i18nMsg);
    }
  }

  /**
   * Launches the graphical uninstall. The graphical uninstall is launched in a
   * different thread that the main thread because if we have a problem with the
   * graphical system (for instance the DISPLAY environment variable is not
   * correctly set) the native libraries will call exit. However if we launch
   * this from another thread, the thread will just be killed.
   *
   * This code also assumes that if the call to SplashWindow.main worked (and
   * the splash screen was displayed) we will never get out of it (we will call
   * a System.exit() when we close the graphical uninstall dialog).
   *
   * @param args String[] the arguments used to call the SplashWindow main
   *         method
   * @return 0 if everything worked fine, or 1 if we could not display properly
   *         the SplashWindow.
   */
  protected int launchGui(final String[] args)
  {
    final int[] returnValue =
      { -1 };
    Thread t = new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          // Setup MacOSX native menu bar before AWT is loaded.
          Utils.setMacOSXMenuBar(getFrameTitle());
          SplashScreen.main(args);
          returnValue[0] = 0;
        }
        catch (Throwable t)
        {
          if (QuickSetupLog.isInitialized())
          {
            LOG.log(Level.WARNING, "Error launching GUI: "+t);
            StringBuilder buf = new StringBuilder();
            while (t != null)
            {
              StackTraceElement[] stack = t.getStackTrace();
              for (StackTraceElement aStack : stack) {
                buf.append(aStack.toString()).append("\n");
              }

              t = t.getCause();
              if (t != null)
              {
                buf.append("Root cause:\n");
              }
            }
            LOG.log(Level.WARNING, buf.toString());
          }
        }
      }
    });
    /*
     * This is done to avoid displaying the stack that might occur if there are
     * problems with the display environment.
     */
    PrintStream printStream = System.err;
    System.setErr(new EmptyPrintStream());
    t.start();
    try
    {
      t.join();
    }
    catch (InterruptedException ie)
    {
      /* An error occurred, so the return value will be -1.  We got nothing to
      do with this exception. */
    }
    System.setErr(printStream);
    return returnValue[0];
  }

  /**
   * Gets the frame title of the GUI application that will be used
   * in some operating systems.
   * @return internationalized String representing the frame title
   */
  abstract protected String getFrameTitle();

  /**
   * Launches the command line based uninstall.
   *
   * @param args the arguments passed
   * @param cliApp the CLI application to launch
   * @return 0 if everything worked fine, and an error code if something wrong
   *         occurred.
   */
  protected int launchCli(String[] args, CliApplication cliApp) {
    System.setProperty("org.opends.quicksetup.cli", "true");
    QuickSetupCli cli = new QuickSetupCli(cliApp, args);
    int returnValue = cli.run();
    if (returnValue == QuickSetupCli.USER_DATA_ERROR) {
      printUsage(true);
      System.exit(QuickSetupCli.USER_DATA_ERROR);
    }
    return returnValue;
  }

  /**
   * Prints the version statement to standard output terminal.
   */
  private void printVersion()
  {
    System.out.print(PRINTABLE_VERSION_STRING);
  }

  /**
   * Prints a usage statement to terminal and exits with an error
   * code.
   * @param toStdErr whether the message must be printed to the standard error
   * or the standard output.
   */
  protected abstract void printUsage(boolean toStdErr);

  /**
   * Creates a CLI application that will be run if the
   * launcher needs to launch a CLI application.
   * @return CliApplication that will be run
   */
  protected abstract CliApplication createCliApplication();

  /**
   * Called before the launcher launches the GUI.  Here
   * subclasses can do any application specific things
   * like set system properties of print status messages
   * that need to be done before the GUI launches.
   */
  protected abstract void willLaunchGui();

  /**
   * Called if launching of the GUI failed.  Here
   * subclasses can so application specific things
   * like print a message.
   * @param logFileName the log file containing more information about why
   * the launch failed.
   */
  protected abstract void guiLaunchFailed(String logFileName);

  /**
   * The main method which is called by the command lines.
   */
  public void launch() {
    if (shouldPrintVersion())
    {
      printVersion();
      System.exit(QuickSetupCli.SUCCESSFUL);
    }
    else if (shouldPrintUsage()) {
      printUsage(false);
      System.exit(QuickSetupCli.SUCCESSFUL);
    } else if (isCli()) {
      CliApplication cliApp = createCliApplication();
      int exitCode = launchCli(args, cliApp);
      preExit(cliApp);
      System.exit(exitCode);
    } else {
      willLaunchGui();
      int exitCode = launchGui(args);
      if (exitCode != 0) {
        File logFile = QuickSetupLog.getLogFile();
        if (logFile != null)
        {
          guiLaunchFailed(logFile.toString());
        }
        else
        {
          guiLaunchFailed(null);
        }
        CliApplication cliApp = createCliApplication();
        exitCode = launchCli(args, cliApp);
        if (exitCode != 0) {
          preExit(cliApp);
          System.exit(exitCode);
        }
      }
    }
  }

  private void preExit(CliApplication cliApp) {
    if (cliApp != null) {
      UserData ud = cliApp.getUserData();
      if (ud != null && !ud.isSilent()) {

        // Add an extra space systematically
        System.out.println();

        File logFile = QuickSetupLog.getLogFile();
        if (logFile != null) {
          System.out.println(getMsg("general-see-for-details",
                QuickSetupLog.getLogFile().getPath()));
        }
      }
    }
  }

  /**
   * This class is used to avoid displaying the error message related to display
   * problems that we might have when trying to display the SplashWindow.
   *
   */
  private class EmptyPrintStream extends PrintStream {
    /**
     * Default constructor.
     *
     */
    public EmptyPrintStream()
    {
      super(new ByteArrayOutputStream(), true);
    }

    /**
     * {@inheritDoc}
     */
    public void println(String msg)
    {
    }
  }

  /**
   * Returns the ResourceProvider used to get localized messages.
   * @return the ResourceProvider used to get localized messages.
   */
  protected ResourceProvider getI18n()
  {
    return ResourceProvider.getInstance();
  }

}
