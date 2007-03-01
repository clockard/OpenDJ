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
package org.opends.server.integration.backend;

import static org.testng.Assert.*;
import org.testng.annotations.*;
import org.opends.server.tools.*;

/*
    Place suite-specific test information here.
    #@TestSuiteName             Backend Import Tests
    #@TestSuitePurpose          Test the import functionality for OpenDS
    #@TestSuiteID               Import Tests
    #@TestSuiteGroup            Import
    #@TestGroup                 Backend
    #@TestScript                ImportTests.java
    #@TestHTMLLink
*/
/**
 * This class contains the TestNG tests for the Backend functional tests for import
 */
@Test
public class ImportTests extends BackendTests
{
/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 1
    #@TestID                    Import1
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, and --ldfiFile.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.BackupTasksTests.testBackupTasks1" })
  public void testImport1(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 1");
    String datafile = integration_test_home + "/backend/data/import.ldif.01";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest1.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 1 Check Entries 1
    #@TestID                    Import1_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport1" })
  public void testImport1_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 1 check entries 1");
    String base = "uid=scarter, ou=People, o=test one, o=import tests, dc=example,dc=com"; 
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest1check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 1 Check Entries 2
    #@TestID                    Import1_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present before the last import test.
				The entry should no longer be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport1_check" })
  public void testImport1_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 1 check entries 2");
    String base = "uid=scarter, ou=People, o=backend tests, dc=example,dc=com"; 
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest1check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 2
    #@TestID                    Import2
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, and --append.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with the --append parameter.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport1_check2" })
  public void testImport2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 2");
    String datafile = integration_test_home + "/backend/data/import.ldif.02";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--append"};
    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest2.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 2 Check Entries 1
    #@TestID                    Import2_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport2" })
  public void testImport2_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 2 check entries 1");
    String base = "uid=scarter, ou=People, o=test two, o=import tests, dc=example,dc=com"; 
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest2check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 2 Check Entries 2
    #@TestID                    Import2_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present before the last import test.
				The entry should still be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport2_check" })
  public void testImport2_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 2 check entries 2");
    String base = "uid=scarter, ou=People, o=test one, o=import tests, dc=example,dc=com"; 
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest2check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 3
    #@TestID                    Import3
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and three --includeAttributes.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with three --includeAttribute parameters.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport2_check2" })
  public void testImport3(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 3");
    String datafile = integration_test_home + "/backend/data/import.ldif.03";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--includeAttribute", "sn", "--includeAttribute", "cn", "--includeAttribute", "ou", "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest3.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 3 Check Entries 1
    #@TestID                    Import3_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport3" })
  public void testImport3_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 3 check entries 1");
    String base = "uid=prigden3,ou=People,o=test one,o=import tests,dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest3check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 3 Check Entries 2
    #@TestID                    Import3_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present before the last import test.
				The entry should still be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport3_check" })
  public void testImport3_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 3 check entries 2");
    String base = "uid=scarter, ou=People, o=test one, o=import tests, dc=example,dc=com"; 
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest3check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 4
    #@TestID                    Import4
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and --excludeAttribute.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with one --excludeAttribute parameter.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport3_check2" })
  public void testImport4(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 4");
    String datafile = integration_test_home + "/backend/data/import.ldif.04";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--excludeAttribute", "telephonenumber", "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest4.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 4 Check Entries 1
    #@TestID                    Import4_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport4" })
  public void testImport4_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 4 check entries 1");
    String base = "uid=prigden4, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest4check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 4 Check Entries 2
    #@TestID                    Import4_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present before the last import test.
				The entry should still be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport4_check" })
  public void testImport4_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 4 check entries 2");
    String base = "uid=scarter, ou=People, o=test one, o=import tests, dc=example,dc=com"; 
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest4check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 5
    #@TestID                    Import5
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and three --excludeAttributes.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with three --excludeAtribute parameters.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport4_check2" })
  public void testImport5(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 5");
    String datafile = integration_test_home + "/backend/data/import.ldif.05";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--excludeAttribute", "telephonenumber", "--excludeAttribute", "mail", "--excludeAttribute", "roomnumber", "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest5.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 5 Check Entries 1
    #@TestID                    Import5_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport5" })
  public void testImport5_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 5 check entries 1");
    String base = "uid=prigden5, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest5check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 5 Check Entries 2
    #@TestID                    Import5_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present before the last import test.
				The entry should still be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport5_check" })
  public void testImport5_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 5 check entries 2");
    String base = "uid=scarter, ou=People, o=test one, o=import tests, dc=example,dc=com"; 
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest5check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 6
    #@TestID                    Import6
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and --includeFilter.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS one --includeFilter parameter.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport5_check2" })
  public void testImport6(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 6");
    String datafile = integration_test_home + "/backend/data/import.ldif.06";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--includeFilter", "(&(uid=prigden6)(telephonenumber=*))", "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest6.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 6 Check Entries 1
    #@TestID                    Import6_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport6" })
  public void testImport6_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 6 check entries 1");
    String base = "uid=prigden6, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest6check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 6 Check Entries 2
    #@TestID                    Import6_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport6_check" })
  public void testImport6_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 6 check entries 2");
    String base = "uid=brigden6, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest6check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 7
    #@TestID                    Import7
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and three --includeFilters.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with three --includeFilter attributes.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport6_check2" })
  public void testImport7(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 7");
    String datafile = integration_test_home + "/backend/data/import.ldif.07";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--includeFilter", "(&(uid=prigden7)(telephonenumber=*))", "--includeFilter", "(&(uid=prigden7)(l=Sunnyvale))", "--includeFilter", "(&(uid=brigden7)(roomnumber=*))", "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest7.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 7 Check Entries 1
    #@TestID                    Import7_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport7" })
  public void testImport7_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 7 check entries 1");
    String base = "uid=prigden7, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest7check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 7 Check Entries 2
    #@TestID                    Import7_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport7_check" })
  public void testImport7_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 7 check entries 2");
    String base = "uid=trigden7, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest7check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 8
    #@TestID                    Import8
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and --excludeFilter.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with one --excludeFilter attribute.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport7_check2" })
  public void testImport8(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 8");
    String datafile = integration_test_home + "/backend/data/import.ldif.08";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--excludeFilter", "(&(uid=prigden8)(telephonenumber=*))", "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest8.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 8 Check Entries 1
    #@TestID                    Import8_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport8" })
  public void testImport8_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 8 check entries 1");
    String base = "uid=brigden8, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest8check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 8 Check Entries 2
    #@TestID                    Import8_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport8_check" })
  public void testImport8_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 8 check entries 2");
    String base = "uid=prigden8, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest8check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 9
    #@TestID                    Import9
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and three --excludeFilters.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with three --excludeFilters parameters.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport8_check2" })
  public void testImport9(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 9");
    String datafile = integration_test_home + "/backend/data/import.ldif.09";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--excludeFilter", "(&(uid=prigden9)(telephonenumber=*))", "--excludeFilter", "(&(uid=prigden9)(l=Sunnyvale))", "--excludeFilter", "(&(uid=brigden9)(roomnumber=*))", "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest9.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 9 Check Entries 1
    #@TestID                    Import9_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport9" })
  public void testImport9_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 9 check entries 1");
    String base = "uid=trigden9, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest9check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 9 Check Entries 2
    #@TestID                    Import9_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport9_check" })
  public void testImport9_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 9 check entries 2");
    String base = "uid=prigden9, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest9check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 9 Check Entries 3
    #@TestID                    Import9_check3
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Third verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport9_check2" })
  public void testImport9_check3(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 9 check entries 3");
    String base = "uid=brigden9, ou=People, o=test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest9check3.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 10
    #@TestID                    Import10
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and --includeBranch.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with one --includeBranch parameter.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport9_check3" })
  public void testImport10(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 10");
    String datafile = integration_test_home + "/backend/data/import.ldif.10";
    String branch = "o=branch test two, o=import tests, dc=example,dc=com";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--includeBranch", branch, "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest10.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 10 Check Entries 1
    #@TestID                    Import10_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport10" })
  public void testImport10_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 10 check entries 1");
    String base = " uid=scarter, ou=People, o=branch test two, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest10check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 10 Check Entries 2
    #@TestID                    Import10_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport10_check" })
  public void testImport10_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 10 check entries 2");
    String base = " uid=scarter, ou=People, o=branch test one, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest10check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 11
    #@TestID                    Import11
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				and --excludeBranch.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with one --excludeBranch parameter.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport10_check2" })
  public void testImport11(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 11");
    String datafile = integration_test_home + "/backend/data/import.ldif.11";
    String branch = "o=branch test four, o=import tests, dc=example,dc=com";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--excludeBranch", branch, "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest11.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 11 Check Entries 1
    #@TestID                    Import11_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport11" })
  public void testImport11_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 11 check entries 1");
    String base = " uid=scarter, ou=People, o=branch test three, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest11check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 11 Check Entries 2
    #@TestID                    Import11_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport11_check" })
  public void testImport11_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 11 check entries 2");
    String base = " uid=scarter, ou=People, o=branch test four, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest11check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 12
    #@TestID                    Import12
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				--excludeAttribute, --excludeFilter and --includeBranch.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with one --excludeAttribute, one --excludeFilter, and
 *  one --includeBranch parameter.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport11_check2" })
  public void testImport12(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 12");
    String datafile = integration_test_home + "/backend/data/import.ldif.12";
    String branch = "o=branch test six, o=import tests, dc=example,dc=com";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--excludeFilter", "(&(uid=prigden)(roomnumber=*))", "--excludeAttribute", "telephonenumber", "--includeBranch", branch, "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest12.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 12 Check Entries 1
    #@TestID                    Import12_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport12" })
  public void testImport12_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 12 check entries 1");
    String base = " uid=scarter, ou=People, o=branch test six, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest12check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 12 Check Entries 2
    #@TestID                    Import12_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport12_check" })
  public void testImport12_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 12 check entries 2");
    String base = " uid=prigden, ou=People, o=branch test six, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest12check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 12 Check Entries 3
    #@TestID                    Import12_check3
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Third verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport12_check2" })
  public void testImport12_check3(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 12 check entries 3");
    String base = " uid=scarter, ou=People, o=branch test five, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest12check3.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 13
    #@TestID                    Import13
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, --append,
				--excludeAttribute, --includeFilter and --excludeBranch.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import data to OpenDS with one --excludeAttribute, one --includeFilter, and
 *  one --excludeBranch parameter.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport12_check3" })
  public void testImport13(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 13");
    String datafile = integration_test_home + "/backend/data/branchTestAdd.ldif";
    String backup_mod_args[] = {"-a", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-f", datafile};

    ds_output.redirectOutput(logDir, "ImportTest13_premod.txt");
    LDAPModify.mainModify(backup_mod_args);
    ds_output.resetOutput();

    datafile = integration_test_home + "/backend/data/import.ldif.13";
    String branch = "o=branch test eight, o=import tests, dc=example,dc=com";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--includeFilter", "(&(uid=prigden)(roomnumber=*))", "--excludeAttribute", "telephonenumber", "--excludeBranch", branch, "--append"};
    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest13.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 13 Check Entries 1
    #@TestID                    Import13_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport13" })
  public void testImport13_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 13 check entries 1");
    String base = " uid=prigden, ou=People, o=branch test seven, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest13check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 13 Check Entries 2
    #@TestID                    Import13_check2
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Second verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport13_check" })
  public void testImport13_check2(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 13 check entries 2");
    String base = " uid=prigden, ou=People, o=branch test eight, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest13check2.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 13 Check Entries 3
    #@TestID                    Import13_check3
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was present in the ldif file
				but should have been filtered out during the import.
				The entry should not be present.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 32
*/
/**
 *  Third verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport13_check2" })
  public void testImport13_check3(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 13 check entries 3");
    String base = " uid=scarter, ou=People, o=branch test eight, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest13check3.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 32;

    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 14
    #@TestID                    Import14
    #@TestPreamble              The OpenDS is stopped.
    #@TestSteps                 Client calls static method ImportLDIF.mainImportLDIF()
                                with the parameters, --configClass, --configFileHandler,
                                backendID, --ldifFile, and --isCompressed.
    #@TestPostamble             The OpenDs is started.
    #@TestResult                Success if ImportLDIF.mainImportLDIF() returns 0
*/
/**
 *  Import compressed data to OpenDS.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
 *  @param  dsee_home              The home directory for the OpenDS
 *                                 installation.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport13_check3" })
  public void testImport14(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 14");
    String datafile = integration_test_home + "/backend/data/import.compressed.ldif";
    String import_args[] = {"--configClass", "org.opends.server.extensions.ConfigFileHandler", "--configFile", dsee_home + "/config/config.ldif", "--backendID", "userRoot", "--ldifFile", datafile, "--isCompressed", "--append"};

    stopOpenDS(dsee_home, port);

    ds_output.redirectOutput(logDir, "ImportTest14.txt");
    int retCode = ImportLDIF.mainImportLDIF(import_args);
    ds_output.resetOutput();
    int expCode = 0;

    if(retCode == expCode)
    {
      if(startOpenDS(dsee_home, hostname, port, bindDN, bindPW, logDir) != 0)
      {
	retCode = 999;
      }
    }
    compareExitCode(retCode, expCode);
  }

/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tests
    #@TestName                  Import 14 Check Entries 1
    #@TestID                    Import14_check
    #@TestPreamble              
    #@TestSteps                 Client calls static method LDAPSearch.mainSearch()
                                for an entry that was imported in the last import test.
    #@TestPostamble             
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  First verification search for the entries that were imported in the last test.
 *
 *  @param  hostname               The hostname for the server where OpenDS
 *                                 is installed.
 *  @param  port                   The port number for OpenDS.
 *  @param  bindDN                 The bind DN.
 *  @param  bindPW                 The password for the bind DN.
 *  @param  integration_test_home  The home directory for the Integration
 *                                 Test Suites.
 *  @param  logDir                 The directory for the log files that are
 *                                 generated during the Integration Tests.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport14" })
  public void testImport14_check(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Test 14 check entries 1");
    String base = "uid=scarter, ou=People, o=compressed test, o=import tests, dc=example,dc=com";
    String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};

    ds_output.redirectOutput(logDir, "ImportTest14check1.txt");
    int retCode = LDAPSearch.mainSearch(search_args);
    ds_output.resetOutput();
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

}
