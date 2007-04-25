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
package org.opends.server.replication.plugin;



import java.io.IOException;
import java.io.InputStream;


/**
 * This class creates an input stream that can be used to read entries generated
 * by SynchroLDIF as if they were being read from another source like a file.
 */
public class ReplLDIFInputStream
extends InputStream
{
  // Indicates whether this input stream has been closed.
  private boolean closed;

  // The domain associated to this import.
  ReplicationDomain domain;

  /**
   * Creates a new ReplLDIFInputStream that will import entries
   * for a synchronzation domain.
   *
   * @param domain The replication domain
   */
  public ReplLDIFInputStream(ReplicationDomain domain)
  {
    this.domain = domain;
    closed       = false;
  }

  /**
   * Closes this input stream so that no more data may be read from it.
   */
  public void close()
  {
    closed      = true;
  }

  /**
   * Reads data from this input stream.
   *
   * @param  b    The array into which the data should be read.
   * @param  off  The position in the array at which point the data read may be
   *              placed.
   * @param  len  The maximum number of bytes that may be read into the
   *              provided array.
   *
   * @return  The number of bytes read from the input stream into the provided
   *          array, or -1 if the end of the stream has been reached.
   *
   * @throws  IOException  If a problem has occurred while generating data for
   *                       use by this input stream.
   */
  public int read(byte[] b, int off, int len)
  throws IOException
  {
    if (closed)
      return -1;

    byte[] bytes = domain.receiveEntryBytes();

    if (bytes==null)
    {
      closed = true;
      return -1;
    }

    int l = bytes.length;
    for (int i =0; i<l; i++)
    {
      b[off+i] = bytes[i];
    }
    return l;
  }

  /**
   * Reads a single byte of data from this input stream.
   *
   * @return  The byte read from the input stream, or -1 if the end of the
   *          stream has been reached.
   *
   * @throws  IOException  If a problem has occurred while generating data for
   *                       use by this input stream.
   */
  public int read()
         throws IOException
  {
    // This method is not supposed to be called to make an LDIF import
    // for replication.
    throw new IOException("Not implemented");
  }
}

