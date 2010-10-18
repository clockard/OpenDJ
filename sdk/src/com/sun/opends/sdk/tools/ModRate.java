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
 *      Copyright 2010 Sun Microsystems, Inc.
 */

package com.sun.opends.sdk.tools;



import static com.sun.opends.sdk.messages.Messages.*;
import static com.sun.opends.sdk.tools.ToolConstants.*;
import static com.sun.opends.sdk.tools.Utils.filterExitCode;

import java.io.InputStream;
import java.io.OutputStream;

import org.opends.sdk.*;
import org.opends.sdk.requests.ModifyRequest;
import org.opends.sdk.requests.Requests;
import org.opends.sdk.responses.Result;



/**
 * A load generation tool that can be used to load a Directory Server with
 * Modify requests using one or more LDAP connections.
 */
public final class ModRate extends ConsoleApplication
{
  private static final class ModifyPerformanceRunner extends PerformanceRunner
  {
    private final class ModifyWorkerThread extends WorkerThread
    {
      private ModifyRequest mr;
      private Object[] data;



      private ModifyWorkerThread(final AsynchronousConnection connection,
          final ConnectionFactory connectionFactory)
      {
        super(connection, connectionFactory);
      }



      @Override
      public FutureResult<?> performOperation(
          final AsynchronousConnection connection,
          final DataSource[] dataSources, long startTime)
      {
        if (dataSources != null)
        {
          data = DataSource.generateData(dataSources, data);
        }
        mr = newModifyRequest(data);
        return connection.modify(mr,
            new UpdateStatsResultHandler<Result>(startTime));
      }



      private ModifyRequest newModifyRequest(final Object[] data)
      {
        String formattedString;
        int colonPos;
        ModifyRequest mr;
        if (data == null)
        {
          mr = Requests.newModifyRequest(baseDN);
        }
        else
        {
          mr = Requests.newModifyRequest(String.format(baseDN, data));
        }
        for (final String modString : modStrings)
        {
          if (data == null)
          {
            formattedString = modString;
          }
          else
          {
            formattedString = String.format(modString, data);
          }
          colonPos = formattedString.indexOf(':');
          if (colonPos > 0)
          {
            mr.addModification(ModificationType.REPLACE, formattedString
                .substring(0, colonPos), formattedString
                .substring(colonPos + 1));
          }
        }
        return mr;
      }
    }



    private String baseDN;

    private String[] modStrings;



    private ModifyPerformanceRunner(final ArgumentParser argParser,
        final ConsoleApplication app) throws ArgumentException
    {
      super(argParser, app, false, false, false);
    }



    @Override
    StatsThread newStatsThread()
    {
      return new StatsThread(new String[0]);
    }



    @Override
    WorkerThread newWorkerThread(final AsynchronousConnection connection,
        final ConnectionFactory connectionFactory)
    {
      return new ModifyWorkerThread(connection, connectionFactory);
    }
  }



  /**
   * The main method for ModRate tool.
   *
   * @param args
   *          The command-line arguments provided to this program.
   */

  public static void main(final String[] args)
  {
    final int retCode = mainModRate(args, System.in, System.out, System.err);
    System.exit(filterExitCode(retCode));
  }



  /**
   * Parses the provided command-line arguments and uses that information to run
   * the modrate tool.
   *
   * @param args
   *          The command-line arguments provided to this program.
   * @return The error code.
   */

  static int mainModRate(final String[] args)
  {
    return mainModRate(args, System.in, System.out, System.err);
  }



  /**
   * Parses the provided command-line arguments and uses that information to run
   * the modrate tool.
   *
   * @param args
   *          The command-line arguments provided to this program.
   * @param inStream
   *          The input stream to use for standard input, or <CODE>null</CODE>
   *          if standard input is not needed.
   * @param outStream
   *          The output stream to use for standard output, or <CODE>null</CODE>
   *          if standard output is not needed.
   * @param errStream
   *          The output stream to use for standard error, or <CODE>null</CODE>
   *          if standard error is not needed.
   * @return The error code.
   */

  static int mainModRate(final String[] args, final InputStream inStream,
      final OutputStream outStream, final OutputStream errStream)

  {
    return new ModRate(inStream, outStream, errStream).run(args);
  }



  private BooleanArgument verbose;



  private ModRate(final InputStream in, final OutputStream out,
      final OutputStream err)
  {
    super(in, out, err);

  }



  /**
   * Indicates whether or not the user has requested advanced mode.
   *
   * @return Returns <code>true</code> if the user has requested advanced mode.
   */
  @Override
  public boolean isAdvancedMode()
  {
    return false;
  }



  /**
   * Indicates whether or not the user has requested interactive behavior.
   *
   * @return Returns <code>true</code> if the user has requested interactive
   *         behavior.
   */
  @Override
  public boolean isInteractive()
  {
    return false;
  }



  /**
   * Indicates whether or not this console application is running in its
   * menu-driven mode. This can be used to dictate whether output should go to
   * the error stream or not. In addition, it may also dictate whether or not
   * sub-menus should display a cancel option as well as a quit option.
   *
   * @return Returns <code>true</code> if this console application is running in
   *         its menu-driven mode.
   */
  @Override
  public boolean isMenuDrivenMode()
  {
    return false;
  }



