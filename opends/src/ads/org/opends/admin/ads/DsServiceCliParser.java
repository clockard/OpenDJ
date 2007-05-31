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
package org.opends.admin.ads;

import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.server.messages.MessageHandler.getMessage;
import static org.opends.server.messages.ToolMessages.*;
import static org.opends.server.tools.ToolConstants.*;
import static org.opends.server.util.ServerConstants.MAX_LINE_WIDTH;
import static org.opends.server.util.StaticUtils.wrapText;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.DebugLogLevel;
import org.opends.server.util.PasswordReader;
import org.opends.server.util.args.ArgumentException;
import org.opends.server.util.args.BooleanArgument;
import org.opends.server.util.args.FileBasedArgument;
import org.opends.server.util.args.IntegerArgument;
import org.opends.server.util.args.StringArgument;
import org.opends.server.util.args.SubCommand;
import org.opends.server.util.args.SubCommandArgumentParser;

/**
 * This class will parser CLI arguments.
 */
public class DsServiceCliParser extends SubCommandArgumentParser
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();

  /**
   * The showUsage' global argument.
   */
  private BooleanArgument showUsageArg = null;

  /**
   * The 'useSSLArg' global argument.
   */
  private BooleanArgument useSSLArg = null;

  /**
   * The 'hostName' global argument.
   */
  private StringArgument hostNameArg = null;

  /**
   * The 'port' global argument.
   */
  private IntegerArgument portArg = null;

  /**
   * The 'binDN' global argument.
   */
  private StringArgument bindDnArg = null;

  /**
   * The 'bindPasswordFile' global argument.
   */
  private FileBasedArgument bindPasswordFileArg = null;

  /**
   * The 'verbose' global argument.
   */
  private BooleanArgument verboseArg = null;

  /**
   * The diferent CLI group.
   */
  public HashSet<DsServiceCliSubCommandGroup> cliGroup;



  /**
   * Creates a new instance of this subcommand argument parser with no
   * arguments.
   *
   * @param mainClassName
   *          The fully-qualified name of the Java class that should
   *          be invoked to launch the program with which this
   *          argument parser is associated.
   * @param toolDescription
   *          A human-readable description for the tool, which will be
   *          included when displaying usage information.
   * @param longArgumentsCaseSensitive
   *          Indicates whether subcommand and long argument names
   *          should be treated in a case-sensitive manner.
   */
  public DsServiceCliParser(String mainClassName, String toolDescription,
      boolean longArgumentsCaseSensitive)
  {
    super(mainClassName, toolDescription, longArgumentsCaseSensitive);
    cliGroup = new HashSet<DsServiceCliSubCommandGroup>();
  }

  /**
   * Initialize the parser with the Gloabal options ans subcommands.
   *
   * @param outStream
   *          The output stream to use for standard output, or <CODE>null</CODE>
   *          if standard output is not needed.
   * @throws ArgumentException
   *           If there is a problem with any of the parameters used
   *           to create this argument.
   */
  public void initializeParser(OutputStream outStream)
      throws ArgumentException
  {
    // Global parameters
    initializeGlobalOption(outStream);

    // ads  Group cli
    cliGroup.add(new DsServiceCliAds());

    // Server Group cli
    cliGroup.add(new DsServiceCliServerGroup());

    // Initialization
    for (DsServiceCliSubCommandGroup oneCli : cliGroup)
    {
      oneCli.initializeCliGroup(this, verboseArg);
    }
  }

  /**
   * Initialize Global option.
   *
   * @param outStream
   *          The output stream used forn the usage.
   * @throws ArgumentException
   *           If there is a problem with any of the parameters used
   *           to create this argument.
   */
  private void initializeGlobalOption(OutputStream outStream)
  throws ArgumentException
  {
    showUsageArg = new BooleanArgument("showUsage", OPTION_SHORT_HELP,
        OPTION_LONG_HELP, MSGID_DESCRIPTION_SHOWUSAGE);
    addGlobalArgument(showUsageArg);
    setUsageArgument(showUsageArg, outStream);

    useSSLArg = new BooleanArgument("useSSL", OPTION_SHORT_USE_SSL,
        OPTION_LONG_USE_SSL, MSGID_DESCRIPTION_USE_SSL);
    addGlobalArgument(useSSLArg);

    hostNameArg = new StringArgument("host", OPTION_SHORT_HOST,
        OPTION_LONG_HOST, false, false, true, OPTION_VALUE_HOST, "localhost",
        null, MSGID_DESCRIPTION_HOST);
    addGlobalArgument(hostNameArg);

    portArg = new IntegerArgument("port", OPTION_SHORT_PORT, OPTION_LONG_PORT,
        false, false, true, OPTION_VALUE_PORT, 389, null,
        MSGID_DESCRIPTION_PORT);
    addGlobalArgument(portArg);

    bindDnArg = new StringArgument("bindDN", OPTION_SHORT_BINDDN,
        OPTION_LONG_BINDDN, false, false, true, OPTION_VALUE_BINDDN,
        "cn=Directory Manager", null, MSGID_DESCRIPTION_BINDDN);
    addGlobalArgument(bindDnArg);

    bindPasswordFileArg = new FileBasedArgument("bindPasswordFile",
        OPTION_SHORT_BINDPWD_FILE, OPTION_LONG_BINDPWD_FILE, false, false,
        OPTION_VALUE_BINDPWD_FILE, null, null,
        MSGID_DESCRIPTION_BINDPASSWORDFILE);
    addGlobalArgument(bindPasswordFileArg);

    verboseArg = new BooleanArgument("verbose", 'v', "verbose",
        MSGID_DESCRIPTION_VERBOSE);
    addGlobalArgument(verboseArg);
  }


  /**
   * Get the host name which has to be used for the command.
   *
   * @return The host name specified by the command line argument, or
   *         the default value, if not specified.
   */
  public String getHostName()
  {
    if (hostNameArg.isPresent())
    {
      return hostNameArg.getValue();
    }
    else
    {
      return hostNameArg.getDefaultValue();
    }

  }

  /**
   * Get the port which has to be used for the command.
   *
   * @return The port specified by the command line argument, or the
   *         default value, if not specified.
   */
  public String getPort()
  {
    if (portArg.isPresent())
    {
      return portArg.getValue();
    }
    else
    {
      return portArg.getDefaultValue();
    }

  }

  /**
   * Get the bindDN which has to be used for the command.
   *
   * @return The bindDN specified by the command line argument, or the
   *         default value, if not specified.
   */
  public String getBindDN()
  {
    if (bindDnArg.isPresent())
    {
      return bindDnArg.getValue();
    }
    else
    {
      return bindDnArg.getDefaultValue();
    }
  }


  /**
   * Get the password which has to be used for the command.
   *
   * @param dn
   *          The user DN for which to password could be asked.
   * @param out
   *          The input stream to used if we have to prompt to the
   *          user.
   * @param err
   *          The error stream to used if we have to prompt to the
   *          user.
   * @return The password stored into the specified file on by the
   *         command line argument, or prompts it if not specified.
   */
  public String getBindPassword(String dn, PrintStream out, PrintStream err)
  {
    if (bindPasswordFileArg.isPresent())
    {
      return bindPasswordFileArg.getValue();
    }
    else
    {
      // read the password from the stdin.
      try
      {
        out.print(getMessage(MSGID_LDAPAUTH_PASSWORD_PROMPT, dn));
        char[] pwChars = PasswordReader.readPassword();
        return new String(pwChars);
      }
      catch (Exception ex)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, ex);
        }
        err.println(wrapText(ex.getMessage(), MAX_LINE_WIDTH));
        return null;
      }
    }
  }

  /**
   * Handle the subcommand.
   *
   * @param adsContext
   *          The context to use to perform ADS operation.
   *
   * @param  outStream         The output stream to use for standard output.
   *
   * @param  errStream         The output stream to use for standard error.
   *
   * @return the return code
   * @throws ADSContextException
   *           If there is a problem with when trying to perform the
   *           operation.
   */
  public int performSubCommand(ADSContext adsContext, OutputStream outStream,
      OutputStream errStream)
    throws ADSContextException
  {
    SubCommand subCmd = getSubCommand();

    for (DsServiceCliSubCommandGroup oneCli : cliGroup)
    {
      if (oneCli.isSubCommand(subCmd))
      {
        return oneCli.performSubCommand(adsContext, subCmd, outStream,
            errStream);
      }
    }

    // Should never occurs: If we are here, it means that the code to
    // handle to subcommand is not yet written.
    return 1;
  }
}
