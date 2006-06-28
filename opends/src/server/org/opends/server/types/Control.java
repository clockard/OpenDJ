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
package org.opends.server.types;



import org.opends.server.protocols.asn1.ASN1OctetString;

import static org.opends.server.loggers.Debug.*;



/**
 * This class defines a data structure that holds information about a
 * control that can be included in a request or response.
 */
public class Control
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
      "org.opends.server.types.Control";



  // The value for this control.
  private ASN1OctetString value;

  // The criticality for this control.
  private boolean isCritical;

  // The OID for this control.
  private String oid;



  /**
   * Creates a new control with no value.
   *
   * @param  oid         The OID for this control.
   * @param  isCritical  Indicates whether this control should be
   *                     considered critical in processing the
   *                     request.
   */
  public Control(String oid, boolean isCritical)
  {
    assert debugEnter(CLASS_NAME, String.valueOf(oid),
                      String.valueOf(isCritical));

    this.oid        = oid;
    this.isCritical = isCritical;
    this.value      = null;
  }



  /**
   * Creates a new control with the specified information.
   *
   * @param  oid         The OID for this control.
   * @param  isCritical  Indicates whether this control should be
   *                     considered critical in processing the
   *                     request.
   * @param  value       The value for this control.
   */
  public Control(String oid, boolean isCritical,
  ASN1OctetString value)
  {
    assert debugEnter(CLASS_NAME, String.valueOf(oid),
                      String.valueOf(isCritical),
                      String.valueOf(value));

    this.oid        = oid;
    this.isCritical = isCritical;
    this.value      = value;
  }



  /**
   * Retrieves the OID for this control.
   *
   * @return  The OID for this control.
   */
  public String getOID()
  {
    assert debugEnter(CLASS_NAME, "getOID");

    return oid;
  }



  /**
   * Specifies the OID for this control.
   *
   * @param  oid  The OID for this control.
   */
  public void setOID(String oid)
  {
    assert debugEnter(CLASS_NAME, "setOID", String.valueOf(oid));

    this.oid = oid;
  }



  /**
   * Indicates whether this control should be considered critical in
   * processing the request.
   *
   * @return  <CODE>true</CODE> if this code should be considered
   *          critical, or <CODE>false</CODE> if not.
   */
  public boolean isCritical()
  {
    assert debugEnter(CLASS_NAME, "isCritical");

    return isCritical;
  }



  /**
   * Specifies whether this control should be considered critical in
   * processing the request.
   *
   * @param  isCritical  Specifies whether this control should be
   *                     considered critical in processing the
   *                     request.
   */
  public void setCritical(boolean isCritical)
  {
    assert debugEnter(CLASS_NAME, "setCritical",
                      String.valueOf(isCritical));

    this.isCritical = isCritical;
  }



  /**
   * Retrieves the value for this control.
   *
   * @return  The value for this control, or <CODE>null</CODE> if
   *          there is no value.
   */
  public ASN1OctetString getValue()
  {
    assert debugEnter(CLASS_NAME, "getValue");

    return value;
  }



  /**
   * Indicates whether this control has a value.
   *
   * @return  <CODE>true</CODE> if this control has a value, or
   *          <CODE>false</CODE> if it does not.
   */
  public boolean hasValue()
  {
    assert debugEnter(CLASS_NAME, "hasValue");

    return (value != null);
  }



  /**
   * Specifies the value for this control.
   *
   * @param  value  The value for this control.
   */
  public void setValue(ASN1OctetString value)
  {
    assert debugEnter(CLASS_NAME, "setValue", String.valueOf(value));

    this.value = value;
  }



  /**
   * Retrieves a string representation of this control.
   *
   * @return  A string representation of this control.
   */
  public String toString()
  {
    assert debugEnter(CLASS_NAME, "toString");

    StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }



  /**
   * Appends a string representation of this control to the provided
   * buffer.
   *
   * @param  buffer  The buffer to which the information should be
   *                 appended.
   */
  public void toString(StringBuilder buffer)
  {
    assert debugEnter(CLASS_NAME, "toString",
                      "java.lang.StringBuilder");

    buffer.append("Control(oid=");
    buffer.append(oid);
    buffer.append(",isCritical=");
    buffer.append(isCritical);

    if (value != null)
    {
      buffer.append(",value=");
      value.toString(buffer);
    }

    buffer.append(")");
  }
}

