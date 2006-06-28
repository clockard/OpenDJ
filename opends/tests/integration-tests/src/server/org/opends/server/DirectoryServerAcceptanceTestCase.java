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
package org.opends.server;

import java.io.*;
import junit.framework.*;


/**
 * This class defines a base JUnit test case that should be subclassed by all
 * acceptance tests used by the Directory Server.  The primary benefit that it adds is
 * the ability to print error messages and automatically have them include the
 * class name.
 */
public abstract class DirectoryServerAcceptanceTestCase
       extends TestCase
{
  //general variables for all functional tests
  public String dsee_home = "/tmp/OpenDS-0.1";
  public String hostname = "auseng036";
  public String port = "389";
  public String sport = "636";
  public String bindDN = "cn=Directory Manager";
  public String bindPW = "password";
  public String acceptance_test_home = "/export00/dsee7/src/openDS/trunk/opends/tests/integration-tests/src/server/org/opends/server//acceptance";
  public String jks_certdir = acceptance_test_home + "/ssl/jks/certs/server";
  public String jks_certdb = acceptance_test_home + "/ssl/jks/certs/client/" + hostname + "/certdb";

  // Object that extends Thread class to start and stop the Directory Server
  DirectoryServerAcceptanceAdmin dsAdmin = null;

  // The print stream to use for printing error messages.
  private PrintStream errorStream;

  /**
   * Creates a new instance of this JUnit test case with the provided name.
   *
   * @param  name  The name to use for this JUnit test case.
   */
  public DirectoryServerAcceptanceTestCase(String name)
  {
    super(name);

    errorStream = System.err;
  }



  /**
   * Prints the provided message to the error stream, prepending the
   * fully-qualified class name.
   *
   * @param  message  The message to be printed to the error stream.
   */
  public void printError(String message)
  {
    errorStream.print(getClass().getName());
    errorStream.print(" -- ");
    errorStream.println(message);
  }



  /**
   * Prints the stack trace for the provided exception to the error stream.
   *
   * @param  exception  The exception to be printed to the error stream.
   */
  public void printException(Throwable exception)
  {
    exception.printStackTrace(errorStream);
  }



  /**
   * Specifies the error stream to which messages will be printed.
   *
   * @param  errorStream  The error stream to which messages will be printed.
   */
  public void setErrorStream(PrintStream errorStream)
  {
    this.errorStream = errorStream;
  }

  public String cmdArrayToString(String cmd[])
  {
    String outStr = cmd[0];
    for(int i = 1; i < cmd.length; i++)
    {
      outStr = outStr + " " + cmd[i];
    }

    return outStr;
  }

  public void compareExitCode(int retCode, int expCode)
  { 
    if (retCode == expCode )
    {
      System.out.println("PASS");
    }
    else
    {
      System.out.println("FAIL" + " - Return code is " + Integer.toString(retCode) + ", expecting " + Integer.toString(expCode));
      fail("Return code is " + Integer.toString(retCode) + ", expecting " + Integer.toString(expCode) + " - FAIL");
    }
  }

  public void startDirectoryServer() throws Exception
  {
    dsAdmin = new DirectoryServerAcceptanceAdmin(dsee_home);

    System.out.println("OpenDS is starting.....");
    dsAdmin.start();
    dsAdmin.sleep(20000);
    System.out.println("OpenDS has started.");
  }

  public void stopDirectoryServer() throws Exception
  {
    if(dsAdmin == null)
    {
      dsAdmin = new DirectoryServerAcceptanceAdmin(dsee_home);
    }

    System.out.println("OpenDS is stopping.....");
    dsAdmin.stopDS();
    dsAdmin.sleep(20000);
    System.out.println("OpenDS has stopped.");
 
    dsAdmin = null;
  }

  public void prepDBEnv() throws Exception
  {
    DirectoryServerAcceptanceAdmin.prepEnv(dsee_home);
  }

  public void undoDBEnv() throws Exception
  {
    DirectoryServerAcceptanceAdmin.undoEnv();
  }

}

