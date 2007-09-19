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
package org.opends.server.backends.task;
import org.opends.messages.Message;



import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.Collections;
import java.util.concurrent.locks.Lock;
import javax.mail.MessagingException;

import org.opends.server.core.DirectoryServer;
import org.opends.server.loggers.ErrorLogger;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.DebugLogLevel;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.Modification;
import org.opends.server.types.ModificationType;


import org.opends.server.types.InitializationException;
import org.opends.server.types.Operation;
import org.opends.server.util.EMailMessage;
import org.opends.server.util.TimeThread;
import org.opends.server.util.StaticUtils;

import static org.opends.server.config.ConfigConstants.*;
import static org.opends.server.loggers.debug.DebugLogger.*;
import static org.opends.messages.BackendMessages.*;

import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines a task that may be executed by the task backend within the
 * Directory Server.
 */
public abstract class Task
       implements Comparable<Task>
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();



  // The DN for the task entry.
  private DN taskEntryDN;

  // The entry that actually defines this task.
  private Entry taskEntry;

  // The action to take if one of the dependencies for this task does not
  // complete successfully.
  private FailedDependencyAction failedDependencyAction;

  // The counter used for log messages associated with this task.
  private int logMessageCounter;

  // The task IDs of other tasks on which this task is dependent.
  private LinkedList<String> dependencyIDs;

  // A set of log messages generated by this task.
  // TODO: convert from String to Message objects.
  // Since these are stored in an entry we would need
  // to adopt some way for writing message to string in such
  // a way that the information could be reparsed from its
  // string value.
  private LinkedList<String> logMessages;

  // The set of e-mail addresses of the users to notify when the task is done
  // running, regardless of whether it completes successfully.
  private LinkedList<String> notifyOnCompletion;

  // The set of e-mail addresses of the users to notify if the task does not
  // complete successfully for some reason.
  private LinkedList<String> notifyOnError;

  // The time that processing actually started for this task.
  private long actualStartTime;

  // The time that actual processing ended for this task.
  private long completionTime;

  // The time that this task was scheduled to start processing.
  private long scheduledStartTime;

  // The operation used to create this task in the server.
  private Operation operation;

  // The ID of the recurring task with which this task is associated.
  private String recurringTaskID;

  // The unique ID assigned to this task.
  private String taskID;

  // The task backend with which this task is associated.
  private TaskBackend taskBackend;

  // The current state of this task.
  private TaskState taskState;

  // The scheduler with which this task is associated.
  private TaskScheduler taskScheduler;

  /**
   * Gets a message that identifies this type of task suitable for
   * presentation to humans in monitoring tools.
   *
   * @return name of task
   */
  public Message getDisplayName() {
    // NOTE: this method is invoked via reflection.  If you rename
    // it be sure to modify the calls.
    return null;
  };

  /**
   * Given an attribute type name returns and locale sensitive
   * representation.
   *
   * @param name of an attribute type associated with the object
   *        class that represents this entry in the directory
   * @return Message diaplay name
   */
  public Message getAttributeDisplayName(String name) {
    // Subclasses that are schedulable from the task interface
    // should override this

    // NOTE: this method is invoked via reflection.  If you rename
    // it be sure to modify the calls.
    return null;
  }

  /**
   * Performs generic initialization for this task based on the information in
   * the provided task entry.
   *
   * @param  taskScheduler  The scheduler with which this task is associated.
   * @param  taskEntry      The entry containing the task configuration.
   *
   * @throws  InitializationException  If a problem occurs while performing the
   *                                   initialization.
   */
  public final void initializeTaskInternal(TaskScheduler taskScheduler,
                                           Entry taskEntry)
         throws InitializationException
  {
    this.taskScheduler = taskScheduler;
    this.taskEntry     = taskEntry;
    this.taskEntryDN   = taskEntry.getDN();

    String taskDN = taskEntryDN.toString();

    taskBackend       = taskScheduler.getTaskBackend();


    // Get the task ID and recurring task ID values.  At least one of them must
    // be provided.  If it's a recurring task and there is no task ID, then
    // generate one on the fly.
    taskID          = getAttributeValue(ATTR_TASK_ID, false);
    recurringTaskID = getAttributeValue(ATTR_RECURRING_TASK_ID, false);
    if (taskID == null)
    {
      if (recurringTaskID == null)
      {
        Message message = ERR_TASK_MISSING_ATTR.get(
            String.valueOf(taskEntry.getDN()), ATTR_TASK_ID);
        throw new InitializationException(message);
      }
      else
      {
        taskID = UUID.randomUUID().toString();
      }
    }


    // Get the current state from the task.  If there is none, then assume it's
    // a new task.
    String stateString = getAttributeValue(ATTR_TASK_STATE, false);
    if (stateString == null)
    {
      taskState = TaskState.UNSCHEDULED;
    }
    else
    {
      taskState = TaskState.fromString(stateString);
      if (taskState == null)
      {
        Message message = ERR_TASK_INVALID_STATE.get(taskDN, stateString);
        throw new InitializationException(message);
      }
    }


    // Get the scheduled start time for the task, if there is one.  It may be
    // in either UTC time (a date followed by a 'Z') or in the local time zone
    // (not followed by a 'Z').
    scheduledStartTime = -1;
    String timeString = getAttributeValue(ATTR_TASK_SCHEDULED_START_TIME,
                                          false);
    if (timeString != null)
    {
      SimpleDateFormat dateFormat;
      if (timeString.endsWith("Z"))
      {
        dateFormat = new SimpleDateFormat(DATE_FORMAT_GMT_TIME);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      }
      else
      {
        dateFormat = new SimpleDateFormat(DATE_FORMAT_COMPACT_LOCAL_TIME);
      }

      try
      {
        scheduledStartTime = dateFormat.parse(timeString).getTime();
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }

        Message message =
            ERR_TASK_CANNOT_PARSE_SCHEDULED_START_TIME.get(timeString, taskDN);
        throw new InitializationException(message, e);
      }
    }


    // Get the actual start time for the task, if there is one.
    actualStartTime = -1;
    timeString = getAttributeValue(ATTR_TASK_ACTUAL_START_TIME, false);
    if (timeString != null)
    {
      SimpleDateFormat dateFormat;
      if (timeString.endsWith("Z"))
      {
        dateFormat = new SimpleDateFormat(DATE_FORMAT_GMT_TIME);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      }
      else
      {
        dateFormat = new SimpleDateFormat(DATE_FORMAT_COMPACT_LOCAL_TIME);
      }

      try
      {
        actualStartTime = dateFormat.parse(timeString).getTime();
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }

        Message message =
            ERR_TASK_CANNOT_PARSE_ACTUAL_START_TIME.get(timeString, taskDN);
        throw new InitializationException(message, e);
      }
    }


    // Get the completion time for the task, if there is one.
    completionTime = -1;
    timeString = getAttributeValue(ATTR_TASK_COMPLETION_TIME, false);
    if (timeString != null)
    {
      SimpleDateFormat dateFormat;
      if (timeString.endsWith("Z"))
      {
        dateFormat = new SimpleDateFormat(DATE_FORMAT_GMT_TIME);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      }
      else
      {
        dateFormat = new SimpleDateFormat(DATE_FORMAT_COMPACT_LOCAL_TIME);
      }

      try
      {
        completionTime = dateFormat.parse(timeString).getTime();
      }
      catch (Exception e)
      {
        if (debugEnabled())
        {
          TRACER.debugCaught(DebugLogLevel.ERROR, e);
        }

        Message message =
            ERR_TASK_CANNOT_PARSE_COMPLETION_TIME.get(timeString, taskDN);
        throw new InitializationException(message, e);
      }
    }


    // Get information about any dependencies that the task might have.
    dependencyIDs = getAttributeValues(ATTR_TASK_DEPENDENCY_IDS);

    failedDependencyAction = FailedDependencyAction.CANCEL;
    String actionString = getAttributeValue(ATTR_TASK_FAILED_DEPENDENCY_ACTION,
                                            false);
    if (actionString != null)
    {
      failedDependencyAction = FailedDependencyAction.fromString(actionString);
      if (failedDependencyAction == null)
      {
        failedDependencyAction = FailedDependencyAction.CANCEL;
      }
    }


    // Get the information about the e-mail addresses to use for notification
    // purposes.
    notifyOnCompletion = getAttributeValues(ATTR_TASK_NOTIFY_ON_COMPLETION);
    notifyOnError      = getAttributeValues(ATTR_TASK_NOTIFY_ON_ERROR);


    // Get the log messages for the task.
    logMessages  = getAttributeValues(ATTR_TASK_LOG_MESSAGES);
    if (logMessages != null) {
      logMessageCounter = logMessages.size();
    }
  }



  /**
   * Retrieves the single value for the requested attribute as a string.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        value.
   * @param  isRequired     Indicates whether the attribute is required to have
   *                        a value.
   *
   * @return  The value for the requested attribute, or <CODE>null</CODE> if it
   *          is not present in the entry and is not required.
   *
   * @throws  InitializationException  If the requested attribute is not present
   *                                   in the entry but is required, or if there
   *                                   are multiple instances of the requested
   *                                   attribute in the entry with different
   *                                   sets of options, or if there are multiple
   *                                   values for the requested attribute.
   */
  private String getAttributeValue(String attributeName, boolean isRequired)
          throws InitializationException
  {
    List<Attribute> attrList =
         taskEntry.getAttribute(attributeName.toLowerCase());
    if ((attrList == null) || attrList.isEmpty())
    {
      if (isRequired)
      {
        Message message = ERR_TASK_MISSING_ATTR.get(
            String.valueOf(taskEntry.getDN()), attributeName);
        throw new InitializationException(message);
      }
      else
      {
        return null;
      }
    }

    if (attrList.size() > 1)
    {
      Message message = ERR_TASK_MULTIPLE_ATTRS_FOR_TYPE.get(
          attributeName, String.valueOf(taskEntry.getDN()));
      throw new InitializationException(message);
    }

    Iterator<AttributeValue> iterator = attrList.get(0).getValues().iterator();
    if (! iterator.hasNext())
    {
      if (isRequired)
      {
        Message message = ERR_TASK_NO_VALUES_FOR_ATTR.get(
            attributeName, String.valueOf(taskEntry.getDN()));
        throw new InitializationException(message);
      }
      else
      {
        return null;
      }
    }

    AttributeValue value = iterator.next();
    if (iterator.hasNext())
    {
      Message message = ERR_TASK_MULTIPLE_VALUES_FOR_ATTR.get(
          attributeName, String.valueOf(taskEntry.getDN()));
      throw new InitializationException(message);
    }

    return value.getStringValue();
  }



  /**
   * Retrieves the values for the requested attribute as a list of strings.
   *
   * @param  attributeName  The name of the attribute for which to retrieve the
   *                        values.
   *
   * @return  The list of values for the requested attribute, or an empty list
   *          if the attribute does not exist or does not have any values.
   *
   * @throws  InitializationException  If there are multiple instances of the
   *                                   requested attribute in the entry with
   *                                   different sets of options.
   */
  private LinkedList<String> getAttributeValues(String attributeName)
          throws InitializationException
  {
    LinkedList<String> valueStrings = new LinkedList<String>();

    List<Attribute> attrList =
         taskEntry.getAttribute(attributeName.toLowerCase());
    if ((attrList == null) || attrList.isEmpty())
    {
      return valueStrings;
    }

    if (attrList.size() > 1)
    {
      Message message = ERR_TASK_MULTIPLE_ATTRS_FOR_TYPE.get(
          attributeName, String.valueOf(taskEntry.getDN()));
      throw new InitializationException(message);
    }

    Iterator<AttributeValue> iterator = attrList.get(0).getValues().iterator();
    while (iterator.hasNext())
    {
      valueStrings.add(iterator.next().getStringValue());
    }

    return valueStrings;
  }



  /**
   * Retrieves the DN of the entry containing the definition for this task.
   *
   * @return  The DN of the entry containing the definition for this task.
   */
  public final DN getTaskEntryDN()
  {
    return taskEntryDN;
  }



  /**
   * Retrieves the entry containing the definition for this task.
   *
   * @return  The entry containing the definition for this task.
   */
  public final Entry getTaskEntry()
  {
    return taskEntry;
  }



  /**
   * Retrieves the operation used to create this task in the server.  Note that
   * this will only be available when the task is first added to the scheduler,
   * and it should only be accessed from within the {@code initializeTask}
   * method (and even that method should not depend on it always being
   * available, since it will not be available if the server is restarted and
   * the task needs to be reinitialized).
   *
   * @return  The operation used to create this task in the server, or
   *          {@code null} if it is not available.
   */
  public final Operation getOperation()
  {
    return operation;
  }



  /**
   * Specifies the operation used to create this task in the server.
   *
   * @param  operation  The operation used to create this task in the server.
   */
  public final void setOperation(Operation operation)
  {
    this.operation = operation;
  }



  /**
   * Retrieves the unique identifier assigned to this task.
   *
   * @return  The unique identifier assigned to this task.
   */
  public final String getTaskID()
  {
    return taskID;
  }



  /**
   * Retrieves the unique identifier assigned to the recurring task that is
   * associated with this task, if there is one.
   *
   * @return  The unique identifier assigned to the recurring task that is
   *          associated with this task, or <CODE>null</CODE> if it is not
   *          associated with any recurring task.
   */
  public final String getRecurringTaskID()
  {
    return recurringTaskID;
  }



  /**
   * Retrieves the current state for this task.
   *
   * @return  The current state for this task.
   */
  public final TaskState getTaskState()
  {
    return taskState;
  }



  /**
   * Sets the state for this task and updates the associated task entry as
   * necessary.  It does not automatically persist the updated task information
   * to disk.
   *
   * @param  taskState  The new state to use for the task.
   */
  void setTaskState(TaskState taskState)
  {
    // We only need to grab the entry-level lock if we don't already hold the
    // broader scheduler lock.
    boolean needLock = (! taskScheduler.holdsSchedulerLock());
    Lock lock = null;
    if (needLock)
    {
      lock = taskScheduler.writeLockEntry(taskEntryDN);
    }

    try
    {
      this.taskState = taskState;

      AttributeType type =
           DirectoryServer.getAttributeType(ATTR_TASK_STATE.toLowerCase());
      if (type == null)
      {
        type = DirectoryServer.getDefaultAttributeType(ATTR_TASK_STATE);
      }

      LinkedHashSet<AttributeValue> values =
           new LinkedHashSet<AttributeValue>();
      values.add(new AttributeValue(type,
                                    new ASN1OctetString(taskState.toString())));

      ArrayList<Attribute> attrList = new ArrayList<Attribute>(1);
      attrList.add(new Attribute(type, ATTR_TASK_STATE, values));
      taskEntry.putAttribute(type, attrList);
    }
    finally
    {
      if (needLock)
      {
        taskScheduler.unlockEntry(taskEntryDN, lock);
      }
    }
  }

  /**
   * Replaces an attribute values of the task entry.
   *
   * @param  name  The name of the attribute that must be replaced.
   *
   * @param  value The value that must replace the previous values of the
   *               attribute.
   *
   * @throws DirectoryException When an error occurs.
   */
  protected void replaceAttributeValue(String name, String value)
  throws DirectoryException
  {
    // We only need to grab the entry-level lock if we don't already hold the
    // broader scheduler lock.
    boolean needLock = (! taskScheduler.holdsSchedulerLock());
    Lock lock = null;
    if (needLock)
    {
      lock = taskScheduler.writeLockEntry(taskEntryDN);
    }

    try
    {
      Entry taskEntry = getTaskEntry();

      ArrayList<Modification> modifications = new ArrayList<Modification>();
      modifications.add(new Modification(ModificationType.REPLACE,
          new Attribute(name, value)));

      taskEntry.applyModifications(modifications);
    }
    finally
    {
      if (needLock)
      {
        taskScheduler.unlockEntry(taskEntryDN, lock);
      }
    }
  }


  /**
   * Retrieves the scheduled start time for this task, if there is one.  The
   * value returned will be in the same format as the return value for
   * <CODE>System.currentTimeMillis()</CODE>.  Any value representing a time in
   * the past, or any negative value, should be taken to mean that the task
   * should be considered eligible for immediate execution.
   *
   * @return  The scheduled start time for this task.
   */
  public final long getScheduledStartTime()
  {
    return scheduledStartTime;
  }



  /**
   * Retrieves the time that this task actually started running, if it has
   * started.  The value returned will be in the same format as the return value
   * for <CODE>System.currentTimeMillis()</CODE>.
   *
   * @return  The time that this task actually started running, or -1 if it has
   *          not yet been started.
   */
  public final long getActualStartTime()
  {
    return actualStartTime;
  }



  /**
   * Sets the actual start time for this task and updates the associated task
   * entry as necessary.  It does not automatically persist the updated task
   * information to disk.
   *
   * @param  actualStartTime  The actual start time to use for this task.
   */
  private void setActualStartTime(long actualStartTime)
  {
    // We only need to grab the entry-level lock if we don't already hold the
    // broader scheduler lock.
    boolean needLock = (! taskScheduler.holdsSchedulerLock());
    Lock lock = null;
    if (needLock)
    {
      lock = taskScheduler.writeLockEntry(taskEntryDN);
    }

    try
    {
      this.actualStartTime = actualStartTime;

      AttributeType type = DirectoryServer.getAttributeType(
                                ATTR_TASK_ACTUAL_START_TIME.toLowerCase());
      if (type == null)
      {
        type = DirectoryServer.getDefaultAttributeType(
                    ATTR_TASK_ACTUAL_START_TIME);
      }

      Date d = new Date(actualStartTime);
      String startTimeStr = StaticUtils.formatDateTimeString(d);
      ASN1OctetString s = new ASN1OctetString(startTimeStr);

      LinkedHashSet<AttributeValue> values =
           new LinkedHashSet<AttributeValue>();
      values.add(new AttributeValue(type, s));

      ArrayList<Attribute> attrList = new ArrayList<Attribute>(1);
      attrList.add(new Attribute(type, ATTR_TASK_ACTUAL_START_TIME, values));
      taskEntry.putAttribute(type, attrList);
    }
    finally
    {
      if (needLock)
      {
        taskScheduler.unlockEntry(taskEntryDN, lock);
      }
    }
  }



  /**
   * Retrieves the time that this task completed all of its associated
   * processing (regardless of whether it was successful), if it has completed.
   * The value returned will be in the same format as the return value for
   * <CODE>System.currentTimeMillis()</CODE>.
   *
   * @return  The time that this task actually completed running, or -1 if it
   *          has not yet completed.
   */
  public final long getCompletionTime()
  {
    return completionTime;
  }



  /**
   * Sets the completion time for this task and updates the associated task
   * entry as necessary.  It does not automatically persist the updated task
   * information to disk.
   *
   * @param  completionTime  The completion time to use for this task.
   */
  private void setCompletionTime(long completionTime)
  {
    // We only need to grab the entry-level lock if we don't already hold the
    // broader scheduler lock.
    boolean needLock = (! taskScheduler.holdsSchedulerLock());
    Lock lock = null;
    if (needLock)
    {
      lock = taskScheduler.writeLockEntry(taskEntryDN);
    }

    try
    {
      this.completionTime = completionTime;

      AttributeType type = DirectoryServer.getAttributeType(
                                ATTR_TASK_COMPLETION_TIME.toLowerCase());
      if (type == null)
      {
        type =
             DirectoryServer.getDefaultAttributeType(ATTR_TASK_COMPLETION_TIME);
      }

      SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_GMT_TIME);
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date d = new Date(completionTime);
      ASN1OctetString s = new ASN1OctetString(dateFormat.format(d));

      LinkedHashSet<AttributeValue> values =
           new LinkedHashSet<AttributeValue>();
      values.add(new AttributeValue(type, s));

      ArrayList<Attribute> attrList = new ArrayList<Attribute>(1);
      attrList.add(new Attribute(type, ATTR_TASK_COMPLETION_TIME, values));
      taskEntry.putAttribute(type, attrList);
    }
    finally
    {
      if (needLock)
      {
        taskScheduler.unlockEntry(taskEntryDN, lock);
      }
    }
  }



  /**
   * Retrieves the set of task IDs for any tasks on which this task is
   * dependent.  This list must not be directly modified by the caller.
   *
   * @return  The set of task IDs for any tasks on which this task is dependent.
   */
  public final LinkedList<String> getDependencyIDs()
  {
    return dependencyIDs;
  }



  /**
   * Retrieves the action that should be taken if any of the dependencies for
   * this task do not complete successfully.
   *
   * @return  The action that should be taken if any of the dependencies for
   *          this task do not complete successfully.
   */
  public final FailedDependencyAction getFailedDependencyAction()
  {
    return failedDependencyAction;
  }



  /**
   * Retrieves the set of e-mail addresses for the users that should receive a
   * notification message when processing for this task has completed.  This
   * notification will be sent to these users regardless of whether the task
   * completed successfully.  This list must not be directly modified by the
   * caller.
   *
   * @return  The set of e-mail addresses for the users that should receive a
   *          notification message when processing for this task has
   *          completed.
   */
  public final LinkedList<String> getNotifyOnCompletionAddresses()
  {
    return notifyOnCompletion;
  }



  /**
   * Retrieves the set of e-mail addresses for the users that should receive a
   * notification message if processing for this task does not complete
   * successfully.  This list must not be directly modified by the caller.
   *
   * @return  The set of e-mail addresses for the users that should receive a
   *          notification message if processing for this task does not complete
   *          successfully.
   */
  public final LinkedList<String> getNotifyOnErrorAddresses()
  {
    return notifyOnError;
  }



  /**
   * Retrieves the set of messages that were logged by this task.  This list
   * must not be directly modified by the caller.
   *
   * @return  The set of messages that were logged by this task.
   */
  public final List<Message> getLogMessages()
  {
    List<Message> msgList = new ArrayList<Message>();
    for(String logString : logMessages) {
      // TODO: a better job or recreating the message
      msgList.add(Message.raw(logString));
    }
    return Collections.unmodifiableList(msgList);
  }



  /**
   * Writes a message to the error log using the provided information.
   * Tasks should use this method to log messages to the error log instead of
   * the one in <code>org.opends.server.loggers.Error</code> to ensure the
   * messages are included in the ds-task-log-message attribute.
   *
   * @param  message   The message to be logged.
   */
  protected void logError(Message message)
  {
    // Simply pass this on to the server error logger, and it will call back
    // to the addLogMessage method for this task.
    ErrorLogger.logError(message);
  }



  /**
   * Adds a log message to the set of messages logged by this task.  This method
   * should not be called directly by tasks, but rather will be called
   * indirectly through the {@code ErrorLog.logError} methods. It does not
   * automatically persist the updated task information to disk.
   *
   * @param  message  he log message
   */
  public void addLogMessage(Message message)
  {
    // We only need to grab the entry-level lock if we don't already hold the
    // broader scheduler lock.
    boolean needLock = (! taskScheduler.holdsSchedulerLock());
    Lock lock = null;
    if (needLock)
    {
      lock = taskScheduler.writeLockEntry(taskEntryDN);
    }

    try
    {
      StringBuilder buffer = new StringBuilder();
      buffer.append("[");
      buffer.append(TimeThread.getLocalTime());
      buffer.append("] severity=\"");
      buffer.append(message.getDescriptor().getSeverity().name());
      buffer.append("\" msgCount=");
      buffer.append(logMessageCounter++);
      buffer.append(" msgID=");
      buffer.append(message.getDescriptor().getId());
      buffer.append(" message=\"");
      buffer.append(message.toString());
      buffer.append("\"");

      String messageString = buffer.toString();
      logMessages.add(messageString);


      AttributeType type = DirectoryServer.getAttributeType(
                                ATTR_TASK_LOG_MESSAGES.toLowerCase());
      if (type == null)
      {
        type = DirectoryServer.getDefaultAttributeType(ATTR_TASK_LOG_MESSAGES);
      }

      List<Attribute> attrList = taskEntry.getAttribute(type);
      LinkedHashSet<AttributeValue> values;
      if (attrList == null)
      {
        attrList = new ArrayList<Attribute>();
        values = new LinkedHashSet<AttributeValue>();
        attrList.add(new Attribute(type, ATTR_TASK_LOG_MESSAGES, values));
        taskEntry.putAttribute(type, attrList);
      }
      else if (attrList.isEmpty())
      {
        values = new LinkedHashSet<AttributeValue>();
        attrList.add(new Attribute(type, ATTR_TASK_LOG_MESSAGES, values));
      }
      else
      {
        Attribute attr = attrList.get(0);
        values = attr.getValues();
      }
      values.add(new AttributeValue(type, new ASN1OctetString(messageString)));
    }
    finally
    {
      if (needLock)
      {
        taskScheduler.unlockEntry(taskEntryDN, lock);
      }
    }
  }



  /**
   * Compares this task with the provided task for the purposes of ordering in a
   * sorted list.  Any completed task will always be ordered before an
   * uncompleted task.  If both tasks are completed, then they will be ordered
   * by completion time.  If both tasks are uncompleted, then a running task
   * will always be ordered before one that has not started.  If both are
   * running, then they will be ordered by actual start time.  If neither have
   * started, then they will be ordered by scheduled start time.  If all else
   * fails, they will be ordered lexicographically by task ID.
   *
   * @param  task  The task to compare with this task.
   *
   * @return  A negative value if the provided task should come before this
   *          task, a positive value if the provided task should come after this
   *          task, or zero if there is no difference with regard to their
   *          order.
   */
  public final int compareTo(Task task)
  {
    if (completionTime > 0)
    {
      if (task.completionTime > 0)
      {
        // They have both completed, so order by completion time.
        if (completionTime < task.completionTime)
        {
          return -1;
        }
        else if (completionTime > task.completionTime)
        {
          return 1;
        }
        else
        {
          // They have the same completion time, so order by task ID.
          return taskID.compareTo(task.taskID);
        }
      }
      else
      {
        // Completed tasks are always ordered before those that haven't
        // completed.
        return -1;
      }
    }
    else if (task.completionTime > 0)
    {
      // Completed tasks are always ordered before those that haven't completed.
      return 1;
    }

    if (actualStartTime > 0)
    {
      if (task.actualStartTime > 0)
      {
        // They are both running, so order by actual start time.
        if (actualStartTime < task.actualStartTime)
        {
          return -1;
        }
        else if (actualStartTime > task.actualStartTime)
        {
          return 1;
        }
        else
        {
          // They have the same actual start time, so order by task ID.
          return taskID.compareTo(task.taskID);
        }
      }
      else
      {
        // Running tasks are always ordered before those that haven't started.
        return -1;
      }
    }
    else if (task.actualStartTime > 0)
    {
      // Running tasks are always ordered before those that haven't started.
      return 1;
    }


    // Neither task has started, so order by scheduled start time, or if nothing
    // else by task ID.
    if (scheduledStartTime < task.scheduledStartTime)
    {
      return -1;
    }
    else if (scheduledStartTime > task.scheduledStartTime)
    {
      return 1;
    }
    else
    {
      return taskID.compareTo(task.taskID);
    }
  }



  /**
   * Begins execution for this task.  This is a wrapper around the
   * <CODE>runTask</CODE> method that performs the appropriate set-up and
   * tear-down.   It should only be invoked by a task thread.
   *
   * @return  The final state to use for the task.
   */
  public final TaskState execute()
  {
    setActualStartTime(TimeThread.getTime());
    setTaskState(TaskState.RUNNING);
    taskScheduler.writeState();

    try
    {
      TaskState taskState = runTask();
      setTaskState(taskState);
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      setTaskState(TaskState.STOPPED_BY_ERROR);

      Message message = ERR_TASK_EXECUTE_FAILED.get(
          String.valueOf(taskEntry.getDN()), stackTraceToSingleLineString(e));
      logError(message);
    }
    finally
    {
      setCompletionTime(TimeThread.getTime());
      taskScheduler.writeState();
    }

    try
    {
      sendNotificationEMailMessage();
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }
    }

    return taskState;
  }



  /**
   * If appropriate, send an e-mail message with information about the
   * completed task.
   *
   * @throws  MessagingException  If a problem occurs while attempting to send
   *                              the message.
   */
  private void sendNotificationEMailMessage()
          throws MessagingException
  {
    if (DirectoryServer.mailServerConfigured())
    {
      LinkedHashSet<String> recipients = new LinkedHashSet<String>();
      recipients.addAll(notifyOnCompletion);
      if (! TaskState.isSuccessful(taskState))
      {
        recipients.addAll(notifyOnError);
      }

      if (! recipients.isEmpty())
      {
        EMailMessage message =
             new EMailMessage(taskBackend.getNotificationSenderAddress(),
                              new ArrayList<String>(recipients),
                              taskState.toString() + " " + taskID);

        String scheduledStartDate;
        if (scheduledStartTime <= 0)
        {
          scheduledStartDate = "";
        }
        else
        {
          scheduledStartDate = new Date(scheduledStartTime).toString();
        }

        String actualStartDate = new Date(actualStartTime).toString();
        String completionDate  = new Date(completionTime).toString();

        message.setBody(INFO_TASK_COMPLETION_BODY.get(taskID,
                                   String.valueOf(taskState),
                                   scheduledStartDate, actualStartDate,
                                   completionDate));

        for (String logMessage : logMessages)
        {
          message.appendToBody(logMessage);
          message.appendToBody("\r\n");
        }

        message.send();
      }
    }
  }



  /**
   * Performs any task-specific initialization that may be required before
   * processing can start.  This default implementation does not do anything,
   * but subclasses may override it as necessary.  This method will be called at
   * the time the task is scheduled, and therefore any failure in this method
   * will be returned to the client.
   *
   * @throws  DirectoryException  If a problem occurs during initialization that
   *                              should be returned to the client.
   */
  public void initializeTask()
         throws DirectoryException
  {
    // No action is performed by default.
  }



  /**
   * Performs the actual core processing for this task.  This method should not
   * return until all processing associated with this task has completed.
   *
   * @return  The final state to use for the task.
   */
  protected abstract TaskState runTask();



  /**
   * Performs any necessary processing to prematurely interrupt the execution of
   * this task.  By default no action is performed, but if it is feasible to
   * gracefully interrupt a task, then subclasses should override this method to
   * do so.
   *
   * @param  interruptState   The state to use for the task if it is
   *                          successfully interrupted.
   * @param  interruptReason  A human-readable explanation for the cancellation.
   */
  public void interruptTask(TaskState interruptState, Message interruptReason)
  {
    // No action is performed by default.

    // NOTE:  if you implement this make sure to override isInterruptable
    //        to return 'true'
  }



  /**
   * Indicates whether or not this task is interruptable or not.
   *
   * @return boolean where true indicates that this task can be interrupted.
   */
  public boolean isInterruptable() {
    return false;
  }

}

