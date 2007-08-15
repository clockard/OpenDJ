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
package org.opends.server.replication.server;
import org.opends.messages.MessageBuilder;

import static org.opends.server.loggers.ErrorLogger.logError;
import static org.opends.messages.ReplicationMessages.*;
import static org.opends.server.util.StaticUtils.stackTraceToSingleLineString;

import java.util.List;
import java.io.UnsupportedEncodingException;

import org.opends.server.types.DN;
import org.opends.server.replication.common.ChangeNumber;
import org.opends.server.replication.protocol.UpdateMessage;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Database;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.Transaction;

/**
 * This class implements the interface between the underlying database
 * and the dbHandler class.
 * This is the only class that should have code using the BDB interfaces.
 */
public class ReplicationDB
{
  private Database db = null;
  private ReplicationDbEnv dbenv = null;
  private ReplicationServer replicationServer;
  private Short serverId;
  private DN baseDn;

  /**
   * Creates a new database or open existing database that will be used
   * to store and retrieve changes from an LDAP server.
   * @param serverId Identifier of the LDAP server.
   * @param baseDn baseDn of the LDAP server.
   * @param replicationServer the ReplicationServer that needs to be shutdown
   * @param dbenv the Db encironemnet to use to create the db
   * @throws DatabaseException if a database problem happened
   */
  public ReplicationDB(Short serverId, DN baseDn,
                     ReplicationServer replicationServer,
                     ReplicationDbEnv dbenv)
                     throws DatabaseException
  {
    this.serverId = serverId;
    this.baseDn = baseDn;
    this.dbenv = dbenv;
    this.replicationServer = replicationServer;
    db = dbenv.getOrAddDb(serverId, baseDn);

  }

  /**
   * add a list of changes to the underlying db.
   *
   * @param changes The list of changes to add to the underlying db.
   */
  public void addEntries(List<UpdateMessage> changes)
  {
    Transaction txn = null;

    try
    {
      txn = dbenv.beginTransaction();

      for (UpdateMessage change : changes)
      {
        DatabaseEntry key = new ReplicationKey(change.getChangeNumber());
        DatabaseEntry data = new ReplicationData(change);

        try
        {
          db.put(txn, key, data);
        } catch (DatabaseException e)
        {
          MessageBuilder mb = new MessageBuilder();
          mb.append(ERR_CHANGELOG_SHUTDOWN_DATABASE_ERROR.get());
          mb.append(stackTraceToSingleLineString(e));
          logError(mb.toMessage());
          replicationServer.shutdown();
        }
      }

      txn.commitWriteNoSync();
      txn = null;
    }
    catch (DatabaseException e)
    {
      MessageBuilder mb = new MessageBuilder();
      mb.append(ERR_CHANGELOG_SHUTDOWN_DATABASE_ERROR.get());
      mb.append(stackTraceToSingleLineString(e));
      logError(mb.toMessage());
      if (txn != null)
      {
        try
        {
          txn.abort();
        } catch (DatabaseException e1)
        {
          // can't do much more. The ReplicationServer is shuting down.
        }
      }
      replicationServer.shutdown();
    }
    catch (UnsupportedEncodingException e)
    {
      MessageBuilder mb = new MessageBuilder();
      mb.append(ERR_CHANGELOG_UNSUPPORTED_UTF8_ENCODING.get());
      mb.append(stackTraceToSingleLineString(e));
      logError(mb.toMessage());
      replicationServer.shutdown();
      if (txn != null)
      {
        try
        {
          txn.abort();
        } catch (DatabaseException e1)
        {
          // can't do much more. The ReplicationServer is shuting down.
        }
      }
      replicationServer.shutdown();
    }
  }


  /**
   * Shutdown the database.
   */
  public void shutdown()
  {
    try
    {
      db.close();
    } catch (DatabaseException e)
    {
      MessageBuilder mb = new MessageBuilder();
      mb.append(NOTE_EXCEPTION_CLOSING_DATABASE.get(this.toString()));
      mb.append(stackTraceToSingleLineString(e));
      logError(mb.toMessage());
    }
  }

