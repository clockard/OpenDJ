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
 *      Copyright 2008 Sun Microsystems, Inc.
 */


package org.opends.server.backends.jeb.importLDIF;

import org.opends.server.types.Entry;
import org.opends.server.backends.jeb.Index;
import org.opends.server.backends.jeb.EntryID;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.dbi.MemoryBudget;
import static org.opends.server.loggers.ErrorLogger.logError;
import org.opends.messages.Message;
import static org.opends.messages.JebMessages.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Manages a shared cache among worker threads that caches substring
 * key/value pairs to avoid DB cache access. Once the cache is above it's
 * memory usage limit, it will start slowly flushing keys (similar to the
 * JEB eviction process) until it is under the limit.
 */

public class BufferManager {

  //Memory usage counter.
  private long memoryUsage=0;

  //Memory limit.
  private long memoryLimit;

  //Next element in the cache to start flushing at during next flushAll cycle.
  private KeyHashElement nextElem;

  //Extra bytes to flushAll.
  private final int extraBytes  = 1024 * 1024;

  //Counters for statistics, total is number of accesses, hit is number of
  //keys found in cache.
  private long total=0, hit=0;

  //Actual map used to buffer keys.
  private TreeMap<KeyHashElement, KeyHashElement> elementMap =
                        new TreeMap<KeyHashElement, KeyHashElement>();

  //The current backup map being used.
  private int currentMap = 1;

  //Reference to use when the maps are switched.
  private TreeMap<KeyHashElement, KeyHashElement> backupMap;

  //The two backup maps to insert into if the main element map is being used.
  private TreeMap<KeyHashElement, KeyHashElement> backupMap2 =
                        new TreeMap<KeyHashElement, KeyHashElement>();
  private TreeMap<KeyHashElement, KeyHashElement> backupMap1 =
                        new TreeMap<KeyHashElement, KeyHashElement>();

  //Overhead values determined from using JHAT. They appear to be the same
  //for both 32 and 64 bit machines. Close enough.
  private final static int TREEMAP_ENTRY_OVERHEAD = 29;
  private final static int KEY_ELEMENT_OVERHEAD = 32;

  //Lock used to get main element map.
  private ReentrantLock lock = new ReentrantLock();

  //Object to synchronize on if backup maps are being written.
  private final Object backupSynchObj = new Object();

  /**
   * Create buffer manager instance.
   *
   * @param memoryLimit The memory limit.
   */
  public BufferManager(long memoryLimit) {
    this.memoryLimit = memoryLimit;
    this.nextElem = null;
    this.backupMap = backupMap1;
  }

  /**
   * Insert an entry ID into the buffer using the both the specified index and
   * entry to build a key set. Will flush the buffer if over the memory limit.
   *
   * @param index  The index to use.
   * @param entry The entry used to build the key set.
   * @param entryID The entry ID to insert into the key set.
   * @param keySet Keyset hash to store the keys in.
   * @throws DatabaseException If a problem happened during a flushAll cycle.
   */

  void insert(Index index, Entry entry,
              EntryID entryID, Set<byte[]> keySet)
          throws DatabaseException {

    keySet.clear();
    index.indexer.indexEntry(entry, keySet);
    if(!lock.tryLock()) {
      insertBackupMap(keySet, index, entryID);
      return;
    }
    insertKeySet(keySet, index, entryID, elementMap, true);
    if(!backupMap.isEmpty()) {
       mergeMap();
    }
    //If over the memory limit, flush some keys from the cache to make room.
    if(memoryUsage > memoryLimit) {
      flushUntilUnderLimit();
    }
    lock.unlock();
  }

  /**
   * Insert an entry ID into buffer using specified id2children and id2subtree
   * indexes.
   *
   * @param id2children The id2children index to use.
   * @param id2subtree The id2subtree index to use.
   * @param entry The entry used to build the key set.
   * @param entryID The entry ID to insert into the key set.
   * @param childKeySet id2children key set hash to use.
   * @param subKeySet subtree key set hash to use.
   * @throws DatabaseException If a problem occurs during processing.
   */
  void insert(Index id2children, Index id2subtree, Entry entry,
              EntryID entryID, Set<byte[]> childKeySet,
              Set<byte[]> subKeySet) throws DatabaseException {
    childKeySet.clear();
    id2children.indexer.indexEntry(entry, childKeySet);
    subKeySet.clear();
    id2subtree.indexer.indexEntry(entry, subKeySet);
    if(!lock.tryLock()) {
      insertBackupMap(childKeySet, id2children, subKeySet, id2subtree, entryID);
      return;
    }
    insertKeySet(childKeySet, id2children, entryID, elementMap, true);
    insertKeySet(subKeySet, id2subtree, entryID, elementMap, true);
    lock.unlock();
  }

