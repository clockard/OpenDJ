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
 *      Copyright 2009-2010 Sun Microsystems, Inc.
 *      Portions copyright 2011 ForgeRock AS
 */

package org.opends.sdk.schema;



import static org.opends.sdk.CoreMessages.*;
import static org.opends.sdk.schema.SchemaConstants.EXTENSIBLE_OBJECT_OBJECTCLASS_OID;
import static org.opends.sdk.schema.SchemaConstants.OMR_GENERIC_ENUM_NAME;
import static org.opends.sdk.schema.SchemaConstants.SCHEMA_PROPERTY_APPROX_RULE;
import static org.opends.sdk.schema.SchemaConstants.TOP_OBJECTCLASS_NAME;
import static org.opends.sdk.schema.SchemaUtils.unmodifiableCopyOfExtraProperties;
import static org.opends.sdk.schema.SchemaUtils.unmodifiableCopyOfList;
import static org.opends.sdk.schema.SchemaUtils.unmodifiableCopyOfSet;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.opends.sdk.Attribute;
import org.opends.sdk.ByteString;
import org.opends.sdk.DecodeException;
import org.opends.sdk.Entry;

import com.forgerock.opendj.util.StaticUtils;
import com.forgerock.opendj.util.SubstringReader;
import com.forgerock.opendj.util.Validator;



/**
 * Schema builders should be used for incremental construction of new schemas.
 */
public final class SchemaBuilder
{

  private Map<Integer, DITStructureRule> id2StructureRules;

  private Map<String, List<AttributeType>> name2AttributeTypes;

  private Map<String, List<DITContentRule>> name2ContentRules;

  private Map<String, List<MatchingRule>> name2MatchingRules;

  private Map<String, List<MatchingRuleUse>> name2MatchingRuleUses;

  private Map<String, List<NameForm>> name2NameForms;

  private Map<String, List<ObjectClass>> name2ObjectClasses;

  private Map<String, List<DITStructureRule>> name2StructureRules;

  private Map<String, List<DITStructureRule>> nameForm2StructureRules;

  private Map<String, AttributeType> numericOID2AttributeTypes;

  private Map<String, DITContentRule> numericOID2ContentRules;

  private Map<String, MatchingRule> numericOID2MatchingRules;

  private Map<String, MatchingRuleUse> numericOID2MatchingRuleUses;

  private Map<String, NameForm> numericOID2NameForms;

  private Map<String, ObjectClass> numericOID2ObjectClasses;

  private Map<String, Syntax> numericOID2Syntaxes;

  private Map<String, List<NameForm>> objectClass2NameForms;

  private SchemaCompatOptions options;

  private List<LocalizableMessage> warnings;

  private Schema schema;

  // A unique ID which can be used to uniquely identify schemas
  // constructed without a name.
  private final AtomicInteger nextSchemaID = new AtomicInteger();



  /**
   * Creates a new schema builder with no schema elements and default
   * compatibility options.
   */
  public SchemaBuilder()
  {
    initBuilder(null);
  }



  /**
   * Creates a new schema builder containing all of the schema elements
   * contained in the provided subschema subentry. Any problems encountered
   * while parsing the entry can be retrieved using the returned schema's
   * {@link Schema#getWarnings()} method.
   *
   * @param entry
   *          The subschema subentry to be parsed.
   * @throws NullPointerException
   *           If {@code entry} was {@code null}.
   */
  public SchemaBuilder(final Entry entry) throws NullPointerException
  {
    initBuilder(entry.getName().toString());

    addSchema(entry, true);
  }



  /**
   * Creates a new schema builder containing all of the schema elements from the
   * provided schema and its compatibility options.
   *
   * @param schema
   *          The initial contents of the schema builder.
   * @throws NullPointerException
   *           If {@code schema} was {@code null}.
   */
  public SchemaBuilder(final Schema schema) throws NullPointerException
  {
    initBuilder(schema.getSchemaName());
    setSchemaCompatOptions(schema.getSchemaCompatOptions());
    addSchema(schema, true);
  }



  /**
   * Creates a new schema builder with no schema elements and default
   * compatibility options.
   *
   * @param schemaName
   *          The user-friendly name of this schema which may be used for
   *          debugging purposes.
   */
  public SchemaBuilder(final String schemaName)
  {
    initBuilder(schemaName);
  }