  /**
   * Indicates whether or not the user has requested quiet output.
   *
   * @return Returns <code>true</code> if the user has requested quiet output.
   */
  @Override
  public boolean isQuiet()
  {
    return false;
  }



  /**
   * Indicates whether or not the user has requested script-friendly output.
   *
   * @return Returns <code>true</code> if the user has requested script-friendly
   *         output.
   */
  @Override
  public boolean isScriptFriendly()
  {
    return false;
  }



  /**
   * Indicates whether or not the user has requested verbose output.
   *
   * @return Returns <code>true</code> if the user has requested verbose output.
   */
  @Override
  public boolean isVerbose()
  {
    return verbose.isPresent();
  }



  private int run(final String[] args)
  {
    // Create the command-line argument parser for use with this
    // program.
    final LocalizableMessage toolDescription =
        INFO_MODRATE_TOOL_DESCRIPTION.get();
    final ArgumentParser argParser = new ArgumentParser(
        ModRate.class.getName(), toolDescription, false, true, 1, 0,
        "[(attribute:value format string) ...]");
    ConnectionFactoryProvider connectionFactoryProvider;
    ConnectionFactory connectionFactory;
    ModifyPerformanceRunner runner;

    BooleanArgument showUsage;
    StringArgument propertiesFileArgument;
    BooleanArgument noPropertiesFileArgument;
    StringArgument baseDN;

    try
    {
      if(System.getProperty("org.opends.sdk.ldap.transport.linger") == null)
      {
        System.setProperty("org.opends.sdk.ldap.transport.linger", "0");
      }
      connectionFactoryProvider =
          new ConnectionFactoryProvider(argParser, this);
      runner = new ModifyPerformanceRunner(argParser, this);
      propertiesFileArgument = new StringArgument("propertiesFilePath", null,
          OPTION_LONG_PROP_FILE_PATH, false, false, true,
          INFO_PROP_FILE_PATH_PLACEHOLDER.get(), null, null,
          INFO_DESCRIPTION_PROP_FILE_PATH.get());
      argParser.addArgument(propertiesFileArgument);
      argParser.setFilePropertiesArgument(propertiesFileArgument);

      noPropertiesFileArgument = new BooleanArgument(
          "noPropertiesFileArgument", null, OPTION_LONG_NO_PROP_FILE,
          INFO_DESCRIPTION_NO_PROP_FILE.get());
      argParser.addArgument(noPropertiesFileArgument);
      argParser.setNoPropertiesFileArgument(noPropertiesFileArgument);

      baseDN = new StringArgument("targetDN", OPTION_SHORT_BASEDN,
          OPTION_LONG_TARGETDN, true, false, true, INFO_TARGETDN_PLACEHOLDER.get(),
          null, null, INFO_MODRATE_TOOL_DESCRIPTION_TARGETDN.get());
      baseDN.setPropertyName(OPTION_LONG_BASEDN);
      argParser.addArgument(baseDN);

      verbose = new BooleanArgument("verbose", 'v', "verbose",
          INFO_DESCRIPTION_VERBOSE.get());
      verbose.setPropertyName("verbose");
      argParser.addArgument(verbose);

      showUsage = new BooleanArgument("showUsage", OPTION_SHORT_HELP,
          OPTION_LONG_HELP, INFO_DESCRIPTION_SHOWUSAGE.get());
      argParser.addArgument(showUsage);
      argParser.setUsageArgument(showUsage, getOutputStream());
    }
    catch (final ArgumentException ae)
    {
      final LocalizableMessage message = ERR_CANNOT_INITIALIZE_ARGS.get(ae
          .getMessage());
      println(message);
      return ResultCode.CLIENT_SIDE_PARAM_ERROR.intValue();
    }

    // Parse the command-line arguments provided to this program.
    try
    {
      argParser.parseArguments(args);
      connectionFactory =
          connectionFactoryProvider.getAuthenticatedConnectionFactory();
      runner.validate();
    }
    catch (final ArgumentException ae)
    {
      final LocalizableMessage message = ERR_ERROR_PARSING_ARGS.get(ae
          .getMessage());
      println(message);
      println(argParser.getUsageMessage());
      return ResultCode.CLIENT_SIDE_PARAM_ERROR.intValue();
    }

    // If we should just display usage or version information,
    // then print it and exit.
    if (argParser.usageOrVersionDisplayed())
    {
      return 0;
    }

    runner.modStrings = argParser.getTrailingArguments().toArray(
        new String[argParser.getTrailingArguments().size()]);
    runner.baseDN = baseDN.getValue();

    try
    {

      // Try it out to make sure the format string and data sources
      // match.
      final Object[] data = DataSource.generateData(runner.getDataSources(),
          null);
      for (final String modString : runner.modStrings)
      {
        String.format(modString, data);
      }
      String.format(runner.baseDN, data);
    }
    catch (final Exception ex1)
    {
      println(LocalizableMessage.raw("Error formatting filter or base DN: "
          + ex1.toString()));
      return ResultCode.CLIENT_SIDE_PARAM_ERROR.intValue();
    }

    return runner.run(connectionFactory);
  }
}
