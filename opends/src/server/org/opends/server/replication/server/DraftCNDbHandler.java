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
 *      Copyright 2009 Sun Microsystems, Inc.
 */
package org.opends.server.replication.server;
import static org.opends.messages.ReplicationMessages.*;
import static org.opends.server.loggers.ErrorLogger.logError;
import static org.opends.server.loggers.debug.DebugLogger.debugEnabled;
import static org.opends.server.loggers.debug.DebugLogger.getTracer;
import static org.opends.server.util.StaticUtils.stackTraceToSingleLineString;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

import org.opends.messages.MessageBuilder;
import org.opends.server.admin.std.server.MonitorProviderCfg;
import org.opends.server.api.DirectoryThread;
import org.opends.server.api.MonitorProvider;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.replication.common.ChangeNumber;
import org.opends.server.replication.common.ServerState;
import org.opends.server.replication.server.DraftCNDB.DraftCNDBCursor;
import org.opends.server.types.Attribute;
import org.opends.server.types.Attributes;
import org.opends.server.types.InitializationException;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.LockConflictException;

/**
 * This class is used for managing the replicationServer database for each
 * server in the topology.
 * It is responsible for efficiently saving the updates that is received from
 * each master server into stable storage.
 * This class is also able to generate a ReplicationIterator that can be
 * used to read all changes from a given ChangeNUmber.
 *
 * This class publish some monitoring information below cn=monitor.
 *
 */