  /**
   * Adds the provided attribute type definition to this schema builder.
   *
   * @param definition
   *          The attribute type definition.
   * @param overwrite
   *          {@code true} if any existing attribute type with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws LocalizedIllegalArgumentException
   *           If the provided attribute type definition could not be parsed.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addAttributeType(final String definition,
      final boolean overwrite) throws ConflictingSchemaElementException,
      LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(definition);
    try
    {
      final SubstringReader reader = new SubstringReader(definition);

      // We'll do this a character at a time. First, skip over any
      // leading whitespace.
      reader.skipWhitespaces();

      if (reader.remaining() <= 0)
      {
        // This means that the definition was empty or contained only
        // whitespace. That is illegal.
        final LocalizableMessage message = ERR_ATTR_SYNTAX_ATTRTYPE_EMPTY_VALUE
            .get();
        throw new LocalizedIllegalArgumentException(message);
      }

      // The next character must be an open parenthesis. If it is not,
      // then that is an error.
      final char c = reader.read();
      if (c != '(')
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_ATTRTYPE_EXPECTED_OPEN_PARENTHESIS
            .get(definition, (reader.pos() - 1), String.valueOf(c));
        throw new LocalizedIllegalArgumentException(message);
      }

      // Skip over any spaces immediately following the opening
      // parenthesis.
      reader.skipWhitespaces();

      // The next set of characters must be the OID.
      final String oid = SchemaUtils.readOID(reader);

      List<String> names = Collections.emptyList();
      String description = "".intern();
      boolean isObsolete = false;
      String superiorType = null;
      String equalityMatchingRule = null;
      String orderingMatchingRule = null;
      String substringMatchingRule = null;
      String approximateMatchingRule = null;
      String syntax = null;
      boolean isSingleValue = false;
      boolean isCollective = false;
      boolean isNoUserModification = false;
      AttributeUsage attributeUsage = AttributeUsage.USER_APPLICATIONS;
      Map<String, List<String>> extraProperties = Collections.emptyMap();

      // At this point, we should have a pretty specific syntax that
      // describes what may come next, but some of the components are
      // optional and it would be pretty easy to put something in the
      // wrong order, so we will be very flexible about what we can
      // accept. Just look at the next token, figure out what it is and
      // how to treat what comes after it, then repeat until we get to
      // the end of the definition. But before we start, set default
      // values for everything else we might need to know.
      while (true)
      {
        final String tokenName = SchemaUtils.readTokenName(reader);

        if (tokenName == null)
        {
          // No more tokens.
          break;
        }
        else if (tokenName.equalsIgnoreCase("name"))
        {
          names = SchemaUtils.readNameDescriptors(reader);
        }
        else if (tokenName.equalsIgnoreCase("desc"))
        {
          // This specifies the description for the attribute type. It
          // is an arbitrary string of characters enclosed in single
          // quotes.
          description = SchemaUtils.readQuotedString(reader);
        }
        else if (tokenName.equalsIgnoreCase("obsolete"))
        {
          // This indicates whether the attribute type should be
          // considered obsolete. We do not need to do any more parsing
          // for this token.
          isObsolete = true;
        }
        else if (tokenName.equalsIgnoreCase("sup"))
        {
          // This specifies the name or OID of the superior attribute
          // type from which this attribute type should inherit its
          // properties.
          superiorType = SchemaUtils.readOID(reader);
        }
        else if (tokenName.equalsIgnoreCase("equality"))
        {
          // This specifies the name or OID of the equality matching
          // rule to use for this attribute type.
          equalityMatchingRule = SchemaUtils.readOID(reader);
        }
        else if (tokenName.equalsIgnoreCase("ordering"))
        {
          // This specifies the name or OID of the ordering matching
          // rule to use for this attribute type.
          orderingMatchingRule = SchemaUtils.readOID(reader);
        }
        else if (tokenName.equalsIgnoreCase("substr"))
        {
          // This specifies the name or OID of the substring matching
          // rule to use for this attribute type.
          substringMatchingRule = SchemaUtils.readOID(reader);
        }
        else if (tokenName.equalsIgnoreCase("syntax"))
        {
          // This specifies the numeric OID of the syntax for this
          // matching rule. It may optionally be immediately followed by
          // an open curly brace, an integer definition, and a close
          // curly brace to suggest the minimum number of characters
          // that should be allowed in values of that type. This
          // implementation will ignore any such length because it does
          // not impose any practical limit on the length of attribute
          // values.
          syntax = SchemaUtils.readOIDLen(reader);
        }
        else if (tokenName.equalsIgnoreCase("single-definition"))
        {
          // This indicates that attributes of this type are allowed to
          // have at most one definition. We do not need any more
          // parsing for this token.
          isSingleValue = true;
        }
        else if (tokenName.equalsIgnoreCase("single-value"))
        {
          // This indicates that attributes of this type are allowed to
          // have at most one value. We do not need any more parsing for
          // this token.
          isSingleValue = true;
        }
        else if (tokenName.equalsIgnoreCase("collective"))
        {
          // This indicates that attributes of this type are collective
          // (i.e., have their values generated dynamically in some
          // way). We do not need any more parsing for this token.
          isCollective = true;
        }
        else if (tokenName.equalsIgnoreCase("no-user-modification"))
        {
          // This indicates that the values of attributes of this type
          // are not to be modified by end users. We do not need any
          // more parsing for this token.
          isNoUserModification = true;
        }
        else if (tokenName.equalsIgnoreCase("usage"))
        {
          // This specifies the usage string for this attribute type. It
          // should be followed by one of the strings
          // "userApplications", "directoryOperation",
          // "distributedOperation", or "dSAOperation".
          int length = 0;

          reader.skipWhitespaces();
          reader.mark();

          while (reader.read() != ' ')
          {
            length++;
          }

          reader.reset();
          final String usageStr = reader.read(length);
          if (usageStr.equalsIgnoreCase("userapplications"))
          {
            attributeUsage = AttributeUsage.USER_APPLICATIONS;
          }
          else if (usageStr.equalsIgnoreCase("directoryoperation"))
          {
            attributeUsage = AttributeUsage.DIRECTORY_OPERATION;
          }
          else if (usageStr.equalsIgnoreCase("distributedoperation"))
          {
            attributeUsage = AttributeUsage.DISTRIBUTED_OPERATION;
          }
          else if (usageStr.equalsIgnoreCase("dsaoperation"))
          {
            attributeUsage = AttributeUsage.DSA_OPERATION;
          }
          else
          {
            final LocalizableMessage message = WARN_ATTR_SYNTAX_ATTRTYPE_INVALID_ATTRIBUTE_USAGE
                .get(String.valueOf(oid), usageStr);
            throw new LocalizedIllegalArgumentException(message);
          }
        }
        else if (tokenName.matches("^X-[A-Za-z_-]+$"))
        {
          // This must be a non-standard property and it must be
          // followed by either a single definition in single quotes or
          // an open parenthesis followed by one or more values in
          // single quotes separated by spaces followed by a close
          // parenthesis.
          if (extraProperties.isEmpty())
          {
            extraProperties = new HashMap<String, List<String>>();
          }
          extraProperties.put(tokenName, SchemaUtils.readExtensions(reader));
        }
        else
        {
          final LocalizableMessage message = ERR_ATTR_SYNTAX_ILLEGAL_TOKEN
              .get(tokenName);
          throw new LocalizedIllegalArgumentException(message);
        }
      }

      final List<String> approxRules = extraProperties
          .get(SCHEMA_PROPERTY_APPROX_RULE);
      if (approxRules != null && !approxRules.isEmpty())
      {
        approximateMatchingRule = approxRules.get(0);
      }

      if (!extraProperties.isEmpty())
      {
        extraProperties = Collections.unmodifiableMap(extraProperties);
      }

      final AttributeType attrType = new AttributeType(oid, names, description,
          isObsolete, superiorType, equalityMatchingRule, orderingMatchingRule,
          substringMatchingRule, approximateMatchingRule, syntax,
          isSingleValue, isCollective, isNoUserModification, attributeUsage,
          extraProperties, definition);

      addAttributeType(attrType, overwrite);
    }
    catch (final DecodeException e)
    {
      throw new LocalizedIllegalArgumentException(e.getMessageObject(), e
          .getCause());
    }
    return this;
  }



  /**
   * Adds the provided attribute type definition to this schema builder.
   *
   * @param oid
   *          The OID of the attribute type definition.
   * @param names
   *          The user-friendly names of the attribute type definition.
   * @param description
   *          The description of the attribute type definition.
   * @param obsolete
   *          {@code true} if the attribute type definition is obsolete,
   *          otherwise {@code false}.
   * @param superiorType
   *          The OID of the superior attribute type definition.
   * @param equalityMatchingRule
   *          The OID of the equality matching rule, which may be {@code null}
   *          indicating that the superior attribute type's matching rule should
   *          be used or, if none is defined, the default matching rule
   *          associated with the syntax.
   * @param orderingMatchingRule
   *          The OID of the ordering matching rule, which may be {@code null}
   *          indicating that the superior attribute type's matching rule should
   *          be used or, if none is defined, the default matching rule
   *          associated with the syntax.
   * @param substringMatchingRule
   *          The OID of the substring matching rule, which may be {@code null}
   *          indicating that the superior attribute type's matching rule should
   *          be used or, if none is defined, the default matching rule
   *          associated with the syntax.
   * @param approximateMatchingRule
   *          The OID of the approximate matching rule, which may be {@code
   *          null} indicating that the superior attribute type's matching rule
   *          should be used or, if none is defined, the default matching rule
   *          associated with the syntax.
   * @param syntax
   *          The OID of the syntax definition.
   * @param singleValue
   *          {@code true} if the attribute type definition is single-valued,
   *          otherwise {@code false}.
   * @param collective
   *          {@code true} if the attribute type definition is a collective
   *          attribute, otherwise {@code false}.
   * @param noUserModification
   *          {@code true} if the attribute type definition is read-only,
   *          otherwise {@code false}.
   * @param attributeUsage
   *          The intended use of the attribute type definition.
   * @param extraProperties
   *          A map containing additional properties associated with the
   *          attribute type definition.
   * @param overwrite
   *          {@code true} if any existing attribute type with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addAttributeType(final String oid,
      final List<String> names, final String description,
      final boolean obsolete, final String superiorType,
      final String equalityMatchingRule, final String orderingMatchingRule,
      final String substringMatchingRule, final String approximateMatchingRule,
      final String syntax, final boolean singleValue, final boolean collective,
      final boolean noUserModification, final AttributeUsage attributeUsage,
      final Map<String, List<String>> extraProperties, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    final AttributeType attrType = new AttributeType(oid,
        unmodifiableCopyOfList(names), description, obsolete, superiorType,
        equalityMatchingRule, orderingMatchingRule, substringMatchingRule,
        approximateMatchingRule, syntax, singleValue, collective,
        noUserModification, attributeUsage,
        unmodifiableCopyOfExtraProperties(extraProperties), null);
    addAttributeType(attrType, overwrite);
    return this;
  }



  /**
   * Adds the provided DIT content rule definition to this schema builder.
   *
   * @param definition
   *          The DIT content rule definition.
   * @param overwrite
   *          {@code true} if any existing DIT content rule with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws LocalizedIllegalArgumentException
   *           If the provided DIT content rule definition could not be parsed.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addDITContentRule(final String definition,
      final boolean overwrite) throws ConflictingSchemaElementException,
      LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(definition);
    try
    {
      final SubstringReader reader = new SubstringReader(definition);

      // We'll do this a character at a time. First, skip over any
      // leading whitespace.
      reader.skipWhitespaces();

      if (reader.remaining() <= 0)
      {
        // This means that the value was empty or contained only
        // whitespace. That is illegal.
        final LocalizableMessage message = ERR_ATTR_SYNTAX_DCR_EMPTY_VALUE
            .get();
        throw new LocalizedIllegalArgumentException(message);
      }

      // The next character must be an open parenthesis. If it is not,
      // then that is an error.
      final char c = reader.read();
      if (c != '(')
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_DCR_EXPECTED_OPEN_PARENTHESIS
            .get(definition, (reader.pos() - 1), String.valueOf(c));
        throw new LocalizedIllegalArgumentException(message);
      }

      // Skip over any spaces immediately following the opening
      // parenthesis.
      reader.skipWhitespaces();

      // The next set of characters must be the OID.
      final String structuralClass = SchemaUtils.readOID(reader);

      List<String> names = Collections.emptyList();
      String description = "".intern();
      boolean isObsolete = false;
      Set<String> auxiliaryClasses = Collections.emptySet();
      Set<String> optionalAttributes = Collections.emptySet();
      Set<String> prohibitedAttributes = Collections.emptySet();
      Set<String> requiredAttributes = Collections.emptySet();
      Map<String, List<String>> extraProperties = Collections.emptyMap();

      // At this point, we should have a pretty specific syntax that
      // describes what may come next, but some of the components are
      // optional and it would be pretty easy to put something in the
      // wrong order, so we will be very flexible about what we can
      // accept. Just look at the next token, figure out what it is and
      // how to treat what comes after it, then repeat until we get to
      // the end of the value. But before we start, set default values
      // for everything else we might need to know.
      while (true)
      {
        final String tokenName = SchemaUtils.readTokenName(reader);

        if (tokenName == null)
        {
          // No more tokens.
          break;
        }
        else if (tokenName.equalsIgnoreCase("name"))
        {
          names = SchemaUtils.readNameDescriptors(reader);
        }
        else if (tokenName.equalsIgnoreCase("desc"))
        {
          // This specifies the description for the attribute type. It
          // is an arbitrary string of characters enclosed in single
          // quotes.
          description = SchemaUtils.readQuotedString(reader);
        }
        else if (tokenName.equalsIgnoreCase("obsolete"))
        {
          // This indicates whether the attribute type should be
          // considered obsolete. We do not need to do any more parsing
          // for this token.
          isObsolete = true;
        }
        else if (tokenName.equalsIgnoreCase("aux"))
        {
          auxiliaryClasses = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.equalsIgnoreCase("must"))
        {
          requiredAttributes = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.equalsIgnoreCase("may"))
        {
          optionalAttributes = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.equalsIgnoreCase("not"))
        {
          prohibitedAttributes = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.matches("^X-[A-Za-z_-]+$"))
        {
          // This must be a non-standard property and it must be
          // followed by either a single definition in single quotes or
          // an open parenthesis followed by one or more values in
          // single quotes separated by spaces followed by a close
          // parenthesis.
          if (extraProperties.isEmpty())
          {
            extraProperties = new HashMap<String, List<String>>();
          }
          extraProperties.put(tokenName, SchemaUtils.readExtensions(reader));
        }
        else
        {
          final LocalizableMessage message = ERR_ATTR_SYNTAX_ILLEGAL_TOKEN
              .get(tokenName);
          throw new LocalizedIllegalArgumentException(message);
        }
      }

      if (!extraProperties.isEmpty())
      {
        extraProperties = Collections.unmodifiableMap(extraProperties);
      }

      final DITContentRule rule = new DITContentRule(structuralClass, names,
          description, isObsolete, auxiliaryClasses, optionalAttributes,
          prohibitedAttributes, requiredAttributes, extraProperties, definition);
      addDITContentRule(rule, overwrite);
    }
    catch (final DecodeException e)
    {
      throw new LocalizedIllegalArgumentException(e.getMessageObject(), e
          .getCause());
    }
    return this;
  }



  /**
   * Adds the provided DIT content rule definition to this schema builder.
   *
   * @param structuralClass
   *          The name of the structural object class to which the DIT content
   *          rule applies.
   * @param names
   *          The user-friendly names of the DIT content rule definition.
   * @param description
   *          The description of the DIT content rule definition.
   * @param obsolete
   *          {@code true} if the DIT content rule definition is obsolete,
   *          otherwise {@code false}.
   * @param auxiliaryClasses
   *          A list of auxiliary object classes that entries subject to the DIT
   *          content rule may belong to.
   * @param optionalAttributes
   *          A list of attribute types that entries subject to the DIT content
   *          rule may contain.
   * @param prohibitedAttributes
   *          A list of attribute types that entries subject to the DIT content
   *          rule must not contain.
   * @param requiredAttributes
   *          A list of attribute types that entries subject to the DIT content
   *          rule must contain.
   * @param extraProperties
   *          A map containing additional properties associated with the DIT
   *          content rule definition.
   * @param overwrite
   *          {@code true} if any existing DIT content rule with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addDITContentRule(final String structuralClass,
      final List<String> names, final String description,
      final boolean obsolete, final Set<String> auxiliaryClasses,
      final Set<String> optionalAttributes,
      final Set<String> prohibitedAttributes,
      final Set<String> requiredAttributes,
      final Map<String, List<String>> extraProperties, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    final DITContentRule rule = new DITContentRule(structuralClass,
        unmodifiableCopyOfList(names), description, obsolete,
        unmodifiableCopyOfSet(auxiliaryClasses),
        unmodifiableCopyOfSet(optionalAttributes),
        unmodifiableCopyOfSet(prohibitedAttributes),
        unmodifiableCopyOfSet(requiredAttributes),
        unmodifiableCopyOfExtraProperties(extraProperties), null);
    addDITContentRule(rule, overwrite);
    return this;
  }



  /**
   * Adds the provided DIT structure rule definition to this schema builder.
   *
   * @param ruleID
   *          The rule identifier of the DIT structure rule.
   * @param names
   *          The user-friendly names of the DIT structure rule definition.
   * @param description
   *          The description of the DIT structure rule definition.
   * @param obsolete
   *          {@code true} if the DIT structure rule definition is obsolete,
   *          otherwise {@code false}.
   * @param nameForm
   *          The name form associated with the DIT structure rule.
   * @param superiorRules
   *          A list of superior rules (by rule id).
   * @param extraProperties
   *          A map containing additional properties associated with the DIT
   *          structure rule definition.
   * @param overwrite
   *          {@code true} if any existing DIT structure rule with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addDITStructureRule(final Integer ruleID,
      final List<String> names, final String description,
      final boolean obsolete, final String nameForm,
      final Set<Integer> superiorRules,
      final Map<String, List<String>> extraProperties, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    final DITStructureRule rule = new DITStructureRule(ruleID,
        unmodifiableCopyOfList(names), description, obsolete, nameForm,
        unmodifiableCopyOfSet(superiorRules),
        unmodifiableCopyOfExtraProperties(extraProperties), null);
    addDITStructureRule(rule, overwrite);
    return this;
  }



  /**
   * Adds the provided DIT structure rule definition to this schema builder.
   *
   * @param definition
   *          The DIT structure rule definition.
   * @param overwrite
   *          {@code true} if any existing DIT structure rule with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws LocalizedIllegalArgumentException
   *           If the provided DIT structure rule definition could not be
   *           parsed.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addDITStructureRule(final String definition,
      final boolean overwrite) throws ConflictingSchemaElementException,
      LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(definition);
    try
    {
      final SubstringReader reader = new SubstringReader(definition);

      // We'll do this a character at a time. First, skip over any
      // leading whitespace.
      reader.skipWhitespaces();

      if (reader.remaining() <= 0)
      {
        // This means that the value was empty or contained only
        // whitespace. That is illegal.
        final LocalizableMessage message = ERR_ATTR_SYNTAX_DSR_EMPTY_VALUE
            .get();
        throw new LocalizedIllegalArgumentException(message);
      }

      // The next character must be an open parenthesis. If it is not,
      // then that is an error.
      final char c = reader.read();
      if (c != '(')
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_DSR_EXPECTED_OPEN_PARENTHESIS
            .get(definition, (reader.pos() - 1), String.valueOf(c));
        throw new LocalizedIllegalArgumentException(message);
      }

      // Skip over any spaces immediately following the opening
      // parenthesis.
      reader.skipWhitespaces();

      // The next set of characters must be the OID.
      final Integer ruleID = SchemaUtils.readRuleID(reader);

      List<String> names = Collections.emptyList();
      String description = "".intern();
      boolean isObsolete = false;
      String nameForm = null;
      Set<Integer> superiorRules = Collections.emptySet();
      Map<String, List<String>> extraProperties = Collections.emptyMap();

      // At this point, we should have a pretty specific syntax that
      // describes what may come next, but some of the components are
      // optional and it would be pretty easy to put something in the
      // wrong order, so we will be very flexible about what we can
      // accept. Just look at the next token, figure out what it is and
      // how to treat what comes after it, then repeat until we get to
      // the end of the value. But before we start, set default values
      // for everything else we might need to know.
      while (true)
      {
        final String tokenName = SchemaUtils.readTokenName(reader);

        if (tokenName == null)
        {
          // No more tokens.
          break;
        }
        else if (tokenName.equalsIgnoreCase("name"))
        {
          names = SchemaUtils.readNameDescriptors(reader);
        }
        else if (tokenName.equalsIgnoreCase("desc"))
        {
          // This specifies the description for the attribute type. It
          // is an arbitrary string of characters enclosed in single
          // quotes.
          description = SchemaUtils.readQuotedString(reader);
        }
        else if (tokenName.equalsIgnoreCase("obsolete"))
        {
          // This indicates whether the attribute type should be
          // considered obsolete. We do not need to do any more parsing
          // for this token.
          isObsolete = true;
        }
        else if (tokenName.equalsIgnoreCase("form"))
        {
          nameForm = SchemaUtils.readOID(reader);
        }
        else if (tokenName.equalsIgnoreCase("sup"))
        {
          superiorRules = SchemaUtils.readRuleIDs(reader);
        }
        else if (tokenName.matches("^X-[A-Za-z_-]+$"))
        {
          // This must be a non-standard property and it must be
          // followed by either a single definition in single quotes or
          // an open parenthesis followed by one or more values in
          // single quotes separated by spaces followed by a close
          // parenthesis.
          if (extraProperties.isEmpty())
          {
            extraProperties = new HashMap<String, List<String>>();
          }
          extraProperties.put(tokenName, SchemaUtils.readExtensions(reader));
        }
        else
        {
          final LocalizableMessage message = ERR_ATTR_SYNTAX_ILLEGAL_TOKEN
              .get(tokenName);
          throw new LocalizedIllegalArgumentException(message);
        }
      }

      if (nameForm == null)
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_DSR_NO_NAME_FORM
            .get(definition);
        throw new LocalizedIllegalArgumentException(message);
      }

      if (!extraProperties.isEmpty())
      {
        extraProperties = Collections.unmodifiableMap(extraProperties);
      }

      final DITStructureRule rule = new DITStructureRule(ruleID, names,
          description, isObsolete, nameForm, superiorRules, extraProperties,
          definition);
      addDITStructureRule(rule, overwrite);
    }
    catch (final DecodeException e)
    {
      throw new LocalizedIllegalArgumentException(e.getMessageObject(), e
          .getCause());
    }
    return this;
  }



  /**
   * Adds the provided enumeration syntax definition to this schema builder.
   *
   * @param oid
   *          The OID of the enumeration syntax definition.
   * @param description
   *          The description of the enumeration syntax definition.
   * @param overwrite
   *          {@code true} if any existing syntax with the same OID should be
   *          overwritten.
   * @param enumerations
   *          The range of values which attribute values must match in order to
   *          be valid.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addEnumerationSyntax(final String oid,
      final String description, final boolean overwrite,
      final String... enumerations) throws ConflictingSchemaElementException
  {
    Validator.ensureNotNull((Object) enumerations);

    final EnumSyntaxImpl enumImpl = new EnumSyntaxImpl(oid, Arrays
        .asList(enumerations));
    final Syntax enumSyntax = new Syntax(oid, description, Collections
        .singletonMap("X-ENUM", Arrays.asList(enumerations)), null, enumImpl);
    final MatchingRule enumOMR = new MatchingRule(enumImpl
        .getOrderingMatchingRule(), Collections
        .singletonList(OMR_GENERIC_ENUM_NAME + oid), "", false, oid,
        CoreSchemaImpl.OPENDS_ORIGIN, null, new EnumOrderingMatchingRule(
            enumImpl));

    addSyntax(enumSyntax, overwrite);
    try
    {
      addMatchingRule(enumOMR, overwrite);
    }
    catch (final ConflictingSchemaElementException e)
    {
      removeSyntax(oid);
    }
    return this;
  }



  /**
   * Adds the provided matching rule definition to this schema builder.
   *
   * @param definition
   *          The matching rule definition.
   * @param overwrite
   *          {@code true} if any existing matching rule with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws LocalizedIllegalArgumentException
   *           If the provided matching rule definition could not be parsed.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addMatchingRule(final String definition,
      final boolean overwrite) throws ConflictingSchemaElementException,
      LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(definition);
    try
    {
      final SubstringReader reader = new SubstringReader(definition);

      // We'll do this a character at a time. First, skip over any
      // leading whitespace.
      reader.skipWhitespaces();

      if (reader.remaining() <= 0)
      {
        // This means that the value was empty or contained only
        // whitespace. That is illegal.
        final LocalizableMessage message = ERR_ATTR_SYNTAX_MR_EMPTY_VALUE.get();
        throw new LocalizedIllegalArgumentException(message);
      }

      // The next character must be an open parenthesis. If it is not,
      // then that is an error.
      final char c = reader.read();
      if (c != '(')
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_MR_EXPECTED_OPEN_PARENTHESIS
            .get(definition, (reader.pos() - 1), String.valueOf(c));
        throw new LocalizedIllegalArgumentException(message);
      }

      // Skip over any spaces immediately following the opening
      // parenthesis.
      reader.skipWhitespaces();

      // The next set of characters must be the OID.
      final String oid = SchemaUtils.readOID(reader);

      List<String> names = Collections.emptyList();
      String description = "".intern();
      boolean isObsolete = false;
      String syntax = null;
      Map<String, List<String>> extraProperties = Collections.emptyMap();

      // At this point, we should have a pretty specific syntax that
      // describes what may come next, but some of the components are
      // optional and it would be pretty easy to put something in the
      // wrong order, so we will be very flexible about what we can
      // accept. Just look at the next token, figure out what it is and
      // how to treat what comes after it, then repeat until we get to
      // the end of the value. But before we start, set default values
      // for everything else we might need to know.
      while (true)
      {
        final String tokenName = SchemaUtils.readTokenName(reader);

        if (tokenName == null)
        {
          // No more tokens.
          break;
        }
        else if (tokenName.equalsIgnoreCase("name"))
        {
          names = SchemaUtils.readNameDescriptors(reader);
        }
        else if (tokenName.equalsIgnoreCase("desc"))
        {
          // This specifies the description for the matching rule. It is
          // an arbitrary string of characters enclosed in single
          // quotes.
          description = SchemaUtils.readQuotedString(reader);
        }
        else if (tokenName.equalsIgnoreCase("obsolete"))
        {
          // This indicates whether the matching rule should be
          // considered obsolete. We do not need to do any more parsing
          // for this token.
          isObsolete = true;
        }
        else if (tokenName.equalsIgnoreCase("syntax"))
        {
          syntax = SchemaUtils.readOID(reader);
        }
        else if (tokenName.matches("^X-[A-Za-z_-]+$"))
        {
          // This must be a non-standard property and it must be
          // followed by either a single definition in single quotes or
          // an open parenthesis followed by one or more values in
          // single quotes separated by spaces followed by a close
          // parenthesis.
          if (extraProperties.isEmpty())
          {
            extraProperties = new HashMap<String, List<String>>();
          }
          extraProperties.put(tokenName, SchemaUtils.readExtensions(reader));
        }
        else
        {
          final LocalizableMessage message = ERR_ATTR_SYNTAX_ILLEGAL_TOKEN
              .get(tokenName);
          throw new LocalizedIllegalArgumentException(message);
        }
      }

      // Make sure that a syntax was specified.
      if (syntax == null)
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_MR_NO_SYNTAX
            .get(definition);
        throw new LocalizedIllegalArgumentException(message);
      }

      if (!extraProperties.isEmpty())
      {
        extraProperties = Collections.unmodifiableMap(extraProperties);
      }

      addMatchingRule(new MatchingRule(oid, names, description, isObsolete,
          syntax, extraProperties, definition, null), overwrite);
    }
    catch (final DecodeException e)
    {
      throw new LocalizedIllegalArgumentException(e.getMessageObject(), e
          .getCause());
    }
    return this;
  }



  /**
   * Adds the provided matching rule definition to this schema builder.
   *
   * @param oid
   *          The OID of the matching rule definition.
   * @param names
   *          The user-friendly names of the matching rule definition.
   * @param description
   *          The description of the matching rule definition.
   * @param obsolete
   *          {@code true} if the matching rule definition is obsolete,
   *          otherwise {@code false}.
   * @param assertionSyntax
   *          The OID of the assertion syntax definition.
   * @param extraProperties
   *          A map containing additional properties associated with the
   *          matching rule definition.
   * @param implementation
   *          The implementation of the matching rule.
   * @param overwrite
   *          {@code true} if any existing matching rule with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addMatchingRule(final String oid,
      final List<String> names, final String description,
      final boolean obsolete, final String assertionSyntax,
      final Map<String, List<String>> extraProperties,
      final MatchingRuleImpl implementation, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    Validator.ensureNotNull(implementation);
    final MatchingRule matchingRule = new MatchingRule(oid,
        unmodifiableCopyOfList(names), description, obsolete, assertionSyntax,
        unmodifiableCopyOfExtraProperties(extraProperties), null,
        implementation);
    addMatchingRule(matchingRule, overwrite);
    return this;
  }



  /**
   * Adds the provided matching rule use definition to this schema builder.
   *
   * @param definition
   *          The matching rule use definition.
   * @param overwrite
   *          {@code true} if any existing matching rule use with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws LocalizedIllegalArgumentException
   *           If the provided matching rule use definition could not be parsed.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addMatchingRuleUse(final String definition,
      final boolean overwrite) throws ConflictingSchemaElementException,
      LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(definition);
    try
    {
      final SubstringReader reader = new SubstringReader(definition);

      // We'll do this a character at a time. First, skip over any
      // leading whitespace.
      reader.skipWhitespaces();

      if (reader.remaining() <= 0)
      {
        // This means that the value was empty or contained only
        // whitespace. That is illegal.
        final LocalizableMessage message = ERR_ATTR_SYNTAX_MRUSE_EMPTY_VALUE
            .get();
        throw new LocalizedIllegalArgumentException(message);
      }

      // The next character must be an open parenthesis. If it is not,
      // then that is an error.
      final char c = reader.read();
      if (c != '(')
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_MRUSE_EXPECTED_OPEN_PARENTHESIS
            .get(definition, (reader.pos() - 1), String.valueOf(c));
        throw new LocalizedIllegalArgumentException(message);
      }

      // Skip over any spaces immediately following the opening
      // parenthesis.
      reader.skipWhitespaces();

      // The next set of characters must be the OID.
      final String oid = SchemaUtils.readOID(reader);

      List<String> names = Collections.emptyList();
      String description = "".intern();
      boolean isObsolete = false;
      Set<String> attributes = null;
      Map<String, List<String>> extraProperties = Collections.emptyMap();

      // At this point, we should have a pretty specific syntax that
      // describes what may come next, but some of the components are
      // optional and it would be pretty easy to put something in the
      // wrong order, so we will be very flexible about what we can
      // accept. Just look at the next token, figure out what it is and
      // how to treat what comes after it, then repeat until we get to
      // the end of the value. But before we start, set default values
      // for everything else we might need to know.
      while (true)
      {
        final String tokenName = SchemaUtils.readTokenName(reader);

        if (tokenName == null)
        {
          // No more tokens.
          break;
        }
        else if (tokenName.equalsIgnoreCase("name"))
        {
          names = SchemaUtils.readNameDescriptors(reader);
        }
        else if (tokenName.equalsIgnoreCase("desc"))
        {
          // This specifies the description for the attribute type. It
          // is an arbitrary string of characters enclosed in single
          // quotes.
          description = SchemaUtils.readQuotedString(reader);
        }
        else if (tokenName.equalsIgnoreCase("obsolete"))
        {
          // This indicates whether the attribute type should be
          // considered obsolete. We do not need to do any more parsing
          // for this token.
          isObsolete = true;
        }
        else if (tokenName.equalsIgnoreCase("applies"))
        {
          attributes = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.matches("^X-[A-Za-z_-]+$"))
        {
          // This must be a non-standard property and it must be
          // followed by either a single definition in single quotes or
          // an open parenthesis followed by one or more values in
          // single quotes separated by spaces followed by a close
          // parenthesis.
          if (extraProperties.isEmpty())
          {
            extraProperties = new HashMap<String, List<String>>();
          }
          extraProperties.put(tokenName, SchemaUtils.readExtensions(reader));
        }
        else
        {
          final LocalizableMessage message = ERR_ATTR_SYNTAX_ILLEGAL_TOKEN
              .get(tokenName);
          throw new LocalizedIllegalArgumentException(message);
        }
      }

      // Make sure that the set of attributes was defined.
      if (attributes == null || attributes.size() == 0)
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_MRUSE_NO_ATTR
            .get(definition);
        throw new LocalizedIllegalArgumentException(message);
      }

      if (!extraProperties.isEmpty())
      {
        extraProperties = Collections.unmodifiableMap(extraProperties);
      }

      final MatchingRuleUse use = new MatchingRuleUse(oid, names, description,
          isObsolete, attributes, extraProperties, definition);
      addMatchingRuleUse(use, overwrite);
    }
    catch (final DecodeException e)
    {
      throw new LocalizedIllegalArgumentException(e.getMessageObject(), e
          .getCause());
    }
    return this;
  }



  /**
   * Adds the provided matching rule use definition to this schema builder.
   *
   * @param oid
   *          The OID of the matching rule use definition.
   * @param names
   *          The user-friendly names of the matching rule use definition.
   * @param description
   *          The description of the matching rule use definition.
   * @param obsolete
   *          {@code true} if the matching rule use definition is obsolete,
   *          otherwise {@code false}.
   * @param attributeOIDs
   *          The list of attribute types the matching rule applies to.
   * @param extraProperties
   *          A map containing additional properties associated with the
   *          matching rule use definition.
   * @param overwrite
   *          {@code true} if any existing matching rule use with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addMatchingRuleUse(final String oid,
      final List<String> names, final String description,
      final boolean obsolete, final Set<String> attributeOIDs,
      final Map<String, List<String>> extraProperties, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    final MatchingRuleUse use = new MatchingRuleUse(oid,
        unmodifiableCopyOfList(names), description, obsolete,
        unmodifiableCopyOfSet(attributeOIDs),
        unmodifiableCopyOfExtraProperties(extraProperties), null);
    addMatchingRuleUse(use, overwrite);
    return this;
  }



  /**
   * Adds the provided name form definition to this schema builder.
   *
   * @param definition
   *          The name form definition.
   * @param overwrite
   *          {@code true} if any existing name form with the same OID should be
   *          overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws LocalizedIllegalArgumentException
   *           If the provided name form definition could not be parsed.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addNameForm(final String definition,
      final boolean overwrite) throws ConflictingSchemaElementException,
      LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(definition);
    try
    {
      final SubstringReader reader = new SubstringReader(definition);

      // We'll do this a character at a time. First, skip over any
      // leading whitespace.
      reader.skipWhitespaces();

      if (reader.remaining() <= 0)
      {
        // This means that the value was empty or contained only
        // whitespace. That is illegal.
        final LocalizableMessage message = ERR_ATTR_SYNTAX_NAME_FORM_EMPTY_VALUE
            .get();
        throw new LocalizedIllegalArgumentException(message);
      }

      // The next character must be an open parenthesis. If it is not,
      // then that is an error.
      final char c = reader.read();
      if (c != '(')
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_NAME_FORM_EXPECTED_OPEN_PARENTHESIS
            .get(definition, (reader.pos() - 1), c);
        throw new LocalizedIllegalArgumentException(message);
      }

      // Skip over any spaces immediately following the opening
      // parenthesis.
      reader.skipWhitespaces();

      // The next set of characters must be the OID.
      final String oid = SchemaUtils.readOID(reader);

      List<String> names = Collections.emptyList();
      String description = "".intern();
      boolean isObsolete = false;
      String structuralClass = null;
      Set<String> optionalAttributes = Collections.emptySet();
      Set<String> requiredAttributes = null;
      Map<String, List<String>> extraProperties = Collections.emptyMap();

      // At this point, we should have a pretty specific syntax that
      // describes what may come next, but some of the components are
      // optional and it would be pretty easy to put something in the
      // wrong order, so we will be very flexible about what we can
      // accept. Just look at the next token, figure out what it is and
      // how to treat what comes after it, then repeat until we get to
      // the end of the value. But before we start, set default values
      // for everything else we might need to know.
      while (true)
      {
        final String tokenName = SchemaUtils.readTokenName(reader);

        if (tokenName == null)
        {
          // No more tokens.
          break;
        }
        else if (tokenName.equalsIgnoreCase("name"))
        {
          names = SchemaUtils.readNameDescriptors(reader);
        }
        else if (tokenName.equalsIgnoreCase("desc"))
        {
          // This specifies the description for the attribute type. It
          // is an arbitrary string of characters enclosed in single
          // quotes.
          description = SchemaUtils.readQuotedString(reader);
        }
        else if (tokenName.equalsIgnoreCase("obsolete"))
        {
          // This indicates whether the attribute type should be
          // considered obsolete. We do not need to do any more parsing
          // for this token.
          isObsolete = true;
        }
        else if (tokenName.equalsIgnoreCase("oc"))
        {
          structuralClass = SchemaUtils.readOID(reader);
        }
        else if (tokenName.equalsIgnoreCase("must"))
        {
          requiredAttributes = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.equalsIgnoreCase("may"))
        {
          optionalAttributes = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.matches("^X-[A-Za-z_-]+$"))
        {
          // This must be a non-standard property and it must be
          // followed by either a single definition in single quotes or
          // an open parenthesis followed by one or more values in
          // single quotes separated by spaces followed by a close
          // parenthesis.
          if (extraProperties.isEmpty())
          {
            extraProperties = new HashMap<String, List<String>>();
          }
          extraProperties.put(tokenName, SchemaUtils.readExtensions(reader));
        }
        else
        {
          final LocalizableMessage message = ERR_ATTR_SYNTAX_ILLEGAL_TOKEN
              .get(tokenName);
          throw new LocalizedIllegalArgumentException(message);
        }
      }

      // Make sure that a structural class was specified. If not, then
      // it cannot be valid.
      if (structuralClass == null)
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_NAME_FORM_NO_STRUCTURAL_CLASS
            .get(definition);
        throw new LocalizedIllegalArgumentException(message);
      }

      if (requiredAttributes == null || requiredAttributes.size() == 0)
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_NAME_FORM_NO_REQUIRED_ATTR
            .get(definition);
        throw new LocalizedIllegalArgumentException(message);
      }

      if (!extraProperties.isEmpty())
      {
        extraProperties = Collections.unmodifiableMap(extraProperties);
      }

      final NameForm nameForm = new NameForm(oid, names, description,
          isObsolete, structuralClass, requiredAttributes, optionalAttributes,
          extraProperties, definition);
      addNameForm(nameForm, overwrite);
    }
    catch (final DecodeException e)
    {
      throw new LocalizedIllegalArgumentException(e.getMessageObject(), e
          .getCause());
    }
    return this;
  }



  /**
   * Adds the provided name form definition to this schema builder.
   *
   * @param oid
   *          The OID of the name form definition.
   * @param names
   *          The user-friendly names of the name form definition.
   * @param description
   *          The description of the name form definition.
   * @param obsolete
   *          {@code true} if the name form definition is obsolete, otherwise
   *          {@code false}.
   * @param structuralClass
   *          The structural object class this rule applies to.
   * @param requiredAttributes
   *          A list of naming attribute types that entries subject to the name
   *          form must contain.
   * @param optionalAttributes
   *          A list of naming attribute types that entries subject to the name
   *          form may contain.
   * @param extraProperties
   *          A map containing additional properties associated with the name
   *          form definition.
   * @param overwrite
   *          {@code true} if any existing name form use with the same OID
   *          should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addNameForm(final String oid, final List<String> names,
      final String description, final boolean obsolete,
      final String structuralClass, final Set<String> requiredAttributes,
      final Set<String> optionalAttributes,
      final Map<String, List<String>> extraProperties, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    final NameForm nameForm = new NameForm(oid, unmodifiableCopyOfList(names),
        description, obsolete, structuralClass,
        unmodifiableCopyOfSet(requiredAttributes),
        unmodifiableCopyOfSet(optionalAttributes),
        unmodifiableCopyOfExtraProperties(extraProperties), null);
    addNameForm(nameForm, overwrite);
    return this;
  }



  /**
   * Adds the provided object class definition to this schema builder.
   *
   * @param definition
   *          The object class definition.
   * @param overwrite
   *          {@code true} if any existing object class with the same OID should
   *          be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws LocalizedIllegalArgumentException
   *           If the provided object class definition could not be parsed.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addObjectClass(final String definition,
      final boolean overwrite) throws ConflictingSchemaElementException,
      LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(definition);
    try
    {
      final SubstringReader reader = new SubstringReader(definition);

      // We'll do this a character at a time. First, skip over any
      // leading whitespace.
      reader.skipWhitespaces();

      if (reader.remaining() <= 0)
      {
        // This means that the value was empty or contained only
        // whitespace. That is illegal.
        final LocalizableMessage message = ERR_ATTR_SYNTAX_OBJECTCLASS_EMPTY_VALUE
            .get();
        throw new LocalizedIllegalArgumentException(message);
      }

      // The next character must be an open parenthesis. If it is not,
      // then that is an error.
      final char c = reader.read();
      if (c != '(')
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_OBJECTCLASS_EXPECTED_OPEN_PARENTHESIS
            .get(definition, (reader.pos() - 1), String.valueOf(c));
        throw new LocalizedIllegalArgumentException(message);
      }

      // Skip over any spaces immediately following the opening
      // parenthesis.
      reader.skipWhitespaces();

      // The next set of characters must be the OID.
      final String oid = SchemaUtils.readOID(reader);

      List<String> names = Collections.emptyList();
      String description = "".intern();
      boolean isObsolete = false;
      Set<String> superiorClasses = Collections.emptySet();
      Set<String> requiredAttributes = Collections.emptySet();
      Set<String> optionalAttributes = Collections.emptySet();
      ObjectClassType objectClassType = ObjectClassType.STRUCTURAL;
      Map<String, List<String>> extraProperties = Collections.emptyMap();

      // At this point, we should have a pretty specific syntax that
      // describes what may come next, but some of the components are
      // optional and it would be pretty easy to put something in the
      // wrong order, so we will be very flexible about what we can
      // accept. Just look at the next token, figure out what it is and
      // how to treat what comes after it, then repeat until we get to
      // the end of the value. But before we start, set default values
      // for everything else we might need to know.
      while (true)
      {
        final String tokenName = SchemaUtils.readTokenName(reader);

        if (tokenName == null)
        {
          // No more tokens.
          break;
        }
        else if (tokenName.equalsIgnoreCase("name"))
        {
          names = SchemaUtils.readNameDescriptors(reader);
        }
        else if (tokenName.equalsIgnoreCase("desc"))
        {
          // This specifies the description for the attribute type. It
          // is an arbitrary string of characters enclosed in single
          // quotes.
          description = SchemaUtils.readQuotedString(reader);
        }
        else if (tokenName.equalsIgnoreCase("obsolete"))
        {
          // This indicates whether the attribute type should be
          // considered obsolete. We do not need to do any more parsing
          // for this token.
          isObsolete = true;
        }
        else if (tokenName.equalsIgnoreCase("sup"))
        {
          superiorClasses = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.equalsIgnoreCase("abstract"))
        {
          // This indicates that entries must not include this
          // objectclass unless they also include a non-abstract
          // objectclass that inherits from this class. We do not need
          // any more parsing for this token.
          objectClassType = ObjectClassType.ABSTRACT;
        }
        else if (tokenName.equalsIgnoreCase("structural"))
        {
          // This indicates that this is a structural objectclass. We do
          // not need any more parsing for this token.
          objectClassType = ObjectClassType.STRUCTURAL;
        }
        else if (tokenName.equalsIgnoreCase("auxiliary"))
        {
          // This indicates that this is an auxiliary objectclass. We do
          // not need any more parsing for this token.
          objectClassType = ObjectClassType.AUXILIARY;
        }
        else if (tokenName.equalsIgnoreCase("must"))
        {
          requiredAttributes = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.equalsIgnoreCase("may"))
        {
          optionalAttributes = SchemaUtils.readOIDs(reader);
        }
        else if (tokenName.matches("^X-[A-Za-z_-]+$"))
        {
          // This must be a non-standard property and it must be
          // followed by either a single definition in single quotes or
          // an open parenthesis followed by one or more values in
          // single quotes separated by spaces followed by a close
          // parenthesis.
          if (extraProperties.isEmpty())
          {
            extraProperties = new HashMap<String, List<String>>();
          }
          extraProperties.put(tokenName, SchemaUtils.readExtensions(reader));
        }
        else
        {
          final LocalizableMessage message = ERR_ATTR_SYNTAX_ILLEGAL_TOKEN
              .get(tokenName);
          throw new LocalizedIllegalArgumentException(message);
        }
      }

      if (oid.equals(EXTENSIBLE_OBJECT_OBJECTCLASS_OID))
      {
        addObjectClass(new ObjectClass(description, extraProperties), overwrite);
      }
      else
      {
        if (objectClassType == ObjectClassType.STRUCTURAL
            && superiorClasses.isEmpty())
        {
          superiorClasses = Collections.singleton(TOP_OBJECTCLASS_NAME);
        }

        if (!extraProperties.isEmpty())
        {
          extraProperties = Collections.unmodifiableMap(extraProperties);
        }

        addObjectClass(new ObjectClass(oid, names, description, isObsolete,
            superiorClasses, requiredAttributes, optionalAttributes,
            objectClassType, extraProperties, definition), overwrite);
      }
    }
    catch (final DecodeException e)
    {
      throw new LocalizedIllegalArgumentException(e.getMessageObject(), e
          .getCause());
    }
    return this;
  }



  /**
   * Adds the provided object class definition to this schema builder.
   *
   * @param oid
   *          The OID of the object class definition.
   * @param names
   *          The user-friendly names of the object class definition.
   * @param description
   *          The description of the object class definition.
   * @param obsolete
   *          {@code true} if the object class definition is obsolete, otherwise
   *          {@code false}.
   * @param superiorClassOIDs
   *          A list of direct superclasses of the object class.
   * @param requiredAttributeOIDs
   *          A list of attribute types that entries must contain.
   * @param optionalAttributeOIDs
   *          A list of attribute types that entries may contain.
   * @param objectClassType
   *          The type of the object class.
   * @param extraProperties
   *          A map containing additional properties associated with the object
   *          class definition.
   * @param overwrite
   *          {@code true} if any existing object class with the same OID should
   *          be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addObjectClass(final String oid,
      final List<String> names, final String description,
      final boolean obsolete, Set<String> superiorClassOIDs,
      final Set<String> requiredAttributeOIDs,
      final Set<String> optionalAttributeOIDs,
      final ObjectClassType objectClassType,
      final Map<String, List<String>> extraProperties, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    if (oid.equals(EXTENSIBLE_OBJECT_OBJECTCLASS_OID))
    {
      addObjectClass(new ObjectClass(description,
          unmodifiableCopyOfExtraProperties(extraProperties)), overwrite);
    }
    else
    {
      if (objectClassType == ObjectClassType.STRUCTURAL
          && superiorClassOIDs.isEmpty())
      {
        superiorClassOIDs = Collections.singleton(TOP_OBJECTCLASS_NAME);
      }

      addObjectClass(new ObjectClass(oid, unmodifiableCopyOfList(names),
          description, obsolete, unmodifiableCopyOfSet(superiorClassOIDs),
          unmodifiableCopyOfSet(requiredAttributeOIDs),
          unmodifiableCopyOfSet(optionalAttributeOIDs), objectClassType,
          unmodifiableCopyOfExtraProperties(extraProperties), null), overwrite);
    }
    return this;
  }



  /**
   * Adds the provided pattern syntax definition to this schema builder.
   *
   * @param oid
   *          The OID of the pattern syntax definition.
   * @param description
   *          The description of the pattern syntax definition.
   * @param pattern
   *          The regular expression pattern which attribute values must match
   *          in order to be valid.
   * @param overwrite
   *          {@code true} if any existing syntax with the same OID should be
   *          overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addPatternSyntax(final String oid,
      final String description, final Pattern pattern, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    Validator.ensureNotNull(pattern);

    addSyntax(
        new Syntax(oid, description, Collections.singletonMap("X-PATTERN",
            Collections.singletonList(pattern.toString())), null, null),
        overwrite);
    return this;
  }



  /**
   * Adds all of the schema elements contained in the provided subschema
   * subentry to this schema builder. Any problems encountered while parsing the
   * entry can be retrieved using the returned schema's
   * {@link Schema#getWarnings()} method.
   *
   * @param entry
   *          The subschema subentry to be parsed.
   * @param overwrite
   *          {@code true} if existing schema elements with the same conflicting
   *          OIDs should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and conflicting schema
   *           elements were found.
   * @throws NullPointerException
   *           If {@code entry} was {@code null}.
   */
  public SchemaBuilder addSchema(final Entry entry, final boolean overwrite)
      throws ConflictingSchemaElementException, NullPointerException
  {
    Validator.ensureNotNull(entry);

    Attribute attr = entry.getAttribute(Schema.ATTR_LDAP_SYNTAXES);
    if (attr != null)
    {
      for (final ByteString def : attr)
      {
        try
        {
          addSyntax(def.toString(), overwrite);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          addWarning(e.getMessageObject());
        }
      }
    }

    attr = entry.getAttribute(Schema.ATTR_ATTRIBUTE_TYPES);
    if (attr != null)
    {
      for (final ByteString def : attr)
      {
        try
        {
          addAttributeType(def.toString(), overwrite);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          addWarning(e.getMessageObject());
        }
      }
    }

    attr = entry.getAttribute(Schema.ATTR_OBJECT_CLASSES);
    if (attr != null)
    {
      for (final ByteString def : attr)
      {
        try
        {
          addObjectClass(def.toString(), overwrite);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          addWarning(e.getMessageObject());
        }
      }
    }

    attr = entry.getAttribute(Schema.ATTR_MATCHING_RULE_USE);
    if (attr != null)
    {
      for (final ByteString def : attr)
      {
        try
        {
          addMatchingRuleUse(def.toString(), overwrite);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          addWarning(e.getMessageObject());
        }
      }
    }

    attr = entry.getAttribute(Schema.ATTR_MATCHING_RULES);
    if (attr != null)
    {
      for (final ByteString def : attr)
      {
        try
        {
          addMatchingRule(def.toString(), overwrite);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          addWarning(e.getMessageObject());
        }
      }
    }

    attr = entry.getAttribute(Schema.ATTR_DIT_CONTENT_RULES);
    if (attr != null)
    {
      for (final ByteString def : attr)
      {
        try
        {
          addDITContentRule(def.toString(), overwrite);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          addWarning(e.getMessageObject());
        }
      }
    }

    attr = entry.getAttribute(Schema.ATTR_DIT_STRUCTURE_RULES);
    if (attr != null)
    {
      for (final ByteString def : attr)
      {
        try
        {
          addDITStructureRule(def.toString(), overwrite);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          addWarning(e.getMessageObject());
        }
      }
    }

    attr = entry.getAttribute(Schema.ATTR_NAME_FORMS);
    if (attr != null)
    {
      for (final ByteString def : attr)
      {
        try
        {
          addNameForm(def.toString(), overwrite);
        }
        catch (final LocalizedIllegalArgumentException e)
        {
          addWarning(e.getMessageObject());
        }
      }
    }

    return this;
  }



