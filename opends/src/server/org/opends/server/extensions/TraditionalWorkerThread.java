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
package org.opends.server.extensions;



import org.opends.server.api.DirectoryThread;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.Operation;
import org.opends.server.types.CancelRequest;
import org.opends.server.types.CancelResult;
import org.opends.server.types.DebugLogCategory;
import org.opends.server.types.DebugLogSeverity;
import org.opends.server.types.DisconnectReason;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.loggers.Error.*;
import static org.opends.server.messages.CoreMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines a data structure for storing and interacting with a
 * Directory Server worker thread.
 */
public class TraditionalWorkerThread
       extends DirectoryThread
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
  "org.opends.server.core.WorkerThread";



  // Indicates whether the Directory Server is shutting down and this thread
  // should stop running.
  private boolean shutdownRequested;

  // Indicates whether this thread was stopped because the server threadnumber
  // was reduced.
  private boolean stoppedByReducedThreadNumber;

  // Indicates whether this thread is currently waiting for work.
  private boolean waitingForWork;

  // The operation that this worker thread is currently processing.
  private Operation operation;

  // The handle to the actual thread for this worker thread.
  private Thread workerThread;

  // The work queue that this worker thread will service.
  private TraditionalWorkQueue workQueue;



  /**
   * Creates a new worker thread that will service the provided work queue and
   * process any new requests that are submitted.
   *
   * @param  workQueue  The work queue with which this worker thread is
   *                    associated.
   * @param  threadID   The thread ID for this worker thread.
   */
  public TraditionalWorkerThread(TraditionalWorkQueue workQueue, int threadID)
  {
    super("Worker Thread " + threadID);

    assert debugConstructor(CLASS_NAME, String.valueOf(workQueue),
                            String.valueOf(threadID));

    this.workQueue = workQueue;

    stoppedByReducedThreadNumber = false;
    shutdownRequested            = false;
    waitingForWork               = false;
    operation                    = null;
    workerThread                 = null;
  }



  /**
   * Indicates that this thread is about to be stopped because the Directory
   * Server configuration has been updated to reduce the number of worker
   * threads.
   */
  public void setStoppedByReducedThreadNumber()
  {
    assert debugEnter(CLASS_NAME, "setStoppedByReducedThreadNumber");

    stoppedByReducedThreadNumber = true;
  }



  /**
   * Operates in a loop, retrieving the next request from the work queue,
   * processing it, and then going back to the queue for more.
   */
  public void run()
  {
    assert debugEnter(CLASS_NAME, "run");
    assert debugMessage(DebugLogCategory.CORE_SERVER, DebugLogSeverity.INFO,
                        CLASS_NAME, "run", getName() + " starting.");

    workerThread = currentThread();

    while (! shutdownRequested)
    {
      try
      {
        waitingForWork = true;
        operation = null;
        operation = workQueue.nextOperation(this);
        waitingForWork = false;


        if (operation == null)
        {
          // The operation may be null if the server is shutting down.  If that
          // is the case, then break out of the while loop.
          break;
        }
        else
        {
          // The operation is not null, so process it.  Make sure that when
          // processing is complete the cancel status is properly set.
          try
          {
            operation.run();
            operation.operationCompleted();
          }
          finally
          {
            if (operation.getCancelResult() == null)
            {
              operation.setCancelResult(CancelResult.TOO_LATE);
            }
          }
        }
      }
      catch (Exception e)
      {
        assert debugMessage(DebugLogCategory.CORE_SERVER,
                            DebugLogSeverity.WARNING, CLASS_NAME, "run",
                            "Uncaught exception in worker thread while " +
                            "processing operation " +
                            String.valueOf(operation) + ":  " + e);
        assert debugException(CLASS_NAME, "run", e);

        try
        {
          int msgID = MSGID_UNCAUGHT_WORKER_THREAD_EXCEPTION;
          String message = getMessage(msgID, getName(),
                                      String.valueOf(operation),
                                      stackTraceToSingleLineString(e));

          logError(ErrorLogCategory.CORE_SERVER, ErrorLogSeverity.SEVERE_ERROR,
                   message, msgID);

          operation.setResultCode(DirectoryServer.getServerErrorResultCode());
          operation.appendErrorMessage(message);
          operation.getClientConnection().sendResponse(operation);
        }
        catch (Exception e2)
        {
          assert debugMessage(DebugLogCategory.CORE_SERVER,
                              DebugLogSeverity.WARNING, CLASS_NAME, "run",
                              "Exception in worker thread while trying to " +
                              "log a message about an uncaught exception " + e +
                              ":  " + e2);
          assert debugException(CLASS_NAME, "run", e2);
        }


        try
        {
          int msgID = MSGID_UNCAUGHT_WORKER_THREAD_EXCEPTION;
          String message = getMessage(msgID, getName(),
                                      String.valueOf(operation),
                                      stackTraceToSingleLineString(e));

          operation.disconnectClient(DisconnectReason.SERVER_ERROR, true,
                                     message, msgID);
        }
        catch (Exception e2)
        {
          assert debugException(CLASS_NAME, "run", e2);
        }
      }
    }


    // If we have gotten here, then we presume that the server thread is
    // shutting down.  However, if that's not the case then that is a problem
    // and we will want to log a message.
    if (stoppedByReducedThreadNumber)
    {
      logError(ErrorLogCategory.CORE_SERVER, ErrorLogSeverity.INFORMATIONAL,
               MSGID_WORKER_STOPPED_BY_REDUCED_THREADNUMBER, getName());
    }
    else if (! workQueue.shutdownRequested())
    {
      logError(ErrorLogCategory.CORE_SERVER, ErrorLogSeverity.SEVERE_WARNING,
               MSGID_UNEXPECTED_WORKER_THREAD_EXIT, getName());
    }


    assert debugMessage(DebugLogCategory.CORE_SERVER, DebugLogSeverity.INFO,
                        CLASS_NAME, "run", getName() + " exiting.");
  }



  /**
   * Indicates that the Directory Server has received a request to stop running
   * and that this thread should stop running as soon as possible.
   */
  public void shutDown()
  {
    assert debugEnter(CLASS_NAME, "shutDown");
    assert debugMessage(DebugLogCategory.CORE_SERVER,
                        DebugLogSeverity.INFO, CLASS_NAME, "shutDown",
                        getName() + " being signaled to shut down.");


    // Set a flag that indicates that the thread should stop running.
    shutdownRequested = true;


    // Check to see if the thread is waiting for work.  If so, then interrupt
    // it.
    if (waitingForWork)
    {
      try
      {
        workerThread.interrupt();
      }
      catch (Exception e)
      {
        assert debugMessage(DebugLogCategory.CORE_SERVER,
                            DebugLogSeverity.WARNING, CLASS_NAME, "shutDown",
                            "Caught an exception while trying to interrupt " +
                            "the worker thread waiting for work:  " + e);
        assert debugException(CLASS_NAME, "shutDown", e);
      }
    }
    else
    {
      try
      {
        CancelRequest cancelRequest =
          new CancelRequest(true, getMessage(MSGID_CANCELED_BY_SHUTDOWN));
        operation.cancel(cancelRequest);
      }
      catch (Exception e)
      {
        assert debugMessage(DebugLogCategory.CORE_SERVER,
                            DebugLogSeverity.WARNING, CLASS_NAME, "shutDown",
                            "Caught an exception while trying to abandon the " +
                            "operation in progress for the worker thread:  " +
                            e);
        assert debugException(CLASS_NAME, "shutDown", e);
      }
    }
  }
}

