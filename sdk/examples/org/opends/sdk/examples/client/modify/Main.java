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

package org.opends.sdk.examples.client.modify;



import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.opends.sdk.Connection;
import org.opends.sdk.ErrorResultException;
import org.opends.sdk.LDAPConnectionFactory;
import org.opends.sdk.ResultCode;
import org.opends.sdk.ldif.ChangeRecord;
import org.opends.sdk.ldif.ConnectionChangeRecordWriter;
import org.opends.sdk.ldif.LDIFChangeRecordReader;



/**
 * An example client application which applies update operations to a Directory
 * Server. The update operations will be read from an LDIF file, or stdin if no
 * filename is provided. This example takes the following command line
 * parameters (it will read from stdin if no LDIF file is provided):
 *
 * <pre>
 *  &lt;host> &lt;port> &lt;username> &lt;password> [&lt;ldifFile>]
 * </pre>
 */
public final class Main
{
  /**
   * Main method.
   *
   * @param args
   *          The command line arguments: host, port, username, password, LDIF
   *          file name containing the update operations (will use stdin if not
   *          provided).
   */
  public static void main(final String[] args)
  {
    if (args.length < 4 || args.length > 5)
    {
      System.err.println("Usage: host port username password [ldifFileName]");
      System.exit(1);
    }

    // Parse command line arguments.
    final String hostName = args[0];
    final int port = Integer.parseInt(args[1]);
    final String userName = args[2];
    final String password = args[3];

    // Create the LDIF reader which will either used the named file, if
    // provided, or stdin.
    InputStream ldif;
    if (args.length > 4)
    {
      try
      {
        ldif = new FileInputStream(args[4]);
      }
      catch (final FileNotFoundException e)
      {
        System.err.println(e.getMessage());
        System.exit(ResultCode.CLIENT_SIDE_PARAM_ERROR.intValue());
        return;
      }
    }
    else
    {
      ldif = System.in;
    }
    final LDIFChangeRecordReader reader = new LDIFChangeRecordReader(ldif);

    // Connect and bind to the server.
    final LDAPConnectionFactory factory = new LDAPConnectionFactory(hostName,
        port);
    Connection connection = null;

    try
    {
      connection = factory.getConnection();
      connection.bind(userName, password);

      // Write the changes.
      final ConnectionChangeRecordWriter writer = new ConnectionChangeRecordWriter(
          connection);
      while (reader.hasNext())
      {
        ChangeRecord changeRecord = reader.readChangeRecord();
        writer.writeChangeRecord(changeRecord);
        System.err.println("Successfully modified entry "
            + changeRecord.getName().toString());
      }
    }
    catch (final ErrorResultException e)
    {
      System.err.println(e.getMessage());
      System.exit(e.getResult().getResultCode().intValue());
      return;
    }
    catch (final InterruptedException e)
    {
      System.err.println(e.getMessage());
      System.exit(ResultCode.CLIENT_SIDE_USER_CANCELLED.intValue());
      return;
    }
    catch (final IOException e)
    {
      System.err.println(e.getMessage());
      System.exit(ResultCode.CLIENT_SIDE_LOCAL_ERROR.intValue());
      return;
    }
    finally
    {
      if (connection != null)
      {
        connection.close();
      }

      try
      {
        reader.close();
      }
      catch (final IOException ignored)
      {
        // Ignore.
      }
    }
  }



  private Main()
  {
    // Not used.
  }
}