  /**
   * Adds all of the schema elements in the provided schema to this schema
   * builder.
   *
   * @param schema
   *          The schema to be copied into this schema builder.
   * @param overwrite
   *          {@code true} if existing schema elements with the same conflicting
   *          OIDs should be overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and conflicting schema
   *           elements were found.
   * @throws NullPointerException
   *           If {@code schema} was {@code null}.
   */
  public SchemaBuilder addSchema(final Schema schema, final boolean overwrite)
      throws ConflictingSchemaElementException, NullPointerException
  {
    Validator.ensureNotNull(schema);
    for (final Syntax syntax : schema.getSyntaxes())
    {
      addSyntax(syntax.duplicate(), overwrite);
    }

    for (final MatchingRule matchingRule : schema.getMatchingRules())
    {
      addMatchingRule(matchingRule.duplicate(), overwrite);
    }

    for (final MatchingRuleUse matchingRuleUse : schema.getMatchingRuleUses())
    {
      addMatchingRuleUse(matchingRuleUse.duplicate(), overwrite);
    }

    for (final AttributeType attributeType : schema.getAttributeTypes())
    {
      addAttributeType(attributeType.duplicate(), overwrite);
    }

    for (final ObjectClass objectClass : schema.getObjectClasses())
    {
      addObjectClass(objectClass.duplicate(), overwrite);
    }

    for (final NameForm nameForm : schema.getNameForms())
    {
      addNameForm(nameForm.duplicate(), overwrite);
    }

    for (final DITContentRule contentRule : schema.getDITContentRules())
    {
      addDITContentRule(contentRule.duplicate(), overwrite);
    }

    for (final DITStructureRule structureRule : schema.getDITStuctureRules())
    {
      addDITStructureRule(structureRule.duplicate(), overwrite);
    }

    return this;
  }



