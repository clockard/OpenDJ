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
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */
package org.opends.server.core;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opends.server.api.Backend;
import org.opends.server.api.ClientConnection;
import org.opends.server.api.plugin.PostOperationPluginResult;
import org.opends.server.api.plugin.PreOperationPluginResult;
import org.opends.server.api.plugin.PreParsePluginResult;
import org.opends.server.api.plugin.SearchEntryPluginResult;
import org.opends.server.api.plugin.SearchReferencePluginResult;
import org.opends.server.controls.AccountUsableResponseControl;
import org.opends.server.controls.LDAPAssertionRequestControl;
import org.opends.server.controls.MatchedValuesControl;
import org.opends.server.controls.PersistentSearchControl;
import org.opends.server.controls.ProxiedAuthV1Control;
import org.opends.server.controls.ProxiedAuthV2Control;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.protocols.ldap.LDAPException;
import org.opends.server.protocols.ldap.LDAPFilter;
import org.opends.server.types.Attribute;
import org.opends.server.types.AttributeType;
import org.opends.server.types.AttributeValue;
import org.opends.server.types.ByteString;
import org.opends.server.types.Control;
import org.opends.server.types.DereferencePolicy;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.ResultCode;
import org.opends.server.types.SearchFilter;
import org.opends.server.types.SearchResultEntry;
import org.opends.server.types.SearchResultReference;
import org.opends.server.types.SearchScope;
import org.opends.server.util.TimeThread;

import static org.opends.server.core.CoreConstants.*;
import static org.opends.server.loggers.Access.*;
import static org.opends.server.loggers.Debug.*;
import static org.opends.server.messages.CoreMessages.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines an operation that may be used to locate entries in the
 * Directory Server based on a given set of criteria.
 */
