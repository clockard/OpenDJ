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
    #@TestSuiteName             Backend Backup Tasks Tests
    #@TestSuitePurpose          Test the backup tasks functionality for OpenDS
    #@TestSuiteID               Backup Tasks Tests
    #@TestSuiteGroup            Backup Tasks
    #@TestGroup                 Backend
    #@TestScript                BackupTasksTests.java
    #@TestHTMLLink
*/
/**
 * This class contains the TestNG tests for the Backend functional tests for backup
 */
public class BackupTasksTests extends BackendTests
{
/*
    Place test-specific test information here.
    The tag, TestMarker, must be present and must be the same as the marker, TestSuiteName.
    #@TestMarker                Backend Backup Tasks Tests
    #@TestName                  Backup Tasks 1
    #@TestID                    BackupTasks1
    #@TestPreamble
    #@TestSteps                 An ldif file is created that describes the backup task to be
                                scheduled. The task is scheduled by adding the ldif file
                                with the static method, LDAPModify.mainModify().
    #@TestPostamble
    #@TestResult                Success if OpenDS returns 0
*/
/**
 *  Create a backup of the data in OpenDS by scheduling a task.
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
  @Parameters({ "hostname", "port", "bindDN", "bindPW", "integration_test_home", "logDir", "exportDir" })
  @Test(alwaysRun=true, dependsOnMethods = { "org.opends.server.integration.backend.BackupTests.testBackup5" })
  public void testBackupTasks1(String hostname, String port, String bindDN, String bindPW, String integration_test_home, String logDir, String exportDir) throws Exception
  {
    System.out.println("*********************************************");
    System.out.println("Backup Tasks Test 1");

    Writer output = null;
    try
    {
      String output_str = "dn: ds-task-id=3,cn=Scheduled Tasks,cn=tasks\n";
      output_str += "objectclass: top\n";
      output_str += "objectclass: ds-task\n";
      output_str += "objectclass: ds-task-backup\n";
      output_str += "ds-task-id: 3\n";
      output_str += "ds-task-class-name: org.opends.server.tasks.BackupTask\n";
      output_str += "ds-task-backup-backend-id: userRoot\n";
      output_str += "ds-backup-directory-path: " + exportDir + "/backup_task\n";

      String backup_task_file = integration_test_home + "/backend/data/add_task_backup.ldif";
      output = new BufferedWriter(new FileWriter(backup_task_file));
      output.write(output_str);
    }
    catch (Exception e)
    {
      System.out.println("Exception occurred while creating add_task_backup.ldif file");
    }
    finally
    {
      if(output != null)
        output.close();
    }

    String datafile = integration_test_home + "/backend/data/add_task_backup.ldif";
    String mod_args[] = {"-a", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-f", datafile};

    ds_output.redirectOutput(logDir, "BackupTasksTests1.txt");
    int retCode = LDAPModify.mainModify(mod_args);
    ds_output.resetOutput();
    if(retCode == 0)
    {
      System.out.println("Waiting for backup task to finish....");
      Thread.sleep(20000);
    }
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

}