  /**
   * Adds the provided substitution syntax definition to this schema builder.
   *
   * @param oid
   *          The OID of the substitution syntax definition.
   * @param description
   *          The description of the substitution syntax definition.
   * @param substituteSyntax
   *          The OID of the syntax whose implementation should be substituted.
   * @param overwrite
   *          {@code true} if any existing syntax with the same OID should be
   *          overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   */
  public SchemaBuilder addSubstitutionSyntax(final String oid,
      final String description, final String substituteSyntax,
      final boolean overwrite) throws ConflictingSchemaElementException
  {
    Validator.ensureNotNull(substituteSyntax);

    addSyntax(new Syntax(oid, description, Collections.singletonMap("X-SUBST",
        Collections.singletonList(substituteSyntax)), null, null), overwrite);
    return this;
  }



  /**
   * Adds the provided syntax definition to this schema builder.
   *
   * @param definition
   *          The syntax definition.
   * @param overwrite
   *          {@code true} if any existing syntax with the same OID should be
   *          overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws LocalizedIllegalArgumentException
   *           If the provided syntax definition could not be parsed.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addSyntax(final String definition,
      final boolean overwrite) throws ConflictingSchemaElementException,
      LocalizedIllegalArgumentException, NullPointerException
  {
    Validator.ensureNotNull(definition);
    try
    {
      final SubstringReader reader = new SubstringReader(definition);

      // We'll do this a character at a time. First, skip over any
      // leading whitespace.
      reader.skipWhitespaces();

      if (reader.remaining() <= 0)
      {
        // This means that the value was empty or contained only
        // whitespace. That is illegal.
        final LocalizableMessage message = ERR_ATTR_SYNTAX_ATTRSYNTAX_EMPTY_VALUE
            .get();
        throw new LocalizedIllegalArgumentException(message);
      }

      // The next character must be an open parenthesis. If it is not,
      // then that is an error.
      final char c = reader.read();
      if (c != '(')
      {
        final LocalizableMessage message = ERR_ATTR_SYNTAX_ATTRSYNTAX_EXPECTED_OPEN_PARENTHESIS
            .get(definition, (reader.pos() - 1), String.valueOf(c));
        throw new LocalizedIllegalArgumentException(message);
      }

      // Skip over any spaces immediately following the opening
      // parenthesis.
      reader.skipWhitespaces();

      // The next set of characters must be the OID.
      final String oid = SchemaUtils.readOID(reader);

      String description = "".intern();
      Map<String, List<String>> extraProperties = Collections.emptyMap();

      // At this point, we should have a pretty specific syntax that
      // describes what may come next, but some of the components are
      // optional and it would be pretty easy to put something in the
      // wrong order, so we will be very flexible about what we can
      // accept. Just look at the next token, figure out what it is and
      // how to treat what comes after it, then repeat until we get to
      // the end of the value. But before we start, set default values
      // for everything else we might need to know.
      while (true)
      {
        final String tokenName = SchemaUtils.readTokenName(reader);

        if (tokenName == null)
        {
          // No more tokens.
          break;
        }
        else if (tokenName.equalsIgnoreCase("desc"))
        {
          // This specifies the description for the syntax. It is an
          // arbitrary string of characters enclosed in single quotes.
          description = SchemaUtils.readQuotedString(reader);
        }
        else if (tokenName.matches("^X-[A-Za-z_-]+$"))
        {
          // This must be a non-standard property and it must be
          // followed by either a single definition in single quotes or
          // an open parenthesis followed by one or more values in
          // single quotes separated by spaces followed by a close
          // parenthesis.
          if (extraProperties.isEmpty())
          {
            extraProperties = new HashMap<String, List<String>>();
          }
          extraProperties.put(tokenName, SchemaUtils.readExtensions(reader));
        }
        else
        {
          final LocalizableMessage message = ERR_ATTR_SYNTAX_ILLEGAL_TOKEN
              .get(tokenName);
          throw new LocalizedIllegalArgumentException(message);
        }
      }

      if (!extraProperties.isEmpty())
      {
        extraProperties = Collections.unmodifiableMap(extraProperties);
      }

      // See if it is a enum syntax
      for (final Map.Entry<String, List<String>> property : extraProperties
          .entrySet())
      {
        if (property.getKey().equalsIgnoreCase("x-enum"))
        {
          final EnumSyntaxImpl enumImpl = new EnumSyntaxImpl(oid, property
              .getValue());
          final Syntax enumSyntax = new Syntax(oid, description,
              extraProperties, definition, enumImpl);
          final MatchingRule enumOMR = new MatchingRule(enumImpl
              .getOrderingMatchingRule(), Collections
              .singletonList(OMR_GENERIC_ENUM_NAME + oid), "", false, oid,
              CoreSchemaImpl.OPENDS_ORIGIN, null, new EnumOrderingMatchingRule(
                  enumImpl));

          addSyntax(enumSyntax, overwrite);
          addMatchingRule(enumOMR, overwrite);
          return this;
        }
      }

      addSyntax(
          new Syntax(oid, description, extraProperties, definition, null),
          overwrite);
    }
    catch (final DecodeException e)
    {
      throw new LocalizedIllegalArgumentException(e.getMessageObject(), e
          .getCause());
    }
    return this;
  }



  /**
   * Adds the provided syntax definition to this schema builder.
   *
   * @param oid
   *          The OID of the syntax definition.
   * @param description
   *          The description of the syntax definition.
   * @param extraProperties
   *          A map containing additional properties associated with the syntax
   *          definition.
   * @param implementation
   *          The implementation of the syntax.
   * @param overwrite
   *          {@code true} if any existing syntax with the same OID should be
   *          overwritten.
   * @return A reference to this schema builder.
   * @throws ConflictingSchemaElementException
   *           If {@code overwrite} was {@code false} and a conflicting schema
   *           element was found.
   * @throws NullPointerException
   *           If {@code definition} was {@code null}.
   */
  public SchemaBuilder addSyntax(final String oid, final String description,
      final Map<String, List<String>> extraProperties,
      final SyntaxImpl implementation, final boolean overwrite)
      throws ConflictingSchemaElementException, NullPointerException
  {
    addSyntax(new Syntax(oid, description,
        unmodifiableCopyOfExtraProperties(extraProperties), null,
        implementation), overwrite);
    return this;
  }



