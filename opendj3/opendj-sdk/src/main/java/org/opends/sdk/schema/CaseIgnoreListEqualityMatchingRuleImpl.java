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



import static com.sun.opends.sdk.util.StringPrepProfile.CASE_FOLD;
import static com.sun.opends.sdk.util.StringPrepProfile.TRIM;
import static com.sun.opends.sdk.util.StringPrepProfile.prepareUnicode;

import org.opends.sdk.ByteSequence;
import org.opends.sdk.ByteString;



/**
 * This class implements the caseIgnoreListMatch matching rule defined in X.520
 * and referenced in RFC 2252.
 */
final class CaseIgnoreListEqualityMatchingRuleImpl extends
    AbstractMatchingRuleImpl
{
  public ByteString normalizeAttributeValue(final Schema schema,
      final ByteSequence value)
  {
    final StringBuilder buffer = new StringBuilder();
    prepareUnicode(buffer, value, TRIM, CASE_FOLD);

    final int bufferLength = buffer.length();
    if (bufferLength == 0)
    {
      if (value.length() > 0)
      {
        // This should only happen if the value is composed entirely of
        // spaces. In that case, the normalized value is a single space.
        return SchemaConstants.SINGLE_SPACE_VALUE;
      }
      else
      {
        // The value is empty, so it is already normalized.
        return ByteString.empty();
      }
    }

    // Replace any consecutive spaces with a single space. Any spaces
    // around a dollar sign will also be removed.
    for (int pos = bufferLength - 1; pos > 0; pos--)
    {
      if (buffer.charAt(pos) == ' ')
      {
        final char c = buffer.charAt(pos - 1);
        if (c == ' ')
        {
          buffer.delete(pos, pos + 1);
        }
        else if (c == '$')
        {
          if (pos <= 1 || buffer.charAt(pos - 2) != '\\')
          {
            buffer.delete(pos, pos + 1);
          }
        }
        else if (buffer.charAt(pos + 1) == '$')
        {
          buffer.delete(pos, pos + 1);
        }
      }
    }

    return ByteString.valueOf(buffer.toString());
  }
}
