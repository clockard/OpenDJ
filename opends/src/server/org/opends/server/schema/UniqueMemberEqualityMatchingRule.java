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
package org.opends.server.schema;
import org.opends.messages.Message;



import java.util.Arrays;

import java.util.Collection;
import java.util.Collections;
import org.opends.server.api.EqualityMatchingRule;
import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.types.ByteString;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.DN;
import org.opends.server.types.ResultCode;

import static org.opends.server.loggers.debug.DebugLogger.*;
import org.opends.server.loggers.debug.DebugTracer;
import org.opends.server.loggers.ErrorLogger;
import org.opends.server.types.DebugLogLevel;
import static org.opends.messages.SchemaMessages.*;
import static org.opends.server.schema.SchemaConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class implements the uniqueMemberMatch matching rule defined in X.520
 * and referenced in RFC 2252.  It is based on the name and optional UID syntax,
 * and will compare values with a distinguished name and optional bit string
 * suffix.
 */
class UniqueMemberEqualityMatchingRule
       extends EqualityMatchingRule
{
  /**
   * The tracer object for the debug logger.
   */
  private static final DebugTracer TRACER = getTracer();



  /**
   * Creates a new instance of this uniqueMemberMatch matching rule.
   */
  public UniqueMemberEqualityMatchingRule()
  {
    super();
  }



  /**
   * {@inheritDoc}
   */
  public Collection<String> getAllNames()
  {
    return Collections.singleton(getName());
  }



  /**
   * Retrieves the common name for this matching rule.
   *
   * @return  The common name for this matching rule, or <CODE>null</CODE> if
   * it does not have a name.
   */
  public String getName()
  {
    return EMR_UNIQUE_MEMBER_NAME;
  }



  /**
   * Retrieves the OID for this matching rule.
   *
   * @return  The OID for this matching rule.
   */
  public String getOID()
  {
    return EMR_UNIQUE_MEMBER_OID;
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
    return SYNTAX_NAME_AND_OPTIONAL_UID_OID;
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
    String valueString = value.stringValue().trim();
    int    valueLength = valueString.length();


    // See if the value contains the "optional uid" portion.  If we think it
    // does, then mark its location.
    int dnEndPos = valueLength;
    int sharpPos = -1;
    if (valueString.endsWith("'B") || valueString.endsWith("'b"))
    {
      sharpPos = valueString.lastIndexOf("#'");
      if (sharpPos > 0)
      {
        dnEndPos = sharpPos;
      }
    }


    // Take the DN portion of the string and try to normalize it.  If it fails,
    // then this will throw an exception.
    StringBuilder valueBuffer = new StringBuilder(valueLength);
    try
    {
      DN dn = DN.decode(valueString.substring(0, dnEndPos));
      dn.toNormalizedString(valueBuffer);
    }
    catch (Exception e)
    {
      if (debugEnabled())
      {
        TRACER.debugCaught(DebugLogLevel.ERROR, e);
      }

      // We couldn't normalize the DN for some reason.  If we're supposed to use
      // strict syntax enforcement, then throw an exception.  Otherwise, log a
      // message and just try our best.
      Message message = ERR_ATTR_SYNTAX_NAMEANDUID_INVALID_DN.get(
              valueString, getExceptionMessage(e));

      switch (DirectoryServer.getSyntaxEnforcementPolicy())
      {
        case REJECT:
          throw new DirectoryException(ResultCode.INVALID_ATTRIBUTE_SYNTAX,
                                       message);
        case WARN:
          ErrorLogger.logError(message);

          valueBuffer.append(toLowerCase(valueString).substring(0, dnEndPos));
          break;

        default:
          valueBuffer.append(toLowerCase(valueString).substring(0, dnEndPos));
          break;
      }
    }



    // If there is an "optional uid", then normalize it and make sure it only
    // contains valid binary digits.
    if (sharpPos > 0)
    {
      valueBuffer.append("#'");

      int     endPos = valueLength - 2;
      boolean logged = false;
      for (int i=sharpPos+2; i < endPos; i++)
      {
        char c = valueString.charAt(i);
        if ((c == '0') || (c == '1'))
        {
          valueBuffer.append(c);
        }
        else
        {
          // There was an invalid binary digit.  We'll either throw an exception
          // or log a message and continue, based on the server's configuration.
          Message message = ERR_ATTR_SYNTAX_NAMEANDUID_ILLEGAL_BINARY_DIGIT.get(
                  valueString, String.valueOf(c), i);

          switch (DirectoryServer.getSyntaxEnforcementPolicy())
          {
            case REJECT:
              throw new DirectoryException(ResultCode.INVALID_ATTRIBUTE_SYNTAX,
                                           message);
            case WARN:
              if (! logged)
              {
                ErrorLogger.logError(message);
                logged = true;
              }
              break;
          }
        }
      }

      valueBuffer.append("'B");
    }

    return new ASN1OctetString(valueBuffer.toString());
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