  /**
   * Removes the named attribute type from this schema builder.
   *
   * @param name
   *          The name or OID of the attribute type to be removed.
   * @return {@code true} if the attribute type was found.
   */
  public boolean removeAttributeType(final String name)
  {
    if (schema.hasAttributeType(name))
    {
      removeAttributeType(schema.getAttributeType(name));
      return true;
    }
    return false;
  }



  /**
   * Removes the named DIT content rule from this schema builder.
   *
   * @param name
   *          The name or OID of the DIT content rule to be removed.
   * @return {@code true} if the DIT content rule was found.
   */
  public boolean removeDITContentRule(final String name)
  {
    if (schema.hasDITContentRule(name))
    {
      removeDITContentRule(schema.getDITContentRule(name));
      return true;
    }
    return false;
  }



  /**
   * Removes the specified DIT structure rule from this schema builder.
   *
   * @param ruleID
   *          The ID of the DIT structure rule to be removed.
   * @return {@code true} if the DIT structure rule was found.
   */
  public boolean removeDITStructureRule(final Integer ruleID)
  {
    if (schema.hasDITStructureRule(ruleID))
    {
      removeDITStructureRule(schema.getDITStructureRule(ruleID));
      return true;
    }
    return false;
  }



  /**
   * Removes the named matching rule from this schema builder.
   *
   * @param name
   *          The name or OID of the matching rule to be removed.
   * @return {@code true} if the matching rule was found.
   */
  public boolean removeMatchingRule(final String name)
  {
    if (schema.hasMatchingRule(name))
    {
      removeMatchingRule(schema.getMatchingRule(name));
      return true;
    }
    return false;
  }



