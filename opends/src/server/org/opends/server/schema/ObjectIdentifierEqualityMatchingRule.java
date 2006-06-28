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



import java.util.Arrays;

import org.opends.server.api.EqualityMatchingRule;
import org.opends.server.api.MatchingRule;
import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.core.DirectoryException;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.InitializationException;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.opends.server.types. AttributeType;
import org.opends.server.types.ByteString;
import org.opends.server.types.ErrorLogCategory;
import org.opends.server.types.ErrorLogSeverity;
import org.opends.server.types.NameForm;
import org.opends.server.types.ObjectClass;
import org.opends.server.types.ResultCode;

import static org.opends.server.loggers.Debug.*;
import static org.opends.server.loggers.Error.*;
import static org.opends.server.messages.MessageHandler.*;
import static org.opends.server.messages.SchemaMessages.*;
import static org.opends.server.schema.SchemaConstants.*;
import static org.opends.server.util.StaticUtils.*;



/**
 * This class defines the objectIdentifierMatch matching rule defined in X.520
 * and referenced in RFC 2252.  This expects to work on OIDs and will match
 * either an attribute/objectclass name or a numeric OID.
 */
public class ObjectIdentifierEqualityMatchingRule
       extends EqualityMatchingRule
{
  /**
   * The fully-qualified name of this class for debugging purposes.
   */
  private static final String CLASS_NAME =
       "org.opends.server.schema.ObjectIdentifierEqualityMatchingRule";



  /**
   * Creates a new instance of this objectIdentifierMatch matching rule.
   */
  public ObjectIdentifierEqualityMatchingRule()
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

    return EMR_OID_NAME;
  }



  /**
   * Retrieves the OID for this matching rule.
   *
   * @return  The OID for this matching rule.
   */
  public String getOID()
  {
    assert debugEnter(CLASS_NAME, "getOID");

    return EMR_OID_OID;
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

    return SYNTAX_OID_OID;
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

    StringBuilder buffer = new StringBuilder();
    toLowerCase(value.value(), buffer, true);
    String lowerValue = buffer.toString();

    // Normalize OIDs into schema names, and secondary schema names into
    // primary schema names.

    String schemaName = null;

    AttributeType attributeType = DirectoryServer.getAttributeType(lowerValue);
    if (attributeType != null)
    {
      schemaName = attributeType.getNameOrOID();
    }

    if (schemaName == null)
    {
      ObjectClass objectClass = DirectoryServer.getObjectClass(lowerValue);
      if (objectClass != null)
      {
        schemaName = objectClass.getNameOrOID();
      }
    }

    if (schemaName == null)
    {
      MatchingRule matchingRule = DirectoryServer.getMatchingRule(lowerValue);
      if (matchingRule != null)
      {
        schemaName = matchingRule.getNameOrOID();
      }
    }

    if (schemaName == null)
    {
      NameForm nameForm = DirectoryServer.getNameForm(lowerValue);
      if (nameForm != null)
      {
        schemaName = nameForm.getNameOrOID();
      }
    }

    if (schemaName != null)
    {
      return new ASN1OctetString(toLowerCase(schemaName));
    }

    // There were no schema matches so we must check the syntax.
    switch (DirectoryServer.getSyntaxEnforcementPolicy())
    {
      case REJECT:
        StringBuilder invalidReason = new StringBuilder();
        if (isValidSchemaElement(lowerValue, 0, lowerValue.length(),
                                invalidReason))
        {
          return new ASN1OctetString(lowerValue);
        }
        else
        {
          int msgID = MSGID_ATTR_SYNTAX_OID_INVALID_VALUE;
          String message = getMessage(msgID, lowerValue,
                                      invalidReason.toString());
          throw new DirectoryException(ResultCode.INVALID_ATTRIBUTE_SYNTAX,
                                       message, msgID);
        }

      case WARN:
        invalidReason = new StringBuilder();
        if (! isValidSchemaElement(lowerValue, 0, lowerValue.length(),
                                   invalidReason))
        {
          int msgID = MSGID_ATTR_SYNTAX_OID_INVALID_VALUE;
          String message = getMessage(msgID, lowerValue,
                                      invalidReason.toString());
          logError(ErrorLogCategory.SCHEMA, ErrorLogSeverity.SEVERE_WARNING,
                   message, msgID);
        }

        return new ASN1OctetString(lowerValue);

      default:
        return new ASN1OctetString(lowerValue);
    }
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
    assert debugEnter(CLASS_NAME, "areEqual", String.valueOf(value1),
                      String.valueOf(value2));


    // First, compare the normalized values to see if they are the same.
    if (Arrays.equals(value1.value(), value2.value()))
    {
      return true;
    }


    // The following code implies that the normalized values cannot be
    // compared byte-for-byte, which would require that the generateHashCode
    // method of EqualityMatchingRule be overridden to avoid using the
    // normalized value.  Instead, values are now normalized such that they
    // can be compared byte-for-byte.  There are still some rare cases where
    // comparison fails.  For example, say there is an object class with primary
    // name "a" and secondary name "b", and there is also an attribute type with
    // primary name "b".  In this case comparing "a" with "b" returns false even
    // though the two values are equivalent in an object class context.

/*
    // It is possible that they are different names referring to the same
    // schema element.  See if we can find a case where that is true in the
    // server configuration for all of the following schema element types:
    // - Attribute Types
    // - Objectclasses
    // - Attribute syntaxes
    // - Matching Rules
    // - Name Forms
    String valueStr1 = value1.stringValue();
    AttributeType attrType1 = DirectoryServer.getAttributeType(valueStr1);
    if (attrType1 != null)
    {
      String valueStr2 = value2.stringValue();
      AttributeType attrType2 = DirectoryServer.getAttributeType(valueStr2);
      if (attrType2 == null)
      {
        return false;
      }
      else
      {
        return attrType1.equals(attrType2);
      }
    }

    ObjectClass oc1 = DirectoryServer.getObjectClass(valueStr1);
    if (oc1 != null)
    {
      String valueStr2 = value2.stringValue();
      ObjectClass oc2 = DirectoryServer.getObjectClass(valueStr2);
      if (oc2 == null)
      {
        return false;
      }
      else
      {
        return oc1.equals(oc2);
      }
    }

    AttributeSyntax syntax1 = DirectoryServer.getAttributeSyntax(valueStr1,
                                                                 false);
    if (syntax1 != null)
    {
      String valueStr2 = value2.stringValue();
      AttributeSyntax syntax2 = DirectoryServer.getAttributeSyntax(valueStr2,
                                                                   false);
      if (syntax2 == null)
      {
        return false;
      }
      else
      {
        return syntax1.equals(syntax2);
      }
    }


    MatchingRule mr1 = DirectoryServer.getMatchingRule(valueStr1);
    if (mr1 != null)
    {
      String valueStr2 = value2.stringValue();
      MatchingRule mr2 = DirectoryServer.getMatchingRule(valueStr2);
      if (mr2 == null)
      {
        return false;
      }
      else
      {
        return mr1.equals(mr2);
      }
    }


    NameForm nf1 = DirectoryServer.getNameForm(valueStr1);
    if (nf1 != null)
    {
      String valueStr2 = value2.stringValue();
      NameForm nf2 = DirectoryServer.getNameForm(valueStr2);
      if (nf2 == null)
      {
        return false;
      }
      else
      {
        return nf1.equals(nf2);
      }
    }
*/


    // If we've gotten here, then we've exhausted all reasonable checking and
    // we can't consider them equal.
    return false;
  }

}