public class DraftCNDbHandler implements Runnable
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();
  // A dedicated thread loops trim().
  // trim()  : deletes from the DB a number of changes that are older than a
  //           certain date.
  //
  static int NO_KEY = 0;

  private DraftCNDB db;
  private int firstkey = NO_KEY;
  private int lastkey = NO_KEY;
  private DbMonitorProvider dbMonitor = new DbMonitorProvider();
  private boolean shutdown = false;
  private boolean trimDone = false;
  private DirectoryThread thread = null;
  private ReplicationServer replicationServer;

  // The maximum number of retries in case of DatabaseDeadlock Exception.
  private static final int DEADLOCK_RETRIES = 10;

  /**
   *
   * The trim age in milliseconds. Changes record in the change DB that
   * are older than this age are removed.
   *
   */
  private long trimage;

  /**
   * Creates a new dbHandler associated to a given LDAP server.
   *
   * @param replicationServer The ReplicationServer that creates this dbHandler.
   * @param dbenv the Database Env to use to create the ReplicationServer DB.
   * server for this domain.
   * @throws DatabaseException If a database problem happened
   */
  public DraftCNDbHandler(ReplicationServer replicationServer,
      ReplicationDbEnv dbenv)
         throws DatabaseException
  {
    this.replicationServer = replicationServer;
    this.trimage = replicationServer.getTrimage();

    // DB initialization
    db = new DraftCNDB(replicationServer, dbenv);
    firstkey = db.readFirstDraftCN();
    lastkey = db.readLastDraftCN();

    // Triming thread
    thread = new DirectoryThread(this, "Replication DraftCN db ");
    thread.start();

    // Monitoring registration
    DirectoryServer.deregisterMonitorProvider(
                      dbMonitor.getMonitorInstanceName());
    DirectoryServer.registerMonitorProvider(dbMonitor);
  }

  /**
   * Add an update to the list of messages that must be saved to the db
   * managed by this db handler.
   * This method is blocking if the size of the list of message is larger
   * than its maximum.
   * @param key The key for this record in the db.
   * @param value The associated value.
   * @param serviceID The associated serviceID.
   * @param cn The associated replication change number.
   *
   */
  public synchronized void add(int key, String value, String serviceID,
      ChangeNumber cn)
  {
    db.addEntry(key, value, serviceID, cn);

    if (debugEnabled())
      TRACER.debugInfo(
          "In DraftCNDbhandler.add, added: "
        + " key=" + key
        + " value=" + value
        + " serviceID=" + serviceID
        + " cn=" + cn);
  }

  /**
   * Get the firstChange.
   * @return Returns the firstChange.
   */
  public int getFirstKey()
  {
    return db.readFirstDraftCN();
  }

  /**
   * Get the lastChange.
   * @return Returns the lastChange.
   */
  public int getLastKey()
  {
    return db.readLastDraftCN();
  }

  /**
   * Get the number of changes.
   * @return Returns the number of changes.
   */
  public long count()
  {
    return db.count();
  }

  /**
   * Get a read cursor on the database from a provided key.
   * The cursor MUST be released after use.
   * @param key The provided key.
   * @return the new cursor.
   */
  public DraftCNDBCursor getReadCursor(int key)
  {
    try
    {
      return db.openReadCursor(key);
    }
    catch(Exception e)
    {
      return null;
    }
  }

  /**
   * Release a provided read cursor.
   * @param cursor The provided read cursor.
   */
  public void releaseReadCursor(DraftCNDBCursor cursor)
  {
    try
    {
      cursor.close();
    }
    catch(Exception e)
    {
    }
  }

  /**
   * Generate a new ReplicationIterator that allows to browse the db
   * managed by this dbHandler and starting at the position defined
   * by a given changeNumber.
   *
   * @param  startDraftCN The position where the iterator must start.
   *
   * @return a new ReplicationIterator that allows to browse the db
   *         managed by this dbHandler and starting at the position defined
   *         by a given changeNumber.
   *
   * @throws DatabaseException if a database problem happened.
   * @throws Exception  If there is no other change to push after change
   *         with changeNumber number.
   */
  public DraftCNDbIterator generateIterator(int startDraftCN)
                           throws DatabaseException, Exception
  {
    DraftCNDbIterator it =
      new DraftCNDbIterator(db, startDraftCN);
    return it;
  }

  /**
   * Shutdown this dbHandler.
   */
  public void shutdown()
  {
    if (shutdown == true)
    {
      return;
    }

    shutdown  = true;
    synchronized (this)
    {
      this.notifyAll();
    }

    synchronized (this)
    {
      while (trimDone  == false)
      {
        try
        {
          this.wait();
        } catch (Exception e)
        {}
      }
    }

    db.shutdown();
    DirectoryServer.deregisterMonitorProvider(
        dbMonitor.getMonitorInstanceName());
  }

  /**
   * Run method for this class.
   * Periodically Flushes the ReplicationServerDomain cache from memory to the
   * stable storage and trims the old updates.
   */
  public void run()
  {
    while (shutdown == false)
    {
      try {
        trim();

        synchronized (this)
        {
          try
          {
            this.wait(1000);
          } catch (InterruptedException e)
          { }
        }
      } catch (Exception end)
      {
        MessageBuilder mb = new MessageBuilder();
        mb.append(ERR_EXCEPTION_CHANGELOG_TRIM_FLUSH.get());
        mb.append(stackTraceToSingleLineString(end));
        logError(mb.toMessage());
        if (replicationServer != null)
          replicationServer.shutdown();
        break;
      }
    }

    synchronized (this)
    {
      trimDone = true;
      this.notifyAll();
    }
  }

  /**
   * Trim old changes from this database.
   * @throws DatabaseException In case of database problem.
   * @throws Exception In case of database problem.
   */
  public void trim() throws DatabaseException, Exception
  {
    if (trimage == 0)
      return;

    clear(null);
  }

  /**
   * Clear the changes from this DB (from both memory cache and DB storage)
   * for the provided serviceID.
   * @param serviceIDToClear The serviceID for which we want to remove the
   *         all records from the DraftCNDb.
   * @throws DatabaseException When an exception occurs while removing the
   * changes from the DB.
   * @throws Exception When an exception occurs while accessing a resource
   * from the DB.
   *
   */
  public void clear(String serviceIDToClear)
  throws DatabaseException, Exception
  {
    if (this.count()==0)
      return;

    int size = 0;
    int tries = 0;
    boolean finished = false;
    boolean done = false;

    // In case of deadlock detection by the Database, this thread can
    // by aborted by a DeadlockException. This is a transient error and
    // the transaction should be attempted again.
    // We will try DEADLOCK_RETRIES times before failing.
    while ((tries++ < DEADLOCK_RETRIES) && (!done))
    {
      DraftCNDBCursor cursor = db.openDeleteCursor();
      try
      {
        while ((size < 5000 ) &&  (!finished))
        {
          // let's traverse the DraftCNDb
          if (!cursor.next())
          {
            finished=true;
          }
          else
          {
            ChangeNumber cn = cursor.currentChangeNumber();

            // From the draftCNDb change record, get the domain and changeNumber
            String serviceID = cursor.currentServiceID();

            if ((serviceIDToClear!=null) &&
                (serviceIDToClear.equalsIgnoreCase(serviceID)))
            {
              size++;
              cursor.delete();
              continue;
            }

            ReplicationServerDomain domain =
              replicationServer.getReplicationServerDomain(serviceID, false);

            if (domain==null)
            {
              // the domain has been removed since the record was written in the
              // draftCNDb, thus it makes no sense to keep the record in the
              // draftCNDb.
              size++;
              cursor.delete();
            }
            else
            {
              // let's get the eligible part of the domain
              ServerState startSS = domain.getStartState();
              ServerState endSS   = domain.getEligibleState(
                  replicationServer.getEligibleCN());
              ChangeNumber fcn = startSS.getMaxChangeNumber(cn.getServerId());
              ChangeNumber lcn = endSS.getMaxChangeNumber(cn.getServerId());

              // if the draftCNDb change record, is out of the eligible part
              //  of the domain, then it can be removed.
              if (cn.older(fcn)||cn.newer(lcn))
              {
                size++;
                cursor.delete();
              }
            }
          }
        }
        cursor.close();
        done = true;
      }
      catch (LockConflictException e)
      {
        cursor.abort();
        if (tries == DEADLOCK_RETRIES)
        {
          // could not handle the Deadlock after DEADLOCK_RETRIES tries.
          // shutdown the ReplicationServer.
          shutdown = true;
          throw (e);
        }
      }
      catch (DatabaseException e)
      {
        // mark shutdown for this db so that we don't try again to
        // stop it from cursor.close() or methods called by cursor.close()
        shutdown = true;
        cursor.abort();
        throw (e);
      }
    }
  }

  /**
   * This internal class is used to implement the Monitoring capabilities
   * of the dbHandler.
   */
  private class DbMonitorProvider extends MonitorProvider<MonitorProviderCfg>
  {
    private DbMonitorProvider()
    {
      super("ReplicationServer DraftCN Database");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ArrayList<Attribute> getMonitorData()
    {
      ArrayList<Attribute> attributes = new ArrayList<Attribute>();
      attributes.add(Attributes.create("first-draft-changenumber",
          Integer.toString(db.readFirstDraftCN())));
      attributes.add(Attributes.create("last-draft-changenumber",
          Integer.toString(db.readLastDraftCN())));
      attributes.add(Attributes.create("count",
          Long.toString(count())));
      return attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMonitorInstanceName()
    {
      return "ReplicationServer DraftCN database ";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getUpdateInterval()
    {
      /* we don't wont to do polling on this monitor */
      return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeMonitorProvider(MonitorProviderCfg configuration)
                            throws ConfigException,InitializationException
    {
      // Nothing to do for now
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMonitorData()
    {
      // As long as getUpdateInterval() returns 0, this will never get called
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return("draftCNdb:" + " " + firstkey + " " + lastkey);
  }

  /**
   * Set the Purge delay for this db Handler.
   * @param delay The purge delay in Milliseconds.
   */
  public void setPurgeDelay(long delay)
  {
    trimage = delay;
  }

  /**
   * Clear the changes from this DB (from both memory cache and DB storage).
   * @throws DatabaseException When an exception occurs while removing the
   * changes from the DB.
   * @throws Exception When an exception occurs while accessing a resource
   * from the DB.
   *
   */
  public void clear() throws DatabaseException, Exception
  {
    db.clear();
    firstkey = db.readFirstDraftCN();
    lastkey = db.readLastDraftCN();
  }

  private ReentrantLock lock = new ReentrantLock();

  /**
   * Tests if the current thread has the lock on this object.
   * @return True if the current thread has the lock.
   */
  public boolean hasLock()
  {
    return (lock.getHoldCount() > 0);
  }

  /**
   * Takes the lock on this object (blocking until lock can be acquired).
   * @throws java.lang.InterruptedException If interrupted.
   */
   public void lock() throws InterruptedException
  {
    lock.lockInterruptibly();
  }

  /**
   * Releases the lock on this object.
   */
  public void release()
  {
    lock.unlock();
  }

  /**
   * Get the value associated to a provided key.
   * @param key the provided key.
   * @return the associated value, null when none.
   */
  public String getValue(int key)
  {
    String value = null;
    DraftCNDBCursor draftCNDBCursor = null;
    try
    {
      draftCNDBCursor = db.openReadCursor(key);
      value = draftCNDBCursor.currentValue();
    }
    catch(Exception e)
    {
      if (debugEnabled())
        TRACER.debugInfo("In DraftCNDbHandler.getGeneralizedState, read: " +
          " key=" + key + " genServerState returned is null" +
          " first=" + db.readFirstDraftCN() +
          " last=" + db.readLastDraftCN() +
          " count=" + db.count() +
          " exception" + e + " " + e.getMessage());
      return null;
    }
    finally
    {
      if (draftCNDBCursor != null)
        draftCNDBCursor.close();
    }
    return value;
  }
}
