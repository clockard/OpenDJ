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
 *      Portions Copyright 2008 Sun Microsystems, Inc.
 */
package org.opends.scratch.txn;



import java.util.List;

import org.opends.scratch.txn.dummy.ConditionResult;
import org.opends.scratch.txn.dummy.DN;
import org.opends.scratch.txn.dummy.DirectoryException;
import org.opends.scratch.txn.dummy.Entry;
import org.opends.scratch.txn.dummy.Filter;
import org.opends.scratch.txn.dummy.Modification;
import org.opends.scratch.txn.dummy.Scope;



/**
 * A backend transaction.
 * <p>
 * The local backend work-flow element uses backend transactions to
 * perform LDAP operations. Backend transaction implementations are
 * responsible for performing all locking and ensuring ACID
 * transaction semantics.
 * <p>
 * Implementations are not required to perform consistency checks
 * since these are performed by the local backend work-flow element.
 * These include:
 * <ul>
 * <li>checking that an entry exists or does not exist
 * <li>checking that a parent entry exists
 * <li>performing schema validation and checking attribute syntax
 * conformance.
 * </ul>
 * Backend transactions may hold locks until the transaction is closed
 * either through calling {@link #commit()} or {@link #abort()}. In
 * addition, if an exception is encountered during transaction
 * processing, the transaction is aborted.
 * <h2>TODO</h2>
 * <ul>
 * <li>cancellation detection handling and processing
 * <li>controls (e.g. paged results).
 * <li>should referral handling be responsibility of the core server
 * or the backend?
 * <li>modify DN uses a subordinate rename plugin
 * </ul>
 */
public interface BackendTxn
{

  /**
   * Aborts this backend transaction. Any updates made to the
   * underlying backend will be discarded. This backend transaction
   * cannot be used once this method has been called.
   * <p>
   * Implementations must ensure that all pending changes are rolled
   * back and any locks owned by this backend transaction are
   * released.
   *
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  void abort() throws IllegalStateException;



  /**
   * Adds an entry if it is not already present.
   * <p>
   * Implementations must atomically add the provided entry only if
   * there is not an existing entry with the same name. If there was
   * an existing entry with the same name then this method must return
   * <code>false</code> indicating that the underlying backend will
   * not be modified as a result of this add.
   * <p>
   * Implementations can assume:
   * <ul>
   * <li>the parent entry exists
   * <li>the parent entry is locked {@link LockType#SHARED}
   * <li>the entry to be added conforms to the schema
   * </ul>
   * <p>
   * Implementations must ensure that the added entry is exclusive
   * locked until this backend transaction completes in order to
   * prevent concurrent access to the added entry.
   *
   * @param entry
   *          The entry to be added.
   * @return <code>false</code> if there was an existing entry with
   *         the same name, or <code>true</code> if there was no
   *         existing entry with the same name and the entry was
   *         successfully added.
   * @throws DirectoryException
   *           If a problem occurs while trying to add the entry.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  boolean addEntryIfAbsent(Entry entry) throws DirectoryException,
      IllegalStateException;



  /**
   * Commits this backend transaction. Any updates made to the
   * underlying backend will be committed. This backend transaction
   * cannot be used once this method has been called.
   * <p>
   * Implementations must ensure that all pending changes are
   * committed to the underlying backend and any locks owned by this
   * backend transaction are released.
   *
   * @throws DirectoryException
   *           If a problem occurs while trying to commit this backend
   *           transaction.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  void commit() throws DirectoryException, IllegalStateException;



  /**
   * Deletes a subtree of entries whose apex is specified by the
   * provided DN.
   * <p>
   * Implementations must ensure that any entries contained within the
   * subtree are removed. Typically this involves performing a
   * top-down deletion of the entries in order to avoid potential
   * dead-locks with other hierarchical operations (e.g. adds,
   * renames, and searches).
   * <p>
   * Implementations can assume:
   * <ul>
   * <li>the parent entry exists
   * <li>the apex entry to be deleted exists
   * <li>the parent entry is locked {@link LockType#SHARED}
   * <li>the apex entry to be deleted is locked
   * {@link LockType#EXCLUSIVE}
   * <li>non-apex entries within the subtree to be deleted are not
   * locked
   * <li>all schema validation has been performed.
   * </ul>
   * <p>
   * Implementations must ensure that any deleted entries are
   * exclusive locked until this backend transaction completes in
   * order to prevent concurrent access to the deleted entries.
   *
   * @param dn
   *          The name of the entry at the apex of the subtree to be
   *          deleted.
   * @return The number of entries which were deleted.
   * @throws DirectoryException
   *           If a problem occurs while trying to delete the subtree.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  int deleteSubtree(DN dn) throws DirectoryException, IllegalStateException;



  /**
   * Indicates whether the named entry exists and locks it if it does.
   * <p>
   * Implementations must atomically make the determination and lock
   * the named entry according to the semantics of the provided lock
   * type. If the named entry does not exist then no locks must be
   * taken.
   * <p>
   * Implementations may implement this method using the following
   * logic:
   *
   * <pre>
   * return getEntry(dn, lockType) != null;
   * </pre>
   *
   * However a more efficient implementation may be possible which
   * avoids reading and decoding the entire entry from the underlying
   * database.
   *
   * @param dn
   *          The name of the entry to make the determination for.
   * @param lockType
   *          The type of lock to use when making the determination.
   * @return <code>true</code> if the entry exists.
   * @throws DirectoryException
   *           If a problem occurs while trying to make the
   *           determination.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  boolean entryExists(DN dn, LockType lockType) throws DirectoryException,
      IllegalStateException;



  /**
   * Gets the named entry and locks it if it was found.
   * <p>
   * Implementations must atomically retrieve the named entry and lock
   * it according to the semantics of the provided lock type. If the
   * named entry does not exist then no locks must be taken.
   *
   * @param dn
   *          The name of the entry to be retrieved and locked.
   * @param lockType
   *          The type of lock to use while retrieving the entry.
   * @return The entry, locked if requested, or <code>null</code> if
   *         the entry does not exist.
   * @throws DirectoryException
   *           If a problem occurs while trying to retrieve the entry.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  Entry getEntry(DN dn, LockType lockType) throws DirectoryException,
      IllegalStateException;



  /**
   * Indicates whether the named entry has any subordinates.
   * <p>
   * Implementations must atomically make the determination and lock
   * the named entry according to the semantics of the provided lock
   * type. If the named entry does not exist then no locks must be
   * taken.
   *
   * @param dn
   *          The name of the entry.
   * @param lockType
   *          The type of lock to use while retrieving the entry.
   * @return <code>ConditionResult.TRUE</code> if the entry has one
   *         or more subordinates or
   *         <code>ConditionResult.FALSE</code> otherwise or
   *         <code>ConditionResult.UNDEFINED</code> if it can not be
   *         determined.
   * @throws DirectoryException
   *           If a problem occurs while trying to make the
   *           determination.
   */
  ConditionResult hasSubordinates(DN dn, LockType lockType)
      throws DirectoryException;



