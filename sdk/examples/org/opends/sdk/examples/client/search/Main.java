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

package org.opends.sdk.examples.client.search;



import java.io.IOException;
import java.util.Arrays;

import org.opends.sdk.*;
import org.opends.sdk.ldif.ConnectionEntryReader;
import org.opends.sdk.ldif.LDIFEntryWriter;
import org.opends.sdk.responses.SearchResultEntry;
import org.opends.sdk.responses.SearchResultReference;



/**
 * An example client application which searches a Directory Server. This example
 * takes the following command line parameters:
 *
 * <pre>
 *  &lt;host> &lt;port> &lt;username> &lt;password>
 *      &lt;baseDN> &lt;scope> &lt;filter> [&lt;attibute> &lt;attribute> ...]
 * </pre>
 */
public final class Main
{
  /**
   * Main method.
   *
   * @param args
   *          The command line arguments: host, port, username, password, base
   *          DN, scope, filter, and zero or more attributes to be retrieved.
   */
  public static void main(final String[] args)
  {
    if (args.length < 7)
    {
      System.err.println("Usage: host port username password baseDN scope "
          + "filter [attribute ...]");
      System.exit(1);
    }

    // Parse command line arguments.
    final String hostName = args[0];
    final int port = Integer.parseInt(args[1]);
    final String userName = args[2];
    final String password = args[3];
    final String baseDN = args[4];
    final String scopeString = args[5];
    final String filter = args[6];
    String[] attributes;
    if (args.length > 7)
    {
      attributes = Arrays.copyOfRange(args, 7, args.length);
    }
    else
    {
      attributes = new String[0];
    }

    SearchScope scope;
    if (scopeString.equalsIgnoreCase("base"))
    {
      scope = SearchScope.BASE_OBJECT;
    }
    else if (scopeString.equalsIgnoreCase("one"))
    {
      scope = SearchScope.SINGLE_LEVEL;
    }
    else if (scopeString.equalsIgnoreCase("sub"))
    {
      scope = SearchScope.WHOLE_SUBTREE;
    }
    else if (scopeString.equalsIgnoreCase("subordinates"))
    {
      scope = SearchScope.SUBORDINATES;
    }
    else
    {
      System.err.println("Unknown scope: " + scopeString);
      System.exit(ResultCode.CLIENT_SIDE_PARAM_ERROR.intValue());
      return;
    }

    // Create an LDIF writer which will write the search results to stdout.
    final LDIFEntryWriter writer = new LDIFEntryWriter(System.out);

    // Connect and bind to the server.
    final LDAPConnectionFactory factory = new LDAPConnectionFactory(hostName,
        port);
    Connection connection = null;

    try
    {
      connection = factory.getConnection();
      connection.bind(userName, password);

      // Read the entries and output them as LDIF.
      final ConnectionEntryReader reader = connection.search(baseDN, scope,
          filter, attributes);
      while (reader.hasNext())
      {
        if (!reader.isReference())
        {
          final SearchResultEntry entry = reader.readEntry();
          writer.writeComment("Search result entry: "
              + entry.getName().toString());
          writer.writeEntry(entry);
        }
        else
        {
          final SearchResultReference ref = reader.readReference();

          // Got a continuation reference.
          writer.writeComment("Search result reference: "
              + ref.getURIs().toString());
        }
      }
      writer.flush();
    }
    catch (final ErrorResultException e)
    {
      System.err.println(e.getMessage());
      System.exit(e.getResult().getResultCode().intValue());
      return;
    }
    catch (final ErrorResultIOException e)
    {
      System.err.println(e.getMessage());
      System.exit(e.getCause().getResult().getResultCode().intValue());
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
    }
  }



  private Main()
  {
    // Not used.
  }
}
