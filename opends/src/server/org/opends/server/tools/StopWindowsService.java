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

package org.opends.server.tools;

import java.io.OutputStream;
import java.io.PrintStream;

import org.opends.server.types.NullOutputStream;

import static org.opends.server.messages.MessageHandler.getMessage;
import static org.opends.server.messages.ToolMessages.*;
import static org.opends.server.util.StaticUtils.*;


/**
  * This class is used to stop the Windows service associated with this
  * instance on this machine.
  * This tool allows to stop OpenDS as a Windows service.
  */
public class StopWindowsService
{
  /**
    * The service was successfully stopped.
    */
  public static int SERVICE_STOP_SUCCESSFUL = 0;
  /**
    * The service could not be found.
    */
  public static int SERVICE_NOT_FOUND = 1;
  /**
    * The service was already stopped.
    */
  public static int SERVICE_ALREADY_STOPPED = 2;
  /**
    * The service could not be stopped.
    */
  public static int SERVICE_STOP_ERROR = 3;

  /**
   * Invokes the net stop on the service corresponding to this server.
   *
   * @param  args  The command-line arguments provided to this program.
   */
  public static void main(String[] args)
  {
    int result = stopWindowsService(System.out, System.err);

    System.exit(filterExitCode(result));
  }

  /**
   * Invokes the net stop on the service corresponding to this server, it writes
   * information and error messages in the provided streams.
   * @return <CODE>SERVICE_STOP_SUCCESSFUL</CODE>,
   * <CODE>SERVICE_NOT_FOUND</CODE>, <CODE>SERVICE_ALREADY_STOPPED</CODE> or
   * <CODE>SERVICE_STOP_ERROR</CODE> depending on whether the service could be
   * stopped or not.
   * @param  outStream  The stream to write standard output messages.
   * @param  errStream  The stream to write error messages.
   */
  public static int stopWindowsService(OutputStream outStream,
                           OutputStream errStream)
  {
    int returnValue;
    PrintStream out;
    if (outStream == null)
    {
      out = NullOutputStream.printStream();
    }
    else
    {
      out = new PrintStream(outStream);
    }

    PrintStream err;
    if (errStream == null)
    {
      err = NullOutputStream.printStream();
    }
    else
    {
      err = new PrintStream(errStream);
    }

    String serviceName = ConfigureWindowsService.getServiceName();
    if (serviceName == null)
    {
      int msgID = MSGID_WINDOWS_SERVICE_NOT_FOUND;
      String message = getMessage(msgID, (Object[])null);
      err.println(message);
      returnValue = SERVICE_NOT_FOUND;
    }
    else
    {
      String[] cmd = {
          "net",
          "stop",
          serviceName
          };
      /* Check if is a running service */
      try
      {
        if (Runtime.getRuntime().exec(cmd).waitFor() == 0)
        {
          returnValue = SERVICE_STOP_SUCCESSFUL;
        }
        else
        {
          returnValue = SERVICE_STOP_ERROR;
        }
      }
      catch (Throwable t)
      {
        int msgID = MSGID_WINDOWS_SERVICE_STOP_ERROR;
        String message = getMessage(msgID, (Object[])null);
        out.println(message);
        returnValue = SERVICE_STOP_ERROR;
      }
    }
    return returnValue;
  }
}