  /**
   * Removes the named matching rule use from this schema builder.
   *
   * @param name
   *          The name or OID of the matching rule use to be removed.
   * @return {@code true} if the matching rule use was found.
   */
  public boolean removeMatchingRuleUse(final String name)
  {
    if (schema.hasMatchingRuleUse(name))
    {
      removeMatchingRuleUse(schema.getMatchingRuleUse(name));
      return true;
    }
    return false;
  }



  /**
   * Removes the named name form from this schema builder.
   *
   * @param name
   *          The name or OID of the name form to be removed.
   * @return {@code true} if the name form was found.
   */
  public boolean removeNameForm(final String name)
  {
    if (schema.hasNameForm(name))
    {
      removeNameForm(schema.getNameForm(name));
      return true;
    }
    return false;
  }



  /**
   * Removes the named object class from this schema builder.
   *
   * @param name
   *          The name or OID of the object class to be removed.
   * @return {@code true} if the object class was found.
   */
  public boolean removeObjectClass(final String name)
  {
    if (schema.hasObjectClass(name))
    {
      removeObjectClass(schema.getObjectClass(name));
      return true;
    }
    return false;
  }



  /**
   * Removes the named syntax from this schema builder.
   *
   * @param numericOID
   *          The name of the syntax to be removed.
   * @return {@code true} if the syntax was found.
   */
  public boolean removeSyntax(final String numericOID)
  {
    if (schema.hasSyntax(numericOID))
    {
      removeSyntax(schema.getSyntax(numericOID));
      return true;
    }
    return false;
  }



