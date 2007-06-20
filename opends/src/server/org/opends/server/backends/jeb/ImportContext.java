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
package org.opends.server.backends.jeb;

import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.util.LDIFReader;
import org.opends.server.admin.std.server.JEBackendCfg;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;

/**
 * This class represents the import context for a destination base DN.
 */
public class ImportContext
{
  /**
   * The name of the entryContainer for the destination base DN.
   */
  private String containerName;

  /**
   * The destination base DN.
   */
  private DN baseDN;

  /**
   * The configuration of the destination backend.
   */
  private JEBackendCfg config;

  /**
   * The requested LDIF import configuration.
   */
  private LDIFImportConfig ldifImportConfig;

  /**
   * A reader for the source LDIF file.
   */
  private LDIFReader ldifReader;

  /**
   * The entry entryContainer for the destination base DN.
   */
  private EntryContainer entryContainer;

  /**
   * The amount of buffer memory available in bytes.
   */
  private long bufferSize;

  /**
   * A queue of entries that have been read from the LDIF and are ready
   * to be imported.
   */
  private BlockingQueue<Entry> queue;

  /**
   * The number of LDAP entries added to the database, used to update the
   * entry database record count after import.  The value is not updated
   * for replaced entries.  Multiple threads may be updating this value.
   */
  private AtomicLong entryInsertCount = new AtomicLong(0);

  /**
   * The parent DN of the previous imported entry.
   */
  private DN parentDN;

  /**
   * The superior IDs, in order from the parent up to the base DN, of the
   * previous imported entry. This is used together with the previous parent DN
   * to save time constructing the subtree index, in the typical case where many
   * contiguous entries from the LDIF file have the same parent.
   */
  private ArrayList<EntryID> IDs;

  /**
   * Get the import entry queue.
   * @return The import entry queue.
   */
  public BlockingQueue<Entry> getQueue()
  {
    return queue;
  }

  /**
   * Set the import entry queue.
   * @param queue The import entry queue.
   */
  public void setQueue(BlockingQueue<Entry> queue)
  {
    this.queue = queue;
  }

  /**
   * Set the name of the entryContainer for the destination base DN.
   * @param containerName The entryContainer name.
   */
  public void setContainerName(String containerName)
  {
    this.containerName = containerName;
  }

  /**
   * Get the name of the entryContainer for the destination base DN.
   * @return The entryContainer name.
   */
  public String getContainerName()
  {
    return containerName;
  }

  /**
   * Set the destination base DN.
   * @param baseDN The destination base DN.
   */
  public void setBaseDN(DN baseDN)
  {
    this.baseDN = baseDN;
  }

  /**
   * Get the destination base DN.
   * @return The destination base DN.
   */
  public DN getBaseDN()
  {
    return baseDN;
  }

  /**
   * Set the configuration of the destination backend.
   * @param config The destination backend configuration.
   */
  public void setConfig(JEBackendCfg config)
  {
    this.config = config;
  }

  /**
   * Get the configuration of the destination backend.
   * @return The destination backend configuration.
   */
  public JEBackendCfg getConfig()
  {
    return config;
  }

  /**
   * Set the requested LDIF import configuration.
   * @param ldifImportConfig The LDIF import configuration.
   */
  public void setLDIFImportConfig(LDIFImportConfig ldifImportConfig)
  {
    this.ldifImportConfig = ldifImportConfig;
  }

  /**
   * Get the requested LDIF import configuration.
   * @return The requested LDIF import configuration.
   */
  public LDIFImportConfig getLDIFImportConfig()
  {
    return ldifImportConfig;
  }

  /**
   * Set the source LDIF reader.
   * @param ldifReader The source LDIF reader.
   */
  public void setLDIFReader(LDIFReader ldifReader)
  {
    this.ldifReader = ldifReader;
  }

  /**
   * Get the source LDIF reader.
   * @return The source LDIF reader.
   */
  public LDIFReader getLDIFReader()
  {
    return ldifReader;
  }

  /**
   * Set the entry entryContainer for the destination base DN.
   * @param entryContainer The entry entryContainer for the destination base DN.
   */
  public void setEntryContainer(EntryContainer entryContainer)
  {
    this.entryContainer = entryContainer;
  }

  /**
   * Get the entry entryContainer for the destination base DN.
   * @return The entry entryContainer for the destination base DN.
   */
  public EntryContainer getEntryContainer()
  {
    return entryContainer;
  }

  /**
   * Get the available buffer size in bytes.
   * @return The available buffer size.
   */
  public long getBufferSize()
  {
    return bufferSize;
  }

  /**
   * Set the available buffer size in bytes.
   * @param bufferSize The available buffer size in bytes.
   */
  public void setBufferSize(long bufferSize)
  {
    this.bufferSize = bufferSize;
  }

  /**
   * Get the number of new LDAP entries imported into the entry database.
   * @return The number of new LDAP entries imported into the entry database.
   */
  public long getEntryInsertCount()
  {
    return entryInsertCount.get();
  }

  /**
   * Increment the number of new LDAP entries imported into the entry database
   * by the given amount.
   * @param delta The amount to add.
   */
  public void incrEntryInsertCount(long delta)
  {
    entryInsertCount.getAndAdd(delta);
  }

  /**
   * Get the parent DN of the previous imported entry.
   * @return The parent DN of the previous imported entry.
   */
  public DN getParentDN()
  {
    return parentDN;
  }

  /**
   * Set the parent DN of the previous imported entry.
   * @param parentDN The parent DN of the previous imported entry.
   */
  public void setParentDN(DN parentDN)
  {
    this.parentDN = parentDN;
  }

  /**
   * Get the superior IDs of the previous imported entry.
   * @return The superior IDs of the previous imported entry.
   */
  public ArrayList<EntryID> getIDs()
  {
    return IDs;
  }

  /**
   * Set the superior IDs of the previous imported entry.
   * @param IDs The superior IDs of the previous imported entry.
   */
  public void setIDs(ArrayList<EntryID> IDs)
  {
    this.IDs = IDs;
  }
}
