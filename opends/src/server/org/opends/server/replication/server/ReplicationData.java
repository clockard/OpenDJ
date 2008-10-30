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
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 */
package org.opends.server.replication.server;

import java.io.UnsupportedEncodingException;

import com.sleepycat.je.DatabaseEntry;

import org.opends.server.replication.protocol.ReplicationMsg;
import org.opends.server.replication.protocol.UpdateMsg;

/**
 * SuperClass of DatabaseEntry used for data stored in the ReplicationServer
 * Databases.
 */
public class ReplicationData extends DatabaseEntry
{
  /**
   * Creates a new ReplicationData object from an UpdateMsg.
   *
   * @param change the UpdateMsg used to create the ReplicationData.
   *
   * @throws UnsupportedEncodingException When the encoding of the message
   *         failed because the UTF-8 encoding is not supported.
   */
  public ReplicationData(UpdateMsg change)
         throws UnsupportedEncodingException
  {
    // Always keep messages in the replication DB with the current protocol
    // version
    this.setData(change.getBytes());
  }

  /**
   * Generate an UpdateMsg from its byte[] form.
   *
   * @param data The DatabaseEntry used to generate the UpdateMsg.
   *
   * @return     The generated change.
   *
   * @throws Exception When the data was not a valid Update Message.
   */
  public static UpdateMsg generateChange(byte[] data)
                                             throws Exception
  {
    return (UpdateMsg) ReplicationMsg.generateMsg(data);
  }
}