  /**
   * Sets the schema compatibility options for this schema builder. The schema
   * builder maintains its own set of compatibility options, so subsequent
   * changes to the provided set of options will not impact this schema builder.
   *
   * @param options
   *          The set of schema compatibility options that this schema builder
   *          should use.
   * @return A reference to this schema builder.
   * @throws NullPointerException
   *           If {@code options} was {@code null}.
   */
  public SchemaBuilder setSchemaCompatOptions(final SchemaCompatOptions options)
      throws NullPointerException
  {
    Validator.ensureNotNull(options);
    this.options.assign(options);
    return this;
  }



  /**
   * Returns a strict {@code Schema} containing all of the schema elements
   * contained in this schema builder as well as the same set of schema
   * compatibility options.
   * <p>
   * When this method returns this schema builder is empty and contains a
   * default set of compatibility options.
   *
   * @return A {@code Schema} containing all of the schema elements contained in
   *         this schema builder as well as the same set of schema compatibility
   *         options
   */
  public Schema toSchema()
  {
    validate();
    final Schema builtSchema = schema;
    initBuilder(null);
    return builtSchema;
  }



  void addWarning(final LocalizableMessage warning)
  {
    warnings.add(warning);
  }



  private void addAttributeType(final AttributeType attribute,
      final boolean overwrite) throws ConflictingSchemaElementException
  {
    AttributeType conflictingAttribute;
    if (numericOID2AttributeTypes.containsKey(attribute.getOID()))
    {
      conflictingAttribute = numericOID2AttributeTypes.get(attribute.getOID());
      if (!overwrite)
      {
        final LocalizableMessage message = ERR_SCHEMA_CONFLICTING_ATTRIBUTE_OID
            .get(attribute.getNameOrOID(), attribute.getOID(),
                conflictingAttribute.getNameOrOID());
        throw new ConflictingSchemaElementException(message);
      }
      removeAttributeType(conflictingAttribute);
    }

    numericOID2AttributeTypes.put(attribute.getOID(), attribute);
    for (final String name : attribute.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      List<AttributeType> attrs;
      if ((attrs = name2AttributeTypes.get(lowerName)) == null)
      {
        name2AttributeTypes
            .put(lowerName, Collections.singletonList(attribute));
      }
      else if (attrs.size() == 1)
      {
        attrs = new ArrayList<AttributeType>(attrs);
        attrs.add(attribute);
        name2AttributeTypes.put(lowerName, attrs);
      }
      else
      {
        attrs.add(attribute);
      }
    }
  }



  private void addDITContentRule(final DITContentRule rule,
      final boolean overwrite) throws ConflictingSchemaElementException
  {
    DITContentRule conflictingRule;
    if (numericOID2ContentRules.containsKey(rule.getStructuralClassOID()))
    {
      conflictingRule = numericOID2ContentRules.get(rule
          .getStructuralClassOID());
      if (!overwrite)
      {
        final LocalizableMessage message = ERR_SCHEMA_CONFLICTING_DIT_CONTENT_RULE
            .get(rule.getNameOrOID(), rule.getStructuralClassOID(),
                conflictingRule.getNameOrOID());
        throw new ConflictingSchemaElementException(message);
      }
      removeDITContentRule(conflictingRule);
    }

    numericOID2ContentRules.put(rule.getStructuralClassOID(), rule);
    for (final String name : rule.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      List<DITContentRule> rules;
      if ((rules = name2ContentRules.get(lowerName)) == null)
      {
        name2ContentRules.put(lowerName, Collections.singletonList(rule));
      }
      else if (rules.size() == 1)
      {
        rules = new ArrayList<DITContentRule>(rules);
        rules.add(rule);
        name2ContentRules.put(lowerName, rules);
      }
      else
      {
        rules.add(rule);
      }
    }
  }



  private void addDITStructureRule(final DITStructureRule rule,
      final boolean overwrite) throws ConflictingSchemaElementException
  {
    DITStructureRule conflictingRule;
    if (id2StructureRules.containsKey(rule.getRuleID()))
    {
      conflictingRule = id2StructureRules.get(rule.getRuleID());
      if (!overwrite)
      {
        final LocalizableMessage message = ERR_SCHEMA_CONFLICTING_DIT_STRUCTURE_RULE_ID
            .get(rule.getNameOrRuleID(), rule.getRuleID(), conflictingRule
                .getNameOrRuleID());
        throw new ConflictingSchemaElementException(message);
      }
      removeDITStructureRule(conflictingRule);
    }

    id2StructureRules.put(rule.getRuleID(), rule);
    for (final String name : rule.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      List<DITStructureRule> rules;
      if ((rules = name2StructureRules.get(lowerName)) == null)
      {
        name2StructureRules.put(lowerName, Collections.singletonList(rule));
      }
      else if (rules.size() == 1)
      {
        rules = new ArrayList<DITStructureRule>(rules);
        rules.add(rule);
        name2StructureRules.put(lowerName, rules);
      }
      else
      {
        rules.add(rule);
      }
    }
  }



  private void addMatchingRule(final MatchingRule rule, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    MatchingRule conflictingRule;
    if (numericOID2MatchingRules.containsKey(rule.getOID()))
    {
      conflictingRule = numericOID2MatchingRules.get(rule.getOID());
      if (!overwrite)
      {
        final LocalizableMessage message = ERR_SCHEMA_CONFLICTING_MR_OID.get(
            rule.getNameOrOID(), rule.getOID(), conflictingRule.getNameOrOID());
        throw new ConflictingSchemaElementException(message);
      }
      removeMatchingRule(conflictingRule);
    }

    numericOID2MatchingRules.put(rule.getOID(), rule);
    for (final String name : rule.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      List<MatchingRule> rules;
      if ((rules = name2MatchingRules.get(lowerName)) == null)
      {
        name2MatchingRules.put(lowerName, Collections.singletonList(rule));
      }
      else if (rules.size() == 1)
      {
        rules = new ArrayList<MatchingRule>(rules);
        rules.add(rule);
        name2MatchingRules.put(lowerName, rules);
      }
      else
      {
        rules.add(rule);
      }
    }
  }



  private void addMatchingRuleUse(final MatchingRuleUse use,
      final boolean overwrite) throws ConflictingSchemaElementException
  {
    MatchingRuleUse conflictingUse;
    if (numericOID2MatchingRuleUses.containsKey(use.getMatchingRuleOID()))
    {
      conflictingUse = numericOID2MatchingRuleUses
          .get(use.getMatchingRuleOID());
      if (!overwrite)
      {
        final LocalizableMessage message = ERR_SCHEMA_CONFLICTING_MATCHING_RULE_USE
            .get(use.getNameOrOID(), use.getMatchingRuleOID(), conflictingUse
                .getNameOrOID());
        throw new ConflictingSchemaElementException(message);
      }
      removeMatchingRuleUse(conflictingUse);
    }

    numericOID2MatchingRuleUses.put(use.getMatchingRuleOID(), use);
    for (final String name : use.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      List<MatchingRuleUse> uses;
      if ((uses = name2MatchingRuleUses.get(lowerName)) == null)
      {
        name2MatchingRuleUses.put(lowerName, Collections.singletonList(use));
      }
      else if (uses.size() == 1)
      {
        uses = new ArrayList<MatchingRuleUse>(uses);
        uses.add(use);
        name2MatchingRuleUses.put(lowerName, uses);
      }
      else
      {
        uses.add(use);
      }
    }
  }



  private void addNameForm(final NameForm form, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    NameForm conflictingForm;
    if (numericOID2NameForms.containsKey(form.getOID()))
    {
      conflictingForm = numericOID2NameForms.get(form.getOID());
      if (!overwrite)
      {
        final LocalizableMessage message = ERR_SCHEMA_CONFLICTING_NAME_FORM_OID
            .get(form.getNameOrOID(), form.getOID(), conflictingForm
                .getNameOrOID());
        throw new ConflictingSchemaElementException(message);
      }
      removeNameForm(conflictingForm);
    }

    numericOID2NameForms.put(form.getOID(), form);
    for (final String name : form.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      List<NameForm> forms;
      if ((forms = name2NameForms.get(lowerName)) == null)
      {
        name2NameForms.put(lowerName, Collections.singletonList(form));
      }
      else if (forms.size() == 1)
      {
        forms = new ArrayList<NameForm>(forms);
        forms.add(form);
        name2NameForms.put(lowerName, forms);
      }
      else
      {
        forms.add(form);
      }
    }
  }



  private void addObjectClass(final ObjectClass oc, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    ObjectClass conflictingOC;
    if (numericOID2ObjectClasses.containsKey(oc.getOID()))
    {
      conflictingOC = numericOID2ObjectClasses.get(oc.getOID());
      if (!overwrite)
      {
        final LocalizableMessage message = ERR_SCHEMA_CONFLICTING_OBJECTCLASS_OID
            .get(oc.getNameOrOID(), oc.getOID(), conflictingOC.getNameOrOID());
        throw new ConflictingSchemaElementException(message);
      }
      removeObjectClass(conflictingOC);
    }

    numericOID2ObjectClasses.put(oc.getOID(), oc);
    for (final String name : oc.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      List<ObjectClass> classes;
      if ((classes = name2ObjectClasses.get(lowerName)) == null)
      {
        name2ObjectClasses.put(lowerName, Collections.singletonList(oc));
      }
      else if (classes.size() == 1)
      {
        classes = new ArrayList<ObjectClass>(classes);
        classes.add(oc);
        name2ObjectClasses.put(lowerName, classes);
      }
      else
      {
        classes.add(oc);
      }
    }
  }



