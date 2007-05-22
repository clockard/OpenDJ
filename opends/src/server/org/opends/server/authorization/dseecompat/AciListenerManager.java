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
 *      Portions Copyright 2007 Sun Microsystems, Inc.
 */

package org.opends.server.authorization.dseecompat;

import org.opends.server.api.ChangeNotificationListener;
import org.opends.server.api.BackendInitializationListener;
import org.opends.server.api.Backend;
import org.opends.server.types.operation.PostResponseAddOperation;
import org.opends.server.types.operation.PostResponseDeleteOperation;
import org.opends.server.types.operation.PostResponseModifyOperation;
import org.opends.server.types.operation.PostResponseModifyDNOperation;
import org.opends.server.protocols.internal.InternalClientConnection;
import org.opends.server.protocols.internal.InternalSearchOperation;
import static org.opends.server.loggers.ErrorLogger.logError;
import static org.opends.server.loggers.debug.DebugLogger.*;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.types.*;
import static org.opends.server.messages.AciMessages.*;
import static org.opends.server.messages.MessageHandler.getMessage;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The AciListenerManager updates an ACI list after each
 * modification operation. Also, updates ACI list when backends are initialized
 * and finalized.
 */
public class AciListenerManager
        implements ChangeNotificationListener, BackendInitializationListener {
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();


    /*
     * The AciList caches the ACIs.
     */
    private AciList aciList;

    /*
     * Search filter used in context search for "aci" attribute types.
     */
    private static SearchFilter aciFilter;

    /*
     * The aci attribute type is operational so we need to specify it to be
     * returned.
     */
    private static LinkedHashSet<String> attrs = new LinkedHashSet<String>();

    static {
        /*
         * Set up the filter used to search private and public contexts.
         */
        try {
            aciFilter=SearchFilter.createFilterFromString("(aci=*)");
        } catch (DirectoryException ex) {
            //TODO should never happen, error message?
        }
        attrs.add("aci");
    }

    /**
     * Save the list created by the AciHandler routine.
     * @param aciList The list object created and loaded by the handler.
     */
    public AciListenerManager(AciList aciList) {
        this.aciList=aciList;
    }

    /**
     * A delete operation succeeded. Remove any ACIs associated with the
     * entry deleted.
     * @param deleteOperation The delete operation.
     * @param entry The entry being deleted.
     */
    public void handleDeleteOperation(PostResponseDeleteOperation
            deleteOperation, Entry entry) {
        boolean hasAci,  hasGlobalAci=false;
        //This entry might have both global and aci attribute types.
        if((hasAci=entry.hasOperationalAttribute(AciHandler.aciType)) ||
                (hasGlobalAci=entry.hasAttribute(AciHandler.globalAciType)))
            aciList.removeAci(entry, hasAci, hasGlobalAci);
    }

    /**
     * An Add operation succeeded. Add any ACIs associated with the
     * entry being added.
     * @param addOperation  The add operation.
     * @param entry   The entry being added.
     */
    public void handleAddOperation(PostResponseAddOperation addOperation,
                                   Entry entry) {
        boolean hasAci, hasGlobalAci=false;
        //This entry might have both global and aci attribute types.
        if((hasAci=entry.hasOperationalAttribute(AciHandler.aciType)) ||
                (hasGlobalAci=entry.hasAttribute(AciHandler.globalAciType)))
            aciList.addAci(entry, hasAci, hasGlobalAci);
    }

    /**
     * A modify operation succeeded. Adjust the ACIs by removing
     * ACIs based on the oldEntry and then adding ACIs based on the new
     * entry.
     * @param modOperation  the modify operation.
     * @param oldEntry The old entry to examine.
     * @param newEntry  The new entry to examine.
     */
    public void handleModifyOperation(PostResponseModifyOperation modOperation,
                                      Entry oldEntry, Entry newEntry)
    {
        // A change to the ACI list is expensive so let's first make sure that
        // the modification included changes to the ACI. We'll check for
        //both "aci" attribute types and global "ds-cfg-global-aci" attribute
        //types.
        boolean hasAci = false, hasGlobalAci=false;
        List<Modification> mods = modOperation.getModifications();
        for (Modification mod : mods) {
            AttributeType attributeType=mod.getAttribute().getAttributeType();
            if (attributeType.equals(AciHandler.aciType))
                hasAci = true;
           else if(attributeType.equals(AciHandler.globalAciType))
                hasGlobalAci=true;
            if(hasAci && hasGlobalAci)
               break;
        }
        if (hasAci || hasGlobalAci)
            aciList.modAciOldNewEntry(oldEntry, newEntry, hasAci, hasGlobalAci);
    }

    /**
     * A modify DN operation has succeeded. Adjust the ACIs by moving ACIs
     * under the old entry DN to the new entry DN.
     * @param modifyDNOperation  The LDAP modify DN operation.
     * @param oldEntry  The old entry.
     * @param newEntry The new entry.
     */
    public void handleModifyDNOperation(
            PostResponseModifyDNOperation modifyDNOperation,
            Entry oldEntry, Entry newEntry)
    {
        aciList.renameAci(oldEntry.getDN(), newEntry.getDN());
    }

    /**
     * {@inheritDoc}  In this case, the server will search the backend to find
     * all aci attribute type values that it may contain and add them to the
     * ACI list.
     */
    public void performBackendInitializationProcessing(Backend backend) {
      InternalClientConnection conn =
           InternalClientConnection.getRootConnection();
      for (DN baseDN : backend.getBaseDNs()) {
        try {
          if (! backend.entryExists(baseDN))  {
            continue;
          }
        } catch (Exception e) {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
          //TODO log message
          continue;
        }
        InternalSearchOperation internalSearch =
             new InternalSearchOperation(
                  conn,
                  InternalClientConnection.nextOperationID(),
                  InternalClientConnection.nextMessageID(),
                  null, baseDN, SearchScope.WHOLE_SUBTREE,
                  DereferencePolicy.NEVER_DEREF_ALIASES,
                  0, 0, false, aciFilter, attrs, null);
        try  {
          backend.search(internalSearch);
        } catch (Exception e) {
          if (debugEnabled())
          {
            TRACER.debugCaught(DebugLogLevel.ERROR, e);
          }
          //TODO log message
          continue;
        }
        if(internalSearch.getSearchEntries().isEmpty()) {
          int    msgID  = MSGID_ACI_ADD_LIST_NO_ACIS;
          String message = getMessage(msgID, String.valueOf(baseDN));
          logError(ErrorLogCategory.ACCESS_CONTROL,
                   ErrorLogSeverity.INFORMATIONAL, message, msgID);
        } else {
          int validAcis = aciList.addAci(
               internalSearch.getSearchEntries());
          int    msgID  = MSGID_ACI_ADD_LIST_ACIS;
          String message = getMessage(msgID, Integer.toString(validAcis),
                                      String.valueOf(baseDN));
          logError(ErrorLogCategory.ACCESS_CONTROL,
                   ErrorLogSeverity.INFORMATIONAL,
                   message, msgID);
        }
      }
    }

    /**
     * {@inheritDoc}  In this case, the server will remove all aci attribute
     * type values associated with entries in the provided backend.
     */
    public void performBackendFinalizationProcessing(Backend backend) {
        aciList.removeAci(backend);
    }
}
