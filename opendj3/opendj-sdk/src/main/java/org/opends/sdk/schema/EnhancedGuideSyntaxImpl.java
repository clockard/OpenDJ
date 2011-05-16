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



import static com.sun.opends.sdk.util.StaticUtils.toLowerCase;
import static org.opends.sdk.CoreMessages.*;
import static org.opends.sdk.schema.SchemaConstants.EMR_OCTET_STRING_OID;
import static org.opends.sdk.schema.SchemaConstants.OMR_OCTET_STRING_OID;
import static org.opends.sdk.schema.SchemaConstants.SYNTAX_ENHANCED_GUIDE_NAME;

import org.forgerock.i18n.LocalizableMessageBuilder;
import org.opends.sdk.ByteSequence;
import org.opends.sdk.DecodeException;

import com.sun.opends.sdk.util.SubstringReader;



/**
 * This class implements the enhanced guide attribute syntax, which may be used
 * to provide criteria for generating search filters for entries of a given
 * objectclass.
 */
final class EnhancedGuideSyntaxImpl extends AbstractSyntaxImpl
{
  @Override
  public String getEqualityMatchingRule()
  {
    return EMR_OCTET_STRING_OID;
  }



  public String getName()
  {
    return SYNTAX_ENHANCED_GUIDE_NAME;
  }



  @Override
  public String getOrderingMatchingRule()
  {
    return OMR_OCTET_STRING_OID;
  }



  public boolean isHumanReadable()
  {
    return true;
  }



  /**
   * Indicates whether the provided value is acceptable for use in an attribute
   * with this syntax. If it is not, then the reason may be appended to the
   * provided buffer.
   *
   * @param schema
   *          The schema in which this syntax is defined.
   * @param value
   *          The value for which to make the determination.
   * @param invalidReason
   *          The buffer to which the invalid reason should be appended.
   * @return <CODE>true</CODE> if the provided value is acceptable for use with
   *         this syntax, or <CODE>false</CODE> if not.
   */
  public boolean valueIsAcceptable(final Schema schema,
      final ByteSequence value, final LocalizableMessageBuilder invalidReason)
  {
    // Get a lowercase string version of the provided value.
    final String valueStr = toLowerCase(value.toString());

    // Find the position of the first octothorpe. It should denote the
    // end of the objectclass.
    final int sharpPos = valueStr.indexOf('#');
    if (sharpPos < 0)
    {

      invalidReason
          .append(ERR_ATTR_SYNTAX_ENHANCEDGUIDE_NO_SHARP.get(valueStr));
      return false;
    }

    // Get the objectclass and see if it is a valid name or OID.
    final String ocName = valueStr.substring(0, sharpPos).trim();
    final int ocLength = ocName.length();
    if (ocLength == 0)
    {

      invalidReason.append(ERR_ATTR_SYNTAX_ENHANCEDGUIDE_NO_OC.get(valueStr));
      return false;
    }

    try
    {
      SchemaUtils.readOID(new SubstringReader(ocName.substring(ocLength)));
    }
    catch (final DecodeException de)
    {
      invalidReason.append(de.getMessageObject());
      return false;
    }

    // Find the last octothorpe and make sure it is followed by a valid
    // scope.
    final int lastSharpPos = valueStr.lastIndexOf('#');
    if (lastSharpPos == sharpPos)
    {

      invalidReason.append(ERR_ATTR_SYNTAX_ENHANCEDGUIDE_NO_FINAL_SHARP
          .get(valueStr));
      return false;
    }

    final String scopeStr = valueStr.substring(lastSharpPos + 1).trim();
    if (!(scopeStr.equals("baseobject") || scopeStr.equals("onelevel")
        || scopeStr.equals("wholesubtree") || scopeStr
        .equals("subordinatesubtree")))
    {
      if (scopeStr.length() == 0)
      {

        invalidReason.append(ERR_ATTR_SYNTAX_ENHANCEDGUIDE_NO_SCOPE
            .get(valueStr));
      }
      else
      {

        invalidReason.append(ERR_ATTR_SYNTAX_ENHANCEDGUIDE_INVALID_SCOPE.get(
            valueStr, scopeStr));
      }

      return false;
    }

    // Everything between the two octothorpes must be the criteria. Make
    // sure it is valid.
    final String criteria = valueStr.substring(sharpPos + 1, lastSharpPos)
        .trim();
    final int criteriaLength = criteria.length();
    if (criteriaLength == 0)
    {

      invalidReason.append(ERR_ATTR_SYNTAX_ENHANCEDGUIDE_NO_CRITERIA
          .get(valueStr));
      return false;
    }

    return GuideSyntaxImpl.criteriaIsValid(criteria, valueStr, invalidReason);
  }
}
