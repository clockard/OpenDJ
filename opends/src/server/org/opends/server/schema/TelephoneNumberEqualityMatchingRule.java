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
package org.opends.server.schema;



import java.util.Arrays;

import org.opends.server.api.EqualityMatchingRule;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.types.ByteString;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.InitializationException;

import static org.opends.server.schema.SchemaConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class implements the telephoneNumberMatch matching rule defined in X.520
 * and referenced in RFC 2252.  Note that although the specification calls for a
 * very rigorous format, this is widely ignored so this matching will compare
 * only numeric digits and strip out everything else.
 */
public class TelephoneNumberEqualityMatchingRule
       extends EqualityMatchingRule
{


  /**
   * Creates a new instance of this telephoneNumberMatch matching rule.
   */
  public TelephoneNumberEqualityMatchingRule()
  {
    super();

  }



  /**
   * Initializes this matching rule based on the information in the provided
   * configuration entry.
   *
   * @param  configEntry  The configuration entry that contains the information
   *                      to use to initialize this matching rule.
   *
   * @throws  ConfigException  If an unrecoverable problem arises in the
   *                           process of performing the initialization.
   *
   * @throws  InitializationException  If a problem that is not
   *                                   configuration-related occurs during
   *                                   initialization.
   */
  public void initializeMatchingRule(ConfigEntry configEntry)
         throws ConfigException, InitializationException
  {
    // No initialization is required.
  }



  /**
   * Retrieves the common name for this matching rule.
   *
   * @return  The common name for this matching rule, or <CODE>null</CODE> if
   * it does not have a name.
   */
  public String getName()
  {
    return EMR_TELEPHONE_NAME;
  }



  /**
   * Retrieves the OID for this matching rule.
   *
   * @return  The OID for this matching rule.
   */
  public String getOID()
  {
    return EMR_TELEPHONE_OID;
  }



  /**
   * Retrieves the description for this matching rule.
   *
   * @return  The description for this matching rule, or <CODE>null</CODE> if
   *          there is none.
   */
  public String getDescription()
  {
    // There is no standard description for this matching rule.
    return null;
  }



  /**
   * Retrieves the OID of the syntax with which this matching rule is
   * associated.
   *
   * @return  The OID of the syntax with which this matching rule is associated.
   */
  public String getSyntaxOID()
  {
    return SYNTAX_TELEPHONE_OID;
  }



  /**
   * Retrieves the normalized form of the provided value, which is best suited
   * for efficiently performing matching operations on that value.
   *
   * @param  value  The value to be normalized.
   *
   * @return  The normalized version of the provided value.
   *
   * @throws  DirectoryException  If the provided value is invalid according to
   *                              the associated attribute syntax.
   */
  public ByteString normalizeValue(ByteString value)
         throws DirectoryException
  {
    String valueString = value.stringValue();
    int    valueLength = valueString.length();
    StringBuilder buffer = new StringBuilder(valueLength);


    // Iterate through the characters in the value and filter out everything
    // that isn't a digit.
    for (int i=0; i < valueLength; i++)
    {
      char c = valueString.charAt(i);
      if (isDigit(c))
      {
        buffer.append(c);
      }
    }


    return new ASN1OctetString(buffer.toString());
  }



  /**
   * Indicates whether the two provided normalized values are equal to each
   * other.
   *
   * @param  value1  The normalized form of the first value to compare.
   * @param  value2  The normalized form of the second value to compare.
   *
   * @return  <CODE>true</CODE> if the provided values are equal, or
   *          <CODE>false</CODE> if not.
   */
  public boolean areEqual(ByteString value1, ByteString value2)
  {
    // Since the values are already normalized, we just need to compare the
    // associated byte arrays.
    return Arrays.equals(value1.value(), value2.value());
  }
}