  /**
   * Insert into a backup tree if can't get a lock on the main table.
   * @param childrenKeySet The id2children keyset to use.
   * @param id2children The id2children index to use.
   * @param subtreeKeySet The subtree keyset to use.
   * @param id2subtree The id2subtree index to use.
   * @param entryID The entry ID to insert into the key set.
   */
  void insertBackupMap(Set<byte[]> childrenKeySet, Index id2children,
                    Set<byte[]> subtreeKeySet,
                    Index id2subtree, EntryID entryID) {
    synchronized(backupSynchObj) {
      insertKeySet(childrenKeySet, id2children, entryID, backupMap,  false);
      insertKeySet(subtreeKeySet, id2subtree, entryID, backupMap,  false);
    }
  }


  /**
   * Insert specified keyset, index and entry ID into the backup map.
   *
   * @param keySet The keyset to use.
   * @param index The index to use.
   * @param entryID The entry ID to use.
   */
  void insertBackupMap(Set<byte[]> keySet, Index index, EntryID entryID) {
       synchronized(backupSynchObj) {
         insertKeySet(keySet, index, entryID, backupMap,  false);
    }
  }


  /**
   * Merge the backup map with the element map after switching the backup
   * map reference to an empty map.
   */
  void mergeMap() {
    TreeMap<KeyHashElement, KeyHashElement> tmpMap;
    synchronized(backupSynchObj) {
      if(currentMap == 1) {
         backupMap = backupMap2;
         tmpMap = backupMap1;
         currentMap = 2;
      } else {
         backupMap = backupMap1;
         tmpMap = backupMap2;
         currentMap = 1;
      }
    }
    TreeSet<KeyHashElement>  tSet =
            new TreeSet<KeyHashElement>(tmpMap.keySet());
    for (KeyHashElement elem : tSet) {
      total++;
      if(!elementMap.containsKey(elem)) {
        elementMap.put(elem, elem);
        memoryUsage += TREEMAP_ENTRY_OVERHEAD + elem.getMemorySize();
      } else {
        KeyHashElement curElem = elementMap.get(elem);
        if(curElem.isDefined() || curElem.getIndex().getMaintainCount()) {
          int oldSize = curElem.getMemorySize();
          curElem.merge(elem);
          memoryUsage += (curElem.getMemorySize() - oldSize);
          hit++;
        }
      }
    }
    tmpMap.clear();
  }

  /**
   * Insert a keySet into the element map using the provided index and entry ID.
   * @param keySet The key set to add to the map.
   * @param index  The index that eventually will contain the entry IDs.
   * @param entryID The entry ID to add to the entry ID set.
   * @param map The map to add the keys to
   * @param trackStats <CODE>True</CODE> if memory and usage should be tracked.
   */
  private void insertKeySet(Set<byte[]> keySet, Index index, EntryID entryID,
                            TreeMap<KeyHashElement, KeyHashElement> map,
                            boolean trackStats) {
    KeyHashElement elem = new KeyHashElement();
    int entryLimit = index.getIndexEntryLimit();
    for(byte[] key : keySet) {
      elem.reset(key, index);
      if(trackStats) {
        total++;
      }
      if(!map.containsKey(elem)) {
        KeyHashElement newElem = new KeyHashElement(key, index, entryID);
        map.put(newElem, newElem);
        if(trackStats) {
          memoryUsage += TREEMAP_ENTRY_OVERHEAD + newElem.getMemorySize();
        }
      } else {
        KeyHashElement curElem = map.get(elem);
        if(curElem.isDefined() || index.getMaintainCount()) {
          int oldSize = curElem.getMemorySize();
          curElem.addEntryID(entryID, entryLimit);
          if(trackStats) {
            memoryUsage += (curElem.getMemorySize() - oldSize);
            hit++;
          }
        }
      }
    }
  }

