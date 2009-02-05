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
package org.opends.server.backends.jeb;

import static org.opends.server.loggers.debug.DebugLogger.*;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.DebugLogLevel;

import org.opends.server.api.OrderingMatchingRule;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.opends.server.types.Modification;

import java.util.*;

/**
 * An implementation of an Indexer for attribute ordering.
 */
public class OrderingIndexer extends Indexer
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();



  /**
   * The attribute type for which this instance will
   * generate index keys.
   */
  private AttributeType attributeType;

  /**
   * The attribute type ordering matching rule which is also the
   * comparator for the index keys generated by this class.
   */
  private OrderingMatchingRule orderingRule;


  /**
   * Create a new attribute ordering indexer for the given index configuration.
   * @param attributeType The attribute type for which an indexer is
   * required.
   */
  public OrderingIndexer(AttributeType attributeType)
  {
    this.attributeType = attributeType;
    this.orderingRule = attributeType.getOrderingMatchingRule();
  }

  /**
   * Get a string representation of this object.  The returned value is
   * used to name an index created using this object.
   * @return A string representation of this object.
   */
  public String toString()
  {
    return attributeType.getNameOrOID() + ".ordering";
  }

  /**
   * Get the comparator that must be used to compare index keys
   * generated by this class.
   *
   * @return A byte array comparator.
   */
  public Comparator<byte[]> getComparator()
  {
    return orderingRule;
  }

  /**
   * Generate the set of index keys for an entry.
   *
   * @param entry The entry.
   * @param keys The set into which the generated keys will be inserted.
   */
  public void indexEntry(Entry entry, Set<byte[]> keys)
  {
    List<Attribute> attrList =
         entry.getAttribute(attributeType);
    if (attrList != null)
    {
      indexAttribute(attrList, keys);
    }
  }

  /**
   * Generate the set of index keys to be added and the set of index keys
   * to be deleted for an entry that has been replaced.
   *
   * @param oldEntry The original entry contents.
   * @param newEntry The new entry contents.
   * @param modifiedKeys The map into which the modified keys will be inserted.
   */
  public void replaceEntry(Entry oldEntry, Entry newEntry,
                           Map<byte[], Boolean> modifiedKeys)
  {
    List<Attribute> newAttributes = newEntry.getAttribute(attributeType, true);
    List<Attribute> oldAttributes = oldEntry.getAttribute(attributeType, true);

    indexAttribute(oldAttributes, modifiedKeys, false);
    indexAttribute(newAttributes, modifiedKeys, true);
  }

  /**
   * Generate the set of index keys to be added and the set of index keys
   * to be deleted for an entry that was modified.
   *
   * @param oldEntry The original entry contents.
   * @param newEntry The new entry contents.
   * @param mods The set of modifications that were applied to the entry.
   * @param modifiedKeys The map into which the modified keys will be inserted.
   */
  public void modifyEntry(Entry oldEntry, Entry newEntry,
                          List<Modification> mods,
                          Map<byte[], Boolean> modifiedKeys)
  {
    List<Attribute> newAttributes = newEntry.getAttribute(attributeType, true);
    List<Attribute> oldAttributes = oldEntry.getAttribute(attributeType, true);

    indexAttribute(oldAttributes, modifiedKeys, false);
    indexAttribute(newAttributes, modifiedKeys, true);
  }



  /**
   * Generate the set of index keys for an attribute.
   * @param attrList The attribute to be indexed.
   * @param keys The set into which the keys will be inserted.
   */
  private void indexAttribute(List<Attribute> attrList,
                              Set<byte[]> keys)
  {
    if (attrList == null) return;

    for (Attribute attr : attrList)
    {
      for (AttributeValue value : attr)
      {
        try
        {
          byte[] keyBytes = orderingRule.normalizeValue(value.getValue())
              .toByteArray();

          keys.add(keyBytes);
        }
        catch (DirectoryException e)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
        }
      }
    }
  }


  /**
   * Generate the set of index keys for an attribute.
   * @param attrList The attribute to be indexed.
   * @param modifiedKeys The map into which the modified
   * keys will be inserted.
   * @param insert <code>true</code> if generated keys should
   * be inserted or <code>false</code> otherwise.
   */
  private void indexAttribute(List<Attribute> attrList,
                              Map<byte[], Boolean> modifiedKeys,
                              Boolean insert)
  {
    if (attrList == null) return;

    for (Attribute attr : attrList)
    {
      for (AttributeValue value : attr)
      {
        try
        {
          byte[] keyBytes =
               orderingRule.normalizeValue(value.getValue()).toByteArray();

          Boolean cInsert = modifiedKeys.get(keyBytes);
          if(cInsert == null)
          {
            modifiedKeys.put(keyBytes, insert);
          }
          else if(!cInsert.equals(insert))
          {
            modifiedKeys.remove(keyBytes);
          }
        }
        catch (DirectoryException e)
        {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
        }
      }
    }
  }
}
