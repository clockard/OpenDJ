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
 *      Copyright 2009 Sun Microsystems, Inc.
 */
package org.opends.sdk.schema;



import static org.opends.messages.SchemaMessages.ERR_ATTR_SYNTAX_EMPTY_VALUE;
import static org.opends.messages.SchemaMessages.ERR_ATTR_SYNTAX_EXPECTED_OPEN_PARENTHESIS;
import static org.opends.messages.SchemaMessages.ERR_EMR_INTFIRSTCOMP_FIRST_COMPONENT_NOT_INT;

import org.opends.messages.Message;
import org.opends.sdk.Assertion;
import org.opends.sdk.ConditionResult;
import org.opends.sdk.DecodeException;
import org.opends.sdk.util.StaticUtils;
import org.opends.sdk.util.SubstringReader;
import org.opends.server.types.ByteSequence;
import org.opends.server.types.ByteString;



/**
 * This class implements the integerFirstComponentMatch matching rule
 * defined in X.520 and referenced in RFC 2252. This rule is intended
 * for use with attributes whose values contain a set of parentheses
 * enclosing a space-delimited set of names and/or name-value pairs
 * (like attribute type or objectclass descriptions) in which the
 * "first component" is the first item after the opening parenthesis.
 */
final class IntegerFirstComponentEqualityMatchingRuleImpl extends
    AbstractMatchingRuleImpl
{

  @Override
  public Assertion getAssertion(Schema schema, ByteSequence value)
      throws DecodeException
  {
    try
    {
      final String definition = value.toString();
      final SubstringReader reader = new SubstringReader(definition);
      final int intValue = SchemaUtils.readRuleID(reader);

      return new Assertion()
      {
        public ConditionResult matches(ByteString attributeValue)
        {
          return intValue == attributeValue.toInt() ? ConditionResult.TRUE
              : ConditionResult.FALSE;
        }
      };
    }
    catch (final Exception e)
    {
      StaticUtils.DEBUG_LOG.throwing(
          "IntegerFirstComponentEqualityMatchingRule", "getAssertion",
          e);

      final Message message =
          ERR_EMR_INTFIRSTCOMP_FIRST_COMPONENT_NOT_INT.get(value
              .toString());
      throw new DecodeException(message);
    }

  }



  public ByteString normalizeAttributeValue(Schema schema,
      ByteSequence value) throws DecodeException
  {
    final String definition = value.toString();
    final SubstringReader reader = new SubstringReader(definition);

    // We'll do this a character at a time. First, skip over any leading
    // whitespace.
    reader.skipWhitespaces();

    if (reader.remaining() <= 0)
    {
      // This means that the value was empty or contained only
      // whitespace.
      // That is illegal.
      final Message message = ERR_ATTR_SYNTAX_EMPTY_VALUE.get();
      throw new DecodeException(message);
    }

    // The next character must be an open parenthesis. If it is not,
    // then that is an error.
    final char c = reader.read();
    if (c != '(')
    {
      final Message message =
          ERR_ATTR_SYNTAX_EXPECTED_OPEN_PARENTHESIS.get(definition,
              (reader.pos() - 1), String.valueOf(c));
      throw new DecodeException(message);
    }

    // Skip over any spaces immediately following the opening
    // parenthesis.
    reader.skipWhitespaces();

    // The next set of characters must be the OID.
    return ByteString.valueOf(SchemaUtils.readRuleID(reader));
  }
}
