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
 *      Copyright 2006-2009 Sun Microsystems, Inc.
 */
package org.opends.server.replication.plugin;

import static org.opends.messages.ReplicationMessages.*;
import static org.opends.server.loggers.ErrorLogger.logError;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.opends.messages.Message;
import org.opends.server.core.DirectoryServer;
import org.opends.server.replication.common.ChangeNumber;
import org.opends.server.replication.protocol.OperationContext;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeBuilder;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.Attributes;
import org.opends.server.types.Entry;
import org.opends.server.types.Modification;
import org.opends.server.types.ModificationType;
import org.opends.server.types.operation.PreOperationAddOperation;
import org.opends.server.types.operation.PreOperationModifyDNOperation;
import org.opends.server.types.operation.PreOperationModifyOperation;

/**
 * This class is used to store historical information that is
 * used to resolve modify conflicts
 *
 * It is assumed that the common case is not to have conflict and
 * therefore is optimized (in order of importance) for :
 *  1- detecting potential conflict
 *  2- fast update of historical information for non-conflicting change
 *  3- fast and efficient purge
 *  4- compact
 *  5- solve conflict. This should also be as fast as possible but
 *     not at the cost of any of the other previous objectives
 *
 * One Historical object is created for each entry in the entry cache
 * each Historical Object contains a list of attribute historical information
 */

public class Historical
{
  /**
   * The name of the attribute used to store historical information.
   */
  public static final String HISTORICALATTRIBUTENAME = "ds-sync-hist";

  /**
   * Name used to store attachment of historical information in the
   * operation.
   */
  public static final String HISTORICAL = "ds-synch-historical";

  /**
   * The name of the entryuuid attribute.
   */
  public static final String ENTRYUIDNAME = "entryuuid";


  /*
   * contains Historical information for each attribute sorted by attribute type
   */
  private HashMap<AttributeType,AttrInfoWithOptions> attributesInfo
                           = new HashMap<AttributeType,AttrInfoWithOptions>();

  // The date when the entry was added.
  private ChangeNumber ADDDate = null;