  /**
   * Flush the buffer to DB until the buffer is under the memory limit.
   *
   * @throws DatabaseException If a problem happens during an index insert.
   */
  private void flushUntilUnderLimit() throws DatabaseException {
    Iterator<KeyHashElement> iter;
    if(nextElem == null) {
      iter = elementMap.keySet().iterator();
    } else {
      iter = elementMap.tailMap(nextElem).keySet().iterator();
    }
    DatabaseEntry dbEntry = new DatabaseEntry();
    DatabaseEntry entry = new DatabaseEntry();
    while((memoryUsage + extraBytes) > memoryLimit) {
      if(iter.hasNext()) {
        KeyHashElement curElem = iter.next();
        //Never flush undefined elements.
        if(curElem.isDefined()) {
          int oldSize = curElem.getMemorySize();
          Index index = curElem.getIndex();
          dbEntry.setData(curElem.getKey());
          index.insert(dbEntry, curElem.getIDSet(), entry);
          if(curElem.isDefined()) {
             memoryUsage -= TREEMAP_ENTRY_OVERHEAD + curElem.getMemorySize();
             iter.remove();
          } else {
            //Went undefined don't remove the element, just substract the
            //memory size difference.
            memoryUsage -= (oldSize - curElem.getMemorySize());
          }
        }
      } else {
        //Wrapped around, start at the first element.
        nextElem = elementMap.firstKey();
        iter = elementMap.keySet().iterator();
      }
    }
    //Start at this element next flushAll cycle.
    nextElem = iter.next();
  }

  /**
   * Called from main thread to prepare for final buffer flush at end of
   * ldif load.
   */
  void prepareFlush() {
    Message msg =
           NOTE_JEB_IMPORT_LDIF_BUFFER_FLUSH.get(elementMap.size(), total, hit);
    logError(msg);
  }

  /**
   * Writes all of the buffer elements to DB. The specific id is used to
   * share the buffer among the worker threads so this function can be
   * multi-threaded.
   *
   * @throws DatabaseException If an error occurred during the insert.
   */
  void flushAll() throws DatabaseException {
    mergeMap();
    TreeSet<KeyHashElement>  tSet =
            new TreeSet<KeyHashElement>(elementMap.keySet());
    DatabaseEntry dbEntry = new DatabaseEntry();
    DatabaseEntry entry = new DatabaseEntry();
    for (KeyHashElement curElem : tSet) {
      if(curElem.isDirty()) {
        Index index = curElem.getIndex();
        dbEntry.setData(curElem.getKey());
        index.insert(dbEntry, curElem.getIDSet(), entry);
      }
    }
  }

  /**
   *  Class used to represent an element in the buffer.
   */
  class KeyHashElement implements Comparable {

    //Bytes representing the key.
    private  byte[] key;

    //Hash code returned from the System.identityHashCode method on the index
    //object.
    private int indexHashCode;

    //Index related to the element.
    private Index index;

    //The set of IDs related to the key.
    private ImportIDSet importIDSet;

    //Used to speed up lookup.
    private int keyHashCode;

    /**
     * Empty constructor for use when the element is being reused.
     */
    public KeyHashElement() {}

    /**
     * Reset the element. Used when the element is being reused.
     *
     * @param key The new key to reset to.
     * @param index The new index to reset to.
     */
    public void reset(byte[] key, Index index) {
      this.key = key;
      this.index = index;
      this.indexHashCode = System.identityHashCode(index);
      this.keyHashCode = Arrays.hashCode(key);
      if(this.importIDSet != null) {
        this.importIDSet.reset();
      }
    }

    /**
     * Create instance of an element for the specified key and index, the add
     * the specified entry ID to the ID set.
     *
     * @param key The key.
     * @param index The index.
     * @param entryID The entry ID to start off with.
     */
    public KeyHashElement(byte[] key, Index index, EntryID entryID) {
      this.key = key;
      this.index = index;
      //Use the integer set for right now. This is good up to 2G number of
      //entries. There is also a LongImportSet, but it currently isn't used.
      this.importIDSet = new IntegerImportIDSet(entryID);
      //Used if there when there are conflicts if two or more indexes have
      //the same key.
      this.indexHashCode = System.identityHashCode(index);
      this.keyHashCode = Arrays.hashCode(key);
    }

