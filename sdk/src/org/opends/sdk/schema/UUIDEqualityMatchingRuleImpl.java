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



import static com.sun.opends.sdk.util.Messages.WARN_ATTR_SYNTAX_UUID_EXPECTED_DASH;
import static com.sun.opends.sdk.util.Messages.WARN_ATTR_SYNTAX_UUID_EXPECTED_HEX;
import static com.sun.opends.sdk.util.Messages.WARN_ATTR_SYNTAX_UUID_INVALID_LENGTH;

import com.sun.opends.sdk.util.Message;
import org.opends.sdk.DecodeException;
import org.opends.sdk.util.ByteSequence;
import org.opends.sdk.util.ByteString;



/**
 * This class defines the uuidMatch matching rule defined in RFC 4530.
 * It will be used as the default equality matching rule for the UUID
 * syntax.
 */
final class UUIDEqualityMatchingRuleImpl extends
    AbstractMatchingRuleImpl
{
  public ByteString normalizeAttributeValue(Schema schema,
      ByteSequence value) throws DecodeException
  {
    if (value.length() != 36)
    {
      final Message message =
          WARN_ATTR_SYNTAX_UUID_INVALID_LENGTH.get(value.toString(),
              value.length());
      throw DecodeException.error(message);
    }

    final StringBuilder builder = new StringBuilder(36);
    char c;
    for (int i = 0; i < 36; i++)
    {
      // The 9th, 14th, 19th, and 24th characters must be dashes. All
      // others must be hex. Convert all uppercase hex characters to
      // lowercase.
      c = (char) value.byteAt(i);
      switch (i)
      {
      case 8:
      case 13:
      case 18:
      case 23:
        if (c != '-')
        {
          final Message message =
              WARN_ATTR_SYNTAX_UUID_EXPECTED_DASH.get(value.toString(),
                  i, String.valueOf(c));
          throw DecodeException.error(message);
        }
        builder.append(c);
        break;
      default:
        switch (c)
        {
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
          // These are all fine.
          builder.append(c);
          break;
        case 'A':
          builder.append('a');
          break;
        case 'B':
          builder.append('b');
          break;
        case 'C':
          builder.append('c');
          break;
        case 'D':
          builder.append('d');
          break;
        case 'E':
          builder.append('e');
          break;
        case 'F':
          builder.append('f');
          break;
        default:
          final Message message =
              WARN_ATTR_SYNTAX_UUID_EXPECTED_HEX.get(value.toString(),
                  i, String.valueOf(value.byteAt(i)));
          throw DecodeException.error(message);
        }
      }
    }

    return ByteString.valueOf(builder.toString());
  }
}
