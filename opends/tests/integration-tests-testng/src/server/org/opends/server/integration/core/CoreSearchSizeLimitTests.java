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
package org.opends.server.integration.core;

import static org.testng.Assert.*;
import org.testng.annotations.*;
import org.opends.server.tools.*;
import java.io.*;

/**
 * This class contains the TestNG tests for the Core functional tests for search size limits.
 */
@Test
public class CoreSearchSizeLimitTests extends CoreTests
{
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreStartupTests.testCoreStartup2" })
  public void testCoreSearchSizeLimit1(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 1");
    String core_args[] = {"-z", "150", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", "ou=People,o=core tests,dc=example,dc=com", "objectclass=*"};

    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit1.txt");
    int retCode = LDAPSearch.mainSearch(core_args);
    ds_output.resetOutput();
    int expCode = 4;

    compareExitCode(retCode, expCode);
  }

  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit1" })
  public void testCoreSearchSizeLimit2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 2");
    String core_args[] = {"-z", "151", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", "ou=People,o=core tests,dc=example,dc=com", "objectclass=*"};

    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit2.txt");
    int retCode = LDAPSearch.mainSearch(core_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

  // Reconfigure the server-wide search size limit
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit2" })
  public void testCoreSearchSizeLimit3(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 3");
    String datafile = integration_test_home + "/core/data/mod_searchsizelimit.ldif";
    String core_args_mod[] = {"-a", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-f", datafile};
    
    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit3.txt");
    int retCode = LDAPModify.mainModify(core_args_mod);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit3" })
  public void testCoreSearchSizeLimit4(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 4");
    String core_args_anon[] = {"-h", hostname, "-p", port, "-b", "ou=People,o=core tests,dc=example,dc=com", "objectclass=*"};

    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit4.txt");
    int retCode = LDAPSearch.mainSearch(core_args_anon);
    ds_output.resetOutput();
    int expCode = 4;

    compareExitCode(retCode, expCode);
  }

  // Increase the server-wide search size limit to 10000
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit4" })
  public void testCoreSearchSizeLimit5(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 5");
    String datafile = integration_test_home + "/core/data/mod_searchsizelimit2.ldif";
    String core_args_mod[] = {"-a", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-f", datafile};
    
    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit5.txt");
    int retCode = LDAPModify.mainModify(core_args_mod);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit5" })
  public void testCoreSearchSizeLimit6(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 6");
    String core_args_anon[] = {"-h", hostname, "-p", port, "-b", "ou=People,o=core tests,dc=example,dc=com", "objectclass=*"};

    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit6.txt");
    int retCode = LDAPSearch.mainSearch(core_args_anon);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

  // Reconfigure the user cn=Directory manager search size limit
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit6" })
  public void testCoreSearchSizeLimit7(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 7");
    String datafile = integration_test_home + "/core/data/mod_searchsizelimit3.ldif";
    String core_args_mod[] = {"-a", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-f", datafile};
    
    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit7.txt");
    int retCode = LDAPModify.mainModify(core_args_mod);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit7" })
  public void testCoreSearchSizeLimit8(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 8");
    String core_args_nolimit[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", "ou=People,o=core tests,dc=example,dc=com", "objectclass=*"};
  
    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit8.txt");
    int retCode = LDAPSearch.mainSearch(core_args_nolimit);
    ds_output.resetOutput();
    int expCode = 4;

    compareExitCode(retCode, expCode);
  }

  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit8" })
  public void testCoreSearchSizeLimit9(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 9");
    String core_args_anon[] = {"-h", hostname, "-p", port, "-b", "ou=People,o=core tests,dc=example,dc=com", "objectclass=*"};

    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit9.txt");
    int retCode = LDAPSearch.mainSearch(core_args_anon);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

  // Return the user cn=Directory manager search size limit to the deault, -1
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit9" })
  public void testCoreSearchSizeLimit10(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 10");
    String datafile = integration_test_home + "/core/data/mod_searchsizelimit4.ldif";
    String core_args_mod[] = {"-a", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-f", datafile};
    
    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit10.txt");
    int retCode = LDAPModify.mainModify(core_args_mod);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.core.CoreSearchSizeLimitTests.testCoreSearchSizeLimit10" })
  public void testCoreSearchSizeLimit11(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Core Search Size Limit test 11");
    String core_args_nolimit[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", "ou=People,o=core tests,dc=example,dc=com", "objectclass=*"};
  
    ds_output.redirectOutput(logDir, "CoreSearchSizeLimit11.txt");
    int retCode = LDAPSearch.mainSearch(core_args_nolimit);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

}
