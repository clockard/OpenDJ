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
 *      Copyright 2007-2009 Sun Microsystems, Inc.
 *      Portions Copyright 2011 ForgeRock AS
 */

package org.opends.server.replication.protocol;

import static org.opends.messages.ReplicationMessages.*;
import static org.opends.server.loggers.ErrorLogger.logError;
import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.server.util.StaticUtils.stackTraceToSingleLineString;

import org.opends.server.loggers.debug.DebugTracer;

import java.io.IOException;

import org.opends.server.api.DirectoryThread;

/**
 * This class implements a thread to monitor heartbeat messages from the
 * replication server.  Each broker runs one of these threads.
 */
public class HeartbeatMonitor extends DirectoryThread
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();



  /**
   * The session on which heartbeats are to be monitored.
   */
  private final ProtocolSession session;


  /**
   * The time in milliseconds between heartbeats from the replication
   * server.  Zero means heartbeats are off.
   */
  private final long heartbeatInterval;


  /**
   * Set this to stop the thread.
   */
  private volatile boolean shutdown = false;

  /**
   * Send StopMsg before session closure or not.
   */
  private final boolean sendStopBeforeClose;


  /**
   * Create a heartbeat monitor thread.
   * @param threadName The name of the heartbeat thread.
   * @param session The session on which heartbeats are to be monitored.
   * @param heartbeatInterval The expected interval between heartbeats received
   * (in milliseconds).
   * @param sendStopBeforeClose Should we send a StopMsg before closing the
   *        session ?
   */
  public HeartbeatMonitor(String threadName, ProtocolSession session,
                          long heartbeatInterval, boolean sendStopBeforeClose)
  {
    super(threadName);
    this.session = session;
    this.heartbeatInterval = heartbeatInterval;
    this.sendStopBeforeClose = sendStopBeforeClose;
  }

  /**
   * Call this method to stop the thread.
   */
  public void shutdown()
  {
    shutdown = true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    boolean gotOneFailure = false;
    if (debugEnabled())
    {
      TRACER.debugInfo(this + " is starting, expected interval is " +
                heartbeatInterval);
    }
    try
    {
      while (!shutdown)
      {
        long now = System.currentTimeMillis();
        long lastReceiveTime = session.getLastReceiveTime();
        if (now > lastReceiveTime + heartbeatInterval)
        {
          if (gotOneFailure == true)
          {
            // Heartbeat is well overdue so the server is assumed to be dead.
            logError(NOTE_HEARTBEAT_FAILURE.get(currentThread().getName()));
            if (sendStopBeforeClose)
            {
              // V4 protocol introduces a StopMsg to properly end communications
              try
              {
                session.publish(new StopMsg());
              }
              catch (IOException ioe)
              {
                // Anyway, going to close session, so nothing to do
              }
            }
            session.close();
            break;
          }
          else
          {
            gotOneFailure = true;
          }
        }
        else
        {
          gotOneFailure = false;
        }
        try
        {
          Thread.sleep(heartbeatInterval);
        }
        catch (InterruptedException e)
        {
          // That's OK.
        }
      }
    }
    catch (IOException e)
    {
      // Hope that's OK.
    }
    finally
    {
      if (debugEnabled())
      {
        TRACER.debugInfo("Heartbeat monitor is exiting." +
            stackTraceToSingleLineString(new Exception()));
      }
    }
  }
}