  private void addSyntax(final Syntax syntax, final boolean overwrite)
      throws ConflictingSchemaElementException
  {
    Syntax conflictingSyntax;
    if (numericOID2Syntaxes.containsKey(syntax.getOID()))
    {
      conflictingSyntax = numericOID2Syntaxes.get(syntax.getOID());
      if (!overwrite)
      {
        final LocalizableMessage message = ERR_SCHEMA_CONFLICTING_SYNTAX_OID
            .get(syntax.toString(), syntax.getOID(), conflictingSyntax.getOID());
        throw new ConflictingSchemaElementException(message);
      }
      removeSyntax(conflictingSyntax);
    }
    numericOID2Syntaxes.put(syntax.getOID(), syntax);
  }



  private void initBuilder(String schemaName)
  {
    numericOID2Syntaxes = new LinkedHashMap<String, Syntax>();
    numericOID2MatchingRules = new LinkedHashMap<String, MatchingRule>();
    numericOID2MatchingRuleUses = new LinkedHashMap<String, MatchingRuleUse>();
    numericOID2AttributeTypes = new LinkedHashMap<String, AttributeType>();
    numericOID2ObjectClasses = new LinkedHashMap<String, ObjectClass>();
    numericOID2NameForms = new LinkedHashMap<String, NameForm>();
    numericOID2ContentRules = new LinkedHashMap<String, DITContentRule>();
    id2StructureRules = new LinkedHashMap<Integer, DITStructureRule>();

    name2MatchingRules = new LinkedHashMap<String, List<MatchingRule>>();
    name2MatchingRuleUses = new LinkedHashMap<String, List<MatchingRuleUse>>();
    name2AttributeTypes = new LinkedHashMap<String, List<AttributeType>>();
    name2ObjectClasses = new LinkedHashMap<String, List<ObjectClass>>();
    name2NameForms = new LinkedHashMap<String, List<NameForm>>();
    name2ContentRules = new LinkedHashMap<String, List<DITContentRule>>();
    name2StructureRules = new LinkedHashMap<String, List<DITStructureRule>>();

    objectClass2NameForms = new HashMap<String, List<NameForm>>();
    nameForm2StructureRules = new HashMap<String, List<DITStructureRule>>();
    options = SchemaCompatOptions.defaultOptions();
    warnings = new LinkedList<LocalizableMessage>();

    if (schemaName == null)
    {
      schemaName = String.format("Schema#%d", nextSchemaID.getAndIncrement());
    }

    schema = new Schema(schemaName, numericOID2Syntaxes,
        numericOID2MatchingRules, numericOID2MatchingRuleUses,
        numericOID2AttributeTypes, numericOID2ObjectClasses,
        numericOID2NameForms, numericOID2ContentRules, id2StructureRules,
        name2MatchingRules, name2MatchingRuleUses, name2AttributeTypes,
        name2ObjectClasses, name2NameForms, name2ContentRules,
        name2StructureRules, objectClass2NameForms, nameForm2StructureRules,
        options, warnings);
  }



  private void removeAttributeType(final AttributeType attributeType)
  {
    numericOID2AttributeTypes.remove(attributeType.getOID());
    for (final String name : attributeType.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      final List<AttributeType> attributes = name2AttributeTypes.get(lowerName);
      if (attributes != null && attributes.contains(attributeType))
      {
        if (attributes.size() <= 1)
        {
          name2AttributeTypes.remove(lowerName);
        }
        else
        {
          attributes.remove(attributeType);
        }
      }
    }
  }



  private void removeDITContentRule(final DITContentRule rule)
  {
    numericOID2ContentRules.remove(rule.getStructuralClassOID());
    for (final String name : rule.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      final List<DITContentRule> rules = name2ContentRules.get(lowerName);
      if (rules != null && rules.contains(rule))
      {
        if (rules.size() <= 1)
        {
          name2AttributeTypes.remove(lowerName);
        }
        else
        {
          rules.remove(rule);
        }
      }
    }
  }



  private void removeDITStructureRule(final DITStructureRule rule)
  {
    id2StructureRules.remove(rule.getRuleID());
    for (final String name : rule.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      final List<DITStructureRule> rules = name2StructureRules.get(lowerName);
      if (rules != null && rules.contains(rule))
      {
        if (rules.size() <= 1)
        {
          name2StructureRules.remove(lowerName);
        }
        else
        {
          rules.remove(rule);
        }
      }
    }
  }



  private void removeMatchingRule(final MatchingRule rule)
  {
    numericOID2MatchingRules.remove(rule.getOID());
    for (final String name : rule.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      final List<MatchingRule> rules = name2MatchingRules.get(lowerName);
      if (rules != null && rules.contains(rule))
      {
        if (rules.size() <= 1)
        {
          name2MatchingRules.remove(lowerName);
        }
        else
        {
          rules.remove(rule);
        }
      }
    }
  }



  private void removeMatchingRuleUse(final MatchingRuleUse use)
  {
    numericOID2MatchingRuleUses.remove(use.getMatchingRuleOID());
    for (final String name : use.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      final List<MatchingRuleUse> uses = name2MatchingRuleUses.get(lowerName);
      if (uses != null && uses.contains(use))
      {
        if (uses.size() <= 1)
        {
          name2MatchingRuleUses.remove(lowerName);
        }
        else
        {
          uses.remove(use);
        }
      }
    }
  }



  private void removeNameForm(final NameForm form)
  {
    numericOID2NameForms.remove(form.getOID());
    name2NameForms.remove(form.getOID());
    for (final String name : form.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      final List<NameForm> forms = name2NameForms.get(lowerName);
      if (forms != null && forms.contains(form))
      {
        if (forms.size() <= 1)
        {
          name2NameForms.remove(lowerName);
        }
        else
        {
          forms.remove(form);
        }
      }
    }
  }



  private void removeObjectClass(final ObjectClass oc)
  {
    numericOID2ObjectClasses.remove(oc.getOID());
    name2ObjectClasses.remove(oc.getOID());
    for (final String name : oc.getNames())
    {
      final String lowerName = StaticUtils.toLowerCase(name);
      final List<ObjectClass> classes = name2ObjectClasses.get(lowerName);
      if (classes != null && classes.contains(oc))
      {
        if (classes.size() <= 1)
        {
          name2ObjectClasses.remove(lowerName);
        }
        else
        {
          classes.remove(oc);
        }
      }
    }
  }



  private void removeSyntax(final Syntax syntax)
  {
    numericOID2Syntaxes.remove(syntax.getOID());
  }



  private void validate()
  {
    // Verify all references in all elements
    for (final Syntax syntax : numericOID2Syntaxes.values().toArray(
        new Syntax[numericOID2Syntaxes.values().size()]))
    {
      try
      {
        syntax.validate(warnings, schema);
      }
      catch (final SchemaException e)
      {
        removeSyntax(syntax);
        warnings.add(ERR_SYNTAX_VALIDATION_FAIL.get(
            syntax.toString(), e.getMessageObject()));
      }
    }

    for (final MatchingRule rule : numericOID2MatchingRules.values().toArray(
        new MatchingRule[numericOID2MatchingRules.values().size()]))
    {
      try
      {
        rule.validate(warnings, schema);
      }
      catch (final SchemaException e)
      {
        removeMatchingRule(rule);
        warnings.add(ERR_MR_VALIDATION_FAIL.get(rule.toString(),
            e.getMessageObject()));
      }
    }

    for (final AttributeType attribute : numericOID2AttributeTypes.values()
        .toArray(new AttributeType[numericOID2AttributeTypes.values().size()]))
    {
      try
      {
        attribute.validate(warnings, schema);
      }
      catch (final SchemaException e)
      {
        removeAttributeType(attribute);
        warnings.add(ERR_ATTR_TYPE_VALIDATION_FAIL.get(attribute.toString(), e
            .getMessageObject()));
      }
    }

    for (final ObjectClass oc : numericOID2ObjectClasses.values().toArray(
        new ObjectClass[numericOID2ObjectClasses.values().size()]))
    {
      try
      {
        oc.validate(warnings, schema);
      }
      catch (final SchemaException e)
      {
        removeObjectClass(oc);
        warnings.add(ERR_OC_VALIDATION_FAIL.get(oc.toString(),
            e.getMessageObject()));
      }
    }

    for (final MatchingRuleUse use : numericOID2MatchingRuleUses.values()
        .toArray(
            new MatchingRuleUse[numericOID2MatchingRuleUses.values().size()]))
    {
      try
      {
        use.validate(warnings, schema);
      }
      catch (final SchemaException e)
      {
        removeMatchingRuleUse(use);
        warnings.add(ERR_MRU_VALIDATION_FAIL.get(use.toString(),
            e.getMessageObject()));
      }
    }

    for (final NameForm form : numericOID2NameForms.values().toArray(
        new NameForm[numericOID2NameForms.values().size()]))
    {
      try
      {
        form.validate(warnings, schema);

        // build the objectClass2NameForms map
        List<NameForm> forms;
        final String ocOID = form.getStructuralClass().getOID();
        if ((forms = objectClass2NameForms.get(ocOID)) == null)
        {
          objectClass2NameForms.put(ocOID, Collections.singletonList(form));
        }
        else if (forms.size() == 1)
        {
          forms = new ArrayList<NameForm>(forms);
          forms.add(form);
          objectClass2NameForms.put(ocOID, forms);
        }
        else
        {
          forms.add(form);
        }
      }
      catch (final SchemaException e)
      {
        removeNameForm(form);
        warnings.add(ERR_NAMEFORM_VALIDATION_FAIL.get(form.toString(), e
            .getMessageObject()));
      }
    }

    for (final DITContentRule rule : numericOID2ContentRules.values().toArray(
        new DITContentRule[numericOID2ContentRules.values().size()]))
    {
      try
      {
        rule.validate(warnings, schema);
      }
      catch (final SchemaException e)
      {
        removeDITContentRule(rule);
        warnings.add(ERR_DCR_VALIDATION_FAIL.get(rule.toString(),
            e.getMessageObject()));
      }
    }

    for (final DITStructureRule rule : id2StructureRules.values().toArray(
        new DITStructureRule[id2StructureRules.values().size()]))
    {
      try
      {
        rule.validate(warnings, schema);

        // build the nameForm2StructureRules map
        List<DITStructureRule> rules;
        final String ocOID = rule.getNameForm().getOID();
        if ((rules = nameForm2StructureRules.get(ocOID)) == null)
        {
          nameForm2StructureRules.put(ocOID, Collections.singletonList(rule));
        }
        else if (rules.size() == 1)
        {
          rules = new ArrayList<DITStructureRule>(rules);
          rules.add(rule);
          nameForm2StructureRules.put(ocOID, rules);
        }
        else
        {
          rules.add(rule);
        }
      }
      catch (final SchemaException e)
      {
        removeDITStructureRule(rule);
        warnings.add(ERR_DSR_VALIDATION_FAIL.get(rule.toString(),
            e.getMessageObject()));
      }
    }
  }
}