  /**
   * Locks the named entry for exclusive access if it exists.
   * <p>
   * This method is intended for use in situations where callers wish
   * to perform a read - modify - write cycle. Simply read locking the
   * entry first and then later write locking it during an update can
   * lead to deadlocks with other transactions attempting the same
   * sequence of accesses.
   * <p>
   * Implementations may implement this method using the following
   * logic:
   *
   * <pre>
   * return getEntry(dn, LockType.EXCLUSIVE) != null;
   * </pre>
   *
   * However a more efficient implementation may be possible which
   * avoids reading and decoding the entire entry from the underlying
   * database.
   *
   * @param dn
   *          The name of the entry to be locked.
   * @return <code>true</code> if the entry exists.
   * @throws DirectoryException
   *           If a problem occurs while trying to lock the entry.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  boolean lockEntryExclusive(DN dn) throws DirectoryException,
      IllegalStateException;



  /**
   * Locks the named entry for shared (repeatable read) access if it
   * exists.
   * <p>
   * This method is intended for use in situations where callers wish
   * to maintain logical consistency between multiple entries. For
   * example, updating one entry based on the values of another.
   * Locking the non-updated entry ensures that the two entries remain
   * consistent during the lifetime of this backend transaction.
   * <p>
   * Implementations may implement this method using the following
   * logic:
   *
   * <pre>
   * return getEntry(dn, LockType.SHARED) != null;
   * </pre>
   *
   * However a more efficient implementation may be possible which
   * avoids reading and decoding the entire entry from the underlying
   * database.
   *
   * @param dn
   *          The name of the entry to be locked.
   * @return <code>true</code> if the entry exists.
   * @throws DirectoryException
   *           If a problem occurs while trying to lock the entry.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  boolean lockEntryShared(DN dn) throws DirectoryException,
      IllegalStateException;



  /**
   * Retrieves the number of subordinates for the named entry.
   * <p>
   * Implementations must atomically determine the number of
   * subordinates and lock the named entry according to the semantics
   * of the provided lock type. If the named entry does not exist then
   * no locks must be taken.
   *
   * @param dn
   *          The name of the entry.
   * @param subtree
   *          <code>true</code> to include all entries from the
   *          requested entry to the lowest level in the tree or
   *          <code>false</code> to only include the entries
   *          immediately below the requested entry.
   * @param lockType
   *          The type of lock to use while retrieving the entry.
   * @return The number of subordinate entries for the requested entry
   *         or -1 if it can not be determined.
   * @throws DirectoryException
   *           If a problem occurs while trying retrieve the number of
   *           subordinates.
   */
  long numSubordinates(DN dn, boolean subtree, LockType lockType)
      throws DirectoryException;