    /**
     * Add an entry ID to the set.
     *
     * @param entryID  The entry ID to add.
     * @param entryLimit The entry limit
     */
    void addEntryID(EntryID entryID, int entryLimit) {
      importIDSet.addEntryID(entryID, entryLimit, index.getMaintainCount());
    }

    /**
     * Return the index.
     *
     * @return The index.
     */
    Index getIndex(){
      return index;
    }

    /**
     * Return the key.
     *
     * @return The key.
     */
    byte[] getKey() {
      return key;
    }

    /**
     * Return value of the key hash code.
     *
     * @return The key hash code value.
     */
    int getKeyHashCode() {
      return keyHashCode;
    }

    /**
     * Return the ID set.
      * @return The import ID set.
     */
    ImportIDSet getIDSet() {
      return importIDSet;
    }

    /**
     * Return if the ID set is defined or not.
     *
     * @return <CODE>True</CODE> if the ID set is defined.
     */
    boolean isDefined() {
      return importIDSet.isDefined();
    }

    /**
     * Compare the bytes of two keys.  The is slow, only use if the hashcode
     * had a collision.
     *
     * @param a  Key a.
     * @param b  Key b.
     * @return  0 if the keys are equal, -1 if key a is less than key b, 1 if
     *          key a is greater than key b.
     */
    private int compare(byte[] a, byte[] b) {
      int i;
      for (i = 0; i < a.length && i < b.length; i++) {
        if (a[i] > b[i]) {
          return 1;
        }
        else if (a[i] < b[i]) {
          return -1;
        }
      }
      if (a.length == b.length) {
        return 0;
      }
      if (a.length > b.length){
        return 1;
      }
      else {
        return -1;
      }
    }

    /**
     * Compare two element keys. First check the precomputed hashCode. If
     * the hashCodes are equal, do a second byte per byte comparision in case
     * there was a  collision.
     *
     * @param elem The element to compare.
     * @return  0 if the keys are equal, -1 if key a is less than key b, 1 if
     *          key a is greater than key b.
     */
    private int compare(KeyHashElement elem) {
      if(keyHashCode == elem.getKeyHashCode()) {
        return compare(key, elem.key);
      } else {
        if(keyHashCode < elem.getKeyHashCode()) {
          return -1;
        } else {
          return 1;
        }
      }
    }

    /**
     * Compare the specified object to the current object. If the keys are
     * equal, then the indexHashCode value is used as a tie-breaker.
     *
     * @param o The object representing a KeyHashElement.
     * @return 0 if the objects are equal, -1 if the current object is less
     *         than the specified object, 1 otherwise.
     */
    public int compareTo(Object o) {
      if (o == null) {
        throw new NullPointerException();
      }
      KeyHashElement inElem = (KeyHashElement) o;
      int keyCompare = compare(inElem);
      if(keyCompare == 0) {
        if(indexHashCode == inElem.indexHashCode) {
          return 0;
        } else if(indexHashCode < inElem.indexHashCode) {
          return -1;
        } else {
          return 1;
        }
      } else {
        return keyCompare;
      }
    }

    /**
     * Return the current total memory size of the element.
     * @return The memory size estimate of a KeyHashElement.
     */
    int getMemorySize() {
      return  KEY_ELEMENT_OVERHEAD +
              MemoryBudget.byteArraySize(key.length) +
              importIDSet.getMemorySize();
    }

    /**
     * Merge the specified element with this element.
     * @param e The element to merge.
     */
    public void merge(KeyHashElement e) {
      importIDSet.merge(e.importIDSet, e.getIndex().getIndexEntryLimit(),
              e.getIndex().getMaintainCount());
    }

    /**
     * Return if an undefined import ID set has been written to the index DB.
     *
     * @return <CODE>True</CODE> if an undefined importID set has been written
     * to the index DB.
     */
    public boolean isDirty() {
      return importIDSet.isDirty();
    }
  }
}
