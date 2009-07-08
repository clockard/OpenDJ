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

package org.opends.common.api.filter;



import static org.opends.messages.ProtocolMessages.*;
import static org.opends.server.protocols.ldap.LDAPConstants.*;
import static org.opends.server.util.StaticUtils.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.opends.common.api.AttributeDescription;
import org.opends.common.protocols.asn1.ASN1Reader;
import org.opends.common.protocols.asn1.ASN1Writer;
import org.opends.messages.Message;
import org.opends.server.types.ByteString;
import org.opends.server.types.ByteStringBuilder;
import org.opends.server.util.Validator;



/**
 * An LDAP search filter as defined in RFC 4511. In addition this class
 * also provides support for the absolute true and absolute false
 * filters as defined in RFC 4526.
 * <p>
 * This class provides many factory methods for creating common types of
 * filter. Applications interact with a filter using
 * {@link FilterVisitor} which is applied to a filter using the
 * {@link #accept(FilterVisitor, Object)} method.
 * <p>
 * The RFC 4515 string representation of a filter can be generated using
 * the {@link #toString} methods and parsed using the
 * {@link #valueOf(String)} factory method.
 * <p>
 * TODO: provide abstract visitor with visitDefault.
 *
 * @see <a href="http://tools.ietf.org/html/rfc4511">RFC 4511 -
 *      Lightweight Directory Access Protocol (LDAP): The Protocol </a>
 * @see <a href="http://tools.ietf.org/html/rfc4515">RFC 4515 - String
 *      Representation of Search Filters </a>
 * @see <a href="http://tools.ietf.org/html/rfc4526">RFC 4526 - Absolute
 *      True and False Filters </a>
 */
public final class Filter
{
  private static final class AndImpl extends Impl
  {
    private final List<Filter> subFilters;



    public AndImpl(List<Filter> subFilters)
    {
      this.subFilters = subFilters;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitAndFilter(p, subFilters);
    }

  }

  private static final class ApproxMatchImpl extends Impl
  {

    private final ByteString assertionValue;
    private final String attributeDescription;



    public ApproxMatchImpl(String attributeDescription,
        ByteString assertionValue)
    {
      this.attributeDescription = attributeDescription;
      this.assertionValue = assertionValue;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitApproxMatchFilter(p, attributeDescription,
          assertionValue);
    }

  }

  private static final class EqualityMatchImpl extends Impl
  {

    private final ByteString assertionValue;
    private final String attributeDescription;



    public EqualityMatchImpl(String attributeDescription,
        ByteString assertionValue)
    {
      this.attributeDescription = attributeDescription;
      this.assertionValue = assertionValue;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitEqualityMatchFilter(p, attributeDescription,
          assertionValue);
    }

  }

  private static final class ExtensibleMatchImpl extends Impl
  {
    private final String attributeDescription;
    private final boolean dnAttributes;
    private final String matchingRule;
    private final ByteString matchValue;



    public ExtensibleMatchImpl(String matchingRule,
        String attributeDescription, ByteString matchValue,
        boolean dnAttributes)
    {
      this.matchingRule = matchingRule;
      this.attributeDescription = attributeDescription;
      this.matchValue = matchValue;
      this.dnAttributes = dnAttributes;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitExtensibleMatchFilter(p, matchingRule,
          attributeDescription, matchValue, dnAttributes);
    }

  }

  private static final class GreaterOrEqualImpl extends Impl
  {

    private final ByteString assertionValue;
    private final String attributeDescription;



    public GreaterOrEqualImpl(String attributeDescription,
        ByteString assertionValue)
    {
      this.attributeDescription = attributeDescription;
      this.assertionValue = assertionValue;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitGreaterOrEqualFilter(p, attributeDescription,
          assertionValue);
    }

  }

  private static abstract class Impl
  {
    protected Impl()
    {
      // Nothing to do.
    }



    public abstract <R, P> R accept(FilterVisitor<R, P> v, P p);
  }

  private static final class LessOrEqualImpl extends Impl
  {

    private final ByteString assertionValue;
    private final String attributeDescription;



    public LessOrEqualImpl(String attributeDescription,
        ByteString assertionValue)
    {
      this.attributeDescription = attributeDescription;
      this.assertionValue = assertionValue;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitLessOrEqualFilter(p, attributeDescription,
          assertionValue);
    }

  }

  private static final class NotImpl extends Impl
  {
    private final Filter subFilter;



    public NotImpl(Filter subFilter)
    {
      this.subFilter = subFilter;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitNotFilter(p, subFilter);
    }

  }

  private static final class OrImpl extends Impl
  {
    private final List<Filter> subFilters;



    public OrImpl(List<Filter> subFilters)
    {
      this.subFilters = subFilters;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitOrFilter(p, subFilters);
    }

  }

  private static final class PresentImpl extends Impl
  {

    private final String attributeDescription;



    public PresentImpl(String attributeDescription)
    {
      this.attributeDescription = attributeDescription;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitPresentFilter(p, attributeDescription);
    }

  }

  private static final class SubstringsImpl extends Impl
  {

    private final List<ByteString> anyStrings;
    private final String attributeDescription;
    private final ByteString finalString;
    private final ByteString initialString;



    public SubstringsImpl(String attributeDescription,
        ByteString initialString, List<ByteString> anyStrings,
        ByteString finalString)
    {
      this.attributeDescription = attributeDescription;
      this.initialString = initialString;
      this.anyStrings = anyStrings;
      this.finalString = finalString;

    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitSubstringsFilter(p, attributeDescription,
          initialString, anyStrings, finalString);
    }

  }

  private static final class UnrecognizedImpl extends Impl
  {

    private final ByteString filterBytes;
    private final byte filterTag;



    public UnrecognizedImpl(byte filterTag, ByteString filterBytes)
    {
      this.filterTag = filterTag;
      this.filterBytes = filterBytes;
    }



    public <R, P> R accept(FilterVisitor<R, P> v, P p)
    {
      return v.visitUnrecognizedFilter(p, filterTag, filterBytes);
    }

  }



