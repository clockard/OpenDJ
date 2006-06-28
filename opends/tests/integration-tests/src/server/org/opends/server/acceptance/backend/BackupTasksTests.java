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
package org.opends.server.acceptance.backend;

import org.opends.server.tools.*;
import org.opends.server.DirectoryServerAcceptanceTestCase;
import org.opends.server.DirectoryServerAcceptanceAdmin;
import java.io.*;

/**
 * This class contains the JUnit tests for the Backend functional tests for backup
 */
public class BackupTasksTests extends DirectoryServerAcceptanceTestCase
{
  public String backup_datafiledir = acceptance_test_home + "/backend/data";
  public String mod_args[] = {"-a", "-h", hostname, "-p", port, "-D", bindDN, "-w", bindPW, "-f", " "};

  public BackupTasksTests(String name)
  {
    super(name);
  }

  public void setUp() throws Exception
  {
    super.setUp();
  }

  public void tearDown() throws Exception
  {
    super.tearDown();
  }

  public void testBackupTasks1() throws Exception
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
      output_str += "ds-task-backup-backend-id: com\n";
      output_str += "ds-backup-directory-path: /tmp/backup_task\n";

      String backup_task_file = backup_datafiledir + "/add_task_backup.ldif";
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

    mod_args[10] = backup_datafiledir + "/add_task_backup.ldif";
    int retCode = LDAPModify.mainModify(mod_args);
    if(retCode == 0)
    {
      System.out.println("Waiting for backup task to finish....");
      Thread.sleep(20000);
    }
    int expCode = 0;

    compareExitCode(retCode, expCode);
  }

}
