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
package org.opends.server.integration.backend;

import static org.testng.Assert.*;
import org.testng.annotations.*;
import org.opends.server.tools.*;
import java.io.*;

/*
    Place suite-specific test information here.
    #@TestSuiteName             Backend Import Tasks Tests
    #@TestSuitePurpose          Test the import tasks functionality for OpenDS
    #@TestSuiteID               Import Tasks Tests
    #@TestSuiteGroup            Import Tasks
    #@TestGroup                 Backend
    #@TestScript                ImportTasksTests.java
    #@TestHTMLLink
*/
/**
 * This class contains the TestNG tests for the Backend functional tests for import
 */
@Test
public class ImportTasksTests extends BackendTests
{
/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Import Tasks Tests
    #@TestName                  Import Tasks 1
    #@TestID                    ImportTasks1
    #@TestPreamble
    #@TestSteps                 An ldif file is created that describes the import task to be
                                scheduled. The task is scheduled by adding the ldif file
                                with the static method, LDAPModify.mainModify().
    #@TestPostamble
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  Import data in OpenDS by scheduling a task.
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
 *  @param  backupDir              The directory where the backup files will
 *                                 be placed.
*/
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "dsee_home" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.ImportTests.testImport14_check" })
  public void testImportTasks1(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String dsee_home) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Import Tasks Test 1");

    Writer output = null;
    try
    {
      String output_str = "dn: ds-task-id=1,cn=Scheduled Tasks,cn=tasks\n";
      output_str += "objectclass: top\n";
      output_str += "objectclass: ds-task\n";
      output_str += "objectclass: ds-task-import\n";
      output_str += "ds-task-id: 1\n";
      output_str += "ds-task-class-name: org.opends.server.tasks.ImportTask\n";
      output_str = output_str + "ds-task-import-ldif-file: " + integration_test_home + "/backend/data/import_task.ldif\n";
      output_str += "ds-task-import-append: TRUE\n";
      output_str += "ds-task-import-replace-existing: FALSE\n";
      output_str += "ds-task-import-backend-id: userRoot\n";
      output_str += "ds-task-import-skip-schema-validation: FALSE\n";

      String import_task_file = logDir + "/add_task_import.ldif";
      output = new BufferedWriter(new FileWriter(import_task_file));
      output.write(output_str);
    }
    catch (Exception e)
    {
      System.out.println("Exception occurred while creating add_task_imports.ldif file");
    }
    finally
    {
      if(output != null)
        output.close();
    }

    String datafile = logDir + "/add_task_import.ldif";
    String mod_args[] = {"-a", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-f", datafile};

    ds_output.redirectOutput(logDir, "ImportTasksTests1.txt");
    int retCode = LDAPModify.mainModify(mod_args);
    ds_output.resetOutput();
    if(retCode == 0)
    {
      System.out.println("Waiting for import task to finish....");
      Thread.sleep(20000);
      String base = "uid=scarter, ou=People, o=test one, o=import tasks tests, dc=example,dc=com";
      String search_args[] = {"-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-b", base, "objectclass=*"};
      ds_output.redirectOutput(logDir, "ImportTasksTests1_check.txt");
      retCode = LDAPSearch.mainSearch(search_args);
      ds_output.resetOutput();
    }
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

}
