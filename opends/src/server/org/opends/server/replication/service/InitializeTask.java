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
package org.opends.server.replication.service;
import org.opends.server.tasks.TaskUtils;

import org.opends.server.types.ResultCode;

import org.opends.messages.MessageBuilder;


import org.opends.messages.Message;

import static org.opends.server.config.ConfigConstants.*;
import static org.opends.server.core.DirectoryServer.getAttributeType;
import static org.opends.server.loggers.debug.DebugLogger.debugEnabled;
import static org.opends.server.loggers.debug.DebugLogger.getTracer;

import java.util.List;

import org.opends.messages.TaskMessages;
import org.opends.server.backends.task.Task;
import org.opends.server.backends.task.TaskState;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;

/**
 * This class provides an implementation of a Directory Server task that can
 * be used to import data over the replication protocol from another
 * server hosting the same replication domain.
 */
public class InitializeTask extends Task
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();

  private String  domainString            = null;
  private short  source;
  private ReplicationDomain domain        = null;
  private TaskState initState;

  // The total number of entries expected to be processed when this import
  // will end successfully
  long total = 0;

  // The number of entries still to be processed for this import to be
  // completed
  long left = 0;

  private Message initTaskError = null;

  /**
   * {@inheritDoc}
   */
  public Message getDisplayName() {
    return TaskMessages.INFO_TASK_INITIALIZE_NAME.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override public void initializeTask() throws DirectoryException
  {
    if (TaskState.isDone(getTaskState()))
    {
      return;
    }

    // FIXME -- Do we need any special authorization here?
    Entry taskEntry = getTaskEntry();

    AttributeType typeDomainBase;
    AttributeType typeSourceScope;

    typeDomainBase =
      getAttributeType(ATTR_TASK_INITIALIZE_DOMAIN_DN, true);
    typeSourceScope =
      getAttributeType(ATTR_TASK_INITIALIZE_SOURCE, true);

    List<Attribute> attrList;
    attrList = taskEntry.getAttribute(typeDomainBase);
    domainString = TaskUtils.getSingleValueString(attrList);

    try
    {
      domain = ReplicationDomain.retrievesReplicationDomain(domainString);
    }
    catch(DirectoryException e)
    {
      MessageBuilder mb = new MessageBuilder();
      mb.append(TaskMessages.ERR_TASK_INITIALIZE_INVALID_DN.get());
      mb.append(e.getMessage());
      throw new DirectoryException(ResultCode.INVALID_DN_SYNTAX, e);
    }

    attrList = taskEntry.getAttribute(typeSourceScope);
    String sourceString = TaskUtils.getSingleValueString(attrList);
    source = domain.decodeSource(sourceString);

    replaceAttributeValue(ATTR_TASK_INITIALIZE_LEFT, String.valueOf(0));
    replaceAttributeValue(ATTR_TASK_INITIALIZE_DONE, String.valueOf(0));
  }

  /**
   * {@inheritDoc}
   */
  protected TaskState runTask()
  {
    if (debugEnabled())
    {
      TRACER.debugInfo("InitializeTask is starting domain: %s source:%d",
                domain.getServiceID(), source);
    }
    initState = getTaskState();
    try
    {
      // launch the import
      domain.initializeFromRemote(source, this);

      synchronized(initState)
      {
        // Waiting for the end of the job
        while (initState == TaskState.RUNNING)
        {
          initState.wait(1000);
          replaceAttributeValue(
              ATTR_TASK_INITIALIZE_LEFT, String.valueOf(left));
          replaceAttributeValue(
              ATTR_TASK_INITIALIZE_DONE, String.valueOf(total-left));
        }
      }
      replaceAttributeValue(ATTR_TASK_INITIALIZE_LEFT, String.valueOf(left));
      replaceAttributeValue(
          ATTR_TASK_INITIALIZE_DONE, String.valueOf(total-left));
    }
    catch(InterruptedException ie) {}
    catch(DirectoryException de)
    {
      logError(de.getMessageObject());
      initState = TaskState.STOPPED_BY_ERROR;
    }

    if (initTaskError != null)
      logError(initTaskError);

    if (debugEnabled())
    {
      TRACER.debugInfo("InitializeTask is ending with state:%s",
          initState.toString());
    }
    return initState;
  }

  /**
   * Set the state for the current task.
   *
   * @param de  When the new state is different from COMPLETED_SUCCESSFULLY
   * this is the exception that contains the cause of the failure.
   */
  public void updateTaskCompletionState(DirectoryException de)
  {
    try
    {
      if (de != null)
      {
        initTaskError = de.getMessageObject();
      }
      if (de == null)
        initState =  TaskState.COMPLETED_SUCCESSFULLY;
      else
        initState =  TaskState.STOPPED_BY_ERROR;

      if (debugEnabled())
      {
        TRACER.debugInfo("InitializeTask/setState: %s", initState);
      }
      synchronized (initState)
      {
        initState.notify();
      }
    }
    catch(Exception e)
    {}
  }


  /**
   * Set the total number of entries expected to be imported.
   * @param total The total number of entries.
   */
  public void setTotal(long total)
  {
    this.total = total;
  }

  /**
   * Set the total number of entries still to be imported.
   * @param left The total number of entries to be imported.
   */
  public void setLeft(long left)
  {
    this.left = left;
  }
}