  /**
   * Create a cursor that can be used to search or iterate on this
   * ReplicationServer DB.
   *
   * @param changeNumber The ChangeNumber from which the cursor must start.
   * @throws DatabaseException If a database error prevented the cursor
   *                           creation.
   * @throws Exception if the ReplServerDBCursor creation failed.
   * @return The ReplServerDBCursor.
   */
  public ReplServerDBCursor openReadCursor(ChangeNumber changeNumber)
                throws DatabaseException, Exception
  {
    return new ReplServerDBCursor(changeNumber);
  }

  /**
   * Create a cursor that can be used to delete some record from this
   * ReplicationServer database.
   *
   * @throws DatabaseException If a database error prevented the cursor
   *                           creation.
   * @throws Exception if the ReplServerDBCursor creation failed.
   * @return The ReplServerDBCursor.
   */
  public ReplServerDBCursor openDeleteCursor()
                throws DatabaseException, Exception
  {
    return new ReplServerDBCursor();
  }

  /**
   * Read the first Change from the database.
   * @return the first ChangeNumber.
   */
  public ChangeNumber readFirstChange()
  {
    Cursor cursor;
    String str = null;

    try
    {
        cursor = db.openCursor(null, null);
    } catch (DatabaseException e1)
    {
        return null;
    }
    try
    {
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry data = new DatabaseEntry();
      OperationStatus status = cursor.getFirst(key, data, LockMode.DEFAULT);
      cursor.close();
      if (status != OperationStatus.SUCCESS)
      {
        /* database is empty */
        return null;
      }
      try
      {
       str = new String(key.getData(), "UTF-8");
      } catch (UnsupportedEncodingException e)
      {
        // never happens
      }
      return new ChangeNumber(str);
    } catch (DatabaseException e)
    {
      try {
      cursor.close();
      }
      catch (DatabaseException dbe)
      {
      }
      /* database is faulty */
      MessageBuilder mb = new MessageBuilder();
      mb.append(ERR_CHANGELOG_SHUTDOWN_DATABASE_ERROR.get());
      mb.append(stackTraceToSingleLineString(e));
      logError(mb.toMessage());
      replicationServer.shutdown();
      return null;
    }
  }

