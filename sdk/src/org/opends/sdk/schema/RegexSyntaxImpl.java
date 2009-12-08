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



import static com.sun.opends.sdk.messages.Messages.*;
import static org.opends.sdk.schema.SchemaConstants.*;

import java.util.regex.Pattern;

import org.opends.sdk.ByteSequence;
import org.opends.sdk.LocalizableMessage;
import org.opends.sdk.LocalizableMessageBuilder;

import com.sun.opends.sdk.util.Validator;



/**
 * This class provides a regex mechanism where a new syntax and its
 * corresponding matching rules can be created on-the-fly. A regex
 * syntax is an LDAPSyntaxDescriptionSyntax with X-PATTERN extension.
 */
final class RegexSyntaxImpl extends AbstractSyntaxImpl
{
  // The Pattern associated with the regex.
  private final Pattern pattern;



  RegexSyntaxImpl(Pattern pattern)
  {
    Validator.ensureNotNull(pattern);
    this.pattern = pattern;
  }



  @Override
  public String getApproximateMatchingRule()
  {
    return AMR_DOUBLE_METAPHONE_OID;
  }



  @Override
  public String getEqualityMatchingRule()
  {
    return EMR_CASE_IGNORE_OID;
  }



  public String getName()
  {
    return "Regex(" + pattern.toString() + ")";
  }



  @Override
  public String getOrderingMatchingRule()
  {
    return OMR_CASE_IGNORE_OID;
  }



  @Override
  public String getSubstringMatchingRule()
  {
    return SMR_CASE_IGNORE_OID;
  }



  public boolean isHumanReadable()
  {
    return true;
  }



  public boolean valueIsAcceptable(Schema schema, ByteSequence value,
      LocalizableMessageBuilder invalidReason)
  {
    final String strValue = value.toString();
    final boolean matches = pattern.matcher(strValue).matches();
    if (!matches)
    {
      final LocalizableMessage message =
          WARN_ATTR_SYNTAX_LDAPSYNTAX_REGEX_INVALID_VALUE.get(strValue,
              pattern.pattern());
      invalidReason.append(message);
    }
    return matches;
  }
}
