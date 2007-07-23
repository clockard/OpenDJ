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

import org.opends.quicksetup.Launcher;
import org.opends.quicksetup.CliApplication;
import org.opends.quicksetup.QuickSetupLog;
import org.opends.quicksetup.Installation;
import org.opends.quicksetup.util.Utils;
import org.opends.quicksetup.i18n.ResourceProvider;
import org.opends.server.util.args.ArgumentParser;
import org.opends.server.util.args.BooleanArgument;
import org.opends.server.util.args.FileBasedArgument;
import org.opends.server.util.ServerConstants;
import static org.opends.server.messages.ToolMessages.*;
import static org.opends.server.tools.ToolConstants.OPTION_SHORT_HELP;
import static org.opends.server.tools.ToolConstants.OPTION_LONG_HELP;

import java.io.File;

/**
 * Launches a reversion operation.
 */
public class ReversionLauncher extends Launcher {

  /** Short form of the option for specifying the reversion files directory. */
  static public final Character DIRECTORY_OPTION_SHORT = 'd';

  /** Long form of the option for specifying the reversion files directory. */
  static public final String DIRECTORY_OPTION_LONG = "directory";

  /** Short form of the option for specifying the 'most recent' option. */
  static public final Character MOST_RECENT_OPTION_SHORT = 'm';

  /** Long form of the option for specifying the 'most recent' option. */
  static public final String MOST_RECENT_OPTION_LONG = "mostRecent";

  static private final String LOG_FILE_PREFIX = "opends-revert-";

  /**
   * Creates and launches a reversion operation.
   * @param args from the command line
   */
  static public void main(String[] args) {
    try {
      QuickSetupLog.initLogFileHandler(
              File.createTempFile(LOG_FILE_PREFIX,
                      QuickSetupLog.LOG_FILE_SUFFIX));
    } catch (Throwable t) {
      System.err.println(
              ResourceProvider.getInstance().getMsg("error-initializing-log"));
      t.printStackTrace();
    }
    new ReversionLauncher(args).launch();
  }

  private ArgumentParser argParser;

  private BooleanArgument showUsage;
  private FileBasedArgument dir;
  private BooleanArgument mostRecent;
  private BooleanArgument silent;
  private BooleanArgument interactive;

  /**
   * Gets the file's directory if specified on the command line.
   * @return File representing the directory where the reversion files are
   * stored.
   */
  public File getFilesDirectory() {
    File f = null;
    String s = dir.getValue();
    if (s != null) {
      f = new File(s);
    }
    return f;
  }

  /**
   * Indicates whether the user has specified the 'mostRecent' option.
   * @return boolean where true indicates use the most recent upgrade backup
   */
  public boolean useMostRecentUpgrade() {
    return mostRecent.isPresent();
  }

  /**
   * {@inheritDoc}
   */
  protected boolean isCli() {
    // for now only CLI is supported via command line
    return true;
  }

  /**
   * {@inheritDoc}
   */
  protected String getFrameTitle() {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  protected CliApplication createCliApplication() {
    return new Reverter();
  }

  /**
   * {@inheritDoc}
   */
  protected void willLaunchGui() {
    // not supported;
  }

  /**
   * {@inheritDoc}
   */
  protected void guiLaunchFailed(String logFileName) {
    // not supported;
  }

  /**
   * {@inheritDoc}
   */
  public ArgumentParser getArgumentParser() {
    return argParser;
  }

  /**
   * Creates a new launcher.
   * @param args from the command line
   */
  protected  ReversionLauncher(String[] args) {
    super(args);

    String scriptName;
    if (Utils.isWindows()) {
      scriptName = Installation.WINDOWS_REVERT_FILE_NAME;
    } else {
      scriptName = Installation.UNIX_REVERT_FILE_NAME;
    }
    System.setProperty(ServerConstants.PROPERTY_SCRIPT_NAME, scriptName);

    argParser = new ArgumentParser(getClass().getName(),
        getI18n().getMsg("revert-launcher-usage-description"), false);
    try
    {
      dir = new FileBasedArgument("directory",
              DIRECTORY_OPTION_SHORT,
              DIRECTORY_OPTION_LONG,
              false, false,
              "{directory}",
              null, null, MSGID_REVERT_DESCRIPTION_DIRECTORY);
      argParser.addArgument(dir);

      mostRecent = new BooleanArgument("mostRecent",
              MOST_RECENT_OPTION_SHORT,
              MOST_RECENT_OPTION_LONG,
              MSGID_REVERT_DESCRIPTION_RECENT);
      argParser.addArgument(mostRecent);

      interactive = new BooleanArgument("interactive", 'i', "interactive",
          MSGID_REVERT_DESCRIPTION_INTERACTIVE);
      argParser.addArgument(interactive);

      silent = new BooleanArgument("silent", 's', "silent",
          MSGID_REVERT_DESCRIPTION_SILENT);
      argParser.addArgument(silent);

      showUsage = new BooleanArgument("showusage", OPTION_SHORT_HELP,
        OPTION_LONG_HELP,
        MSGID_DESCRIPTION_USAGE);
      argParser.addArgument(showUsage);
      argParser.setUsageArgument(showUsage);

      argParser.parseArguments(args);
    }
    catch (Throwable t)
    {
      System.out.println("ERROR: "+t);
      t.printStackTrace();
    }

  }

  private void validate(ArgumentParser argParser) {

  }

}