  /**
   * Prepares this backend transaction for commit (optional
   * operation).
   * <p>
   * Implementations should try to perform as much commit processing
   * as possible while still permitting a subsequent abort and
   * roll-back to occur.
   * <p>
   * Note that this method is optional, and implementations can choose
   * to provide an empty implementation.
   * <p>
   * If the backend is going to be unable to commit this backend
   * transaction it must throw a {@link DirectoryException}.
   *
   * @throws DirectoryException
   *           If a problem occurs while trying to prepare this
   *           backend transaction.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  void prepare() throws DirectoryException, IllegalStateException;



  /**
   * Renames a subtree of entries whose apex is specified by the DN of
   * the provided entry.
   * <p>
   * Implementations must ensure that any entries contained within the
   * subtree are renamed. Typically this involves performing a
   * top-down rename of the entries in order to avoid potential
   * dead-locks with other hierarchical operations (e.g. adds, subtree
   * deletes, and searches).
   * <p>
   * Implementations can assume:
   * <ul>
   * <li>the old parent entry exists
   * <li>the new parent entry exists (if applicable)
   * <li>the entry to be renamed exists
   * <li>the old parent entry is locked {@link LockType#SHARED}
   * <li>the new parent entry is locked {@link LockType#SHARED}
   * <li>the entry to be renamed is locked {@link LockType#EXCLUSIVE}
   * <li>non-apex entries within the subtree to be renamed are not
   * locked
   * <li>the entry to be renamed conforms to the schema
   * <li><code>oldEntry</code>, <code>newEntry</code>, and
   * <code>modifications</code> are consistent with each other: the
   * DNs are different and the modifications correspond to the
   * differences between the old and new entry.
   * </ul>
   * <p>
   * Implementations must ensure that any renamed entries are
   * exclusive locked until this backend transaction completes in
   * order to prevent concurrent access to the renamed entries.
   *
   * @param oldEntry
   *          The existing apex entry before it was renamed and any
   *          modifications were made.
   * @param newEntry
   *          The renamed apex entry with the modifications applied.
   * @param modifications
   *          The list of modifications that were applied to
   *          <code>oldEntry</code> in order to rename it to
   *          <code>newEntry</code>, this includes modifications
   *          implied by the renaming.
   * @return The number of entries which were renamed.
   * @throws DirectoryException
   *           If a problem occurs while trying to rename the entry.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  int renameSubtree(Entry oldEntry, Entry newEntry,
      List<Modification> modifications) throws DirectoryException,
      IllegalStateException;



  /**
   * Replaces an existing entry with the provided modified entry.
   * <p>
   * Implementations can assume:
   * <ul>
   * <li>the parent entry exists
   * <li>the entry to be replaced exists
   * <li>the parent entry is locked {@link LockType#SHARED}
   * <li>the entry to be replaced is locked
   * {@link LockType#EXCLUSIVE}
   * <li>the entry to be replaced conforms to the schema
   * <li><code>oldEntry</code>, <code>newEntry</code>, and
   * <code>modifications</code> are consistent with each other: the
   * DNs are the same and the modifications correspond to the
   * differences between the old and new entry.
   * </ul>
   * <p>
   * Implementations must ensure that the replaced entry remains
   * exclusive locked until this backend transaction completes in
   * order to prevent concurrent access to the modified entry.
   *
   * @param oldEntry
   *          The existing entry before any modifications were made.
   * @param newEntry
   *          The new version of the entry with the modifications
   *          applied.
   * @param modifications
   *          The list of modifications that were applied to
   *          <code>oldEntry</code> in order to transform it to
   *          <code>newEntry</code>.
   * @throws DirectoryException
   *           If a problem occurs while trying to replace the entry.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  void replaceEntry(Entry oldEntry, Entry newEntry,
      List<Modification> modifications) throws DirectoryException,
      IllegalStateException;



  /**
   * Searches for entries matching the provided criteria and locks
   * them if requested.
   * <p>
   * Implementations must atomically retrieve the matching entries and
   * lock them according to the semantics of the provided lock type.
   *
   * @param dn
   *          The name of the entry to search.
   * @param scope
   *          The scope of the search.
   * @param filter
   *          The filter to use in order to determine if an entry
   *          should be returned.
   * @param lockType
   *          The type of lock to use while retrieving entries.
   * @param handler
   *          The search result handler to be used for processing
   *          search results.
   * @throws DirectoryException
   *           If a problem occurs while searching.
   * @throws IllegalStateException
   *           If this backend transaction has already been committed
   *           or aborted.
   */
  void search(DN dn, Scope scope, Filter filter, LockType lockType,
      SearchResultHandler handler) throws DirectoryException,
      IllegalStateException;

}