public class SearchOperation
       extends Operation
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.core.SearchOperation";



  // Indicates whether a search result done response has been sent to the
  // client.
  private AtomicBoolean responseSent;

  // Indicates whether the client is able to handle referrals.
  private boolean clientAcceptsReferrals;

  // Indicates whether to include the account usable control with search result
  // entries.
  private boolean includeUsableControl;

  // Indicates whether LDAP subentries should be returned.
  private boolean returnLDAPSubentries;

  // Indicates whether to include attribute types only or both types and values.
  private boolean typesOnly;

  // The raw, unprocessed base DN as included in the request from the client.
  private ByteString rawBaseDN;

  // The cancel request that has been issued for this search operation.
  private CancelRequest cancelRequest;

  // The dereferencing policy for the search operation.
  private DereferencePolicy derefPolicy;

  // The base DN for the search operation.
  private DN baseDN;

  // The number of entries that have been sent to the client.
  private int entriesSent;

  // The number of search result references that have been sent to the client.
  private int referencesSent;

  // The size limit for the search operation.
  private int sizeLimit;

  // The time limit for the search operation.
  private int timeLimit;

  // The raw, unprocessed filter as included in the request from the client.
  private LDAPFilter rawFilter;

  // The set of attributes that should be returned in matching entries.
  private LinkedHashSet<String> attributes;

  // The set of response controls for this search operation.
  private List<Control> responseControls;

  // The time that processing started on this operation.
  private long processingStartTime;

  // The time that processing ended on this operation.
  private long processingStopTime;

  // The time that the search time limit has expired.
  private long timeLimitExpiration;

  // The matched values control associated with this search operation.
  private MatchedValuesControl matchedValuesControl;

  // The persistent search associated with this search operation.
  private PersistentSearch persistentSearch;

  // The search filter for the search operation.
  private SearchFilter filter;

  // The search scope for the search operation.
  private SearchScope scope;



  /**
   * Creates a new search operation with the provided information.
   *
   * @param  clientConnection  The client connection with which this operation
   *                           is associated.
   * @param  operationID       The operation ID for this operation.
   * @param  messageID         The message ID of the request with which this
   *                           operation is associated.
   * @param  requestControls   The set of controls included in the request.
   * @param  rawBaseDN         The raw, unprocessed base DN as included in the
   *                           request from the client.
   * @param  scope             The scope for this search operation.
   * @param  derefPolicy       The alias dereferencing policy for this search
   *                           operation.
   * @param  sizeLimit         The size limit for this search operation.
   * @param  timeLimit         The time limit for this search operation.
   * @param  typesOnly         The typesOnly flag for this search operation.
   * @param  rawFilter         the raw, unprocessed filter as included in the
   *                           request from the client.
   * @param  attributes        The requested attributes for this search
   *                           operation.
   */
  public SearchOperation(ClientConnection clientConnection, long operationID,
                         int messageID, List<Control> requestControls,
                         ByteString rawBaseDN, SearchScope scope,
                         DereferencePolicy derefPolicy, int sizeLimit,
                         int timeLimit, boolean typesOnly, LDAPFilter rawFilter,
                         LinkedHashSet<String> attributes)
  {
    super(clientConnection, operationID, messageID, requestControls);

    assert debugConstructor(CLASS_NAME,
                            new String[]
                            {
                              String.valueOf(clientConnection),
                              String.valueOf(operationID),
                              String.valueOf(messageID),
                              String.valueOf(requestControls),
                              String.valueOf(rawBaseDN),
                              String.valueOf(scope),
                              String.valueOf(derefPolicy),
                              String.valueOf(sizeLimit),
                              String.valueOf(timeLimit),
                              String.valueOf(typesOnly),
                              String.valueOf(rawFilter),
                              String.valueOf(attributes)
                            });

    this.rawBaseDN   = rawBaseDN;
    this.scope       = scope;
    this.derefPolicy = derefPolicy;
    this.sizeLimit   = sizeLimit;
    this.timeLimit   = timeLimit;
    this.typesOnly   = typesOnly;
    this.rawFilter   = rawFilter;
    this.attributes  = attributes;


    if (clientConnection.getSizeLimit() <= 0)
    {
      this.sizeLimit = sizeLimit;
    }
    else
    {
      if (sizeLimit <= 0)
      {
        this.sizeLimit = clientConnection.getSizeLimit();
      }
      else
      {
        this.sizeLimit = Math.min(sizeLimit, clientConnection.getSizeLimit());
      }
    }


    if (clientConnection.getTimeLimit() <= 0)
    {
      this.timeLimit = timeLimit;
    }
    else
    {
      if (timeLimit <= 0)
      {
        this.timeLimit = clientConnection.getTimeLimit();
      }
      else
      {
        this.timeLimit = Math.min(timeLimit, clientConnection.getTimeLimit());
      }
    }


    baseDN                 = null;
    filter                 = null;
    entriesSent            = 0;
    referencesSent         = 0;
    responseControls       = new ArrayList<Control>();
    cancelRequest          = null;
    clientAcceptsReferrals = true;
    includeUsableControl   = false;
    responseSent           = new AtomicBoolean(false);
    persistentSearch       = null;
    returnLDAPSubentries   = false;
    matchedValuesControl   = null;
  }



  /**
   * Creates a new search operation with the provided information.
   *
   * @param  clientConnection  The client connection with which this operation
   *                           is associated.
   * @param  operationID       The operation ID for this operation.
   * @param  messageID         The message ID of the request with which this
   *                           operation is associated.
   * @param  requestControls   The set of controls included in the request.
   * @param  baseDN            The base DN for this search operation.
   * @param  scope             The scope for this search operation.
   * @param  derefPolicy       The alias dereferencing policy for this search
   *                           operation.
   * @param  sizeLimit         The size limit for this search operation.
   * @param  timeLimit         The time limit for this search operation.
   * @param  typesOnly         The typesOnly flag for this search operation.
   * @param  filter            The filter for this search operation.
   * @param  attributes        The attributes for this search operation.
   */
  public SearchOperation(ClientConnection clientConnection, long operationID,
                         int messageID, List<Control> requestControls,
                         DN baseDN, SearchScope scope,
                         DereferencePolicy derefPolicy, int sizeLimit,
                         int timeLimit, boolean typesOnly, SearchFilter filter,
                         LinkedHashSet<String> attributes)
  {
    super(clientConnection, operationID, messageID, requestControls);

    assert debugConstructor(CLASS_NAME,
                            new String[]
                            {
                              String.valueOf(clientConnection),
                              String.valueOf(operationID),
                              String.valueOf(messageID),
                              String.valueOf(requestControls),
                              String.valueOf(baseDN),
                              String.valueOf(scope),
                              String.valueOf(derefPolicy),
                              String.valueOf(sizeLimit),
                              String.valueOf(timeLimit),
                              String.valueOf(typesOnly),
                              String.valueOf(filter),
                              String.valueOf(attributes)
                            });

    this.baseDN      = baseDN;
    this.scope       = scope;
    this.derefPolicy = derefPolicy;
    this.sizeLimit   = sizeLimit;
    this.timeLimit   = timeLimit;
    this.typesOnly   = typesOnly;
    this.filter      = filter;
    this.attributes  = attributes;

    rawBaseDN = new ASN1OctetString(baseDN.toString());
    rawFilter = new LDAPFilter(filter);


    if (clientConnection.getSizeLimit() <= 0)
    {
      this.sizeLimit = sizeLimit;
    }
    else
    {
      if (sizeLimit <= 0)
      {
        this.sizeLimit = clientConnection.getSizeLimit();
      }
      else
      {
        this.sizeLimit = Math.min(sizeLimit, clientConnection.getSizeLimit());
      }
    }


    if (clientConnection.getTimeLimit() <= 0)
    {
      this.timeLimit = timeLimit;
    }
    else
    {
      if (timeLimit <= 0)
      {
        this.timeLimit = clientConnection.getTimeLimit();
      }
      else
      {
        this.timeLimit = Math.min(timeLimit, clientConnection.getTimeLimit());
      }
    }


    entriesSent            = 0;
    referencesSent         = 0;
    responseControls       = new ArrayList<Control>();
    cancelRequest          = null;
    clientAcceptsReferrals = true;
    includeUsableControl   = false;
    responseSent           = new AtomicBoolean(false);
    persistentSearch       = null;
    returnLDAPSubentries   = false;
    matchedValuesControl   = null;
  }



  /**
   * Retrieves the raw, unprocessed base DN as included in the request from the
   * client.  This may or may not contain a valid DN, as no validation will have
   * been performed.
   *
   * @return  The raw, unprocessed base DN as included in the request from the
   *          client.
   */
  public ByteString getRawBaseDN()
  {
    assert debugEnter(CLASS_NAME, "getRawBaseDN");

    return rawBaseDN;
  }



  /**
   * Specifies the raw, unprocessed base DN as included in the request from the
   * client.  This method should only be called by pre-parse plugins.  Any other
   * code that wishes to alter the base DN should use the <CODE>setBaseDN</CODE>
   * method instead.
   *
   * @param  rawBaseDN  The raw, unprocessed base DN as included in the request
   *                    from the client.
   */
  public void setRawBaseDN(ByteString rawBaseDN)
  {
    assert debugEnter(CLASS_NAME, "setRawBaseDN", String.valueOf(rawBaseDN));

    this.rawBaseDN = rawBaseDN;

    baseDN = null;
  }



  /**
   * Retrieves the base DN for this search operation.  This should not be called
   * by pre-parse plugins, as the raw base DN will not yet have been processed.
   * Instead, they should use the <CODE>getRawBaseDN</CODE> method.
   *
   * @return  The base DN for this search operation, or <CODE>null</CODE> if the
   *          raw base DN has not yet been processed.
   */
  public DN getBaseDN()
  {
    assert debugEnter(CLASS_NAME, "getBaseDN");

    return baseDN;
  }



  /**
   * Specifies the base DN for this search operation.  This should not be called
   * by pre-parse plugins, which should use the <CODE>setRawBaseDN</CODE> method
   * instead.
   *
   * @param  baseDN  The base DN for this search operation.
   */
  public void setBaseDN(DN baseDN)
  {
    assert debugEnter(CLASS_NAME, "setBaseDN", String.valueOf(baseDN));

    this.baseDN = baseDN;
  }



  /**
   * Retrieves the scope for this search operation.
   *
   * @return  The scope for this search operation.
   */
  public SearchScope getScope()
  {
    assert debugEnter(CLASS_NAME, "getScope");

    return scope;
  }



  /**
   * Specifies the scope for this search operation.
   *
   * @param  scope  The scope for this search operation.
   */
  public void setScope(SearchScope scope)
  {
    assert debugEnter(CLASS_NAME, "setScope", String.valueOf(scope));

    this.scope = scope;
  }



  /**
   * Retrieves the alias dereferencing policy for this search operation.
   *
   * @return  The alias dereferencing policy for this search operation.
   */
  public DereferencePolicy getDerefPolicy()
  {
    assert debugEnter(CLASS_NAME, "getDerefPolicy");

    return derefPolicy;
  }



  /**
   * Specifies the alias dereferencing policy for this search operation.
   *
   * @param  derefPolicy  The alias dereferencing policy for this search
   *                      operation.
   */
  public void setDerefPolicy(DereferencePolicy derefPolicy)
  {
    assert debugEnter(CLASS_NAME, "setDerefPolicy",
                      String.valueOf(derefPolicy));

    this.derefPolicy = derefPolicy;
  }



  /**
   * Retrieves the size limit for this search operation.
   *
   * @return  The size limit for this search operation.
   */
  public int getSizeLimit()
  {
    assert debugEnter(CLASS_NAME, "getSizeLimit");

    return sizeLimit;
  }



  /**
   * Specifies the size limit for this search operation.
   *
   * @param  sizeLimit  The size limit for this search operation.
   */
  public void setSizeLimit(int sizeLimit)
  {
    assert debugEnter(CLASS_NAME, "setSizeLimit", String.valueOf(sizeLimit));

    this.sizeLimit = sizeLimit;
  }



  /**
   * Retrieves the time limit for this search operation.
   *
   * @return  The time limit for this search operation.
   */
  public int getTimeLimit()
  {
    assert debugEnter(CLASS_NAME, "getTimeLimit");

    return timeLimit;
  }



  /**
   * Specifies the time limit for this search operation.
   *
   * @param  timeLimit  The time limit for this search operation.
   */
  public void setTimeLimit(int timeLimit)
  {
    assert debugEnter(CLASS_NAME, "setTimeLimit", String.valueOf(timeLimit));

    this.timeLimit = timeLimit;
  }



  /**
   * Retrieves the typesOnly flag for this search operation.
   *
   * @return  The typesOnly flag for this search operation.
   */
  public boolean getTypesOnly()
  {
    assert debugEnter(CLASS_NAME, "getTypesOnly");

    return typesOnly;
  }



  /**
   * Specifies the typesOnly flag for this search operation.
   *
   * @param  typesOnly  The typesOnly flag for this search operation.
   */
  public void setTypesOnly(boolean typesOnly)
  {
    assert debugEnter(CLASS_NAME, "setTypesOnly", String.valueOf(typesOnly));

    this.typesOnly = typesOnly;
  }



  /**
   * Retrieves the raw, unprocessed search filter as included in the request
   * from the client.  It may or may not contain a valid filter (e.g.,
   * unsupported attribute types or values with an invalid syntax) because no
   * validation will have been performed on it.
   *
   * @return  The raw, unprocessed search filter as included in the request from
   *          the client.
   */
  public LDAPFilter getRawFilter()
  {
    assert debugEnter(CLASS_NAME, "getRawFilter");

    return rawFilter;
  }



  /**
   * Specifies the raw, unprocessed search filter as included in the request
   * from the client.  This method should only be called by pre-parse plugins.
   * All later processing that wishes to change the filter should use the
   * <CODE>setFilter</CODE> method instead.
   *
   * @param  rawFilter  The raw, unprocessed search filter as included in the
   *                    request from the client.
   */
  public void setRawFilter(LDAPFilter rawFilter)
  {
    assert debugEnter(CLASS_NAME, "setRawFilter", String.valueOf(rawFilter));

    this.rawFilter = rawFilter;

    filter = null;
  }



  /**
   * Retrieves the filter for this search operation.  This should not be called
   * by pre-parse plugins, because the raw filter will not yet have been
   * processed.  Instead, they should use the <CODE>getRawFilter</CODE> method.
   *
   * @return  The filter for this search operation, or <CODE>null</CODE> if the
   *          raw filter has not yet been processed.
   */
  public SearchFilter getFilter()
  {
    assert debugEnter(CLASS_NAME, "getFilter");

    return filter;
  }



  /**
   * Specifies the filter for this search operation.  This should not be called
   * by pre-parse plugins, which should instead use the
   * <CODE>setRawFilter</CODE> method.
   *
   * @param  filter  The filter for this search operation.
   */
  public void setFilter(SearchFilter filter)
  {
    assert debugEnter(CLASS_NAME, "setFilter", String.valueOf(filter));

    this.filter = filter;
  }



  /**
   * Retrieves the set of requested attributes for this search operation.  Its
   * contents may be altered in pre-parse or pre-operation plugins.
   *
   * @return  The set of requested attributes for this search operation.
   */
  public LinkedHashSet<String> getAttributes()
  {
    assert debugEnter(CLASS_NAME, "getAttributes");

    return attributes;
  }



  /**
   * Retrieves the number of entries sent to the client for this search
   * operation.
   *
   * @return  The number of entries sent to the client for this search
   *          operation.
   */
  public int getEntriesSent()
  {
    assert debugEnter(CLASS_NAME, "getEntriesSent");

    return entriesSent;
  }



  /**
   * Retrieves the number of search references sent to the client for this
   * search operation.
   *
   * @return  The number of search references sent to the client for this search
   *          operation.
   */
  public int getReferencesSent()
  {
    assert debugEnter(CLASS_NAME, "getReferencesSent");

    return referencesSent;
  }



  /**
   * Retrieves the time that processing started for this operation.
   *
   * @return  The time that processing started for this operation.
   */
  public long getProcessingStartTime()
  {
    assert debugEnter(CLASS_NAME, "getProcessingStartTime");

    return processingStartTime;
  }



  /**
   * Retrieves the time that processing stopped for this operation.  This will
   * actually hold a time immediately before the response was sent to the
   * client.
   *
   * @return  The time that processing stopped for this operation.
   */
  public long getProcessingStopTime()
  {
    assert debugEnter(CLASS_NAME, "getProcessingStopTime");

    return processingStopTime;
  }



  /**
   * Retrieves the length of time in milliseconds that the server spent
   * processing this operation.  This should not be called until after the
   * server has sent the response to the client.
   *
   * @return  The length of time in milliseconds that the server spent
   *          processing this operation.
   */
  public long getProcessingTime()
  {
    assert debugEnter(CLASS_NAME, "getProcessingTime");

    return (processingStopTime - processingStartTime);
  }



  /**
   * Used as a callback for backends to indicate that the provided entry matches
   * the search criteria and that additional processing should be performed to
   * potentially send it back to the client.
   *
   * @param  entry     The entry that matches the search criteria and should be
   *                   sent to the client.
   * @param  controls  The set of controls to include with the entry (may be
   *                   <CODE>null</CODE> if none are needed).
   *
   * @return  <CODE>true</CODE> if the caller should continue processing the
   *          search request and sending additional entries and references, or
   *          <CODE>false</CODE> if not for some reason (e.g., the size limit
   *          has been reached or the search has been abandoned).
   */
  public boolean returnEntry(Entry entry, List<Control> controls)
  {
    assert debugEnter(CLASS_NAME, "returnEntry", String.valueOf(entry));


    // See if the operation has been abandoned.  If so, then don't send the
    // entry and indicate that the search should end.
    if (cancelRequest != null)
    {
      setResultCode(ResultCode.CANCELED);
      return false;
    }


    // See if the size limit has been exceeded.  If so, then don't send the
    // entry and indicate that the search should end.
    if ((sizeLimit > 0) && (entriesSent >= sizeLimit))
    {
      setResultCode(ResultCode.SIZE_LIMIT_EXCEEDED);
      appendErrorMessage(getMessage(MSGID_SEARCH_SIZE_LIMIT_EXCEEDED,
                                    sizeLimit));
      return false;
    }


    // See if the time limit has expired.  If so, then don't send the entry and
    // indicate that the search should end.
    if ((timeLimit > 0) && (TimeThread.getTime() >= timeLimitExpiration))
    {
      setResultCode(ResultCode.TIME_LIMIT_EXCEEDED);
      appendErrorMessage(getMessage(MSGID_SEARCH_TIME_LIMIT_EXCEEDED,
                                    timeLimit));
      return false;
    }


    // Determine whether the provided entry is a subentry and if so whether it
    // should be returned.
    if ((scope != SearchScope.BASE_OBJECT) && (! returnLDAPSubentries) &&
        entry.isLDAPSubentry())
    {
      // This is a subentry and we should not return it to the client.  Just
      // throw it away without doing anything.
      return true;
    }


    // Determine whether to include the account usable control.  If so, then
    // create it now.
    if (includeUsableControl)
    {
      try
      {
        // FIXME -- Need a way to enable PWP debugging.
        PasswordPolicyState pwpState = new PasswordPolicyState(entry, false,
                                                               false);

        boolean isInactive           = pwpState.isDisabled() ||
                                       pwpState.isAccountExpired();
        boolean isLocked             = pwpState.lockedDueToFailures() ||
                                       pwpState.lockedDueToMaximumResetAge() ||
                                       pwpState.lockedDueToIdleInterval();
        boolean isReset              = pwpState.mustChangePassword();
        boolean isExpired            = pwpState.isPasswordExpired();

        if (isInactive || isLocked || isReset || isExpired)
        {
          int secondsBeforeUnlock  = pwpState.getSecondsUntilUnlock();
          int remainingGraceLogins = pwpState.getGraceLoginsRemaining();

          if (controls == null)
          {
            controls = new ArrayList<Control>(1);
          }

          controls.add(new AccountUsableResponseControl(isInactive, isReset,
                                isExpired, remainingGraceLogins, isLocked,
                                secondsBeforeUnlock));
        }
        else
        {
          if (controls == null)
          {
            controls = new ArrayList<Control>(1);
          }

          int secondsBeforeExpiration = pwpState.getSecondsUntilExpiration();
          controls.add(new AccountUsableResponseControl(
                                secondsBeforeExpiration));
        }
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "returnEntry", e);
      }
    }

    // Check to see if the entry can be read by the client.
    SearchResultEntry tmpSearchEntry = new SearchResultEntry(entry,
        controls);
    if (AccessControlConfigManager.getInstance()
        .getAccessControlHandler().maySend(this, tmpSearchEntry) == false) {
      return true;
    }

    // Make a copy of the entry and pare it down to only include the set
    // of
    // requested attributes.
    Entry entryToReturn;
    if ((attributes == null) || attributes.isEmpty())
    {
      entryToReturn = entry.duplicateWithoutOperationalAttributes(typesOnly);
    }
    else
    {
      entryToReturn = entry.duplicateWithoutAttributes();

      for (String attrName : attributes)
      {
        if (attrName.equals("*"))
        {
          // This is a special placeholder indicating that all user attributes
          // should be returned.
          if (typesOnly)
          {
            // First, add the placeholder for the objectclass attribute.
            AttributeType ocType =
                 DirectoryServer.getObjectClassAttributeType();
            List<Attribute> ocList = new ArrayList<Attribute>(1);
            ocList.add(new Attribute(ocType));
            entryToReturn.putAttribute(ocType, ocList);

            // Next, iterate through all the user attributes and include them.
            for (AttributeType t : entry.getUserAttributes().keySet())
            {
              List<Attribute> attrList = new ArrayList<Attribute>(1);
              attrList.add(new Attribute(t));
              entryToReturn.putAttribute(t, attrList);
            }
          }
          else
          {
            // First, add the objectclass attribute.
            Attribute ocAttr = entry.getObjectClassAttribute();
            List<Attribute> ocList = new ArrayList<Attribute>(1);
            ocList.add(ocAttr);
            entryToReturn.putAttribute(ocAttr.getAttributeType(), ocList);


            // Next iterate through all the user attributes and include them.
            for (AttributeType t : entry.getUserAttributes().keySet())
            {
              entryToReturn.putAttribute(t, entry.duplicateUserAttribute(t));
            }
          }

          continue;
        }
        else if (attrName.equals("+"))
        {
          // This is a special placeholder indicating that all operational
          // attributes should be returned.
          for (AttributeType t : entry.getOperationalAttributes().keySet())
          {
            if (typesOnly)
            {
              List<Attribute> attrList = new ArrayList<Attribute>(1);
              attrList.add(new Attribute(t));
              entryToReturn.putAttribute(t, attrList);
            }
            else
            {
              entryToReturn.putAttribute(t,
                                 entry.duplicateOperationalAttribute(t));
            }
          }

          continue;
        }

        String lowerName;
        HashSet<String> options;
        int semicolonPos = attrName.indexOf(';');
        if (semicolonPos > 0)
        {
          lowerName = toLowerCase(attrName.substring(0, semicolonPos));
          int nextPos = attrName.indexOf(';', semicolonPos+1);
          options = new HashSet<String>();
          while (nextPos > 0)
          {
            options.add(attrName.substring(semicolonPos+1, nextPos));

            semicolonPos = nextPos;
            nextPos = attrName.indexOf(';', semicolonPos+1);
          }

          options.add(attrName.substring(semicolonPos+1));
        }
        else
        {
          lowerName = toLowerCase(attrName);
          options = null;
        }


        AttributeType attrType = DirectoryServer.getAttributeType(lowerName);
        if (attrType == null)
        {
          boolean added = false;
          for (AttributeType t : entry.getUserAttributes().keySet())
          {
            if (t.hasNameOrOID(lowerName))
            {
              if ((options == null) || options.isEmpty())
              {
                if (typesOnly)
                {
                  List<Attribute> attrList = new ArrayList<Attribute>(1);
                  attrList.add(new Attribute(t));
                  entryToReturn.putAttribute(t, attrList);
                }
                else
                {
                  entryToReturn.putAttribute(t,
                                             entry.duplicateUserAttribute(t));
                }

                added = true;
                break;
              }
              else
              {
                List<Attribute> attrList = entry.duplicateUserAttribute(t);
                List<Attribute> includeAttrs =
                     new ArrayList<Attribute>(attrList.size());
                for (Attribute a : attrList)
                {
                  if (a.hasOptions(options))
                  {
                    includeAttrs.add(a);
                  }
                }

                if (! includeAttrs.isEmpty())
                {
                  if (typesOnly)
                  {
                    attrList = new ArrayList<Attribute>(1);
                    attrList.add(new Attribute(t));
                    entryToReturn.putAttribute(t, attrList);
                  }
                  else
                  {
                    entryToReturn.putAttribute(t, includeAttrs);
                  }

                  added = true;
                  break;
                }
              }
            }
          }

          if (added)
          {
            continue;
          }

          for (AttributeType t : entry.getOperationalAttributes().keySet())
          {
            if (t.hasNameOrOID(lowerName))
            {
              if ((options == null) || options.isEmpty())
              {
                if (typesOnly)
                {
                  List<Attribute> attrList = new ArrayList<Attribute>(1);
                  attrList.add(new Attribute(t));
                  entryToReturn.putAttribute(t, attrList);
                }
                else
                {
                  entryToReturn.putAttribute(t,
                                     entry.duplicateOperationalAttribute(t));
                }

                added = true;
                break;
              }
              else
              {
                List<Attribute> attrList =
                     entry.duplicateOperationalAttribute(t);
                List<Attribute> includeAttrs =
                     new ArrayList<Attribute>(attrList.size());
                for (Attribute a : attrList)
                {
                  if (a.hasOptions(options))
                  {
                    includeAttrs.add(a);
                  }
                }

                if (! includeAttrs.isEmpty())
                {
                  if (typesOnly)
                  {
                    attrList = new ArrayList<Attribute>(1);
                    attrList.add(new Attribute(t));
                    entryToReturn.putAttribute(t, attrList);
                  }
                  else
                  {
                    entryToReturn.putAttribute(t, includeAttrs);
                  }

                  added = true;
                  break;
                }
              }
            }
          }
        }
        else
        {
          List<Attribute> attrList = entry.getAttribute(attrType, options);
          if (attrList != null)
          {
            if (typesOnly)
            {
              attrList = new ArrayList<Attribute>(1);
              attrList.add(new Attribute(attrType));
              entryToReturn.putAttribute(attrType, attrList);
            }
            else
            {
              entryToReturn.putAttribute(attrType, attrList);
            }
          }
        }
      }
    }


    // If there is a matched values control, then further pare down the entry
    // based on the filters that it contains.
    if ((matchedValuesControl != null) && (! typesOnly))
    {
      // First, look at the set of objectclasses.
      AttributeType attrType = DirectoryServer.getObjectClassAttributeType();
      Iterator<String> ocIterator =
           entryToReturn.getObjectClasses().values().iterator();
      while (ocIterator.hasNext())
      {
        String ocName = ocIterator.next();
        AttributeValue v = new AttributeValue(attrType,
                                              new ASN1OctetString(ocName));
        if (! matchedValuesControl.valueMatches(attrType, v))
        {
          ocIterator.remove();
        }
      }


      // Next, the set of user attributes.
      for (AttributeType t : entryToReturn.getUserAttributes().keySet())
      {
        for (Attribute a : entryToReturn.getUserAttribute(t))
        {
          Iterator<AttributeValue> valueIterator = a.getValues().iterator();
          while (valueIterator.hasNext())
          {
            AttributeValue v = valueIterator.next();
            if (! matchedValuesControl.valueMatches(t, v))
            {
              valueIterator.remove();
            }
          }
        }
      }


      // Then the set of operational attributes.
      for (AttributeType t : entryToReturn.getOperationalAttributes().keySet())
      {
        for (Attribute a : entryToReturn.getOperationalAttribute(t))
        {
          Iterator<AttributeValue> valueIterator = a.getValues().iterator();
          while (valueIterator.hasNext())
          {
            AttributeValue v = valueIterator.next();
            if (! matchedValuesControl.valueMatches(t, v))
            {
              valueIterator.remove();
            }
          }
        }
      }
    }


    // Convert the provided entry to a search result entry.
    SearchResultEntry searchEntry = new SearchResultEntry(entryToReturn,
                                                          controls);

    // Strip out any attributes that the client does not have access to.

    // FIXME: need some way to prevent plugins from adding attributes or
    // values that the client is not permitted to see.
    searchEntry = AccessControlConfigManager.getInstance()
        .getAccessControlHandler().filterEntry(this, searchEntry);

    // Invoke any search entry plugins that may be registered with the server.
    SearchEntryPluginResult pluginResult =
         DirectoryServer.getPluginConfigManager().
              invokeSearchResultEntryPlugins(this, searchEntry);
    if (pluginResult.connectionTerminated())
    {
      // We won't attempt to send this entry, and we won't continue with
      // any processing.  Just update the operation to indicate that it was
      // cancelled and return false.
      setResultCode(ResultCode.CANCELED);
      appendErrorMessage(getMessage(MSGID_CANCELED_BY_SEARCH_ENTRY_DISCONNECT,
                                    String.valueOf(entry.getDN())));
      return false;
    }


    // Send the entry to the client.
    if (pluginResult.sendEntry())
    {
      clientConnection.sendSearchEntry(this, searchEntry);

      // Log the entry sent to the client.
      logSearchResultEntry(this, searchEntry);

      entriesSent++;
    }


    return pluginResult.continueSearch();
  }



  /**
   * Used as a callback for backends to indicate that the provided search
   * reference was encountered during processing and that additional processing
   * should be performed to potentially send it back to the client.
   *
   * @param  reference  The search reference to send to the client.
   *
   * @return  <CODE>true</CODE> if the caller should continue processing the
   *          search request and sending additional entries and references , or
   *          <CODE>false</CODE> if not for some reason (e.g., the size limit
   *          has been reached or the search has been abandoned).
   */
  public boolean returnReference(SearchResultReference reference)
  {
    assert debugEnter(CLASS_NAME, "returnReference", String.valueOf(reference));

    // See if the operation has been abandoned.  If so, then don't send the
    // reference and indicate that the search should end.
    if (cancelRequest != null)
    {
      setResultCode(ResultCode.CANCELED);
      return false;
    }


    // See if the time limit has expired.  If so, then don't send the entry and
    // indicate that the search should end.
    if ((timeLimit > 0) && (TimeThread.getTime() >= timeLimitExpiration))
    {
      setResultCode(ResultCode.TIME_LIMIT_EXCEEDED);
      appendErrorMessage(getMessage(MSGID_SEARCH_TIME_LIMIT_EXCEEDED,
                                    timeLimit));
      return false;
    }


    // See if we know that this client can't handle referrals.  If so, then
    // don't even try to send it.
    if (! clientAcceptsReferrals)
    {
      return true;
    }


    // See if the client has permission to read this reference.
    if (AccessControlConfigManager.getInstance()
        .getAccessControlHandler().maySend(this, reference) == false) {
      return true;
    }


    // Invoke any search reference plugins that may be registered with the
    // server.
    SearchReferencePluginResult pluginResult =
         DirectoryServer.getPluginConfigManager().
              invokeSearchResultReferencePlugins(this, reference);
    if (pluginResult.connectionTerminated())
    {
      // We won't attempt to send this entry, and we won't continue with
      // any processing.  Just update the operation to indicate that it was
      // cancelled and return false.
      setResultCode(ResultCode.CANCELED);
      appendErrorMessage(getMessage(MSGID_CANCELED_BY_SEARCH_REF_DISCONNECT,
           String.valueOf(reference.getReferralURLString())));
      return false;
    }


    // Send the reference to the client.  Note that this could throw an
    // exception, which would indicate that the associated client can't handle
    // referrals.  If that't the case, then set a flag so we'll know not to try
    // to send any more.
    if (pluginResult.sendReference())
    {
      if (clientConnection.sendSearchReference(this, reference))
      {
        // Log the entry sent to the client.
        logSearchResultReference(this, reference);
        referencesSent++;

        // FIXME -- Should the size limit apply here?
      }
      else
      {
        // We know that the client can't handle referrals, so we won't try to
        // send it any more.
        clientAcceptsReferrals = false;
      }
    }


    return pluginResult.continueSearch();
  }



  /**
   * Sends the search result done message to the client.  Note that this method
   * should only be called from external classes in special cases (e.g.,
   * persistent search) where they are sure that the result won't be sent by the
   * core server.  Also note that the result code and optionally the error
   * message should have been set for this operation before this method is
   * called.
   */
  public void sendSearchResultDone()
  {
    assert debugEnter(CLASS_NAME, "sendSearchResultDone");


    // Send the search result done message to the client.  We want to make sure
    // that this only gets sent once, and it's possible that this could be
    // multithreaded in the event of a persistent search, so do it safely.
    if (responseSent.compareAndSet(false, true))
    {
      // Send the response to the client.
      clientConnection.sendResponse(this);

      // Log the search result.
      logSearchResultDone(this);


      // Invoke the post-response search plugins.
      DirectoryServer.getPluginConfigManager().
           invokePostResponseSearchPlugins(this);
    }
  }



  /**
   * Retrieves the operation type for this operation.
   *
   * @return  The operation type for this operation.
   */
  public OperationType getOperationType()
  {
    // Note that no debugging will be done in this method because it is a likely
    // candidate for being called by the logging subsystem.

    return OperationType.SEARCH;
  }



  /**
   * Retrieves a standard set of elements that should be logged in requests for
   * this type of operation.  Each element in the array will itself be a
   * two-element array in which the first element is the name of the field and
   * the second is a string representation of the value, or <CODE>null</CODE> if
   * there is no value for that field.
   *
   * @return  A standard set of elements that should be logged in requests for
   *          this type of operation.
   */
  public String[][] getRequestLogElements()
  {
    // Note that no debugging will be done in this method because it is a likely
    // candidate for being called by the logging subsystem.

    String attrs;
    if ((attributes == null) || attributes.isEmpty())
    {
      attrs = null;
    }
    else
    {
      StringBuilder attrBuffer = new StringBuilder();
      Iterator<String> iterator = attributes.iterator();
      attrBuffer.append(iterator.next());

      while (iterator.hasNext())
      {
        attrBuffer.append(", ");
        attrBuffer.append(iterator.next());
      }

      attrs = attrBuffer.toString();
    }

    return new String[][]
    {
      new String[] { LOG_ELEMENT_BASE_DN, String.valueOf(rawBaseDN) },
      new String[] { LOG_ELEMENT_SCOPE, String.valueOf(scope) },
      new String[] { LOG_ELEMENT_SIZE_LIMIT, String.valueOf(sizeLimit) },
      new String[] { LOG_ELEMENT_TIME_LIMIT, String.valueOf(timeLimit) },
      new String[] { LOG_ELEMENT_FILTER, String.valueOf(rawFilter) },
      new String[] { LOG_ELEMENT_REQUESTED_ATTRIBUTES, attrs }
    };
  }



  /**
   * Retrieves a standard set of elements that should be logged in responses for
   * this type of operation.  Each element in the array will itself be a
   * two-element array in which the first element is the name of the field and
   * the second is a string representation of the value, or <CODE>null</CODE> if
   * there is no value for that field.
   *
   * @return  A standard set of elements that should be logged in responses for
   *          this type of operation.
   */
  public String[][] getResponseLogElements()
  {
    // Note that no debugging will be done in this method because it is a likely
    // candidate for being called by the logging subsystem.

    String resultCode = String.valueOf(getResultCode().getIntValue());

    String errorMessage;
    StringBuilder errorMessageBuffer = getErrorMessage();
    if (errorMessageBuffer == null)
    {
      errorMessage = null;
    }
    else
    {
      errorMessage = errorMessageBuffer.toString();
    }

    String matchedDNStr;
    DN matchedDN = getMatchedDN();
    if (matchedDN == null)
    {
      matchedDNStr = null;
    }
    else
    {
      matchedDNStr = matchedDN.toString();
    }

    String referrals;
    List<String> referralURLs = getReferralURLs();
    if ((referralURLs == null) || referralURLs.isEmpty())
    {
      referrals = null;
    }
    else
    {
      StringBuilder buffer = new StringBuilder();
      Iterator<String> iterator = referralURLs.iterator();
      buffer.append(iterator.next());

      while (iterator.hasNext())
      {
        buffer.append(", ");
        buffer.append(iterator.next());
      }

      referrals = buffer.toString();
    }

    String processingTime =
         String.valueOf(processingStopTime - processingStartTime);

    return new String[][]
    {
      new String[] { LOG_ELEMENT_RESULT_CODE, resultCode },
      new String[] { LOG_ELEMENT_ERROR_MESSAGE, errorMessage },
      new String[] { LOG_ELEMENT_MATCHED_DN, matchedDNStr },
      new String[] { LOG_ELEMENT_REFERRAL_URLS, referrals },
      new String[] { LOG_ELEMENT_ENTRIES_SENT, String.valueOf(entriesSent) },
      new String[] { LOG_ELEMENT_REFERENCES_SENT,
                     String.valueOf(referencesSent ) },
      new String[] { LOG_ELEMENT_PROCESSING_TIME, processingTime }
    };
  }



  /**
   * Retrieves the set of controls to include in the response to the client.
   * Note that the contents of this list should not be altered after
   * post-operation plugins have been called.
   *
   * @return  The set of controls to include in the response to the client.
   */
  public List<Control> getResponseControls()
  {
    assert debugEnter(CLASS_NAME, "getResponseControls");

    return responseControls;
  }



  /**
   * Performs the work of actually processing this operation.  This should
   * include all processing for the operation, including invoking plugins,
   * logging messages, performing access control, managing synchronization, and
   * any other work that might need to be done in the course of processing.
   */
  public void run()
  {
    assert debugEnter(CLASS_NAME, "run");

    setResultCode(ResultCode.UNDEFINED);
    boolean sendResponse = true;


    // Get the plugin config manager that will be used for invoking plugins.
    PluginConfigManager pluginConfigManager =
         DirectoryServer.getPluginConfigManager();
    boolean skipPostOperation = false;


    // Start the processing timer.
    processingStartTime = System.currentTimeMillis();
    if (timeLimit <= 0)
    {
      timeLimitExpiration = Long.MAX_VALUE;
    }
    else
    {
      // FIXME -- Factor in the user's effective time limit.
      timeLimitExpiration = processingStartTime + (1000 * timeLimit);
    }


    // Check for and handle a request to cancel this operation.
    if (cancelRequest != null)
    {
      indicateCancelled(cancelRequest);
      processingStopTime = System.currentTimeMillis();
      return;
    }


    // Create a labeled block of code that we can break out of if a problem is
    // detected.
searchProcessing:
    {
      PreParsePluginResult preParseResult =
           pluginConfigManager.invokePreParseSearchPlugins(this);
      if (preParseResult.connectionTerminated())
      {
        // There's no point in continuing with anything.  Log the request and
        // result and return.
        setResultCode(ResultCode.CANCELED);

        int msgID = MSGID_CANCELED_BY_PREPARSE_DISCONNECT;
        appendErrorMessage(getMessage(msgID));

        processingStopTime = System.currentTimeMillis();

        logSearchRequest(this);
        logSearchResultDone(this);
        return;
      }
      else if (preParseResult.sendResponseImmediately())
      {
        skipPostOperation = true;
        logSearchRequest(this);
        break searchProcessing;
      }


      // Log the search request message.
      logSearchRequest(this);


      // Check for and handle a request to cancel this operation.
      if (cancelRequest != null)
      {
        indicateCancelled(cancelRequest);
        processingStopTime = System.currentTimeMillis();
        logSearchResultDone(this);
        return;
      }


      // Process the search base and filter to convert them from their raw forms
      // as provided by the client to the forms required for the rest of the
      // search processing.
      try
      {
        if (baseDN == null)
        {
          baseDN = DN.decode(rawBaseDN);
        }
      }
      catch (DirectoryException de)
      {
        assert debugException(CLASS_NAME, "run", de);

        setResultCode(de.getResultCode());
        appendErrorMessage(de.getErrorMessage());
        setMatchedDN(de.getMatchedDN());
        setReferralURLs(de.getReferralURLs());

        break searchProcessing;
      }

      if (filter == null)
      {
        filter = rawFilter.toSearchFilter();
      }

      // Check to see if the client has permission to perform the
      // search.

      // FIXME: for now assume that this will check all permission
      // pertinent to the operation. This includes proxy authorization
      // and any other controls specified.
      if (AccessControlConfigManager.getInstance()
          .getAccessControlHandler().isAllowed(this) == false) {
        setResultCode(ResultCode.INSUFFICIENT_ACCESS_RIGHTS);

        int msgID = MSGID_SEARCH_AUTHZ_INSUFFICIENT_ACCESS_RIGHTS;
        appendErrorMessage(getMessage(msgID, String.valueOf(baseDN)));

        skipPostOperation = true;
        break searchProcessing;
      }

      // Check to see if there are any controls in the request.  If so, then
      // see if there is any special processing required.
      boolean       processSearch    = true;
      List<Control> requestControls  = getRequestControls();
      if ((requestControls != null) && (! requestControls.isEmpty()))
      {
        for (int i=0; i < requestControls.size(); i++)
        {
          Control c   = requestControls.get(i);
          String  oid = c.getOID();

          if (oid.equals(OID_LDAP_ASSERTION))
          {
            LDAPAssertionRequestControl assertControl;
            if (c instanceof LDAPAssertionRequestControl)
            {
              assertControl = (LDAPAssertionRequestControl) c;
            }
            else
            {
              try
              {
                assertControl = LDAPAssertionRequestControl.decodeControl(c);
                requestControls.set(i, assertControl);
              }
              catch (LDAPException le)
              {
                assert debugException(CLASS_NAME, "run", le);

                setResultCode(ResultCode.valueOf(le.getResultCode()));
                appendErrorMessage(le.getMessage());

                break searchProcessing;
              }
            }

            SearchFilter assertionFilter = assertControl.getSearchFilter();
            try
            {
              // FIXME -- We need to determine whether the current user has
              //          permission to make this determination.

              Entry entry;
              try
              {
                entry = DirectoryServer.getEntry(baseDN);
              }
              catch (DirectoryException de)
              {
                assert debugException(CLASS_NAME, "run", de);

                setResultCode(de.getResultCode());

                int msgID = MSGID_SEARCH_CANNOT_GET_ENTRY_FOR_ASSERTION;
                appendErrorMessage(getMessage(msgID, de.getErrorMessage()));

                break searchProcessing;
              }

              if (entry == null)
              {
                setResultCode(ResultCode.NO_SUCH_OBJECT);

                int msgID = MSGID_SEARCH_NO_SUCH_ENTRY_FOR_ASSERTION;
                appendErrorMessage(getMessage(msgID));

                break searchProcessing;
              }


              if (! assertionFilter.matchesEntry(entry))
              {
                setResultCode(ResultCode.ASSERTION_FAILED);

                appendErrorMessage(getMessage(MSGID_SEARCH_ASSERTION_FAILED));

                break searchProcessing;
              }
            }
            catch (DirectoryException de)
            {
              assert debugException(CLASS_NAME, "run", de);

              setResultCode(ResultCode.PROTOCOL_ERROR);

              int msgID = MSGID_SEARCH_CANNOT_PROCESS_ASSERTION_FILTER;
              appendErrorMessage(getMessage(msgID, de.getErrorMessage()));

              break searchProcessing;
            }
          }
          else if (oid.equals(OID_PROXIED_AUTH_V1))
          {
            ProxiedAuthV1Control proxyControl;
            if (c instanceof ProxiedAuthV1Control)
            {
              proxyControl = (ProxiedAuthV1Control) c;
            }
            else
            {
              try
              {
                proxyControl = ProxiedAuthV1Control.decodeControl(c);
              }
              catch (LDAPException le)
              {
                assert debugException(CLASS_NAME, "run", le);

                setResultCode(ResultCode.valueOf(le.getResultCode()));
                appendErrorMessage(le.getMessage());

                break searchProcessing;
              }
            }


            DN authzDN;
            try
            {
              authzDN = proxyControl.getValidatedAuthorizationDN();
            }
            catch (DirectoryException de)
            {
              assert debugException(CLASS_NAME, "run", de);

              setResultCode(de.getResultCode());
              appendErrorMessage(de.getErrorMessage());

              break searchProcessing;
            }


            // FIXME -- Should we specifically check permissions here, or let
            //          the earlier access control checks handle it?
            setAuthorizationDN(authzDN);
          }
          else if (oid.equals(OID_PROXIED_AUTH_V2))
          {
            ProxiedAuthV2Control proxyControl;
            if (c instanceof ProxiedAuthV2Control)
            {
              proxyControl = (ProxiedAuthV2Control) c;
            }
            else
            {
              try
              {
                proxyControl = ProxiedAuthV2Control.decodeControl(c);
              }
              catch (LDAPException le)
              {
                assert debugException(CLASS_NAME, "run", le);

                setResultCode(ResultCode.valueOf(le.getResultCode()));
                appendErrorMessage(le.getMessage());

                break searchProcessing;
              }
            }


            DN authzDN;
            try
            {
              authzDN = proxyControl.getValidatedAuthorizationDN();
            }
            catch (DirectoryException de)
            {
              assert debugException(CLASS_NAME, "run", de);

              setResultCode(de.getResultCode());
              appendErrorMessage(de.getErrorMessage());

              break searchProcessing;
            }


            // FIXME -- Should we specifically check permissions here, or let
            //          the earlier access control checks handle it?
            setAuthorizationDN(authzDN);
          }
          else if (oid.equals(OID_PERSISTENT_SEARCH))
          {
            PersistentSearchControl psearchControl;
            if (c instanceof PersistentSearchControl)
            {
              psearchControl = (PersistentSearchControl) c;
            }
            else
            {
              try
              {
                psearchControl = PersistentSearchControl.decodeControl(c);
              }
              catch (LDAPException le)
              {
                assert debugException(CLASS_NAME, "run", le);

                setResultCode(ResultCode.valueOf(le.getResultCode()));
                appendErrorMessage(le.getMessage());

                break searchProcessing;
              }
            }

            persistentSearch =
                 new PersistentSearch(this, psearchControl.getChangeTypes(),
                                      psearchControl.getReturnECs());

            // If we're only interested in changes, then we don't actually want
            // to process the search now.
            if (psearchControl.getChangesOnly())
            {
              processSearch = false;
            }
          }
          else if (oid.equals(OID_LDAP_SUBENTRIES))
          {
            returnLDAPSubentries = true;
          }
          else if (oid.equals(OID_MATCHED_VALUES))
          {
            if (c instanceof MatchedValuesControl)
            {
              matchedValuesControl = (MatchedValuesControl) c;
            }
            else
            {
              try
              {
                matchedValuesControl = MatchedValuesControl.decodeControl(c);
              }
              catch (LDAPException le)
              {
                assert debugException(CLASS_NAME, "run", le);

                setResultCode(ResultCode.valueOf(le.getResultCode()));
                appendErrorMessage(le.getMessage());

                break searchProcessing;
              }
            }
          }
          else if (oid.equals(OID_ACCOUNT_USABLE_CONTROL))
          {
            includeUsableControl = true;
          }

          // NYI -- Add support for additional controls.
          else if (c.isCritical())
          {
            Backend backend = DirectoryServer.getBackend(baseDN);
            if ((backend == null) || (! backend.supportsControl(oid)))
            {
              setResultCode(ResultCode.UNAVAILABLE_CRITICAL_EXTENSION);

              int msgID = MSGID_SEARCH_UNSUPPORTED_CRITICAL_CONTROL;
              appendErrorMessage(getMessage(msgID, oid));

              break searchProcessing;
            }
          }
        }
      }


      // Check for and handle a request to cancel this operation.
      if (cancelRequest != null)
      {
        indicateCancelled(cancelRequest);
        processingStopTime = System.currentTimeMillis();
        logSearchResultDone(this);
        return;
      }


      // Invoke the pre-operation search plugins.
      PreOperationPluginResult preOpResult =
           pluginConfigManager.invokePreOperationSearchPlugins(this);
      if (preOpResult.connectionTerminated())
      {
        // There's no point in continuing with anything.  Log the request and
        // result and return.
        setResultCode(ResultCode.CANCELED);

        int msgID = MSGID_CANCELED_BY_PREOP_DISCONNECT;
        appendErrorMessage(getMessage(msgID));

        processingStopTime = System.currentTimeMillis();
        logSearchResultDone(this);
        return;
      }
      else if (preOpResult.sendResponseImmediately())
      {
        skipPostOperation = true;
        break searchProcessing;
      }


      // Check for and handle a request to cancel this operation.
      if (cancelRequest != null)
      {
        indicateCancelled(cancelRequest);
        processingStopTime = System.currentTimeMillis();
        logSearchResultDone(this);
        return;
      }


      // Get the backend that should hold the search base.  If there is none,
      // then fail.
      Backend backend = DirectoryServer.getBackend(baseDN);
      if (backend == null)
      {
        setResultCode(ResultCode.NO_SUCH_OBJECT);
        appendErrorMessage(getMessage(MSGID_SEARCH_BASE_DOESNT_EXIST,
                                      String.valueOf(baseDN)));
        break searchProcessing;
      }


      // We'll set the result code to "success".  If a problem occurs, then it
      // will be overwritten.
      setResultCode(ResultCode.SUCCESS);


      // If there's a persistent search, then register it with the server.
      if (persistentSearch != null)
      {
        DirectoryServer.registerPersistentSearch(persistentSearch);
        sendResponse = false;
      }


      // Process the search in the backend and all its subordinates.
      try
      {
        if (processSearch)
        {
          searchBackend(backend);
        }
      }
      catch (DirectoryException de)
      {
        assert debugException(CLASS_NAME, "run", de);

        setResultCode(de.getResultCode());
        appendErrorMessage(de.getErrorMessage());
        setMatchedDN(de.getMatchedDN());
        setReferralURLs(de.getReferralURLs());

        if (persistentSearch != null)
        {
          DirectoryServer.deregisterPersistentSearch(persistentSearch);
          sendResponse = true;
        }

        break searchProcessing;
      }
      catch (CancelledOperationException coe)
      {
        assert debugException(CLASS_NAME, "run", coe);

        CancelResult cancelResult = coe.getCancelResult();

        setCancelResult(cancelResult);
        setResultCode(cancelResult.getResultCode());

        String message = coe.getMessage();
        if ((message != null) && (message.length() > 0))
        {
          appendErrorMessage(message);
        }

        if (persistentSearch != null)
        {
          DirectoryServer.deregisterPersistentSearch(persistentSearch);
          sendResponse = true;
        }

        skipPostOperation = true;
        break searchProcessing;
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "run", e);

        setResultCode(DirectoryServer.getServerErrorResultCode());

        int msgID = MSGID_SEARCH_BACKEND_EXCEPTION;
        appendErrorMessage(getMessage(msgID, stackTraceToSingleLineString(e)));

        if (persistentSearch != null)
        {
          DirectoryServer.deregisterPersistentSearch(persistentSearch);
          sendResponse = true;
        }

        skipPostOperation = true;
        break searchProcessing;
      }
    }


    // Check for and handle a request to cancel this operation.
    if (cancelRequest != null)
    {
      indicateCancelled(cancelRequest);
      processingStopTime = System.currentTimeMillis();
      logSearchResultDone(this);
      return;
    }


    // Invoke the post-operation search plugins.
    if (! skipPostOperation)
    {
      PostOperationPluginResult postOperationResult =
           pluginConfigManager.invokePostOperationSearchPlugins(this);
      if (postOperationResult.connectionTerminated())
      {
        setResultCode(ResultCode.CANCELED);

        int msgID = MSGID_CANCELED_BY_POSTOP_DISCONNECT;
        appendErrorMessage(getMessage(msgID));

        processingStopTime = System.currentTimeMillis();
        logSearchResultDone(this);
        return;
      }
    }


    // Indicate that it is now too late to attempt to cancel the operation.
    setCancelResult(CancelResult.TOO_LATE);


    // Stop the processing timer.
    processingStopTime = System.currentTimeMillis();


    // Send the search result done message to the client.
    if (sendResponse)
    {
      sendSearchResultDone();
    }
  }



  /**
   * Processes the search in the provided backend and recursively through its
   * subordinate backends.
   *
   * @param  backend  The backend in which to process the search.
   *
   * @throws  DirectoryException  If a problem occurs while processing the
   *                              search.
   *
   * @throws  CancelledOperationException  If the backend noticed and reacted
   *                                       to a request to cancel or abandon the
   *                                       search operation.
   */
  private void searchBackend(Backend backend)
          throws DirectoryException, CancelledOperationException
  {
    assert debugEnter(CLASS_NAME, "searchBackend", String.valueOf(backend));


    // Check for and handle a request to cancel this operation.
    if (cancelRequest != null)
    {
      setCancelResult(CancelResult.CANCELED);
      processingStopTime = System.currentTimeMillis();
      return;
    }


    // Perform the search in the provided backend.
    backend.search(this);


    // If there are any subordinate backends, then process the search there as
    // well.
    Backend[] subBackends = backend.getSubordinateBackends();
    for (Backend b : subBackends)
    {
      DN[] baseDNs = b.getBaseDNs();
      for (DN dn : baseDNs)
      {
        if (dn.isDescendantOf(baseDN))
        {
          searchBackend(b);
          break;
        }
      }
    }
  }



  /**
   * Attempts to cancel this operation before processing has completed.
   *
   * @param  cancelRequest  Information about the way in which the operation
   *                        should be canceled.
   *
   * @return  A code providing information on the result of the cancellation.
   */
  public CancelResult cancel(CancelRequest cancelRequest)
  {
    assert debugEnter(CLASS_NAME, "cancel", String.valueOf(cancelRequest));

    this.cancelRequest = cancelRequest;

    if (persistentSearch != null)
    {
      DirectoryServer.deregisterPersistentSearch(persistentSearch);
      persistentSearch = null;
    }

    CancelResult cancelResult = getCancelResult();
    long stopWaitingTime = System.currentTimeMillis() + 5000;
    while ((cancelResult == null) &&
           (System.currentTimeMillis() < stopWaitingTime))
    {
      try
      {
        Thread.sleep(50);
      }
      catch (Exception e)
      {
        assert debugException(CLASS_NAME, "cancel", e);
      }

      cancelResult = getCancelResult();
    }

    if (cancelResult == null)
    {
      // This can happen in some rare cases (e.g., if a client disconnects and
      // there is still a lot of data to send to that client), and in this case
      // we'll prevent the cancel thread from blocking for a long period of
      // time.
      cancelResult = CancelResult.CANNOT_CANCEL;
    }

    return cancelResult;
  }



  /**
   * Retrieves the cancel request that has been issued for this operation, if
   * there is one.
   *
   * @return  The cancel request that has been issued for this operation, or
   *          <CODE>null</CODE> if there has not been any request to cancel.
   */
  public CancelRequest getCancelRequest()
  {
    assert debugEnter(CLASS_NAME, "getCancelRequest");

    return cancelRequest;
  }



  /**
   * Appends a string representation of this operation to the provided buffer.
   *
   * @param  buffer  The buffer into which a string representation of this
   *                 operation should be appended.
   */
  public void toString(StringBuilder buffer)
  {
    assert debugEnter(CLASS_NAME, "toString", "java.lang.StringBuilder");

    buffer.append("SearchOperation(connID=");
    buffer.append(clientConnection.getConnectionID());
    buffer.append(", opID=");
    buffer.append(operationID);
    buffer.append(", baseDN=");
    buffer.append(rawBaseDN);
    buffer.append(", scope=");
    buffer.append(scope.toString());
    buffer.append(", filter=");
    buffer.append(rawFilter.toString());
    buffer.append(")");
  }
}

