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
package org.opends.server.controls;



import org.opends.server.protocols.ldap.LDAPException;
import org.opends.server.protocols.ldap.LDAPResultCode;
import org.opends.server.types.Control;

import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.messages.ProtocolMessages.*;
import static org.opends.server.util.ServerConstants.*;



/**
 * This class implements the Netscape password expired control. This is a very
 * simple control because it does not have a value.
 */
public class PasswordExpiredControl
       extends Control
{



  /**
   * Creates a new instance of the password expired control with the default
   * settings.
   */
  public PasswordExpiredControl()
  {
    super(OID_NS_PASSWORD_EXPIRED, false);

  }



  /**
   * Creates a new instance of the password expired control with the provided
   * information.
   *
   * @param  oid         The OID to use for this control.
   * @param  isCritical  Indicates whether support for this control should be
   *                     considered a critical part of the client processing.
   */
  public PasswordExpiredControl(String oid, boolean isCritical)
  {
    super(oid, isCritical);

  }



  /**
   * Creates a new password expired control from the contents of the provided
   * control.
   *
   * @param  control  The generic control containing the information to use to
   *                  create this password expired control.
   *
   * @return  The password expired control decoded from the provided control.
   *
   * @throws  LDAPException  If this control cannot be decoded as a valid
   *                         password expired control.
   */
  public static PasswordExpiredControl decodeControl(Control control)
         throws LDAPException
  {

    if (control.hasValue())
    {
      int    msgID   = MSGID_PWEXPIRED_CONTROL_HAS_VALUE;
      String message = getMessage(msgID);
      throw new LDAPException(LDAPResultCode.PROTOCOL_ERROR, msgID, message);
    }


    return new PasswordExpiredControl(control.getOID(), control.isCritical());
  }



  /**
   * Retrieves a string representation of this password expired control.
   *
   * @return  A string representation of this password expired control.
   */
  public String toString()
  {

    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this password expired control to the
   * provided buffer.
   *
   * @param  buffer  The buffer to which the information should be appended.
   */
  public void toString(StringBuilder buffer)
  {

    buffer.append("PasswordExpiredControl()");
  }
}