  private static final FilterVisitor<IOException, ASN1Writer> ASN1_ENCODER =
      new FilterVisitor<IOException, ASN1Writer>()
      {

        public IOException visitAndFilter(ASN1Writer writer,
            List<Filter> subFilters)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_AND);
            for (Filter subFilter : subFilters)
            {
              IOException e = subFilter.accept(this, writer);
              if (e != null)
              {
                return e;
              }
            }
            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitApproxMatchFilter(ASN1Writer writer,
            String attributeDescription, ByteString assertionValue)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_APPROXIMATE);
            writer.writeOctetString(attributeDescription);
            writer.writeOctetString(assertionValue);
            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitEqualityMatchFilter(ASN1Writer writer,
            String attributeDescription, ByteString assertionValue)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_EQUALITY);
            writer.writeOctetString(attributeDescription);
            writer.writeOctetString(assertionValue);
            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitExtensibleMatchFilter(
            ASN1Writer writer, String matchingRule,
            String attributeDescription, ByteString assertionValue,
            boolean dnAttributes)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_EXTENSIBLE_MATCH);

            if (matchingRule != null)
            {
              writer.writeOctetString(TYPE_MATCHING_RULE_ID,
                  matchingRule);
            }

            if (attributeDescription != null)
            {
              writer.writeOctetString(TYPE_MATCHING_RULE_TYPE,
                  attributeDescription);
            }

            writer.writeOctetString(TYPE_MATCHING_RULE_VALUE,
                assertionValue);

            if (dnAttributes)
            {
              writer.writeBoolean(TYPE_MATCHING_RULE_DN_ATTRIBUTES,
                  true);
            }

            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitGreaterOrEqualFilter(ASN1Writer writer,
            String attributeDescription, ByteString assertionValue)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_GREATER_OR_EQUAL);
            writer.writeOctetString(attributeDescription);
            writer.writeOctetString(assertionValue);
            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitLessOrEqualFilter(ASN1Writer writer,
            String attributeDescription, ByteString assertionValue)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_LESS_OR_EQUAL);
            writer.writeOctetString(attributeDescription);
            writer.writeOctetString(assertionValue);
            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitNotFilter(ASN1Writer writer,
            Filter subFilter)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_NOT);
            IOException e = subFilter.accept(this, writer);
            if (e != null)
            {
              return e;
            }
            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitOrFilter(ASN1Writer writer,
            List<Filter> subFilters)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_OR);
            for (Filter subFilter : subFilters)
            {
              IOException e = subFilter.accept(this, writer);
              if (e != null)
              {
                return e;
              }
            }
            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitPresentFilter(ASN1Writer writer,
            String attributeDescription)
        {
          try
          {
            writer.writeOctetString(TYPE_FILTER_PRESENCE,
                attributeDescription);
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitSubstringsFilter(ASN1Writer writer,
            String attributeDescription, ByteString initialSubstring,
            List<ByteString> anySubstrings, ByteString finalSubstring)
        {
          try
          {
            writer.writeStartSequence(TYPE_FILTER_SUBSTRING);
            writer.writeOctetString(attributeDescription);

            writer.writeStartSequence();
            if (initialSubstring != null)
            {
              writer
                  .writeOctetString(TYPE_SUBINITIAL, initialSubstring);
            }

            for (ByteString anySubstring : anySubstrings)
            {
              writer.writeOctetString(TYPE_SUBANY, anySubstring);
            }

            if (finalSubstring != null)
            {
              writer.writeOctetString(TYPE_SUBFINAL, finalSubstring);
            }
            writer.writeEndSequence();

            writer.writeEndSequence();
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }



        public IOException visitUnrecognizedFilter(ASN1Writer writer,
            byte filterTag, ByteString filterBytes)
        {
          try
          {
            writer.writeOctetString(filterTag, filterBytes);
            return null;
          }
          catch (IOException e)
          {
            return e;
          }
        }
      };

  // RFC 4526 - FALSE filter.
  private static final Filter FALSE =
      new Filter(new OrImpl(Collections.<Filter> emptyList()));

  // Heavily used (objectClass=*) filter.
  private static final Filter OBJECT_CLASS_PRESENT =
      new Filter(new PresentImpl("objectClass"));

  private static final FilterVisitor<StringBuilder, StringBuilder> TO_STRING_VISITOR =
      new FilterVisitor<StringBuilder, StringBuilder>()
      {

        public StringBuilder visitAndFilter(StringBuilder builder,
            List<Filter> subFilters)
        {
          builder.append("(&");
          for (Filter subFilter : subFilters)
          {
            subFilter.accept(this, builder);
          }
          builder.append(')');
          return builder;
        }



        public StringBuilder visitApproxMatchFilter(
            StringBuilder builder, String attributeDescription,
            ByteString assertionValue)
        {
          builder.append('(');
          builder.append(attributeDescription);
          builder.append("~=");
          valueToFilterString(builder, assertionValue);
          builder.append(')');
          return builder;
        }



        public StringBuilder visitEqualityMatchFilter(
            StringBuilder builder, String attributeDescription,
            ByteString assertionValue)
        {
          builder.append('(');
          builder.append(attributeDescription);
          builder.append("=");
          valueToFilterString(builder, assertionValue);
          builder.append(')');
          return builder;
        }



        public StringBuilder visitExtensibleMatchFilter(
            StringBuilder builder, String matchingRule,
            String attributeDescription, ByteString assertionValue,
            boolean dnAttributes)
        {
          builder.append('(');

          if (attributeDescription != null)
          {
            builder.append(attributeDescription);
          }

          if (dnAttributes)
          {
            builder.append(":dn");
          }

          if (matchingRule != null)
          {
            builder.append(':');
            builder.append(matchingRule);
          }

          builder.append(":=");
          valueToFilterString(builder, assertionValue);
          builder.append(')');
          return builder;
        }



        public StringBuilder visitGreaterOrEqualFilter(
            StringBuilder builder, String attributeDescription,
            ByteString assertionValue)
        {
          builder.append('(');
          builder.append(attributeDescription);
          builder.append(">=");
          valueToFilterString(builder, assertionValue);
          builder.append(')');
          return builder;
        }



        public StringBuilder visitLessOrEqualFilter(
            StringBuilder builder, String attributeDescription,
            ByteString assertionValue)
        {
          builder.append('(');
          builder.append(attributeDescription);
          builder.append("<=");
          valueToFilterString(builder, assertionValue);
          builder.append(')');
          return builder;
        }



        public StringBuilder visitNotFilter(StringBuilder builder,
            Filter subFilter)
        {
          builder.append("(|");
          subFilter.accept(this, builder);
          builder.append(')');
          return builder;
        }



        public StringBuilder visitOrFilter(StringBuilder builder,
            List<Filter> subFilters)
        {
          builder.append("(|");
          for (Filter subFilter : subFilters)
          {
            subFilter.accept(this, builder);
          }
          builder.append(')');
          return builder;
        }



        public StringBuilder visitPresentFilter(StringBuilder builder,
            String attributeDescription)
        {
          builder.append('(');
          builder.append(attributeDescription);
          builder.append("=*)");
          return builder;
        }



        public StringBuilder visitSubstringsFilter(
            StringBuilder builder, String attributeDescription,
            ByteString initialSubstring,
            List<ByteString> anySubstrings, ByteString finalSubstring)
        {
          builder.append('(');
          builder.append(attributeDescription);
          builder.append("=");
          if (initialSubstring != null)
          {
            valueToFilterString(builder, initialSubstring);
          }
          for (ByteString anySubstring : anySubstrings)
          {
            builder.append('*');
            valueToFilterString(builder, anySubstring);
          }
          builder.append('*');
          if (finalSubstring != null)
          {
            valueToFilterString(builder, finalSubstring);
          }
          builder.append(')');
          return builder;
        }



        public StringBuilder visitUnrecognizedFilter(
            StringBuilder builder, byte filterTag,
            ByteString filterBytes)
        {
          // Fake up a representation.
          builder.append('(');
          builder.append(byteToHex(filterTag));
          builder.append(':');
          builder.append(filterBytes.toHex());
          builder.append(')');
          return builder;
        }
      };

  // RFC 4526 - TRUE filter.
  private static final Filter TRUE =
      new Filter(new AndImpl(Collections.<Filter> emptyList()));



  /**
   * Reads the next ASN.1 element from the provided {@code ASN1Reader}
   * as a {@code Filter}.
   *
   * @param reader
   *          The {@code ASN1Reader} from which the ASN.1 encoded
   *          {@code Filter} should be read.
   * @return The decoded {@code Filter}.
   * @throws IOException
   *           If an error occurs while reading from {@code reader}.
   */
  public static Filter decode(ASN1Reader reader) throws IOException
  {
    byte type = reader.peekType();

    switch (type)
    {
    case TYPE_FILTER_AND:
      return decodeAndFilter(reader);

    case TYPE_FILTER_OR:
      return decodeOrFilter(reader);

    case TYPE_FILTER_NOT:
      return decodeNotFilter(reader);

    case TYPE_FILTER_EQUALITY:
      return decodeEqualityMatchFilter(reader);

    case TYPE_FILTER_GREATER_OR_EQUAL:
      return decodeGreaterOrEqualMatchFilter(reader);

    case TYPE_FILTER_LESS_OR_EQUAL:
      return decodeLessOrEqualMatchFilter(reader);

    case TYPE_FILTER_APPROXIMATE:
      return decodeApproxMatchFilter(reader);

    case TYPE_FILTER_SUBSTRING:
      return decodeSubstringsFilter(reader);

    case TYPE_FILTER_PRESENCE:
      return new Filter(new PresentImpl(reader
          .readOctetStringAsString(type)));

    case TYPE_FILTER_EXTENSIBLE_MATCH:
      return decodeExtensibleMatchFilter(reader);

    default:
      return new Filter(new UnrecognizedImpl(type, reader
          .readOctetString(type)));
    }
  }



  /**
   * Returns the {@code absolute false} filter as defined in RFC 4526
   * which is comprised of an {@code or} filter containing zero
   * components.
   *
   * @return The absolute false filter.
   * @see <a href="http://tools.ietf.org/html/rfc4526">RFC 4526</a>
   */
  public static Filter getAbsoluteFalseFilter()
  {
    return FALSE;
  }



  /**
   * Returns the {@code absolute true} filter as defined in RFC 4526
   * which is comprised of an {@code and} filter containing zero
   * components.
   *
   * @return The absolute true filter.
   * @see <a href="http://tools.ietf.org/html/rfc4526">RFC 4526</a>
   */
  public static Filter getAbsoluteTrueFilter()
  {
    return TRUE;
  }



  /**
   * Returns the {@code objectClass} presence filter {@code
   * (objectClass=*)}.
   * <p>
   * A call to this method is equivalent to but more efficient than the
   * following code:
   *
   * <pre>
   * Filter.present(&quot;objectClass&quot;);
   * </pre>
   *
   * @return The {@code objectClass} presence filter {@code
   *         (objectClass=*)}.
   */
  public static Filter getObjectClassPresentFilter()
  {
    return OBJECT_CLASS_PRESENT;
  }



  /**
   * Creates a new {@code and} filter using the provided list of
   * sub-filters.
   * <p>
   * Creating a new {@code and} filter with a {@code null} or empty list
   * of sub-filters is equivalent to calling
   * {@link #getAbsoluteTrueFilter()}.
   *
   * @param subFilters
   *          The list of sub-filters, may be empty or {@code null}.
   * @return The newly created {@code and} filter.
   */
  public static Filter newAndFilter(Filter... subFilters)
  {
    if (subFilters == null || subFilters.length == 0)
    {
      // RFC 4526 - TRUE filter.
      return getAbsoluteTrueFilter();
    }
    else if (subFilters.length == 1)
    {
      Validator.ensureNotNull(subFilters[0]);
      return new Filter(new AndImpl(Collections
          .singletonList(subFilters[0])));
    }
    else
    {
      List<Filter> subFiltersList =
          new ArrayList<Filter>(subFilters.length);
      for (Filter subFilter : subFilters)
      {
        Validator.ensureNotNull(subFilter);
        subFiltersList.add(subFilter);
      }
      return new Filter(new AndImpl(Collections
          .unmodifiableList(subFiltersList)));
    }
  }



  /**
   * Creates a new {@code approximate match} filter using the provided
   * attribute description and assertion value.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param assertionValue
   *          The assertion value.
   * @return The newly created {@code approximate match} filter.
   */
  public static Filter newApproxMatchFilter(
      AttributeDescription attributeDescription,
      ByteString assertionValue)
  {
    return newApproxMatchFilter(attributeDescription.toString(),
        assertionValue);
  }



  /**
   * Creates a new {@code approximate match} filter using the provided
   * attribute description and assertion value.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param assertionValue
   *          The assertion value.
   * @return The newly created {@code approximate match} filter.
   */
  public static Filter newApproxMatchFilter(
      String attributeDescription, ByteString assertionValue)
  {
    Validator.ensureNotNull(attributeDescription);
    Validator.ensureNotNull(assertionValue);
    return new Filter(new ApproxMatchImpl(attributeDescription,
        assertionValue));
  }



  /**
   * Creates a new {@code equality match} filter using the provided
   * attribute description and assertion value.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param assertionValue
   *          The assertion value.
   * @return The newly created {@code equality match} filter.
   */
  public static Filter newEqualityMatchFilter(
      AttributeDescription attributeDescription,
      ByteString assertionValue)
  {
    return newEqualityMatchFilter(attributeDescription.toString(),
        assertionValue);
  }



  /**
   * Creates a new {@code equality match} filter using the provided
   * attribute description and assertion value.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param assertionValue
   *          The assertion value.
   * @return The newly created {@code equality match} filter.
   */
  public static Filter newEqualityMatchFilter(
      String attributeDescription, ByteString assertionValue)
  {
    Validator.ensureNotNull(attributeDescription);
    Validator.ensureNotNull(assertionValue);
    return new Filter(new EqualityMatchImpl(attributeDescription,
        assertionValue));
  }



  /**
   * Creates a new {@code extensible match} filter.
   *
   * @param matchingRule
   *          The matching rule name, may be {@code null} if {@code
   *          attributeDescription} is specified.
   * @param attributeDescription
   *          The attribute description, may be {@code null} if {@code
   *          matchingRule} is specified.
   * @param assertionValue
   *          The assertion value.
   * @param dnAttributes
   *          Indicates whether DN matching should be performed.
   * @return The newly created {@code extensible match} filter.
   */
  public static Filter newExtensibleMatchFilter(String matchingRule,
      AttributeDescription attributeDescription,
      ByteString assertionValue, boolean dnAttributes)
  {
    return newExtensibleMatchFilter(matchingRule, attributeDescription,
        assertionValue, dnAttributes);
  }



  /**
   * Creates a new {@code extensible match} filter.
   *
   * @param matchingRule
   *          The matching rule name, may be {@code null} if {@code
   *          attributeDescription} is specified.
   * @param attributeDescription
   *          The attribute description, may be {@code null} if {@code
   *          matchingRule} is specified.
   * @param assertionValue
   *          The assertion value.
   * @param dnAttributes
   *          Indicates whether DN matching should be performed.
   * @return The newly created {@code extensible match} filter.
   */
  public static Filter newExtensibleMatchFilter(String matchingRule,
      String attributeDescription, ByteString assertionValue,
      boolean dnAttributes)
  {
    Validator.ensureTrue(matchingRule != null
        || attributeDescription != null);
    Validator.ensureNotNull(assertionValue);
    return new Filter(new ExtensibleMatchImpl(matchingRule,
        attributeDescription, assertionValue, dnAttributes));
  }



  /**
   * Creates a new {@code greater or equal} filter using the provided
   * attribute description and assertion value.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param assertionValue
   *          The assertion value.
   * @return The newly created {@code greater or equal} filter.
   */
  public static Filter newGreaterOrEqualFilter(
      AttributeDescription attributeDescription,
      ByteString assertionValue)
  {
    return newGreaterOrEqualFilter(attributeDescription.toString(),
        assertionValue);
  }



  /**
   * Creates a new {@code greater or equal} filter using the provided
   * attribute description and assertion value.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param assertionValue
   *          The assertion value.
   * @return The newly created {@code greater or equal} filter.
   */
  public static Filter newGreaterOrEqualFilter(
      String attributeDescription, ByteString assertionValue)
  {
    Validator.ensureNotNull(attributeDescription);
    Validator.ensureNotNull(assertionValue);
    return new Filter(new GreaterOrEqualImpl(attributeDescription,
        assertionValue));
  }



  /**
   * Creates a new {@code less or equal} filter using the provided
   * attribute description and assertion value.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param assertionValue
   *          The assertion value.
   * @return The newly created {@code less or equal} filter.
   */
  public static Filter newLessOrEqualFilter(
      AttributeDescription attributeDescription,
      ByteString assertionValue)
  {
    return newLessOrEqualFilter(attributeDescription.toString(),
        assertionValue);
  }



  /**
   * Creates a new {@code less or equal} filter using the provided
   * attribute description and assertion value.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param assertionValue
   *          The assertion value.
   * @return The newly created {@code less or equal} filter.
   */
  public static Filter newLessOrEqualFilter(
      String attributeDescription, ByteString assertionValue)
  {
    Validator.ensureNotNull(attributeDescription);
    Validator.ensureNotNull(assertionValue);
    return new Filter(new LessOrEqualImpl(attributeDescription,
        assertionValue));
  }



  /**
   * Creates a new {@code not} filter using the provided sub-filter.
   *
   * @param subFilter
   *          The sub-filter.
   * @return The newly created {@code not} filter.
   */
  public static Filter newNotFilter(Filter subFilter)
  {
    Validator.ensureNotNull(subFilter);
    return new Filter(new NotImpl(subFilter));
  }



  /**
   * Creates a new {@code or} filter using the provided list of
   * sub-filters.
   * <p>
   * Creating a new {@code or} filter with a {@code null} or empty list
   * of sub-filters is equivalent to calling
   * {@link #getAbsoluteFalseFilter()}.
   *
   * @param subFilters
   *          The list of sub-filters, may be empty or {@code null}.
   * @return The newly created {@code or} filter.
   */
  public static Filter newOrFilter(Filter... subFilters)
  {
    if (subFilters == null || subFilters.length == 0)
    {
      // RFC 4526 - FALSE filter.
      return getAbsoluteFalseFilter();
    }
    else if (subFilters.length == 1)
    {
      Validator.ensureNotNull(subFilters[0]);
      return new Filter(new OrImpl(Collections
          .singletonList(subFilters[0])));
    }
    else
    {
      List<Filter> subFiltersList =
          new ArrayList<Filter>(subFilters.length);
      for (Filter subFilter : subFilters)
      {
        Validator.ensureNotNull(subFilter);
        subFiltersList.add(subFilter);
      }
      return new Filter(new OrImpl(Collections
          .unmodifiableList(subFiltersList)));
    }
  }



  /**
   * Creates a new {@code present} filter using the provided attribute
   * description.
   *
   * @param attributeDescription
   *          The attribute description.
   * @return The newly created {@code present} filter.
   */
  public static Filter newPresentFilter(
      AttributeDescription attributeDescription)
  {
    return newPresentFilter(attributeDescription.toString());
  }



  /**
   * Creates a new {@code present} filter using the provided attribute
   * description.
   *
   * @param attributeDescription
   *          The attribute description.
   * @return The newly created {@code present} filter.
   */
  public static Filter newPresentFilter(String attributeDescription)
  {
    Validator.ensureNotNull(attributeDescription);
    if (toLowerCase(attributeDescription).equals("objectclass"))
    {
      return OBJECT_CLASS_PRESENT;
    }
    return new Filter(new PresentImpl(attributeDescription));
  }



  /**
   * Creates a new {@code substrings} filter using the provided
   * attribute description, {@code initial}, {@code final}, and {@code
   * any} sub-strings.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param initialSubstring
   *          The initial sub-string, may be {@code null} if either
   *          {@code finalSubstring} or {@code anySubstrings} are
   *          specified.
   * @param finalSubstring
   *          The final sub-string, may be {@code null}, may be {@code
   *          null} if either {@code initialSubstring} or {@code
   *          anySubstrings} are specified.
   * @param anySubstrings
   *          The final sub-string, may be {@code null} or empty if
   *          either {@code finalSubstring} or {@code initialSubstring}
   *          are specified.
   * @return The newly created {@code substrings} filter.
   */
  public static Filter newSubstringsFilter(
      AttributeDescription attributeDescription,
      ByteString initialSubstring, ByteString finalSubstring,
      ByteString... anySubstrings)
  {
    return newSubstringsFilter(attributeDescription.toString(),
        initialSubstring, finalSubstring, anySubstrings);
  }



  /**
   * Creates a new {@code substrings} filter using the provided
   * attribute description, {@code initial}, {@code final}, and {@code
   * any} sub-strings.
   *
   * @param attributeDescription
   *          The attribute description.
   * @param initialSubstring
   *          The initial sub-string, may be {@code null} if either
   *          {@code finalSubstring} or {@code anySubstrings} are
   *          specified.
   * @param finalSubstring
   *          The final sub-string, may be {@code null}, may be {@code
   *          null} if either {@code initialSubstring} or {@code
   *          anySubstrings} are specified.
   * @param anySubstrings
   *          The final sub-string, may be {@code null} or empty if
   *          either {@code finalSubstring} or {@code initialSubstring}
   *          are specified.
   * @return The newly created {@code substrings} filter.
   */
  public static Filter newSubstringsFilter(String attributeDescription,
      ByteString initialSubstring, ByteString finalSubstring,
      ByteString... anySubstrings)
  {
    Validator.ensureNotNull(attributeDescription);
    Validator.ensureTrue(initialSubstring != null
        || finalSubstring != null
        || (anySubstrings != null && anySubstrings.length > 0));

    List<ByteString> anySubstringList;
    if (anySubstrings == null || anySubstrings.length == 0)
    {
      anySubstringList = Collections.emptyList();
    }
    else if (anySubstrings.length == 1)
    {
      Validator.ensureNotNull(anySubstrings[0]);
      anySubstringList = Collections.singletonList(anySubstrings[0]);
    }
    else
    {
      anySubstringList =
          new ArrayList<ByteString>(anySubstrings.length);
      for (ByteString anySubstring : anySubstrings)
      {
        Validator.ensureNotNull(anySubstring);

        anySubstringList.add(anySubstring);
      }
      anySubstringList = Collections.unmodifiableList(anySubstringList);
    }

    return new Filter(new SubstringsImpl(attributeDescription,
        initialSubstring, anySubstringList, finalSubstring));
  }



  /**
   * Creates a new {@code unrecognized} filter using the provided ASN1
   * filter tag and content. This type of filter should be used for
   * filters which are not part of the standard filter definition.
   *
   * @param filterTag
   *          The ASN.1 tag.
   * @param filterBytes
   *          The filter content.
   * @return The newly created {@code unrecognized} filter.
   */
  public static Filter newUnrecognizedFilter(byte filterTag,
      ByteString filterBytes)
  {
    Validator.ensureNotNull(filterBytes);
    return new Filter(new UnrecognizedImpl(filterTag, filterBytes));
  }



  /**
   * Parses the provided LDAP string representation of a filter as a
   * {@code Filter}.
   *
   * @param string
   *          The LDAP string representation of a filter.
   * @return The parsed {@code Filter}.
   * @throws IllegalFilterException
   *           If {@code string} is not a valid LDAP string
   *           representation of a filter.
   */
  public static Filter valueOf(String string)
      throws IllegalFilterException
  {
    Validator.ensureNotNull(string);

    // If the filter is enclosed in a pair of single quotes it
    // is invalid (issue #1024).
    if (string.length() > 1 && string.startsWith("'")
        && string.endsWith("'"))
    {
      Message message =
          ERR_LDAP_FILTER_ENCLOSED_IN_APOSTROPHES.get(string);
      throw new IllegalFilterException(message);
    }

    if (string.startsWith("("))
    {
      if (string.endsWith(")"))
      {
        return valueOf0(string, 1, string.length() - 1);
      }
      else
      {
        Message message =
            ERR_LDAP_FILTER_MISMATCHED_PARENTHESES.get(string, 1,
                string.length());
        throw new IllegalFilterException(message);
      }
    }
    else
    {
      // We tolerate the top level filter component not being surrounded
      // by parentheses.
      return valueOf0(string, 0, string.length());
    }
  }



  // Decodes an and filter.
  private static Filter decodeAndFilter(ASN1Reader reader)
      throws IOException
  {
    reader.readStartSequence(TYPE_FILTER_AND);
    if (reader.hasNextElement())
    {
      List<Filter> subFilters = new LinkedList<Filter>();
      do
      {
        subFilters.add(decode(reader));
      }
      while (reader.hasNextElement());
      reader.readEndSequence();
      return new Filter(new AndImpl(Collections
          .unmodifiableList(subFilters)));
    }
    else
    {
      // No sub-filters - this is an RFC 4526 absolute true filter.
      reader.readEndSequence();
      return getAbsoluteTrueFilter();
    }
  }



  // Decodes an approximate match filter.
  private static Filter decodeApproxMatchFilter(ASN1Reader reader)
      throws IOException
  {
    reader.readStartSequence(TYPE_FILTER_APPROXIMATE);
    String attributeDescription = reader.readOctetStringAsString();
    ByteString assertionValue = reader.readOctetString();
    reader.readEndSequence();
    return new Filter(new ApproxMatchImpl(attributeDescription,
        assertionValue));
  }



  // Decodes an equality match filter.
  private static Filter decodeEqualityMatchFilter(ASN1Reader reader)
      throws IOException
  {
    reader.readStartSequence(TYPE_FILTER_EQUALITY);
    String attributeDescription = reader.readOctetStringAsString();
    ByteString assertionValue = reader.readOctetString();
    reader.readEndSequence();
    return new Filter(new EqualityMatchImpl(attributeDescription,
        assertionValue));
  }



  // Decodes an extensible match filter.
  private static Filter decodeExtensibleMatchFilter(ASN1Reader reader)
      throws IOException
  {
    reader.readStartSequence(TYPE_FILTER_EXTENSIBLE_MATCH);

    String matchingRule = null;
    if (reader.peekType() == TYPE_MATCHING_RULE_ID)
    {
      matchingRule =
          reader.readOctetStringAsString(TYPE_MATCHING_RULE_ID);
    }

    String attributeDescription = null;
    if (reader.peekType() == TYPE_MATCHING_RULE_TYPE)
    {
      attributeDescription =
          reader.readOctetStringAsString(TYPE_MATCHING_RULE_TYPE);
    }

    // FIXME: ensure that either matching rule or attribute
    // description are present.

    boolean dnAttributes = false;
    if (reader.hasNextElement()
        && reader.peekType() == TYPE_MATCHING_RULE_DN_ATTRIBUTES)
    {
      dnAttributes = reader.readBoolean();
    }

    ByteString assertionValue =
        reader.readOctetString(TYPE_MATCHING_RULE_VALUE);

    reader.readEndSequence();

    return new Filter(new ExtensibleMatchImpl(matchingRule,
        attributeDescription, assertionValue, dnAttributes));
  }



  // Decodes a greater than or equal filter.
  private static Filter decodeGreaterOrEqualMatchFilter(
      ASN1Reader reader) throws IOException
  {
    reader.readStartSequence(TYPE_FILTER_GREATER_OR_EQUAL);
    String attributeDescription = reader.readOctetStringAsString();
    ByteString assertionValue = reader.readOctetString();
    reader.readEndSequence();
    return new Filter(new GreaterOrEqualImpl(attributeDescription,
        assertionValue));
  }



  // Decodes a less than or equal filter.
  private static Filter decodeLessOrEqualMatchFilter(ASN1Reader reader)
      throws IOException
  {
    reader.readStartSequence(TYPE_FILTER_LESS_OR_EQUAL);
    String attributeDescription = reader.readOctetStringAsString();
    ByteString assertionValue = reader.readOctetString();
    reader.readEndSequence();
    return new Filter(new LessOrEqualImpl(attributeDescription,
        assertionValue));
  }



  // Decodes a not filter.
  private static Filter decodeNotFilter(ASN1Reader reader)
      throws IOException
  {
    reader.readStartSequence(TYPE_FILTER_NOT);
    Filter subFilter = decode(reader);
    reader.readEndSequence();
    return new Filter(new NotImpl(subFilter));
  }



  // Decodes an or filter.
  private static Filter decodeOrFilter(ASN1Reader reader)
      throws IOException
  {
    reader.readStartSequence(TYPE_FILTER_OR);
    if (reader.hasNextElement())
    {
      List<Filter> subFilters = new LinkedList<Filter>();
      do
      {
        subFilters.add(decode(reader));
      }
      while (reader.hasNextElement());
      reader.readEndSequence();
      return new Filter(new OrImpl(Collections
          .unmodifiableList(subFilters)));
    }
    else
    {
      // No sub-filters - this is an RFC 4526 absolute false filter.
      reader.readEndSequence();
      return getAbsoluteFalseFilter();
    }
  }



  // Decodes a sub-strings filter.
  private static Filter decodeSubstringsFilter(ASN1Reader reader)
      throws IOException
  {
    ByteString initialSubstring = null;
    LinkedList<ByteString> anySubstrings = null;
    ByteString finalSubstring = null;

    reader.readStartSequence(TYPE_FILTER_SUBSTRING);
    String attributeDescription = reader.readOctetStringAsString();
    reader.readStartSequence();

    // FIXME: There should be at least one element in this substring
    // filter sequence.
    if (reader.peekType() == TYPE_SUBINITIAL)
    {
      initialSubstring = reader.readOctetString(TYPE_SUBINITIAL);
    }

    if (reader.hasNextElement() && reader.peekType() == TYPE_SUBANY)
    {
      anySubstrings = new LinkedList<ByteString>();
      do
      {
        anySubstrings.add(reader.readOctetString(TYPE_SUBANY));
      }
      while (reader.hasNextElement()
          && reader.peekType() == TYPE_SUBANY);
    }

    if (reader.hasNextElement() && reader.peekType() == TYPE_SUBFINAL)
    {
      finalSubstring = reader.readOctetString(TYPE_SUBFINAL);
    }

    reader.readEndSequence();
    reader.readEndSequence();

    List<ByteString> tmp;

    if (anySubstrings == null)
    {
      tmp = Collections.emptyList();
    }
    else if (anySubstrings.size() == 1)
    {
      tmp = Collections.singletonList(anySubstrings.getFirst());
    }
    else
    {
      tmp = Collections.unmodifiableList(anySubstrings);
    }

    return new Filter(new SubstringsImpl(attributeDescription,
        initialSubstring, tmp, finalSubstring));
  }



  private static Filter valueOf0(String string,
      int beginIndex /* inclusive */, int endIndex /* exclusive */)
      throws IllegalFilterException
  {
    if (beginIndex >= endIndex)
    {
      Message message = ERR_LDAP_FILTER_STRING_NULL.get();
      throw new IllegalFilterException(message);
    }

    int index = beginIndex;
    char c = string.charAt(index);

    if (c == '&')
    {
      List<Filter> subFilters =
          valueOfFilterList(string, index + 1, endIndex);
      if (subFilters.isEmpty())
      {
        return getAbsoluteTrueFilter();
      }
      else
      {
        return new Filter(new AndImpl(subFilters));
      }
    }
    else if (c == '|')
    {
      List<Filter> subFilters =
          valueOfFilterList(string, index + 1, endIndex);
      if (subFilters.isEmpty())
      {
        return getAbsoluteFalseFilter();
      }
      else
      {
        return new Filter(new OrImpl(subFilters));
      }
    }
    else if (c == '!')
    {
      if ((string.charAt(index + 1) != '(')
          || (string.charAt(endIndex - 1) != ')'))
      {
        Message message =
            ERR_LDAP_FILTER_COMPOUND_MISSING_PARENTHESES.get(string,
                index, endIndex - 1);
        throw new IllegalFilterException(message);
      }

      Filter subFilter = valueOf0(string, index + 2, endIndex - 2);
      return new Filter(new NotImpl(subFilter));
    }
    else
    {
      // It must be a simple filter. It must have an equal sign at some
      // point, so find it.
      int equalPos = -1;
      for (int i = index; i < endIndex; i++)
      {
        if (string.charAt(i) == '=')
        {
          equalPos = i;
          break;
        }
      }

      // Look at the character immediately before the equal sign,
      // because it may help determine the filter type.
      String attributeDescription;
      ByteString assertionValue;

      switch (string.charAt(equalPos - 1))
      {
      case '~':
        attributeDescription =
            valueOfAttributeDescription(string, index, equalPos - 1);
        assertionValue =
            valueOfAssertionValue(string, equalPos + 1, endIndex);
        return new Filter(new ApproxMatchImpl(attributeDescription,
            assertionValue));
      case '>':
        attributeDescription =
            valueOfAttributeDescription(string, index, equalPos - 1);
        assertionValue =
            valueOfAssertionValue(string, equalPos + 1, endIndex);
        return new Filter(new GreaterOrEqualImpl(attributeDescription,
            assertionValue));
      case '<':
        attributeDescription =
            valueOfAttributeDescription(string, index, equalPos - 1);
        assertionValue =
            valueOfAssertionValue(string, equalPos + 1, endIndex);
        return new Filter(new LessOrEqualImpl(attributeDescription,
            assertionValue));
      case ':':
        return valueOfExtensibleFilter(string, index, equalPos,
            endIndex);
      default:
        attributeDescription =
            valueOfAttributeDescription(string, index, equalPos - 1);
        return valueOfGenericFilter(string, attributeDescription,
            equalPos + 1, endIndex);
      }
    }
  }



  private static ByteString valueOfAssertionValue(String string,
      int startIndex, int endIndex) throws IllegalFilterException
  {
    boolean hasEscape = false;
    byte[] valueBytes =
        getBytes(string.substring(startIndex, endIndex));
    for (int i = 0; i < valueBytes.length; i++)
    {
      if (valueBytes[i] == 0x5C) // The backslash character
      {
        hasEscape = true;
        break;
      }
    }

    if (hasEscape)
    {
      ByteStringBuilder valueBuffer =
          new ByteStringBuilder(valueBytes.length);
      for (int i = 0; i < valueBytes.length; i++)
      {
        if (valueBytes[i] == 0x5C) // The backslash character
        {
          // The next two bytes must be the hex characters that comprise
          // the binary value.
          if ((i + 2) >= valueBytes.length)
          {
            Message message =
                ERR_LDAP_FILTER_INVALID_ESCAPED_BYTE.get(string,
                    startIndex + i + 1);
            throw new IllegalFilterException(message);
          }

          byte byteValue = 0;
          switch (valueBytes[++i])
          {
          case 0x30: // '0'
            break;
          case 0x31: // '1'
            byteValue = (byte) 0x10;
            break;
          case 0x32: // '2'
            byteValue = (byte) 0x20;
            break;
          case 0x33: // '3'
            byteValue = (byte) 0x30;
            break;
          case 0x34: // '4'
            byteValue = (byte) 0x40;
            break;
          case 0x35: // '5'
            byteValue = (byte) 0x50;
            break;
          case 0x36: // '6'
            byteValue = (byte) 0x60;
            break;
          case 0x37: // '7'
            byteValue = (byte) 0x70;
            break;
          case 0x38: // '8'
            byteValue = (byte) 0x80;
            break;
          case 0x39: // '9'
            byteValue = (byte) 0x90;
            break;
          case 0x41: // 'A'
          case 0x61: // 'a'
            byteValue = (byte) 0xA0;
            break;
          case 0x42: // 'B'
          case 0x62: // 'b'
            byteValue = (byte) 0xB0;
            break;
          case 0x43: // 'C'
          case 0x63: // 'c'
            byteValue = (byte) 0xC0;
            break;
          case 0x44: // 'D'
          case 0x64: // 'd'
            byteValue = (byte) 0xD0;
            break;
          case 0x45: // 'E'
          case 0x65: // 'e'
            byteValue = (byte) 0xE0;
            break;
          case 0x46: // 'F'
          case 0x66: // 'f'
            byteValue = (byte) 0xF0;
            break;
          default:
            Message message =
                ERR_LDAP_FILTER_INVALID_ESCAPED_BYTE.get(string,
                    startIndex + i + 1);
            throw new IllegalFilterException(message);
          }

          switch (valueBytes[++i])
          {
          case 0x30: // '0'
            break;
          case 0x31: // '1'
            byteValue |= (byte) 0x01;
            break;
          case 0x32: // '2'
            byteValue |= (byte) 0x02;
            break;
          case 0x33: // '3'
            byteValue |= (byte) 0x03;
            break;
          case 0x34: // '4'
            byteValue |= (byte) 0x04;
            break;
          case 0x35: // '5'
            byteValue |= (byte) 0x05;
            break;
          case 0x36: // '6'
            byteValue |= (byte) 0x06;
            break;
          case 0x37: // '7'
            byteValue |= (byte) 0x07;
            break;
          case 0x38: // '8'
            byteValue |= (byte) 0x08;
            break;
          case 0x39: // '9'
            byteValue |= (byte) 0x09;
            break;
          case 0x41: // 'A'
          case 0x61: // 'a'
            byteValue |= (byte) 0x0A;
            break;
          case 0x42: // 'B'
          case 0x62: // 'b'
            byteValue |= (byte) 0x0B;
            break;
          case 0x43: // 'C'
          case 0x63: // 'c'
            byteValue |= (byte) 0x0C;
            break;
          case 0x44: // 'D'
          case 0x64: // 'd'
            byteValue |= (byte) 0x0D;
            break;
          case 0x45: // 'E'
          case 0x65: // 'e'
            byteValue |= (byte) 0x0E;
            break;
          case 0x46: // 'F'
          case 0x66: // 'f'
            byteValue |= (byte) 0x0F;
            break;
          default:
            Message message =
                ERR_LDAP_FILTER_INVALID_ESCAPED_BYTE.get(string,
                    startIndex + i + 1);
            throw new IllegalFilterException(message);
          }

          valueBuffer.append(byteValue);
        }
        else
        {
          valueBuffer.append(valueBytes[i]);
        }
      }

      return valueBuffer.toByteString();
    }
    else
    {
      return ByteString.wrap(valueBytes);
    }
  }



  private static String valueOfAttributeDescription(String string,
      int startIndex, int endIndex) throws IllegalFilterException
  {
    // The part of the filter string before the equal sign should be the
    // attribute type. Make sure that the characters it contains are
    // acceptable for attribute types, including those allowed by
    // attribute name exceptions (ASCII letters and digits, the dash,
    // and the underscore). We also need to allow attribute options,
    // which includes the semicolon and the equal sign.
    String attrType = string.substring(startIndex, endIndex);
    for (int i = 0; i < attrType.length(); i++)
    {
      switch (attrType.charAt(i))
      {
      case '-':
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
      case ';':
      case '=':
      case 'A':
      case 'B':
      case 'C':
      case 'D':
      case 'E':
      case 'F':
      case 'G':
      case 'H':
      case 'I':
      case 'J':
      case 'K':
      case 'L':
      case 'M':
      case 'N':
      case 'O':
      case 'P':
      case 'Q':
      case 'R':
      case 'S':
      case 'T':
      case 'U':
      case 'V':
      case 'W':
      case 'X':
      case 'Y':
      case 'Z':
      case '_':
      case 'a':
      case 'b':
      case 'c':
      case 'd':
      case 'e':
      case 'f':
      case 'g':
      case 'h':
      case 'i':
      case 'j':
      case 'k':
      case 'l':
      case 'm':
      case 'n':
      case 'o':
      case 'p':
      case 'q':
      case 'r':
      case 's':
      case 't':
      case 'u':
      case 'v':
      case 'w':
      case 'x':
      case 'y':
      case 'z':
        // These are all OK.
        break;

      case '.':
      case '/':
      case ':':
      case '<':
      case '>':
      case '?':
      case '@':
      case '[':
      case '\\':
      case ']':
      case '^':
      case '`':
        // These are not allowed, but they are explicitly called out
        // because they are included in the range of values between '-'
        // and 'z', and making sure all possible characters are included
        // can help make the switch statement more efficient. We'll fall
        // through to the default clause to reject them.
      default:
        Message message =
            ERR_LDAP_FILTER_INVALID_CHAR_IN_ATTR_TYPE.get(attrType,
                String.valueOf(attrType.charAt(i)), i);
        throw new IllegalFilterException(message);
      }
    }

    return attrType;
  }



  private static Filter valueOfExtensibleFilter(String string,
      int startIndex, int equalIndex, int endIndex)
      throws IllegalFilterException
  {
    String attributeDescription = null;
    boolean dnAttributes = false;
    String matchingRule = null;

    // Look at the first character. If it is a colon, then it must be
    // followed by either the string "dn" or the matching rule ID. If it
    // is not, then must be the attribute type.
    String lowerLeftStr =
        toLowerCase(string.substring(startIndex, equalIndex));
    if (string.charAt(startIndex) == ':')
    {
      // See if it starts with ":dn". Otherwise, it much be the matching
      // rule ID.
      if (lowerLeftStr.startsWith(":dn:"))
      {
        dnAttributes = true;

        if ((startIndex + 4) < (equalIndex - 1))
        {
          matchingRule =
              string.substring(startIndex + 4, equalIndex - 1);
        }
      }
      else
      {
        matchingRule = string.substring(startIndex + 1, equalIndex - 1);
      }
    }
    else
    {
      int colonPos = string.indexOf(':', startIndex);
      if (colonPos < 0)
      {
        Message message =
            ERR_LDAP_FILTER_EXTENSIBLE_MATCH_NO_COLON.get(string,
                startIndex);
        throw new IllegalFilterException(message);
      }

      attributeDescription = string.substring(startIndex, colonPos);

      // If there is anything left, then it should be ":dn" and/or ":"
      // followed by the matching rule ID.
      if (colonPos < (equalIndex - 1))
      {
        if (lowerLeftStr.startsWith(":dn:", colonPos - startIndex))
        {
          dnAttributes = true;

          if ((colonPos + 4) < (equalIndex - 1))
          {
            matchingRule =
                string.substring(colonPos + 4, equalIndex - 1);
          }
        }
        else
        {
          matchingRule = string.substring(colonPos + 1, equalIndex - 1);
        }
      }
    }

    // Parse out the attribute value.
    ByteString matchValue =
        valueOfAssertionValue(string, equalIndex + 1, endIndex);

    // Make sure that the filter has at least one of an attribute
    // description and/or a matching rule ID.
    if ((attributeDescription == null) && (matchingRule == null))
    {
      Message message =
          ERR_LDAP_FILTER_EXTENSIBLE_MATCH_NO_AD_OR_MR.get(string,
              startIndex);
      throw new IllegalFilterException(message);
    }

    return new Filter(new ExtensibleMatchImpl(matchingRule,
        attributeDescription, matchValue, dnAttributes));
  }



  private static List<Filter> valueOfFilterList(String string,
      int startIndex, int endIndex) throws IllegalFilterException
  {
    // If the end index is equal to the start index, then there are no
    // components.
    if (startIndex >= endIndex)
    {
      return Collections.emptyList();
    }

    // At least one sub-filter.
    Filter firstFilter = null;
    List<Filter> subFilters = null;

    // The first and last characters must be parentheses. If not, then
    // that's an error.
    if ((string.charAt(startIndex) != '(')
        || (string.charAt(endIndex - 1) != ')'))
    {
      Message message =
          ERR_LDAP_FILTER_COMPOUND_MISSING_PARENTHESES.get(string,
              startIndex, endIndex);
      throw new IllegalFilterException(message);
    }

    // Iterate through the characters in the value. Whenever an open
    // parenthesis is found, locate the corresponding close parenthesis
    // by counting the number of intermediate open/close parentheses.
    int pendingOpens = 0;
    int openIndex = -1;
    for (int i = startIndex; i < endIndex; i++)
    {
      char c = string.charAt(i);
      if (c == '(')
      {
        if (openIndex < 0)
        {
          openIndex = i;
        }
        pendingOpens++;
      }
      else if (c == ')')
      {
        pendingOpens--;
        if (pendingOpens == 0)
        {
          Filter subFilter = valueOf0(string, openIndex + 1, i - 1);
          if (subFilters != null)
          {
            subFilters.add(subFilter);
          }
          else if (firstFilter != null)
          {
            subFilters = new LinkedList<Filter>();
            subFilters.add(firstFilter);
            subFilters.add(subFilter);
            firstFilter = null;
          }
          else
          {
            firstFilter = subFilter;
          }
          openIndex = -1;
        }
        else if (pendingOpens < 0)
        {
          Message message =
              ERR_LDAP_FILTER_NO_CORRESPONDING_OPEN_PARENTHESIS.get(
                  string, i);
          throw new IllegalFilterException(message);
        }
      }
      else if (pendingOpens <= 0)
      {
        Message message =
            ERR_LDAP_FILTER_COMPOUND_MISSING_PARENTHESES.get(string,
                startIndex, endIndex);
        throw new IllegalFilterException(message);
      }
    }

    // At this point, we have parsed the entire set of filter
    // components. The list of open parenthesis positions must be empty.
    if (pendingOpens != 0)
    {
      Message message =
          ERR_LDAP_FILTER_NO_CORRESPONDING_CLOSE_PARENTHESIS.get(
              string, openIndex);
      throw new IllegalFilterException(message);
    }

    if (subFilters != null)
    {
      return Collections.unmodifiableList(subFilters);
    }
    else
    {
      return Collections.singletonList(firstFilter);
    }
  }



  private static Filter valueOfGenericFilter(String string,
      String attributeDescription, int startIndex, int endIndex)
      throws IllegalFilterException
  {
    if (startIndex >= endIndex)
    {
      // Equality filter with empty assertion value.
      return new Filter(new EqualityMatchImpl(attributeDescription,
          ByteString.empty()));
    }
    else if (endIndex - startIndex == 1
        && string.charAt(startIndex) == '*')
    {
      // Single asterisk is a present filter.
      return newPresentFilter(attributeDescription);
    }
    else
    {
      // Either an equality or substring filter.
      ByteString assertionValue =
          valueOfAssertionValue(string, startIndex, endIndex);

      ByteString initialString = null;
      ByteString finalString = null;
      LinkedList<ByteString> anyStrings = null;

      int lastAsteriskIndex = -1;
      int length = assertionValue.length();
      for (int i = 0; i < length; i++)
      {
        if (assertionValue.byteAt(i) == '*')
        {
          if (lastAsteriskIndex == -1)
          {
            if (i > 0)
            {
              // Got an initial substring.
              initialString = assertionValue.subSequence(0, i);
            }
            lastAsteriskIndex = i;
          }
          else
          {
            // Got an any substring.
            if (anyStrings == null)
            {
              anyStrings = new LinkedList<ByteString>();
            }

            int s = lastAsteriskIndex + 1;
            if (s == i)
            {
              // A zero length substring.
              Message message =
                  ERR_LDAP_FILTER_BAD_SUBSTRING.get(string, string
                      .subSequence(startIndex, endIndex));
              throw new IllegalFilterException(message);
            }

            anyStrings.add(assertionValue.subSequence(s, i));
            lastAsteriskIndex = i;
          }
        }
      }

      if (lastAsteriskIndex == length - 1)
      {
        // Got a final substring.
        finalString =
            assertionValue.subSequence(lastAsteriskIndex, length);
      }

      if (initialString == null && anyStrings == null
          && finalString == null)
      {
        return new Filter(new EqualityMatchImpl(attributeDescription,
            assertionValue));
      }
      else
      {
        List<ByteString> tmp;

        if (anyStrings == null)
        {
          tmp = Collections.emptyList();
        }
        else if (anyStrings.size() == 1)
        {
          tmp = Collections.singletonList(anyStrings.getFirst());
        }
        else
        {
          tmp = Collections.unmodifiableList(anyStrings);
        }

        return new Filter(new SubstringsImpl(attributeDescription,
            initialString, tmp, finalString));
      }
    }
  }



  /**
   * Appends a properly-cleaned version of the provided value to the
   * given builder so that it can be safely used in string
   * representations of this search filter. The formatting changes that
   * may be performed will be in compliance with the specification in
   * RFC 2254.
   *
   * @param builder
   *          The builder to which the "safe" version of the value will
   *          be appended.
   * @param value
   *          The value to be appended to the builder.
   */
  private static void valueToFilterString(StringBuilder builder,
      ByteString value)
  {
    // Get the binary representation of the value and iterate through
    // it to see if there are any unsafe characters. If there are,
    // then escape them and replace them with a two-digit hex
    // equivalent.
    builder.ensureCapacity(builder.length() + value.length());
    for (int i = 0; i < value.length(); i++)
    {
      // TODO: this is a bit overkill - it will escape all non-ascii
      // chars!
      byte b = value.byteAt(i);
      if (((b & 0x7F) != b) || // Not 7-bit clean
          (b <= 0x1F) || // Below the printable character range
          (b == 0x28) || // Open parenthesis
          (b == 0x29) || // Close parenthesis
          (b == 0x2A) || // Asterisk
          (b == 0x5C) || // Backslash
          (b == 0x7F)) // Delete character
      {
        builder.append('\\');
        builder.append(byteToHex(b));
      }
      else
      {
        builder.append((char) b);
      }
    }
  }



  private final Impl pimpl;



  private Filter(Impl pimpl)
  {
    this.pimpl = pimpl;
  }



  /**
   * Applies a {@code FilterVisitor} to this {@code Filter}.
   *
   * @param <R>
   *          The return type of the visitor's methods.
   * @param <P>
   *          The type of the additional parameters to the visitor's
   *          methods.
   * @param v
   *          The filter visitor.
   * @param p
   *          Optional additional visitor parameter.
   * @return A result as specified by the visitor.
   */
  public <R, P> R accept(FilterVisitor<R, P> v, P p)
  {
    return pimpl.accept(v, p);
  }



  /**
   * Writes the ASN.1 encoding of this {@code Filter} to the provided
   * {@code ASN1Writer}.
   *
   * @param writer
   *          The {@code ASN1Writer} to which the ASN.1 encoding of this
   *          {@code Filter} should be written.
   * @return The updated {@code ASN1Writer}.
   * @throws IOException
   *           If an error occurs while writing to the {@code writer}.
   */
  public ASN1Writer encode(ASN1Writer writer) throws IOException
  {
    IOException e = pimpl.accept(ASN1_ENCODER, writer);
    if (e != null)
    {
      throw e;
    }
    else
    {
      return writer;
    }
  }



  /**
   * Returns a {@code String} whose contents is the LDAP string
   * representation of this {@code Filter}.
   *
   * @return The LDAP string representation of this {@code Filter}.
   */
  @Override
  public final String toString()
  {
    StringBuilder builder = new StringBuilder();
    return toString(builder).toString();
  }



  /**
   * Appends the LDAP string representation of this {@code Filter} to
   * the provided {@code StringBuilder}.
   *
   * @param builder
   *          The {@code StringBuilder} to which the LDAP string
   *          representation of this {@code Filter} should be appended.
   * @return The updated {@code StringBuilder}.
   */
  public StringBuilder toString(StringBuilder builder)
  {
    return pimpl.accept(TO_STRING_VISITOR, builder);
  }

}
