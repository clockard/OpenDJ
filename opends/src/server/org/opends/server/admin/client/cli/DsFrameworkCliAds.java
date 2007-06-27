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
package org.opends.server.admin.client.cli;

import static org.opends.server.messages.AdminMessages.*;
import static org.opends.server.tools.ToolConstants.*;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.opends.admin.ads.ADSContext;
import org.opends.admin.ads.ADSContextException;
import org.opends.admin.ads.ADSContextHelper;
import org.opends.server.admin.client.cli.DsFrameworkCliReturnCode.ReturnCode;
import org.opends.server.util.args.ArgumentException;
import org.opends.server.util.args.BooleanArgument;
import org.opends.server.util.args.StringArgument;
import org.opends.server.util.args.SubCommand;
import org.opends.server.util.args.SubCommandArgumentParser;

/**
 * This class is handling server group CLI.
 */
public class DsFrameworkCliAds implements DsFrameworkCliSubCommandGroup
{
  /**
   * The enumeration containing the different subCommand names.
   */
  private enum SubCommandNameEnum
  {
    /**
     * The create-ads subcommand.
     */
    CREATE_ADS("create-ads"),

    /**
     * The delete-ads subcommand.
     */
    DELETE_ADS("delete-ads");

    // String representation of the value.
    private final String name;

    // Private constructor.
    private SubCommandNameEnum(String name)
    {
      this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
      return name;
    }

    // A lookup table for resolving a unit from its name.
    private static final List<String> nameToSubCmdName ;
    static
    {
      nameToSubCmdName = new ArrayList<String>();

      for (SubCommandNameEnum subCmd : SubCommandNameEnum.values())
      {
        nameToSubCmdName.add(subCmd.toString());
      }
    }
    public static boolean  isSubCommand(String name)
    {
      return nameToSubCmdName.contains(name);
    }
  }

  /**
   * The 'create-ads' subcommand.
   */
  public SubCommand createAdsSubCmd;

  /**
   * The 'backend-name' argument of the 'create-ads' subcommand.
   */
  private StringArgument createAdsBackendNameArg;

  /**
   * The 'delete-ads' subcommand.
   */
  private SubCommand deleteAdsSubCmd;

  /**
   * The 'backend-name' argument of the 'delete-ads' subcommand.
   */
  private StringArgument deleteAdsBackendNameArg;

  /**
   * {@inheritDoc}
   */
  public void initializeCliGroup(SubCommandArgumentParser argParser,
      BooleanArgument verboseArg)
      throws ArgumentException
  {
    // Create-ads subcommand
    createAdsSubCmd = new SubCommand(argParser, SubCommandNameEnum.CREATE_ADS
        .toString(), MSGID_ADMIN_SUBCMD_CREATE_ADS_DESCRIPTION);
    createAdsSubCmd.setHidden(true);

    createAdsBackendNameArg = new StringArgument("backendName",
        OPTION_SHORT_BACKENDNAME, OPTION_LONG_BACKENDNAME, true, true,
        OPTION_VALUE_BACKENDNAME,
        MSGID_ADMIN_ARG_BACKENDNAME_DESCRIPTION);
    createAdsSubCmd.addArgument(createAdsBackendNameArg);

    // delete-ads
    deleteAdsSubCmd = new SubCommand(argParser,SubCommandNameEnum.DELETE_ADS
        .toString(), MSGID_ADMIN_SUBCMD_DELETE_ADS_DESCRIPTION);
    deleteAdsSubCmd.setHidden(true);

    deleteAdsBackendNameArg = new StringArgument("backendName",
        OPTION_SHORT_BACKENDNAME, OPTION_LONG_BACKENDNAME, true, true,
        OPTION_VALUE_BACKENDNAME,
        MSGID_ADMIN_ARG_BACKENDNAME_DESCRIPTION);
    deleteAdsSubCmd.addArgument(deleteAdsBackendNameArg);
  }

  /**
   * {@inheritDoc}
   */
  public boolean isSubCommand(SubCommand subCmd)
  {
      return SubCommandNameEnum.isSubCommand(subCmd.getName());
  }


  /**
   * {@inheritDoc}
   */
  public ReturnCode performSubCommand(ADSContext adsContext, SubCommand subCmd,
      OutputStream outStream, OutputStream errStream)
      throws ADSContextException
  {
    //
    // create-ads subcommand
    if (subCmd.getName().equals(createAdsSubCmd.getName()))
    {
      String backendName = createAdsBackendNameArg.getValue();
      adsContext.createAdminData(backendName);
      return ReturnCode.SUCCESSFUL;
    }
    else if (subCmd.getName().equals(deleteAdsSubCmd.getName()))
    {
      String backendName = deleteAdsBackendNameArg.getValue();
      ADSContextHelper helper = new ADSContextHelper();
      helper.removeAdministrationSuffix(adsContext.getDirContext(),
          backendName);
      return ReturnCode.SUCCESSFUL;
    }

    // Should never occur: If we are here, it means that the code to
    // handle to subcommand is not yet written.
    return ReturnCode.ERROR_UNEXPECTED;
  }
}