  // The date when the entry was last renamed.
  private ChangeNumber MODDNDate = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder builder = new StringBuilder();
    builder.append(encode());
    return builder.toString();
  }

  /**
   * Process an operation.
   * This method is responsible for detecting and resolving conflict for
   * modifyOperation. This is done by using the historical information.
   *
   * @param modifyOperation the operation to be processed
   * @param modifiedEntry the entry that is being modified (before modification)
   * @return true if the replayed operation was in conflict
   */
  public boolean replayOperation(PreOperationModifyOperation modifyOperation,
                                 Entry modifiedEntry)
  {
    boolean bConflict = false;
    List<Modification> mods = modifyOperation.getModifications();
    ChangeNumber changeNumber =
      OperationContext.getChangeNumber(modifyOperation);

    for (Iterator<Modification> modsIterator = mods.iterator();
         modsIterator.hasNext(); )
    {
      Modification m = modsIterator.next();

      AttributeInfo attrInfo = getAttrInfo(m);

      if (attrInfo.replayOperation(modsIterator, changeNumber,
                                   modifiedEntry, m))
      {
        bConflict = true;
      }
    }

    return bConflict;
  }

  /**
   * Append replacement of state information to a given modification.
   *
   * @param modifyOperation the modification.
   */
  public void generateState(PreOperationModifyOperation modifyOperation)
  {
    List<Modification> mods = modifyOperation.getModifications();
    Entry modifiedEntry = modifyOperation.getModifiedEntry();
    ChangeNumber changeNumber =
      OperationContext.getChangeNumber(modifyOperation);

    /*
     * If this is a local operation we need first to update the historical
     * information, then update the entry with the historical information
     * If this is a replicated operation the historical information has
     * already been set in the resolveConflict phase and we only need
     * to update the entry
     */
    if (!modifyOperation.isSynchronizationOperation())
    {
      for (Modification mod : mods)
      {
        AttributeInfo attrInfo = getAttrInfo(mod);
        if (attrInfo != null)
          attrInfo.processLocalOrNonConflictModification(changeNumber, mod);
      }
    }

    Attribute attr = encode();
    Modification mod = new Modification(ModificationType.REPLACE, attr);
    mods.add(mod);
    modifiedEntry.replaceAttribute(attr);
  }

  /**
     * Add historical information for a MODRDN operation to existing
     * historical information.
     *
     * @param modifyDNOperation the modification for which the historical
     *                          information should be created.
     */
  public void generateState(PreOperationModifyDNOperation modifyDNOperation)
  {
    // Update this historical information with the operation ChangeNumber.
    this.MODDNDate = OperationContext.getChangeNumber(modifyDNOperation);

    // Update the operations mods and the modified entry so that the
    // historical information gets stored in the DB and indexed accordingly.
    Entry modifiedEntry = modifyDNOperation.getUpdatedEntry();
    List<Modification> mods = modifyDNOperation.getModifications();

    Attribute attr = encode();
    Modification mod;
    mod = new Modification(ModificationType.REPLACE, attr);
    mods.add(mod);

    modifiedEntry.removeAttribute(attr.getAttributeType());
    modifiedEntry.addAttribute(attr, null);
  }

  /**
   * Generate and add to the Operation the historical information for
   * the ADD Operation.
   * This historical information will be used to generate fake operation
   * in case a Directory Server can not find a Replication Server with
   * all its changes at connection time.
   * This should only happen if a Directory Server or a Replication Server
   * crashes.
   *
   * @param addOperation     The Operation to process.
   */
  public static void generateState(PreOperationAddOperation addOperation)
  {
    AttributeType historicalAttrType =
      DirectoryServer.getSchema().getAttributeType(HISTORICALATTRIBUTENAME);

    Attribute attr =
      Attributes.create(historicalAttrType,
          encodeAddHistorical(OperationContext.getChangeNumber(addOperation)));

    List<Attribute> attrList = new LinkedList<Attribute>();
    attrList.add(attr);
    addOperation.setAttribute(historicalAttrType, attrList);
  }

  /**
   * Generate historical information for an ADD Operation.
   * This historical information will be used to generate fake operation
   * in case a Directory Server can not find a Replication Server with
   * all its changes at connection time.
   * This should only happen if a Directory Server or a Replication Server
   * crashes.
   *
   * @param cn     The date when the ADD Operation happened.
   * @return       The encoded historical information for the ADD Operation.
   */
  private static AttributeValue encodeAddHistorical(ChangeNumber cn)
  {
    AttributeType historicalAttrType =
      DirectoryServer.getSchema().getAttributeType(HISTORICALATTRIBUTENAME);

    String strValue = "dn:" + cn.toString() +":add";
    AttributeValue val = new AttributeValue(historicalAttrType, strValue);
    return val;
  }

  /**
   * Generate historical information for a MODDN Operation.
   * This historical information will be used to generate fake operation
   * in case a Directory Server can not find a Replication Server with
   * all its changes at connection time.
   * This should only happen if a Directory Server or a Replication Server
   * crashes.
   *
   * @param cn     The date when the MODDN Operation happened.
   * @return       The encoded historical information for the MODDN Operation.
   */
  private static AttributeValue encodeMODDNHistorical(ChangeNumber cn)
  {
    AttributeType historicalAttrType =
      DirectoryServer.getSchema().getAttributeType(HISTORICALATTRIBUTENAME);

    String strValue = "dn:" + cn.toString() +":moddn";
    AttributeValue val = new AttributeValue(historicalAttrType, strValue);
    return val;
  }

  /**
   * Get the AttrInfo for a given Modification.
   * The AttrInfo is the object that is used to store the historical
   * information of a given attribute type.
   * If there is no historical information for this attribute yet, a new
   * empty AttrInfo is created and returned.
   *
   * @param mod The Modification that must be used.
   * @return The AttrInfo corresponding to the given Modification.
   */
  private AttributeInfo getAttrInfo(Modification mod)
  {
    Attribute modAttr = mod.getAttribute();
    if (isHistoricalAttribute(modAttr))
    {
      // Don't keep historical information for the attribute that is
      // used to store the historical information.
      return null;
    }
    Set<String> options = modAttr.getOptions();
    AttributeType type = modAttr.getAttributeType();
    AttrInfoWithOptions attrInfoWithOptions =  attributesInfo.get(type);
    AttributeInfo attrInfo;
    if (attrInfoWithOptions != null)
    {
      attrInfo = attrInfoWithOptions.get(options);
    }
    else
    {
      attrInfoWithOptions = new AttrInfoWithOptions();
      attributesInfo.put(type, attrInfoWithOptions);
      attrInfo = null;
    }

    if (attrInfo == null)
    {
      attrInfo = AttributeInfo.createAttributeInfo(type);
      attrInfoWithOptions.put(options, attrInfo);
    }
    return attrInfo;
  }

  /**
   * Encode the historical information in an operational attribute.
   * @return The historical information encoded in an operational attribute.
   */
  public Attribute encode()
  {
    AttributeType historicalAttrType =
      DirectoryServer.getSchema().getAttributeType(HISTORICALATTRIBUTENAME);
    AttributeBuilder builder = new AttributeBuilder(historicalAttrType);

    // Encode the historical information for modify operation.
    for (Map.Entry<AttributeType, AttrInfoWithOptions> entryWithOptions :
                                                   attributesInfo.entrySet())
    {
      AttributeType type = entryWithOptions.getKey();
      HashMap<Set<String> , AttributeInfo> attrwithoptions =
                                entryWithOptions.getValue().getAttributesInfo();

      for (Map.Entry<Set<String>, AttributeInfo> entry :
           attrwithoptions.entrySet())
      {
        boolean delAttr = false;
        Set<String> options = entry.getKey();
        String optionsString = "";
        AttributeInfo info = entry.getValue();


        if (options != null)
        {
          StringBuilder optionsBuilder = new StringBuilder();
          for (String s : options)
          {
            optionsBuilder.append(';');
            optionsBuilder.append(s);
          }
          optionsString = optionsBuilder.toString();
        }

        ChangeNumber deleteTime = info.getDeleteTime();
        /* generate the historical information for deleted attributes */
        if (deleteTime != null)
        {
          delAttr = true;
        }

        /* generate the historical information for modified attribute values */
        for (ValueInfo valInfo : info.getValuesInfo())
        {
          String strValue;
          if (valInfo.getValueDeleteTime() != null)
          {
            strValue = type.getNormalizedPrimaryName() + optionsString + ":" +
            valInfo.getValueDeleteTime().toString() +
            ":del:" + valInfo.getValue().toString();
            AttributeValue val = new AttributeValue(historicalAttrType,
                                                    strValue);
            builder.add(val);
          }
          else if (valInfo.getValueUpdateTime() != null)
          {
            if ((delAttr && valInfo.getValueUpdateTime() == deleteTime)
               && (valInfo.getValue() != null))
            {
              strValue = type.getNormalizedPrimaryName() + optionsString + ":" +
              valInfo.getValueUpdateTime().toString() +  ":repl:" +
              valInfo.getValue().toString();
              delAttr = false;
            }
            else
            {
              if (valInfo.getValue() == null)
              {
                strValue = type.getNormalizedPrimaryName() + optionsString
                           + ":" + valInfo.getValueUpdateTime().toString() +
                           ":add";
              }
              else
              {
                strValue = type.getNormalizedPrimaryName() + optionsString
                           + ":" + valInfo.getValueUpdateTime().toString() +
                           ":add:" + valInfo.getValue().toString();
              }
            }

            AttributeValue val = new AttributeValue(historicalAttrType,
                                                    strValue);
            builder.add(val);
          }
        }

        if (delAttr)
        {
          String strValue = type.getNormalizedPrimaryName()
              + optionsString + ":" + deleteTime.toString()
              + ":attrDel";
          AttributeValue val = new AttributeValue(historicalAttrType, strValue);
          builder.add(val);
        }
      }
    }

    // Encode the historical information for the ADD Operation.
    if (ADDDate != null)
    {
      builder.add(encodeAddHistorical(ADDDate));
    }

    // Encode the historical information for the MODDN Operation.
    if (MODDNDate != null)
    {
      builder.add(encodeMODDNHistorical(MODDNDate));
    }

    return builder.toAttribute();
  }

  /**
   * Indicates if the Entry was renamed or added after the ChangeNumber
   * that is given as a parameter.
   *
   * @param cn The ChangeNumber with which the ADD or Rename date must be
   *           compared.
   *
   * @return A boolean indicating if the Entry was renamed or added after
   *                   the ChangeNumber that is given as a parameter.
   */
  public boolean AddedOrRenamedAfter(ChangeNumber cn)
  {
    if (cn.older(ADDDate) || cn.older(MODDNDate))
    {
      return true;
    }
    else
      return false;
  }


  /**
   * read the historical information from the entry attribute and
   * load it into the Historical object attached to the entry.
   * @param entry The entry which historical information must be loaded
   * @return the generated Historical information
   */
  public static Historical load(Entry entry)
  {
    List<Attribute> hist = getHistoricalAttr(entry);
    Historical histObj = new Historical();
    AttributeType lastAttrType = null;
    Set<String> lastOptions = new HashSet<String>();
    AttributeInfo attrInfo = null;
    AttrInfoWithOptions attrInfoWithOptions = null;

    if (hist == null)
    {
      return histObj;
    }

    try
    {
      for (Attribute attr : hist)
      {
        for (AttributeValue val : attr)
        {
          HistVal histVal = new HistVal(val.getStringValue());
          AttributeType attrType = histVal.getAttrType();
          Set<String> options = histVal.getOptions();
          ChangeNumber cn = histVal.getCn();
          AttributeValue value = histVal.getAttributeValue();
          HistKey histKey = histVal.getHistKey();

          if (histVal.isADDOperation())
          {
            histObj.ADDDate = cn;
          }
          else if (histVal.isMODDNOperation())
          {
            histObj.MODDNDate = cn;
          }
          else
          {
            if (attrType == null)
            {
              /*
               * This attribute is unknown from the schema
               * Just skip it, the modification will be processed but no
               * historical information is going to be kept.
               * Log information for the repair tool.
               */
              Message message = ERR_UNKNOWN_ATTRIBUTE_IN_HISTORICAL.get(
                  entry.getDN().toNormalizedString(), histVal.getAttrString());
              logError(message);
              continue;
            }

            /* if attribute type does not match we create new
             *   AttrInfoWithOptions and AttrInfo
             *   we also add old AttrInfoWithOptions into histObj.attributesInfo
             * if attribute type match but options does not match we create new
             *   AttrInfo that we add to AttrInfoWithOptions
             * if both match we keep everything
             */
            if (attrType != lastAttrType)
            {
              attrInfo = AttributeInfo.createAttributeInfo(attrType);
              attrInfoWithOptions = new AttrInfoWithOptions();
              attrInfoWithOptions.put(options, attrInfo);
              histObj.attributesInfo.put(attrType, attrInfoWithOptions);

              lastAttrType = attrType;
              lastOptions = options;
            }
            else
            {
              if (!options.equals(lastOptions))
              {
                attrInfo = AttributeInfo.createAttributeInfo(attrType);
                attrInfoWithOptions.put(options, attrInfo);
                lastOptions = options;
              }
            }

            attrInfo.load(histKey, value, cn);
          }
        }
      }
    } catch (Exception e)
    {
      // Any exception happening here means that the coding of the historical
      // information was wrong.
      // Log an error and continue with an empty historical.
      Message message = ERR_BAD_HISTORICAL.get(entry.getDN().toString());
      logError(message);
    }

    /* set the reference to the historical information in the entry */
    return histObj;
  }


  /**
   * Use this historical information to generate fake operations that would
   * result in this historical information.
   * TODO : This is only implemented for MODIFY, MODRDN and  ADD
   *        need to complete with DELETE.
   * @param entry The Entry to use to generate the FakeOperation Iterable.
   *
   * @return an Iterable of FakeOperation that would result in this historical
   *         information.
   */
  public static Iterable<FakeOperation> generateFakeOperations(Entry entry)
  {
    TreeMap<ChangeNumber, FakeOperation> operations =
            new TreeMap<ChangeNumber, FakeOperation>();
    List<Attribute> attrs = getHistoricalAttr(entry);
    if (attrs != null)
    {
      for (Attribute attr : attrs)
      {
        for (AttributeValue val : attr)
        {
          HistVal histVal = new HistVal(val.getStringValue());
          if (histVal.isADDOperation())
          {
            // Found some historical information indicating that this
            // entry was just added.
            // Create the corresponding ADD operation.
            operations.put(histVal.getCn(),
                new FakeAddOperation(histVal.getCn(), entry));
          }
          else if (histVal.isMODDNOperation())
          {
            // Found some historical information indicating that this
            // entry was just renamed.
            // Create the corresponding ADD operation.
            operations.put(histVal.getCn(),
                new FakeModdnOperation(histVal.getCn(), entry));
          }
          else
          {
            // Found some historical information for modify operation.
            // Generate the corresponding ModifyOperation or update
            // the already generated Operation if it can be found.
            ChangeNumber cn = histVal.getCn();
            Modification mod = histVal.generateMod();
            FakeModifyOperation modifyFakeOperation;
            FakeOperation fakeOperation = operations.get(cn);

            if ((fakeOperation != null) &&
                      (fakeOperation instanceof FakeModifyOperation))
            {
              modifyFakeOperation = (FakeModifyOperation) fakeOperation;
              modifyFakeOperation.addModification(mod);
            }
            else
            {
              String uuidString = getEntryUuid(entry);
              if (uuidString != null)
              {
                modifyFakeOperation = new FakeModifyOperation(entry.getDN(),
                    cn, uuidString);

                modifyFakeOperation.addModification(mod);
                operations.put(histVal.getCn(), modifyFakeOperation);
              }
            }
          }
        }
      }
    }
    return operations.values();
  }

  /**
   * Get the Attribute used to store the historical information from
   * the given Entry.
   *
   * @param   entry  The entry containing the historical information.
   *
   * @return  The Attribute used to store the historical information.
   */
  public static List<Attribute> getHistoricalAttr(Entry entry)
  {
    return entry.getAttribute(HISTORICALATTRIBUTENAME);
  }

  /**
   * Get the entry unique Id in String form.
   *
   * @param entry The entry for which the unique id should be returned.
   *
   * @return The Unique Id of the entry if it has one. null, otherwise.
   */
  public static String getEntryUuid(Entry entry)
  {
    String uuidString = null;
    AttributeType entryuuidAttrType =
      DirectoryServer.getSchema().getAttributeType(ENTRYUIDNAME);
    List<Attribute> uuidAttrs =
             entry.getOperationalAttribute(entryuuidAttrType);
    if (uuidAttrs != null)
    {
      Attribute uuid = uuidAttrs.get(0);
      if (!uuid.isEmpty())
      {
        AttributeValue uuidVal = uuid.iterator().next();
        uuidString =  uuidVal.getStringValue();
      }
    }
    return uuidString;
  }

  /**
   * Get the Entry Unique Id from an add operation.
   * This must be called after the entry uuid preop plugin (i.e no
   * sooner than the replication provider pre-op)
   *
   * @param op The operation
   * @return The Entry Unique Id String form.
   */
  public static String getEntryUuid(PreOperationAddOperation op)
  {
    String uuidString = null;
    Map<AttributeType, List<Attribute>> attrs = op.getOperationalAttributes();
    AttributeType entryuuidAttrType =
      DirectoryServer.getSchema().getAttributeType(ENTRYUIDNAME);
    List<Attribute> uuidAttrs = attrs.get(entryuuidAttrType);

    if (uuidAttrs != null)
    {
      Attribute uuid = uuidAttrs.get(0);
      if (!uuid.isEmpty())
      {
        AttributeValue uuidVal = uuid.iterator().next();
        uuidString =  uuidVal.getStringValue();
      }
    }
    return uuidString;
  }

  /**
   * Check if a given attribute is an attribute used to store historical
   * information.
   *
   * @param   attr The attribute that needs to be checked.
   *
   * @return  a boolean indicating if the given attribute is
   *          used to store historical information.
   */
  public static boolean isHistoricalAttribute(Attribute attr)
  {
    AttributeType attrType = attr.getAttributeType();
    return attrType.getNameOrOID().equals(Historical.HISTORICALATTRIBUTENAME);
  }
}

