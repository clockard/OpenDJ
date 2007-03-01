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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */
package org.opends.server.controls;



import java.util.concurrent.locks.Lock;

import org.opends.server.api.IdentityMapper;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.PasswordPolicyState;
import org.opends.server.protocols.asn1.ASN1Exception;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.protocols.ldap.LDAPException;
import org.opends.server.protocols.ldap.LDAPResultCode;
import org.opends.server.types.Control;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.DN;
import org.opends.server.types.Entry;
import org.opends.server.types.LockManager;
import org.opends.server.types.ResultCode;

import static org.opends.server.loggers.debug.DebugLogger.debugCought;
import static org.opends.server.loggers.debug.DebugLogger.debugEnabled;
import org.opends.server.types.DebugLogLevel;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.messages.ProtocolMessages.*;
import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;
import static org.opends.server.util.Validator.*;



/**
 * This class implements version 2 of the proxied authorization control as
 * defined in RFC 4370.  It makes it possible for one user to request that an
 * operation be performed under the authorization of another.  The target user
 * is specified using an authorization ID, which may be in the form "dn:"
 * immediately followed by the DN of that user, or "u:" followed by a user ID
 * string.
 */
public class ProxiedAuthV2Control
       extends Control
{



  // The authorization ID from the control value.
  private ASN1OctetString authorizationID;



  /**
   * Creates a new instance of the proxied authorization v2 control with the
   * provided information.
   *
   * @param  authorizationID  The authorization ID from the control value.
   */
  public ProxiedAuthV2Control(ASN1OctetString authorizationID)
  {
    super(OID_PROXIED_AUTH_V2, true, authorizationID);


    ensureNotNull(authorizationID);
    this.authorizationID = authorizationID;
  }



  /**
   * Creates a new instance of the proxied authorization v2 control with the
   * provided information.
   *
   * @param  oid              The OID to use for this control.
   * @param  isCritical       Indicates whether support for this control
   *                          should be considered a critical part of the
   *                          server processing.
   * @param  authorizationID  The authorization ID from the control value.
   */
  private ProxiedAuthV2Control(String oid, boolean isCritical,
                             ASN1OctetString authorizationID)
  {
    super(oid, isCritical, authorizationID);


    this.authorizationID = authorizationID;
  }



  /**
   * Creates a new proxied authorization v2 control from the contents of the
   * provided control.
   *
   * @param  control  The generic control containing the information to use to
   *                  create this proxied authorization v2 control.  It must not
   *                  be {@code null}.
   *
   * @return  The proxied authorization v2 control decoded from the provided
   *          control.
   *
   * @throws  LDAPException  If this control cannot be decoded as a valid
   *                         proxied authorization v2 control.
   */
  public static ProxiedAuthV2Control decodeControl(Control control)
         throws LDAPException
  {

    ensureNotNull(control);

    if (! control.isCritical())
    {
      int    msgID   = MSGID_PROXYAUTH2_CONTROL_NOT_CRITICAL;
      String message = getMessage(msgID);
      throw new LDAPException(LDAPResultCode.PROTOCOL_ERROR, msgID,
                              message);
    }

    if (! control.hasValue())
    {
      int    msgID   = MSGID_PROXYAUTH2_NO_CONTROL_VALUE;
      String message = getMessage(msgID);
      throw new LDAPException(LDAPResultCode.PROTOCOL_ERROR, msgID, message);
    }

    ASN1OctetString authorizationID;
    try
    {
      authorizationID =
           ASN1OctetString.decodeAsOctetString(control.getValue().value());
    }
    catch (ASN1Exception ae)
    {
      String lowerAuthZIDStr = toLowerCase(control.getValue().stringValue());
      if (lowerAuthZIDStr.startsWith("dn:") || lowerAuthZIDStr.startsWith("u:"))
      {
        authorizationID = control.getValue();
      }
      else
      {
        if (debugEnabled())
        {
          debugCought(DebugLogLevel.ERROR, ae);
        }

        int    msgID   = MSGID_PROXYAUTH2_CANNOT_DECODE_VALUE;
        String message = getMessage(msgID, stackTraceToSingleLineString(ae));
        throw new LDAPException(LDAPResultCode.PROTOCOL_ERROR, msgID, message,
                                ae);
      }
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        debugCought(DebugLogLevel.ERROR, e);
      }

      int    msgID   = MSGID_PROXYAUTH2_CANNOT_DECODE_VALUE;
      String message = getMessage(msgID, stackTraceToSingleLineString(e));
      throw new LDAPException(LDAPResultCode.PROTOCOL_ERROR, msgID, message, e);
    }

    return new ProxiedAuthV2Control(control.getOID(), control.isCritical(),
                                    authorizationID);
  }



  /**
   * Retrieves the authorization ID for this proxied authorization V2 control.
   *
   * @return  The authorization ID for this proxied authorization V2 control.
   */
  public ASN1OctetString getAuthorizationID()
  {

    return authorizationID;
  }



  /**
   * Specifies the authorization ID for this proxied authorization V2 control.
   *
   * @param  authorizationID  The authorization ID for this proxied
   *                          authorization V2 control.
   */
  public void setAuthorizationID(ASN1OctetString authorizationID)
  {

    if (authorizationID == null)
    {
      this.authorizationID = new ASN1OctetString();
      setValue(this.authorizationID);
    }
    else
    {
      this.authorizationID = authorizationID;
      setValue(authorizationID);
    }
  }



  /**
   * Retrieves the authorization entry for this proxied authorization V2
   * control.  It will also perform any necessary password policy checks to
   * ensure that the associated user account is suitable for use in performing
   * this processing.
   *
   * @return  The entry for user specified as the authorization identity in this
   *          proxied authorization V1 control, or {@code null} if the
   *          authorization DN is the null DN.
   *
   * @throws  DirectoryException  If the target user does not exist or is not
   *                              available for use, or if a problem occurs
   *                              while making the determination.
   */
  public Entry getAuthorizationEntry()
         throws DirectoryException
  {


    // Check for a zero-length value, which would be for an anonymous user.
    if (authorizationID.value().length == 0)
    {
      return null;
    }


    // Get a lowercase string representation.  It must start with either "dn:"
    // or "u:".
    String authzID = authorizationID.stringValue();
    String lowerAuthzID = toLowerCase(authzID);
    if (lowerAuthzID.startsWith("dn:"))
    {
      // It's a DN, so decode it and see if it exists.  If it's the null DN,
      // then just assume that it does.
      DN authzDN = DN.decode(authzID.substring(3));
      if (authzDN.isNullDN())
      {
        return null;
      }
      else
      {
        // See if the authorization DN is one of the alternate bind DNs for one
        // of the root users and if so then map it accordingly.
        DN actualDN = DirectoryServer.getActualRootBindDN(authzDN);
        if (actualDN != null)
        {
          authzDN = actualDN;
        }

        Lock entryLock = null;
        for (int i=0; i < 3; i++)
        {
          entryLock = LockManager.lockRead(authzDN);
          if (entryLock != null)
          {
            break;
          }
        }

        if (entryLock == null)
        {
          int    msgID   = MSGID_PROXYAUTH2_CANNOT_LOCK_USER;
          String message = getMessage(msgID, String.valueOf(authzDN));
          throw new DirectoryException(ResultCode.AUTHORIZATION_DENIED, message,
                                       msgID);
        }

        try
        {
          Entry userEntry = DirectoryServer.getEntry(authzDN);
          if (userEntry == null)
          {
            // The requested user does not exist.
            int    msgID   = MSGID_PROXYAUTH2_NO_SUCH_USER;
            String message = getMessage(msgID, authzID);
            throw new DirectoryException(ResultCode.AUTHORIZATION_DENIED,
                                         message, msgID);
          }

          // FIXME -- We should provide some mechanism for enabling debug
          // processing.
          PasswordPolicyState pwpState =
               new PasswordPolicyState(userEntry, false, false);
          if (pwpState.isDisabled() || pwpState.isAccountExpired() ||
              pwpState.lockedDueToFailures() ||
              pwpState.lockedDueToIdleInterval() ||
              pwpState.lockedDueToMaximumResetAge() ||
              pwpState.isPasswordExpired())
          {
            int    msgID   = MSGID_PROXYAUTH2_UNUSABLE_ACCOUNT;
            String message = getMessage(msgID, String.valueOf(authzDN));
            throw new DirectoryException(ResultCode.AUTHORIZATION_DENIED,
                                         message, msgID);
          }


          // If we've made it here, then the user is acceptable.
          return userEntry;
        }
        finally
        {
          LockManager.unlock(authzDN, entryLock);
        }
      }
    }
    else if (lowerAuthzID.startsWith("u:"))
    {
      // If the authorization ID is just "u:", then it's an anonymous request.
      if (lowerAuthzID.length() == 2)
      {
        return null;
      }


      // Use the proxied authorization identity mapper to resolve the username
      // to an entry.
      IdentityMapper proxyMapper =
           DirectoryServer.getProxiedAuthorizationIdentityMapper();
      if (proxyMapper == null)
      {
        int    msgID   = MSGID_PROXYAUTH2_NO_IDENTITY_MAPPER;
        String message = getMessage(msgID);
        throw new DirectoryException(ResultCode.AUTHORIZATION_DENIED, message,
                                     msgID);
      }

      Entry userEntry = proxyMapper.getEntryForID(authzID.substring(2));
      if (userEntry == null)
      {
        int    msgID   = MSGID_PROXYAUTH2_NO_SUCH_USER;
        String message = getMessage(msgID, authzID);
        throw new DirectoryException(ResultCode.AUTHORIZATION_DENIED, message,
                                     msgID);
      }
      else
      {
        // FIXME -- We should provide some mechanism for enabling debug
        // processing.
        PasswordPolicyState pwpState =
             new PasswordPolicyState(userEntry, false, false);
        if (pwpState.isDisabled() || pwpState.isAccountExpired() ||
            pwpState.lockedDueToFailures() ||
            pwpState.lockedDueToIdleInterval() ||
            pwpState.lockedDueToMaximumResetAge() ||
            pwpState.isPasswordExpired())
        {
          int    msgID   = MSGID_PROXYAUTH2_UNUSABLE_ACCOUNT;
          String message = getMessage(msgID, String.valueOf(userEntry.getDN()));
          throw new DirectoryException(ResultCode.AUTHORIZATION_DENIED,
                                       message, msgID);
        }

        return userEntry;
      }
    }
    else
    {
      int    msgID   = MSGID_PROXYAUTH2_INVALID_AUTHZID;
      String message = getMessage(msgID, authzID);
      throw new DirectoryException(ResultCode.PROTOCOL_ERROR, message, msgID);
    }
  }



  /**
   * Retrieves a string representation of this proxied auth v2 control.
   *
   * @return  A string representation of this proxied auth v2 control.
   */
  public String toString()
  {

    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this proxied auth v2 control to the
   * provided buffer.
   *
   * @param  buffer  The buffer to which the information should be appended.
   */
  public void toString(StringBuilder buffer)
  {

    buffer.append("ProxiedAuthorizationV2Control(authzID=\"");
    authorizationID.toString(buffer);
    buffer.append("\")");
  }
}

