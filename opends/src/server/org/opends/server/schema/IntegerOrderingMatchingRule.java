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
package org.opends.server.schema;



import org.opends.server.api.OrderingMatchingRule;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.InitializationException;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.types.ByteString;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.ResultCode;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.loggers.Error.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.messages.SchemaMessages.*;
import static org.opends.server.schema.SchemaConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines the integerOrderingMatch matching rule defined in X.520
 * and referenced in RFC 4519.
 */
public class IntegerOrderingMatchingRule
       extends OrderingMatchingRule
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.schema.IntegerOrderingMatchingRule";



  /**
   * The serial version identifier required to satisfy the compiler because this
   * class implements the <CODE>java.io.Serializable</CODE> interface.  This
   * value was generated using the <CODE>serialver</CODE> command-line utility
   * included with the Java SDK.
   */
  private static final long serialVersionUID = 6654300545706161754L;



  /**
   * Creates a new instance of this integerOrderingMatch matching rule.
   */
  public IntegerOrderingMatchingRule()
  {
    super();

    assert debugConstructor(CLASS_NAME);
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
    assert debugEnter(CLASS_NAME, "initializeMatchingRule",
                      String.valueOf(configEntry));

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
    assert debugEnter(CLASS_NAME, "getName");

    return OMR_INTEGER_NAME;
  }



  /**
   * Retrieves the OID for this matching rule.
   *
   * @return  The OID for this matching rule.
   */
  public String getOID()
  {
    assert debugEnter(CLASS_NAME, "getOID");

    return OMR_INTEGER_OID;
  }



  /**
   * Retrieves the description for this matching rule.
   *
   * @return  The description for this matching rule, or <CODE>null</CODE> if
   *          there is none.
   */
  public String getDescription()
  {
    assert debugEnter(CLASS_NAME, "getDescription");

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
    assert debugEnter(CLASS_NAME, "getSyntaxOID");

    return SYNTAX_INTEGER_OID;
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
    assert debugEnter(CLASS_NAME, "normalizeValue", String.valueOf(value));

    byte[] valueBytes = value.value();

    int length = valueBytes.length;
    StringBuilder buffer = new StringBuilder(length);

    boolean logged = false;
    for (int i=0; i < length; i++)
    {
      switch (valueBytes[i])
      {
        case '0':
          switch (buffer.length())
          {
            case 0:
              // This is only OK if the value is zero
              if (i == (length-1))
              {
                buffer.append("0");
              }
              else
              {
                int    msgID   = MSGID_ATTR_SYNTAX_INTEGER_INITIAL_ZERO;
                String message = getMessage(msgID, value.stringValue());

                switch (DirectoryServer.getSyntaxEnforcementPolicy())
                {
                  case REJECT:
                    throw new DirectoryException(
                                   ResultCode.INVALID_ATTRIBUTE_SYNTAX, message,
                                   msgID);
                  case WARN:
                    if (! logged)
                    {
                      logged = true;
                      logError(ErrorLogCategory.SCHEMA,
                               ErrorLogSeverity.SEVERE_WARNING, message, msgID);
                    }
                    break;
                }
              }
              break;
            case 1:
              // This is OK as long as the first character isn't a dash.
              if (buffer.charAt(0) == '-')
              {
                int    msgID   = MSGID_ATTR_SYNTAX_INTEGER_INITIAL_ZERO;
                String message = getMessage(msgID, value.stringValue());

                switch (DirectoryServer.getSyntaxEnforcementPolicy())
                {
                  case REJECT:
                    throw new DirectoryException(
                                   ResultCode.INVALID_ATTRIBUTE_SYNTAX, message,
                                   msgID);
                  case WARN:
                    if (! logged)
                    {
                      logged = true;
                      logError(ErrorLogCategory.SCHEMA,
                               ErrorLogSeverity.SEVERE_WARNING, message, msgID);
                    }
                    break;
                }
              }
              else
              {
                buffer.append("0");
              }
              break;
            default:
              // This is always fine.
              buffer.append("0");
              break;
          }
          break;
        case '1':
          buffer.append('1');
          break;
        case '2':
          buffer.append('2');
          break;
        case '3':
          buffer.append('3');
          break;
        case '4':
          buffer.append('4');
          break;
        case '5':
          buffer.append('5');
          break;
        case '6':
          buffer.append('6');
          break;
        case '7':
          buffer.append('7');
          break;
        case '8':
          buffer.append('8');
          break;
        case '9':
          buffer.append('9');
          break;
        case '-':
          // This is only OK if the buffer is empty.
          if (buffer.length() == 0)
          {
            buffer.append("-");
          }
          else
          {
            int    msgID   = MSGID_ATTR_SYNTAX_INTEGER_MISPLACED_DASH;
            String message = getMessage(msgID, value.stringValue());

            switch (DirectoryServer.getSyntaxEnforcementPolicy())
            {
              case REJECT:
                throw new DirectoryException(
                               ResultCode.INVALID_ATTRIBUTE_SYNTAX, message,
                               msgID);
              case WARN:
                if (! logged)
                {
                  logged = true;
                  logError(ErrorLogCategory.SCHEMA,
                           ErrorLogSeverity.SEVERE_WARNING, message, msgID);
                }
                break;
            }
          }
          break;
        default:
          int    msgID   = MSGID_ATTR_SYNTAX_INTEGER_INVALID_CHARACTER;
          String message = getMessage(msgID, value.stringValue(),
                                      ((char) valueBytes[i]), i);
          switch (DirectoryServer.getSyntaxEnforcementPolicy())
          {
            case REJECT:
              throw new DirectoryException(
                             ResultCode.INVALID_ATTRIBUTE_SYNTAX, message,
                             msgID);
            case WARN:
              if (! logged)
              {
                logged = true;
                logError(ErrorLogCategory.SCHEMA,
                         ErrorLogSeverity.SEVERE_WARNING, message, msgID);
              }
              break;
          }
      }
    }

    if (buffer.length() == 0)
    {
      int    msgID   = MSGID_ATTR_SYNTAX_INTEGER_EMPTY_VALUE;
      String message = getMessage(msgID, value.stringValue());

      switch (DirectoryServer.getSyntaxEnforcementPolicy())
      {
        case REJECT:
          throw new DirectoryException(ResultCode.INVALID_ATTRIBUTE_SYNTAX,
                                       message, msgID);

        case WARN:
          if (! logged)
          {
            logged = true;
            logError(ErrorLogCategory.SCHEMA, ErrorLogSeverity.SEVERE_WARNING,
                     message, msgID);
          }

          buffer.append("0");
          break;

        default:
          buffer.append("0");
          break;
      }
    }
    else if ((buffer.length() == 1) && (buffer.charAt(0) == '-'))
    {
      int    msgID   = MSGID_ATTR_SYNTAX_INTEGER_DASH_NEEDS_VALUE;
      String message = getMessage(msgID, value.stringValue());

      switch (DirectoryServer.getSyntaxEnforcementPolicy())
      {
        case REJECT:
          throw new DirectoryException(ResultCode.INVALID_ATTRIBUTE_SYNTAX,
                                       message, msgID);

        case WARN:
          if (! logged)
          {
            logged = true;
            logError(ErrorLogCategory.SCHEMA, ErrorLogSeverity.SEVERE_WARNING,
                     message, msgID);
          }

          buffer.setCharAt(0, '0');
          break;

        default:
          buffer.setCharAt(0, '0');
          break;
      }
    }

    return new ASN1OctetString(buffer.toString());
  }



  /**
   * Compares the first value to the second and returns a value that indicates
   * their relative order.
   *
   * @param  value1  The normalized form of the first value to compare.
   * @param  value2  The normalized form of the second value to compare.
   *
   * @return  A negative integer if <CODE>value1</CODE> should come before
   *          <CODE>value2</CODE> in ascending order, a positive integer if
   *          <CODE>value1</CODE> should come after <CODE>value2</CODE> in
   *          ascending order, or zero if there is no difference between the
   *          values with regard to ordering.
   */
  public int compareValues(ByteString value1, ByteString value2)
  {
    assert debugEnter(CLASS_NAME, "compareValues", String.valueOf(value1),
                      String.valueOf(value2));

    return compare(value1.value(), value2.value());
  }



  /**
   * Compares the contents of the provided byte arrays to determine their
   * relative order.
   *
   * @param  b1  The first byte array to use in the comparison.
   * @param  b2  The second byte array to use in the comparison.
   *
   * @return  A negative integer if <CODE>b1</CODE> should come before
   *          <CODE>b2</CODE> in ascending order, a positive integer if
   *          <CODE>b1</CODE> should come after <CODE>b2</CODE> in ascending
   *          order, or zero if there is no difference between the values with
   *          regard to ordering.
   */
  public int compare(byte[] b1, byte[] b2)
  {
    assert debugEnter(CLASS_NAME, "compare", String.valueOf(b1),
                      String.valueOf(b2));

    int b1Length = b1.length;
    int b2Length = b2.length;


    // A length of zero should be considered a value of zero.
    if (b1Length == 0)
    {
      if (b2Length == 0)
      {
        return 0;
      }
      else if (b2[0] == '-')
      {
        return 1;
      }
      else
      {
        return -1;
      }
    }
    else if (b2Length == 0)
    {
      if (b1[0] == '-')
      {
        return -1;
      }
      else
      {
        return 1;
      }
    }


    // Starting with a dash should be an indicator of a negative value.
    if (b1[0] == '-')
    {
      if (b2[0] == '-')
      {
        if (b1Length > b2Length)
        {
          return -1;
        }
        else if (b2Length > b1Length)
        {
          return 1;
        }
        else
        {
          for (int i=1; i < b1Length; i++)
          {
            if (b1[i] > b2[i])
            {
              return -1;
            }
            else if (b1[i] < b2[i])
            {
              return 1;
            }
          }

          return 0;
        }
      }
      else
      {
        return -1;
      }
    }
    else if (b2[0] == '-')
    {
      return 1;
    }


    // They are both positive, so see which one's bigger.
    if (b1Length > b2Length)
    {
      return 1;
    }
    else if (b2Length > b1Length)
    {
      return -1;
    }
    else
    {
      for (int i=0; i < b1Length; i++)
      {
        if (b1[i] > b2[i])
        {
          return 1;
        }
        else if (b1[i] < b2[i])
        {
          return -1;
        }
      }

      return 0;
    }
  }
}

