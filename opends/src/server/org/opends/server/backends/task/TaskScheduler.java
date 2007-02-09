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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.backends.task;



import java.io.IOException;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.opends.server.api.AlertGenerator;
import org.opends.server.api.DirectoryThread;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.Operation;
import org.opends.server.core.SearchOperation;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.ExistingFileBehavior;
import org.opends.server.types.InitializationException;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.types.LDIFExportConfig;
import org.opends.server.types.LockManager;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchFilter;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.opends.server.util.LDIFWriter;
import org.opends.server.util.TimeThread;

import static org.opends.server.config.ConfigConstants.*;
import static org.opends.server.loggers.Debug.*;
import static org.opends.server.loggers.Error.*;
import static org.opends.server.messages.BackendMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines a task scheduler for the Directory Server that will
 * control the execution of scheduled tasks and other administrative functions
 * that need to occur on a regular basis.
 */
public class TaskScheduler
       extends DirectoryThread
       implements AlertGenerator
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.backends.task.TaskScheduler";



  /**
   * The maximum length of time in milliseconds to sleep between iterations
   * through the scheduler loop.
   */
  private static long MAX_SLEEP_TIME = 5000;



  // Indicates whether the scheduler is currently running.
  private boolean isRunning;

  // Indicates whether a request has been received to stop the scheduler.
  private boolean stopRequested;

  // The entry that serves as the immediate parent for recurring tasks.
  private Entry recurringTaskParentEntry;

  // The entry that serves as the immediate parent for scheduled tasks.
  private Entry scheduledTaskParentEntry;

  // The top-level entry at the root of the task tree.
  private Entry taskRootEntry;

  // The set of recurring tasks defined in the server.
  private HashMap<String,RecurringTask> recurringTasks;

  // The set of tasks associated with this scheduler.
  private HashMap<String,Task> tasks;

  // The set of worker threads that are actively busy processing tasks.
  private HashMap<String,TaskThread> activeThreads;

  // The thread ID for the next task thread to be created;
  private int nextThreadID;

  // The set of worker threads that may be used to process tasks.
  private LinkedList<TaskThread> idleThreads;

  // The lock used to provide threadsafe access to the scheduler.
  private ReentrantLock schedulerLock;

  // The task backend with which this scheduler is associated.
  private TaskBackend taskBackend;

  // The thread being used to actually run the scheduler.
  private Thread schedulerThread;

  // The set of recently-completed tasks that need to be retained.
  private TreeSet<Task> completedTasks;

  // The set of tasks that have been scheduled but not yet arrived.
  private TreeSet<Task> pendingTasks;

  // The set of tasks that are currently running.
  private TreeSet<Task> runningTasks;



  /**
   * Creates a new task scheduler that will be used to ensure that tasks are
   * invoked at the appropriate times.
   *
   * @param  taskBackend  The task backend with which this scheduler is
   *                      associated.
   *
   * @throws  InitializationException  If a problem occurs while initializing
   *                                   the scheduler from the backing file.
   */
  public TaskScheduler(TaskBackend taskBackend)
         throws InitializationException
  {
    super("Task Scheduler Thread");

    assert debugConstructor(CLASS_NAME, String.valueOf(taskBackend));

    this.taskBackend = taskBackend;

    schedulerLock            = new ReentrantLock();
    isRunning                = false;
    stopRequested            = false;
    schedulerThread          = null;
    nextThreadID             = 1;
    recurringTasks           = new HashMap<String,RecurringTask>();
    tasks                    = new HashMap<String,Task>();
    activeThreads            = new HashMap<String,TaskThread>();
    idleThreads              = new LinkedList<TaskThread>();
    completedTasks           = new TreeSet<Task>();
    pendingTasks             = new TreeSet<Task>();
    runningTasks             = new TreeSet<Task>();
    taskRootEntry            = null;
    recurringTaskParentEntry = null;
    scheduledTaskParentEntry = null;

    DirectoryServer.registerAlertGenerator(this);

    initializeTasksFromBackingFile();
  }



  /**
   * Adds a recurring task to the scheduler, optionally scheduling the first
   * iteration for processing.
   *
   * @param  recurringTask      The recurring task to add to the scheduler.
   * @param  scheduleIteration  Indicates whether to schedule an iteration of
   *                            the recurring task.
   *
   * @throws  DirectoryException  If a problem occurs while trying to add the
   *                              recurring task (e.g., there's already another
   *                              recurring task defined with the same ID).
   */
  public void addRecurringTask(RecurringTask recurringTask,
                               boolean scheduleIteration)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "addRecurringTask",
                      String.valueOf(recurringTask),
                      String.valueOf(scheduleIteration));

    schedulerLock.lock();

    try
    {
      String id = recurringTask.getRecurringTaskID();

      if (recurringTasks.containsKey(id))
      {
        int    msgID   = MSGID_TASKSCHED_DUPLICATE_RECURRING_ID;
        String message = getMessage(msgID, String.valueOf(id));
        throw new DirectoryException(ResultCode.ENTRY_ALREADY_EXISTS, message,
                                     msgID);
      }

      recurringTasks.put(id, recurringTask);

      if (scheduleIteration)
      {
        Task task = recurringTask.scheduleNextIteration();
        if (task != null)
        {
          // FIXME -- What to do if task is null?
          scheduleTask(task, false);
        }
      }

      writeState();
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Removes the recurring task with the given ID.
   *
   * @param  recurringTaskID  The ID of the recurring task to remove.
   *
   * @return  The recurring task that was removed, or <CODE>null</CODE> if there
   *          was no such recurring task.
   *
   * @throws  DirectoryException  If there is currently a pending or running
   *                              iteration of the associated recurring task.
   */
  public RecurringTask removeRecurringTask(String recurringTaskID)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "removeRecurringTask",
                      String.valueOf(recurringTaskID));

    schedulerLock.lock();

    try
    {
      for (Task t : tasks.values())
      {
        if ((t.getRecurringTaskID() != null) &&
            (t.getRecurringTaskID().equals(recurringTaskID)) &&
            (! TaskState.isDone(t.getTaskState())))
        {
          int    msgID   = MSGID_TASKSCHED_REMOVE_RECURRING_EXISTING_ITERATION;
          String message = getMessage(msgID, String.valueOf(recurringTaskID),
                                      String.valueOf(t.getTaskID()));
          throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message,
                                       msgID);
        }
      }


      RecurringTask recurringTask = recurringTasks.remove(recurringTaskID);
      writeState();

      return recurringTask;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Schedules the provided task for execution.  If the scheduler is active and
   * the start time has arrived, then the task will begin execution immediately.
   * Otherwise, it will be placed in the pending queue to be started at the
   * appropriate time.
   *
   * @param  task        The task to be scheduled.
   * @param  writeState  Indicates whether the current state information for
   *                     the scheduler should be persisted to disk once the
   *                     task is scheduled.
   *
   * @throws  DirectoryException  If a problem occurs while trying to schedule
   *                              the task (e.g., there's already another task
   *                              defined with the same ID).
   */
  public void scheduleTask(Task task, boolean writeState)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "scheduleTask", String.valueOf(task));

    schedulerLock.lock();


    try
    {
      String id = task.getTaskID();

      if (tasks.containsKey(id))
      {
        int    msgID   = MSGID_TASKSCHED_DUPLICATE_TASK_ID;
        String message = getMessage(msgID, String.valueOf(id));
        throw new DirectoryException(ResultCode.ENTRY_ALREADY_EXISTS, message,
                                     msgID);
      }

      tasks.put(id, task);

      TaskState state = shouldStart(task);
      task.setTaskState(state);

      if (state == TaskState.RUNNING)
      {
        TaskThread taskThread;
        if (idleThreads.isEmpty())
        {
          taskThread = new TaskThread(this, nextThreadID++);
          taskThread.start();
        }
        else
        {
          taskThread = idleThreads.removeFirst();
        }

        runningTasks.add(task);
        activeThreads.put(task.getTaskID(), taskThread);
        taskThread.setTask(task);
      }
      else
      {
        pendingTasks.add(task);
      }

      if (writeState)
      {
        writeState();
      }
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Attempts to cancel the task with the given task ID.  This will only cancel
   * the task if it has not yet started running.  If it has started, then it
   * will not be interrupted.
   *
   * @param  taskID  The task ID of the task to cancel.
   *
   * @return  The requested task, which may or may not have actually been
   *          cancelled (the task state should make it possible to determine
   *          whether it was cancelled), or <CODE>null</CODE> if there is no
   *          such task.
   */
  public Task cancelTask(String taskID)
  {
    assert debugEnter(CLASS_NAME, "cancelTask", String.valueOf(taskID));

    schedulerLock.lock();

    try
    {
      Task t = tasks.get(taskID);
      if (t == null)
      {
        return null;
      }

      if (TaskState.isPending(t.getTaskState()))
      {
        pendingTasks.remove(t);
        t.setTaskState(TaskState.CANCELED_BEFORE_STARTING);
        addCompletedTask(t);
        writeState();
      }

      return t;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Removes the specified pending task.  It will be completely removed rather
   * than moving it to the set of completed tasks.
   *
   * @param  taskID  The task ID of the pending task to remove.
   *
   * @return  The task that was removed.
   *
   * @throws  DirectoryException  If the requested task is not in the pending
   *                              queue.
   */
  public Task removePendingTask(String taskID)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "removePendingTask", String.valueOf(taskID));

    schedulerLock.lock();

    try
    {
      Task t = tasks.get(taskID);
      if (t == null)
      {
        int    msgID   = MSGID_TASKSCHED_REMOVE_PENDING_NO_SUCH_TASK;
        String message = getMessage(msgID, String.valueOf(taskID));
        throw new DirectoryException(ResultCode.NO_SUCH_OBJECT, message, msgID);
      }

      if (TaskState.isPending(t.getTaskState()))
      {
        tasks.remove(taskID);
        pendingTasks.remove(t);
        writeState();
        return t;
      }
      else
      {
        int   msgID    = MSGID_TASKSCHED_REMOVE_PENDING_NOT_PENDING;
        String message = getMessage(msgID, String.valueOf(taskID));
        throw new DirectoryException(ResultCode.UNWILLING_TO_PERFORM, message,
                                     msgID);
      }
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Removes the specified completed task.
   *
   * @param  taskID  The task ID of the completed task to remove.
   *
   * @return  The task that was removed.
   *
   * @throws  DirectoryException  If the requested task could not be found.
   */
  public Task removeCompletedTask(String taskID)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "removeCompletedTask",
                      String.valueOf(taskID));

    schedulerLock.lock();

    try
    {
      Iterator<Task> iterator = completedTasks.iterator();
      while (iterator.hasNext())
      {
        Task t = iterator.next();
        if (t.getTaskID().equals(taskID))
        {
          iterator.remove();
          tasks.remove(taskID);
          writeState();
          return t;
        }
      }

      int    msgID   = MSGID_TASKSCHED_REMOVE_COMPLETED_NO_SUCH_TASK;
      String message = getMessage(msgID, String.valueOf(taskID));
      throw new DirectoryException(ResultCode.NO_SUCH_OBJECT, message, msgID);
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Indicates that processing has completed on the provided task thread and
   * that it is now available for processing other tasks.  The thread may be
   * immediately used for processing another task if appropriate.
   *
   * @param  taskThread     The thread that has completed processing on its
   *                        previously-assigned task.
   * @param  completedTask  The task for which processing has been completed.
   *
   * @return  <CODE>true</CODE> if the thread should continue running and
   *          wait for the next task to process, or <CODE>false</CODE> if it
   *          should exit immediately.
   */
  public boolean threadDone(TaskThread taskThread, Task completedTask)
  {
    assert debugEnter(CLASS_NAME, "threadDone", String.valueOf(taskThread));

    schedulerLock.lock();

    try
    {
      addCompletedTask(completedTask);

      String taskID = completedTask.getTaskID();
      if (activeThreads.remove(taskID) == null)
      {
        return false;
      }


      // See if the task is part of a recurring task.  If so, then schedule the
      // next iteration.
      String recurringTaskID = completedTask.getRecurringTaskID();
      if (recurringTaskID != null)
      {
        RecurringTask recurringTask = recurringTasks.get(recurringTaskID);
        if (recurringTask == null)
        {
          // This shouldn't happen, but handle it anyway.
          int    msgID   = MSGID_TASKSCHED_CANNOT_FIND_RECURRING_TASK;
          String message = getMessage(msgID, String.valueOf(taskID),
                                      String.valueOf(recurringTaskID));
          logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR,
                   message, msgID);

          DirectoryServer.sendAlertNotification(this,
                               ALERT_TYPE_CANNOT_FIND_RECURRING_TASK, msgID,
                               message);
        }
        else
        {
          Task newIteration = recurringTask.scheduleNextIteration();
          if (newIteration != null)
          {
            // FIXME -- What to do if new iteration is null?

            try
            {
              scheduleTask(newIteration, false);
            }
            catch (DirectoryException de)
            {
              assert debugException(CLASS_NAME, "threadDone", de);

              int msgID = MSGID_TASKSCHED_ERROR_SCHEDULING_RECURRING_ITERATION;
              String message = getMessage(msgID, recurringTaskID,
                                          de.getErrorMessage());
              logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR,
                       message, msgID);

              DirectoryServer.sendAlertNotification(this,
                   ALERT_TYPE_CANNOT_SCHEDULE_RECURRING_ITERATION, msgID,
                   message);
            }
          }
        }
      }


      writeState();


      if (isRunning)
      {
        idleThreads.add(taskThread);
        return true;
      }
      else
      {
        return false;
      }
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Adds the provided task to the set of completed tasks associated with the
   * scheduler.  It will be automatically removed after the appropriate
   * retention time has elapsed.
   *
   * @param  completedTask  The task for which processing has completed.
   */
  public void addCompletedTask(Task completedTask)
  {
    assert debugEnter(CLASS_NAME, "addCompletedTask",
                      String.valueOf(completedTask));

    // The scheduler lock is reentrant, so even if we already hold it, we can
    // acquire it again.
    schedulerLock.lock();

    try
    {
      completedTasks.add(completedTask);
      runningTasks.remove(completedTask);
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Stops the scheduler so that it will not start any scheduled tasks.  It will
   * not attempt to interrupt any tasks that are already running.  Note that
   * once the scheduler has been stopped, it cannot be restarted and it will be
   * necessary to restart the task backend to start a new scheduler instance.
   */
  public void stopScheduler()
  {
    assert debugEnter(CLASS_NAME, "stopScheduler");

    stopRequested = true;

    try
    {
      schedulerThread.interrupt();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "stopScheduler", e);
    }

    try
    {
      schedulerThread.join();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "stopScheduler", e);
    }

    pendingTasks.clear();
    runningTasks.clear();
    completedTasks.clear();
    tasks.clear();

    for (TaskThread thread : idleThreads)
    {
      int    msgID   = MSGID_TASKBE_INTERRUPTED_BY_SHUTDOWN;
      String message = getMessage(msgID);
      thread.interruptTask(TaskState.STOPPED_BY_SHUTDOWN, message, true);
    }
  }



  /**
   * Attempts to interrupt any tasks that are actively running.  This will not
   * make any attempt to stop the scheduler.
   *
   * @param  interruptState   The state that should be assigned to the tasks if
   *                          they are successfully interrupted.
   * @param  interruptReason  A human-readable message indicating the reason
   *                          that the tasks are to be interrupted.
   * @param  waitForStop      Indicates whether this method should wait until
   *                          all active tasks have stopped before returning.
   */
  public void interruptRunningTasks(TaskState interruptState,
                                    String interruptReason, boolean waitForStop)
  {
    assert debugEnter(CLASS_NAME, "interruptRunningTasks",
                      String.valueOf(waitForStop));


    // Grab a copy of the running threads so that we can operate on them without
    // holding the lock.
    LinkedList<TaskThread> threadList = new LinkedList<TaskThread>();

    schedulerLock.lock();

    try
    {
      threadList.addAll(activeThreads.values());
    }
    finally
    {
      schedulerLock.unlock();
    }


    // Iterate through all the task threads and request that they stop
    // processing.
    for (TaskThread t : threadList)
    {
      try
      {
        t.interruptTask(interruptState, interruptReason, true);
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "interruptRunningTasks", e);
      }
    }


    // If we should actually wait for all the task threads to stop, then do so.
    if (waitForStop)
    {
      for (TaskThread t : threadList)
      {
        try
        {
          t.join();
        }
        catch (Exception e)
        {
          assert debugException(CLASS_NAME, "interruptRunningTasks", e);
        }
      }
    }
  }



  /**
   * Operates in a loop, launching tasks at the appropriate time and performing
   * any necessary periodic cleanup.
   */
  public void run()
  {
    assert debugEnter(CLASS_NAME, "run");

    isRunning       = true;
    schedulerThread = currentThread();

    try
    {
      while (! stopRequested)
      {
        schedulerLock.lock();

        boolean writeState = false;
        long sleepTime = MAX_SLEEP_TIME;

        try
        {
          // If there are any pending tasks that need to be started, then do so
          // now.
          Iterator<Task> iterator = pendingTasks.iterator();
          while (iterator.hasNext())
          {
            Task t = iterator.next();
            TaskState state = shouldStart(t);

            if (state == TaskState.RUNNING)
            {
              TaskThread taskThread;
              if (idleThreads.isEmpty())
              {
                taskThread = new TaskThread(this, nextThreadID++);
                taskThread.start();
              }
              else
              {
                taskThread = idleThreads.removeFirst();
              }

              runningTasks.add(t);
              activeThreads.put(t.getTaskID(), taskThread);
              taskThread.setTask(t);

              iterator.remove();
              writeState = true;
            }
            else if (state == TaskState.WAITING_ON_START_TIME)
            {
              // If we're waiting for the start time to arrive, then see if that
              // will come before the next sleep time is up.
              long waitTime = t.getScheduledStartTime() - TimeThread.getTime();
              sleepTime = Math.min(sleepTime, waitTime);
            }

            if (state != t.getTaskState())
            {
              t.setTaskState(state);
              writeState = true;
            }
          }


          // Clean up any completed tasks that have been around long enough.
          long oldestRetainedCompletionTime =
                    TimeThread.getTime() - taskBackend.getRetentionTime();
          iterator = completedTasks.iterator();
          while (iterator.hasNext())
          {
            Task t = iterator.next();
            if (t.getCompletionTime() < oldestRetainedCompletionTime)
            {
              iterator.remove();
              writeState = true;
            }

            // FIXME -- If the completed tasks list is sorted, can we break out
            //          of the iterator as soon as we hit one that's not old
            //          enough to be expired?
          }


          // FIXME -- Should we check to see if any of the running jobs have
          //          logged any messages?


          // If anything changed, then make sure that the on-disk state gets
          // updated.
          if (writeState)
          {
            writeState();
          }
        }
        finally
        {
          schedulerLock.unlock();
        }


        try
        {
          if (sleepTime > 0)
          {
            Thread.sleep(sleepTime);
          }
        } catch (InterruptedException ie){}

        // Clean up any completed tasks that have been around long enough.
      }
    }
    finally
    {
      isRunning = false;
    }
  }



  /**
   * Determines whether the specified task should start running.  This is based
   * on the start time, the set of dependencies, and whether or not the
   * scheduler is active.  Note that the caller to this method must hold the
   * scheduler lock.
   *
   * @param  task  The task for which to make the determination.
   *
   * @return  The task state that should be used for the task.  It should be
   *          RUNNING if the task should be started, or some other state if not.
   */
  private TaskState shouldStart(Task task)
  {
    assert debugEnter(CLASS_NAME, "shouldStart", String.valueOf(task));

    if (! isRunning)
    {
      return TaskState.UNSCHEDULED;
    }

    if (task.getScheduledStartTime() > TimeThread.getTime())
    {
      return TaskState.WAITING_ON_START_TIME;
    }

    LinkedList<String> dependencyIDs = task.getDependencyIDs();
    if (dependencyIDs != null)
    {
      for (String dependencyID : task.getDependencyIDs())
      {
        Task t = tasks.get(dependencyID);
        if ((t != null) && (! TaskState.isDone(t.getTaskState())))
        {
          return TaskState.WAITING_ON_DEPENDENCY;
        }
      }
    }

    return TaskState.RUNNING;
  }



  /**
   * Populates the scheduler with information read from the task backing file.
   * If no backing file is found, then create a new one.  The caller must
   * already hold the scheduler lock or otherwise ensure that this is a
   * threadsafe operation.
   *
   * @throws  InitializationException  If a fatal error occurs while attempting
   *                                   to perform the initialization.
   */
  private void initializeTasksFromBackingFile()
          throws InitializationException
  {
    assert debugEnter(CLASS_NAME, "initializeTasksFromBackingFile");

    String backingFilePath = taskBackend.getTaskBackingFile();

    try
    {
      File backingFile = getFileForPath(backingFilePath);
      if (! backingFile.exists())
      {
        createNewTaskBackingFile();
        return;
      }


      LDIFImportConfig importConfig = new LDIFImportConfig(backingFilePath);
      LDIFReader ldifReader = new LDIFReader(importConfig);

      taskRootEntry            = null;
      recurringTaskParentEntry = null;
      scheduledTaskParentEntry = null;

      while (true)
      {
        Entry entry;

        try
        {
          entry = ldifReader.readEntry();
        }
        catch (LDIFException le)
        {
          assert debugException(CLASS_NAME, "initializeTasksFromBackingFile",
                                le);

          if (le.canContinueReading())
          {
            int    msgID   = MSGID_TASKSCHED_CANNOT_PARSE_ENTRY_RECOVERABLE;
            String message = getMessage(msgID, backingFilePath,
                                        le.getLineNumber(), le.getMessage());
            logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR,
                     message, msgID);

            continue;
          }
          else
          {
            try
            {
              ldifReader.close();
            }
            catch (Exception e)
            {
              assert debugException(CLASS_NAME,
                                    "initializeTasksFromBackingFile", e);
            }

            int    msgID   = MSGID_TASKSCHED_CANNOT_PARSE_ENTRY_FATAL;
            String message = getMessage(msgID, backingFilePath,
                                        le.getLineNumber(), le.getMessage());
            throw new InitializationException(msgID, message);
          }
        }

        if (entry == null)
        {
          break;
        }

        DN entryDN = entry.getDN();
        if (entryDN.equals(taskBackend.getTaskRootDN()))
        {
          taskRootEntry = entry;
        }
        else if (entryDN.equals(taskBackend.getRecurringTasksParentDN()))
        {
          recurringTaskParentEntry = entry;
        }
        else if (entryDN.equals(taskBackend.getScheduledTasksParentDN()))
        {
          scheduledTaskParentEntry = entry;
        }
        else
        {
          DN parentDN = entryDN.getParentDNInSuffix();
          if (parentDN == null)
          {
            int    msgID   = MSGID_TASKSCHED_ENTRY_HAS_NO_PARENT;
            String message = getMessage(msgID, String.valueOf(entryDN),
                                  String.valueOf(taskBackend.getTaskRootDN()));
            logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR,
                     message, msgID);
          }
          else if (parentDN.equals(taskBackend.getRecurringTasksParentDN()))
          {
            try
            {
              RecurringTask recurringTask = entryToRecurringTask(entry);
              addRecurringTask(recurringTask, false);
            }
            catch (DirectoryException de)
            {
              assert debugException(CLASS_NAME,
                                    "initializeTasksFromBackingFile", de);

              int msgID =
                   MSGID_TASKSCHED_CANNOT_SCHEDULE_RECURRING_TASK_FROM_ENTRY;
              String message = getMessage(msgID, String.valueOf(entryDN),
                                          de.getErrorMessage());
              logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR,
                       message, msgID);
            }
          }
          else if (parentDN.equals(taskBackend.getScheduledTasksParentDN()))
          {
            try
            {
              Task task = entryToScheduledTask(entry, null);
              if (TaskState.isDone(task.getTaskState()))
              {
                completedTasks.add(task);
              }
              else
              {
                scheduleTask(task, false);
              }
            }
            catch (DirectoryException de)
            {
              assert debugException(CLASS_NAME,
                                    "initializeTasksFromBackingFile", de);

              int    msgID   = MSGID_TASKSCHED_CANNOT_SCHEDULE_TASK_FROM_ENTRY;
              String message = getMessage(msgID, String.valueOf(entryDN),
                                          de.getErrorMessage());
              logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR,
                       message, msgID);
            }
          }
          else
          {
            int    msgID   = MSGID_TASKSCHED_INVALID_TASK_ENTRY_DN;
            String message = getMessage(msgID, String.valueOf(entryDN),
                                        backingFilePath);
            logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR,
                     message, msgID);
          }
        }
      }

      ldifReader.close();
    }
    catch (IOException ioe)
    {
      assert debugException(CLASS_NAME, "initializeTasksFromBackingFile", ioe);

      int msgID = MSGID_TASKSCHED_ERROR_READING_TASK_BACKING_FILE;
      String message = getMessage(msgID, String.valueOf(backingFilePath),
                                  stackTraceToSingleLineString(ioe));
      throw new InitializationException(msgID, message, ioe);
    }
  }



  /**
   * Creates a new task backing file that contains only the basic structure but
   * no scheduled or recurring task entries.  The caller must already hold the
   * scheduler lock or otherwise ensure that this is a threadsafe operation.
   *
   * @throws  InitializationException  If a problem occurs while attempting to
   *                                   create the backing file.
   */
  private void createNewTaskBackingFile()
          throws InitializationException
  {
    assert debugEnter(CLASS_NAME, "createNewTaskBackingFile");

    String backingFile = taskBackend.getTaskBackingFile();
    LDIFExportConfig exportConfig =
         new LDIFExportConfig(backingFile, ExistingFileBehavior.OVERWRITE);

    try
    {
      LDIFWriter writer = new LDIFWriter(exportConfig);

      // First, write a header to the top of the file to indicate that it should
      // not be manually edited.
      writer.writeComment(getMessage(MSGID_TASKBE_BACKING_FILE_HEADER), 80);


      // Next, create the required hierarchical entries and add them to the
      // LDIF.
      taskRootEntry = createEntry(taskBackend.getTaskRootDN());
      writer.writeEntry(taskRootEntry);

      scheduledTaskParentEntry =
           createEntry(taskBackend.getScheduledTasksParentDN());
      writer.writeEntry(scheduledTaskParentEntry);

      recurringTaskParentEntry =
           createEntry(taskBackend.getRecurringTasksParentDN());
      writer.writeEntry(recurringTaskParentEntry);


      // Close the file and we're done.
      writer.close();
    }
    catch (IOException ioe)
    {
      assert debugException(CLASS_NAME, "createNewTaskBackingFile", ioe);

      int    msgID   = MSGID_TASKSCHED_CANNOT_CREATE_BACKING_FILE;
      String message = getMessage(msgID, backingFile,
                                  stackTraceToSingleLineString(ioe));
      throw new InitializationException(msgID, message, ioe);
    }
    catch (LDIFException le)
    {
      assert debugException(CLASS_NAME, "createNewTaskBackingFile", le);


      int    msgID   = MSGID_TASKSCHED_CANNOT_CREATE_BACKING_FILE;
      String message = getMessage(msgID, backingFile, le.getMessage());
      throw new InitializationException(msgID, message, le);
    }
  }



  /**
   * Writes state information about all tasks and recurring tasks to disk.
   */
  public void writeState()
  {
    assert debugEnter(CLASS_NAME, "writeState");


    String backingFilePath = taskBackend.getTaskBackingFile();
    String tmpFilePath     = backingFilePath + ".tmp";
    LDIFExportConfig exportConfig =
         new LDIFExportConfig(tmpFilePath, ExistingFileBehavior.OVERWRITE);


    schedulerLock.lock();

    try
    {
      LDIFWriter writer = new LDIFWriter(exportConfig);

      // First, write a header to the top of the file to indicate that it should
      // not be manually edited.
      writer.writeComment(getMessage(MSGID_TASKBE_BACKING_FILE_HEADER), 80);


      // Next, write the structural entries to the top of the LDIF.
      writer.writeEntry(taskRootEntry);
      writer.writeEntry(scheduledTaskParentEntry);
      writer.writeEntry(recurringTaskParentEntry);


      // Iterate through all the recurring tasks and write them to LDIF.
      for (RecurringTask recurringTask : recurringTasks.values())
      {
        writer.writeEntry(recurringTask.getRecurringTaskEntry());
      }


      // Iterate through all the scheduled tasks and write them to LDIF.
      for (Task task : tasks.values())
      {
        writer.writeEntry(task.getTaskEntry());
      }


      // Close the file.
      writer.close();


      // See if there is a ".save" file.  If so, then delete it.
      File saveFile = getFileForPath(backingFilePath + ".save");
      try
      {
        if (saveFile.exists())
        {
          saveFile.delete();
        }
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "writeState", e);
      }


      // If there is an existing backing file, then rename it to ".save".
      File backingFile = getFileForPath(backingFilePath);
      try
      {
        if (backingFile.exists())
        {
          backingFile.renameTo(saveFile);
        }
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "writeState", e);

        int msgID = MSGID_TASKSCHED_CANNOT_RENAME_CURRENT_BACKING_FILE;
        String message = getMessage(msgID, String.valueOf(backingFilePath),
                                    String.valueOf(saveFile.getAbsolutePath()),
                                    stackTraceToSingleLineString(e));
        logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_WARNING,
                 message, msgID);

        DirectoryServer.sendAlertNotification(this,
                             ALERT_TYPE_CANNOT_RENAME_CURRENT_TASK_FILE, msgID,
                             message);
      }


      // Rename the ".tmp" file into place.
      File tmpFile = getFileForPath(tmpFilePath);
      try
      {
        tmpFile.renameTo(backingFile);
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "writeState", e);

        int msgID = MSGID_TASKSCHED_CANNOT_RENAME_NEW_BACKING_FILE;
        String message = getMessage(msgID, String.valueOf(tmpFilePath),
                                    String.valueOf(backingFilePath),
                                    stackTraceToSingleLineString(e));
        logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR, message,
                 msgID);

        DirectoryServer.sendAlertNotification(this,
                             ALERT_TYPE_CANNOT_RENAME_NEW_TASK_FILE, msgID,
                             message);
      }
    }
    catch (IOException ioe)
    {
      assert debugException(CLASS_NAME, "createNewTaskBackingFile", ioe);

      int    msgID   = MSGID_TASKSCHED_CANNOT_WRITE_BACKING_FILE;
      String message = getMessage(msgID, tmpFilePath,
                                  stackTraceToSingleLineString(ioe));

      logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR, message,
               msgID);
      DirectoryServer.sendAlertNotification(this,
                           ALERT_TYPE_CANNOT_WRITE_TASK_FILE, msgID, message);
    }
    catch (LDIFException le)
    {
      assert debugException(CLASS_NAME, "createNewTaskBackingFile", le);


      int    msgID   = MSGID_TASKSCHED_CANNOT_WRITE_BACKING_FILE;
      String message = getMessage(msgID, tmpFilePath, le.getMessage());

      logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR, message,
               msgID);
      DirectoryServer.sendAlertNotification(this,
                           ALERT_TYPE_CANNOT_WRITE_TASK_FILE, msgID, message);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "createNewTaskBackingFile", e);

      int    msgID   = MSGID_TASKSCHED_CANNOT_WRITE_BACKING_FILE;
      String message = getMessage(msgID, tmpFilePath,
                                  stackTraceToSingleLineString(e));

      logError(ErrorLogCategory.TASK, ErrorLogSeverity.SEVERE_ERROR, message,
               msgID);
      DirectoryServer.sendAlertNotification(this,
                           ALERT_TYPE_CANNOT_WRITE_TASK_FILE, msgID, message);
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Retrieves the total number of entries in the task backend.
   *
   * @return  The total number of entries in the task backend.
   */
  public long getEntryCount()
  {
    assert debugEnter(CLASS_NAME, "getEntryCount");

    schedulerLock.lock();

    try
    {
      return tasks.size() + recurringTasks.size() + 3;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Retrieves the root entry that is the common ancestor for all entries in the
   * task backend.
   *
   * @return  The root entry that is the common ancestor for all entries in the
   *          task backend.
   */
  public Entry getTaskRootEntry()
  {
    assert debugEnter(CLASS_NAME, "getTaskRootEntry");

    return taskRootEntry;
  }



  /**
   * Retrieves the entry that is the immediate parent for all scheduled task
   * entries in the task backend.
   *
   * @return  The entry that is the immediate parent for all scheduled task
   *          entries in the task backend.
   */
  public Entry getScheduledTaskParentEntry()
  {
    assert debugEnter(CLASS_NAME, "getScheduledTaskParentEntry");

    return scheduledTaskParentEntry;
  }



  /**
   * Retrieves the entry that is the immediate parent for all recurring task
   * entries in the task backend.
   *
   * @return  The entry that is the immediate parent for all recurring task
   *          entries in the task backend.
   */
  public Entry getRecurringTaskParentEntry()
  {
    assert debugEnter(CLASS_NAME, "getRecurringTaskParentEntry");

    return recurringTaskParentEntry;
  }



  /**
   * Retrieves the scheduled task with the given task ID.
   *
   * @param  taskID  The task ID for the scheduled task to retrieve.
   *
   * @return  The requested scheduled task, or <CODE>null</CODE> if there is no
   *          such task.
   */
  public Task getScheduledTask(String taskID)
  {
    assert debugEnter(CLASS_NAME, "getScheduledTask", String.valueOf(taskID));

    schedulerLock.lock();

    try
    {
      return tasks.get(taskID);
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Retrieves the scheduled task created from the specified entry.
   *
   * @param  taskEntryDN  The DN of the task configuration entry associated
   *                       with the task to retrieve.
   *
   * @return  The requested scheduled task, or <CODE>null</CODE> if there is no
   *          such task.
   */
  public Task getScheduledTask(DN taskEntryDN)
  {
    assert debugEnter(CLASS_NAME, "getScheduledTask",
                      String.valueOf(taskEntryDN));

    schedulerLock.lock();

    try
    {
      for (Task t : tasks.values())
      {
        if (taskEntryDN.equals(t.getTaskEntry().getDN()))
        {
          return t;
        }
      }

      return null;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Attempts to acquire a write lock on the specified entry, trying as many
   * times as necessary until the lock has been acquired.
   *
   * @param  entryDN  The DN of the entry for which to acquire the write lock.
   *
   * @return  The write lock that has been acquired for the entry.
   */
  Lock writeLockEntry(DN entryDN)
  {
    assert debugEnter(CLASS_NAME, "lockEntry", String.valueOf(entryDN));

    Lock lock = LockManager.lockWrite(entryDN);
    while (lock == null)
    {
      lock = LockManager.lockWrite(entryDN);
    }

    return lock;
  }



  /**
   * Attempts to acquire a read lock on the specified entry, trying up to five
   * times before failing.
   *
   * @param  entryDN  The DN of the entry for which to acquire the read lock.
   *
   * @return  The read lock that has been acquired for the entry.
   *
   * @throws  DirectoryException  If the read lock cannot be acquired.
   */
  Lock readLockEntry(DN entryDN)
       throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "lockEntry", String.valueOf(entryDN));

    Lock lock = LockManager.lockRead(entryDN);
    for (int i=0; ((lock == null) && (i < 4)); i++)
    {
      lock = LockManager.lockRead(entryDN);
    }

    if (lock == null)
    {
      int    msgID   = MSGID_BACKEND_CANNOT_LOCK_ENTRY;
      String message = getMessage(msgID, String.valueOf(entryDN));
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, msgID);
    }
    else
    {
      return lock;
    }
  }



  /**
   * Releases the lock held on the specified entry.
   *
   * @param  entryDN  The DN of the entry for which the lock is held.
   * @param  lock     The lock held on the entry.
   */
  void unlockEntry(DN entryDN, Lock lock)
  {
    assert debugEnter(CLASS_NAME, "unlockEntry", String.valueOf(entryDN),
                      String.valueOf(lock));

    LockManager.unlock(entryDN, lock);
  }



  /**
   * Retrieves the scheduled task entry with the provided DN.  The caller should
   * hold a read lock on the target entry.
   *
   * @param  scheduledTaskEntryDN  The entry DN that indicates which scheduled
   *                               task entry to retrieve.
   *
   * @return  The scheduled task entry with the provided DN, or
   *          <CODE>null</CODE> if no scheduled task has the provided DN.
   */
  public Entry getScheduledTaskEntry(DN scheduledTaskEntryDN)
  {
    assert debugEnter(CLASS_NAME, "getScheduledTaskEntry",
                      String.valueOf(scheduledTaskEntryDN));


    schedulerLock.lock();

    try
    {
      for (Task task : tasks.values())
      {
        Entry taskEntry = task.getTaskEntry();

        if (scheduledTaskEntryDN.equals(taskEntry.getDN()))
        {
          return taskEntry;
        }
      }

      return null;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Compares the filter in the provided search operation against each of the
   * task entries, returning any that match.  Note that only the search filter
   * will be used -- the base and scope will be ignored, so the caller must
   * ensure that they are correct for scheduled tasks.
   *
   * @param  searchOperation  The search operation to use when performing the
   *                          search.
   *
   * @return  <CODE>true</CODE> if processing should continue on the search
   *          operation, or <CODE>false</CODE> if it should not for some reason
   *          (e.g., a size or time limit was reached).
   *
   * @throws  DirectoryException  If a problem occurs while processing the
   *                              search operation against the scheduled tasks.
   */
  public boolean searchScheduledTasks(SearchOperation searchOperation)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "searchScheduledTasks",
                      String.valueOf(searchOperation));

    SearchFilter filter = searchOperation.getFilter();

    schedulerLock.lock();

    try
    {
      for (Task t : tasks.values())
      {
        DN taskEntryDN = t.getTaskEntryDN();
        Lock lock = readLockEntry(taskEntryDN);

        try
        {
          Entry e = t.getTaskEntry();
          if (filter.matchesEntry(e))
          {
            if (! searchOperation.returnEntry(e, null))
            {
              return false;
            }
          }
        }
        finally
        {
          unlockEntry(taskEntryDN, lock);
        }
      }

      return true;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Retrieves the recurring task with the given recurring task ID.
   *
   * @param  recurringTaskID  The recurring task ID for the recurring task to
   *                          retrieve.
   *
   * @return  The requested recurring task, or <CODE>null</CODE> if there is no
   *          such recurring task.
   */
  public RecurringTask getRecurringTask(String recurringTaskID)
  {
    assert debugEnter(CLASS_NAME, "getRecurringTask",
                      String.valueOf(recurringTaskID));

    schedulerLock.lock();

    try
    {
      return recurringTasks.get(recurringTaskID);
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Retrieves the recurring task with the given recurring task ID.
   *
   * @param  recurringTaskEntryDN  The recurring task ID for the recurring task
   *                               to retrieve.
   *
   * @return  The requested recurring task, or <CODE>null</CODE> if there is no
   *          such recurring task.
   */
  public RecurringTask getRecurringTask(DN recurringTaskEntryDN)
  {
    assert debugEnter(CLASS_NAME, "getRecurringTask",
                      String.valueOf(recurringTaskEntryDN));

    schedulerLock.lock();

    try
    {
      for (RecurringTask rt : recurringTasks.values())
      {
        if (recurringTaskEntryDN.equals(rt.getRecurringTaskEntry().getDN()))
        {
          return rt;
        }
      }

      return null;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Retrieves the recurring task entry with the provided DN.  The caller should
   * hold a read lock on the target entry.
   *
   * @param  recurringTaskEntryDN  The entry DN that indicates which recurring
   *                               task entry to retrieve.
   *
   * @return  The recurring task entry with the provided DN, or
   *          <CODE>null</CODE> if no recurring task has the provided DN.
   */
  public Entry getRecurringTaskEntry(DN recurringTaskEntryDN)
  {
    assert debugEnter(CLASS_NAME, "getRecurringTaskEntry",
                      String.valueOf(recurringTaskEntryDN));


    schedulerLock.lock();

    try
    {
      for (RecurringTask recurringTask : recurringTasks.values())
      {
        Entry recurringTaskEntry = recurringTask.getRecurringTaskEntry();

        if (recurringTaskEntryDN.equals(recurringTaskEntry.getDN()))
        {
          return recurringTaskEntry;
        }
      }

      return null;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Compares the filter in the provided search operation against each of the
   * recurring task entries, returning any that match.  Note that only the
   * search filter will be used -- the base and scope will be ignored, so the
   * caller must ensure that they are correct for recurring tasks.
   *
   * @param  searchOperation  The search operation to use when performing the
   *                          search.
   *
   * @return  <CODE>true</CODE> if processing should continue on the search
   *          operation, or <CODE>false</CODE> if it should not for some reason
   *          (e.g., a size or time limit was reached).
   *
   * @throws  DirectoryException  If a problem occurs while processing the
   *                              search operation against the recurring tasks.
   */
  public boolean searchRecurringTasks(SearchOperation searchOperation)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "searchRecurringTasks",
                      String.valueOf(searchOperation));

    SearchFilter filter = searchOperation.getFilter();

    schedulerLock.lock();

    try
    {
      for (RecurringTask rt : recurringTasks.values())
      {
        DN recurringTaskEntryDN = rt.getRecurringTaskEntryDN();
        Lock lock = readLockEntry(recurringTaskEntryDN);

        try
        {
          Entry e = rt.getRecurringTaskEntry();
          if (filter.matchesEntry(e))
          {
            if (! searchOperation.returnEntry(e, null))
            {
              return false;
            }
          }
        }
        finally
        {
          unlockEntry(recurringTaskEntryDN, lock);
        }
      }

      return true;
    }
    finally
    {
      schedulerLock.unlock();
    }
  }



  /**
   * Decodes the contents of the provided entry as a scheduled task.  The
   * resulting task will not actually be scheduled for processing.
   *
   * @param  entry      The entry to decode as a scheduled task.
   * @param  operation  The operation used to create this task in the server, or
   *                    {@code null} if the operation is not available.
   *
   * @return  The scheduled task decoded from the provided entry.
   *
   * @throws  DirectoryException  If the provided entry cannot be decoded as a
   *                              scheduled task.
   */
  public Task entryToScheduledTask(Entry entry, Operation operation)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "entryToScheduledTask",
                      String.valueOf(entry));


    // Get the name of the class that implements the task logic.
    AttributeType attrType =
         DirectoryServer.getAttributeType(ATTR_TASK_CLASS.toLowerCase());
    if (attrType == null)
    {
      attrType = DirectoryServer.getDefaultAttributeType(ATTR_TASK_CLASS);
    }

    List<Attribute> attrList = entry.getAttribute(attrType);
    if ((attrList == null) || attrList.isEmpty())
    {
      int    msgID   = MSGID_TASKSCHED_NO_CLASS_ATTRIBUTE;
      String message = getMessage(msgID, ATTR_TASK_ID);
      throw new DirectoryException(ResultCode.CONSTRAINT_VIOLATION, message,
                                   msgID);
    }

    if (attrList.size() > 1)
    {
      int    msgID   = MSGID_TASKSCHED_MULTIPLE_CLASS_TYPES;
      String message = getMessage(msgID, ATTR_TASK_ID);
      throw new DirectoryException(ResultCode.CONSTRAINT_VIOLATION, message,
                                   msgID);
    }

    Attribute attr = attrList.get(0);
    LinkedHashSet<AttributeValue> values = attr.getValues();
    if ((values == null) || values.isEmpty())
    {
      int    msgID   = MSGID_TASKSCHED_NO_CLASS_VALUES;
      String message = getMessage(msgID, ATTR_TASK_ID);
      throw new DirectoryException(ResultCode.CONSTRAINT_VIOLATION, message,
                                   msgID);
    }

    Iterator<AttributeValue> iterator = values.iterator();
    AttributeValue value = iterator.next();
    if (iterator.hasNext())
    {
      int    msgID   = MSGID_TASKSCHED_MULTIPLE_CLASS_VALUES;
      String message = getMessage(msgID, ATTR_TASK_ID);
      throw new DirectoryException(ResultCode.OBJECTCLASS_VIOLATION, message,
                                   msgID);
    }

    String taskClassName = value.getStringValue();


    // Try to load the specified class.
    Class taskClass;
    try
    {
      // FIXME -- Should we use a custom class loader for this?
      taskClass = Class.forName(taskClassName);
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "entryToScheduledTask", e);

      int    msgID   = MSGID_TASKSCHED_CANNOT_LOAD_CLASS;
      String message = getMessage(msgID, String.valueOf(taskClassName),
                                  ATTR_TASK_CLASS,
                                  stackTraceToSingleLineString(e));
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, msgID);
    }


    // Instantiate the class as a task.
    Task task;
    try
    {
      task = (Task) taskClass.newInstance();
    }
    catch (Exception e)
    {
      assert debugException(CLASS_NAME, "entryToScheduledTask", e);

      int    msgID   = MSGID_TASKSCHED_CANNOT_INSTANTIATE_CLASS_AS_TASK;
      String message = getMessage(msgID, String.valueOf(taskClassName));
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, msgID);
    }


    // Perform the necessary internal and external initialization for the task.
    try
    {
      task.initializeTaskInternal(this, entry);
    }
    catch (InitializationException ie)
    {
      assert debugException(CLASS_NAME, "entryToScheduledTask", ie);

      int    msgID   = MSGID_TASKSCHED_CANNOT_INITIALIZE_INTERNAL;
      String message = getMessage(msgID, String.valueOf(taskClassName),
                                  ie.getMessage());
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, msgID);
    }
    catch (Exception e)
    {
      int    msgID   = MSGID_TASKSCHED_CANNOT_INITIALIZE_INTERNAL;
      String message = getMessage(msgID, String.valueOf(taskClassName),
                                  stackTraceToSingleLineString(e));
      throw new DirectoryException(DirectoryServer.getServerErrorResultCode(),
                                   message, msgID);
    }


    task.setOperation(operation);
    task.initializeTask();
    task.setOperation(null);
    return task;
  }



  /**
   * Decodes the contents of the provided entry as a recurring task.  The
   * resulting recurring task will not actually be added to the scheduler.
   *
   * @param  entry  The entry to decode as a recurring task.
   *
   * @return  The recurring task decoded from the provided entry.
   *
   * @throws  DirectoryException  If the provided entry cannot be decoded as a
   *                              recurring task.
   */
  public RecurringTask entryToRecurringTask(Entry entry)
         throws DirectoryException
  {
    assert debugEnter(CLASS_NAME, "entryToRecurringTask",
                      String.valueOf(entry));

    return new RecurringTask(this, entry);
  }



  /**
   * Retrieves the DN of the configuration entry with which this alert generator
   * is associated.
   *
   * @return  The DN of the configuration entry with which this alert generator
   *          is associated.
   */
  public DN getComponentEntryDN()
  {
    assert debugEnter(CLASS_NAME, "getComponentEntryDN");

    return taskBackend.getConfigEntryDN();
  }



  /**
   * Retrieves the fully-qualified name of the Java class for this alert
   * generator implementation.
   *
   * @return  The fully-qualified name of the Java class for this alert
   *          generator implementation.
   */
  public String getClassName()
  {
    assert debugEnter(CLASS_NAME, "getClassName");

    return CLASS_NAME;
  }



  /**
   * Retrieves information about the set of alerts that this generator may
   * produce.  The map returned should be between the notification type for a
   * particular notification and the human-readable description for that
   * notification.  This alert generator must not generate any alerts with types
   * that are not contained in this list.
   *
   * @return  Information about the set of alerts that this generator may
   *          produce.
   */
  public LinkedHashMap<String,String> getAlerts()
  {
    assert debugEnter(CLASS_NAME, "getAlerts");

    LinkedHashMap<String,String> alerts = new LinkedHashMap<String,String>();

    alerts.put(ALERT_TYPE_CANNOT_FIND_RECURRING_TASK,
               ALERT_DESCRIPTION_CANNOT_FIND_RECURRING_TASK);
    alerts.put(ALERT_TYPE_CANNOT_SCHEDULE_RECURRING_ITERATION,
               ALERT_DESCRIPTION_CANNOT_SCHEDULE_RECURRING_ITERATION);
    alerts.put(ALERT_TYPE_CANNOT_RENAME_CURRENT_TASK_FILE,
               ALERT_DESCRIPTION_CANNOT_RENAME_CURRENT_TASK_FILE);
    alerts.put(ALERT_TYPE_CANNOT_RENAME_NEW_TASK_FILE,
               ALERT_DESCRIPTION_CANNOT_RENAME_NEW_TASK_FILE);
    alerts.put(ALERT_TYPE_CANNOT_WRITE_TASK_FILE,
               ALERT_DESCRIPTION_CANNOT_WRITE_TASK_FILE);

    return alerts;
  }
}