  /**
   * Read the last Change from the database.
   * @return the last ChangeNumber.
   */
  public ChangeNumber readLastChange()
  {
    Cursor cursor;
    String str = null;

    try
    {
      cursor = db.openCursor(null, null);
      DatabaseEntry key = new DatabaseEntry();
      DatabaseEntry data = new DatabaseEntry();
      OperationStatus status = cursor.getLast(key, data, LockMode.DEFAULT);
      cursor.close();
      if (status != OperationStatus.SUCCESS)
      {
        /* database is empty */
        return null;
      }
      try
      {
       str = new String(key.getData(), "UTF-8");
      } catch (UnsupportedEncodingException e)
      {
        // never happens
      }
      return new ChangeNumber(str);
    } catch (DatabaseException e)
    {
      MessageBuilder mb = new MessageBuilder();
      mb.append(ERR_CHANGELOG_SHUTDOWN_DATABASE_ERROR.get());
      mb.append(stackTraceToSingleLineString(e));
      logError(mb.toMessage());
      replicationServer.shutdown();
      return null;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return serverId.toString() + baseDn.toString();
  }

  /**
   * This Class implements a cursor that can be used to browse a
   * replicationServer database.
   */
  public class ReplServerDBCursor
  {
    private Cursor cursor = null;
    private Transaction txn = null;
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry data = new DatabaseEntry();

    /**
     * Creates a ReplServerDBCursor that can be used for browsing a
     * replicationServer db.
     *
     * @param startingChangeNumber The ChangeNumber from which the cursor must
     *        start.
     * @throws Exception When the startingChangeNumber does not exist.
     */
    private ReplServerDBCursor(ChangeNumber startingChangeNumber)
            throws Exception
    {
      cursor = db.openCursor(txn, null);

      if (startingChangeNumber != null)
      {
        key = new ReplicationKey(startingChangeNumber);
        data = new DatabaseEntry();

        if (cursor.getSearchKey(key, data, LockMode.DEFAULT) !=
          OperationStatus.SUCCESS)
        {
          if (cursor.getSearchKeyRange(key, data, LockMode.DEFAULT) !=
            OperationStatus.SUCCESS)
          {
              throw new Exception("ChangeNumber not available");
          }
          else
          {
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry data = new DatabaseEntry();
            if (cursor.getPrev(key, data, LockMode.DEFAULT) !=
             OperationStatus.SUCCESS)
           {
             cursor = db.openCursor(txn, null);
           }
          }
        }
      }
    }

    private ReplServerDBCursor() throws DatabaseException
    {
      txn = dbenv.beginTransaction();
      cursor = db.openCursor(txn, null);
    }

    /**
     * Close the ReplicationServer Cursor.
     */
    public void close()
    {
      if (cursor == null)
        return;
      try
      {
        cursor.close();
        cursor = null;
      } catch (DatabaseException e)
      {
        MessageBuilder mb = new MessageBuilder();
        mb.append(ERR_CHANGELOG_SHUTDOWN_DATABASE_ERROR.get());
        mb.append(stackTraceToSingleLineString(e));
        logError(mb.toMessage());
        replicationServer.shutdown();
      }
      if (txn != null)
      {
        try
        {
          txn.commit();
        } catch (DatabaseException e)
        {
          MessageBuilder mb = new MessageBuilder();
          mb.append(ERR_CHANGELOG_SHUTDOWN_DATABASE_ERROR.get());
          mb.append(stackTraceToSingleLineString(e));
          logError(mb.toMessage());
          replicationServer.shutdown();
        }
      }
    }

    /**
     * Get the next ChangeNumber in the database from this Cursor.
     *
     * @return The next ChangeNumber in the database from this cursor.
     * @throws DatabaseException In case of underlying database problem.
     */
    public ChangeNumber nextChangeNumber() throws DatabaseException
    {
      OperationStatus status = cursor.getNext(key, data, LockMode.DEFAULT);

      if (status != OperationStatus.SUCCESS)
      {
        return null;
      }
      try
      {
        String csnString = new String(key.getData(), "UTF-8");
        return new ChangeNumber(csnString);
      } catch (UnsupportedEncodingException e)
      {
        // can't happen
        return null;
      }
    }

    /**
     * Get the next UpdateMessage from this cursor.
     *
     * @return the next UpdateMessage.
     */
    public UpdateMessage next()
    {
      UpdateMessage currentChange = null;
      while (currentChange == null)
      {
        try
        {
          OperationStatus status = cursor.getNext(key, data, LockMode.DEFAULT);
          if (status != OperationStatus.SUCCESS)
          {
            return null;
          }
        } catch (DatabaseException e)
        {
          return null;
        }
        try {
          currentChange = ReplicationData.generateChange(data.getData());
        } catch (Exception e) {
          /*
           * An error happening trying to convert the data from the
           * replicationServer database to an Update Message.
           * This can only happen if the database is corrupted.
           * There is not much more that we can do at this point except trying
           * to continue with the next record.
           * In such case, it is therefore possible that we miss some changes.
           * TODO. log an error message.
           * TODO : REPAIR : Such problem should be handled by the
           *        repair functionality.
           */
        }
      }
      return currentChange;
    }

    /**
     * Delete the record at the current cursor position.
     *
     * @throws DatabaseException In case of database problem.
     */
    public void delete() throws DatabaseException
    {
      cursor.delete();
    }
  }
}
