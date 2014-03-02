/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at legal-notices/CDDLv1_0.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2009-2010 Sun Microsystems, Inc.
 *      Portions copyright 2013-2014 ForgeRock AS.
 *      Portions copyright 2014 Manuel Gaupp
 */
package org.forgerock.opendj.ldap.schema;

import static org.forgerock.opendj.ldap.schema.SchemaConstants.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CoreSchemaImpl {
    private static final Map<String, List<String>> X500_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("X.500"));

    private static final Map<String, List<String>> RFC2252_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 2252"));

    private static final Map<String, List<String>> RFC3045_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 3045"));

    private static final Map<String, List<String>> RFC3112_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 3112"));

    private static final Map<String, List<String>> RFC4512_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 4512"));

    private static final Map<String, List<String>> RFC4517_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 4517"));

    private static final Map<String, List<String>> RFC4519_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 4519"));

    private static final Map<String, List<String>> RFC4523_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 4523"));

    private static final Map<String, List<String>> RFC4530_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 4530"));

    private static final Map<String, List<String>> RFC5020_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("RFC 5020"));

    static final Map<String, List<String>> OPENDJ_ORIGIN = Collections.singletonMap(
            SCHEMA_PROPERTY_ORIGIN, Collections.singletonList("OpenDJ Directory Server"));

    private static final String EMPTY_STRING = "".intern();

    private static final Set<String> EMPTY_STRING_SET = Collections.emptySet();

    private static final Schema SINGLETON;

    static {
        final SchemaBuilder builder = new SchemaBuilder("Core Schema");
        defaultSyntaxes(builder);
        defaultMatchingRules(builder);
        defaultAttributeTypes(builder);
        defaultObjectClasses(builder);

        addRFC4519(builder);
        addRFC4523(builder);
        addRFC4530(builder);
        addRFC3045(builder);
        addRFC3112(builder);
        addRFC5020(builder);
        addSunProprietary(builder);

        SINGLETON = builder.toSchema().asNonStrictSchema();
    }

    static Schema getInstance() {
        return SINGLETON;
    }

    private static void addRFC3045(final SchemaBuilder builder) {
        builder.addAttributeType("1.3.6.1.1.4", Collections.singletonList("vendorName"),
                EMPTY_STRING, false, null, EMR_CASE_EXACT_IA5_OID, null, null, null,
                SYNTAX_DIRECTORY_STRING_OID, true, false, true, AttributeUsage.DSA_OPERATION,
                RFC3045_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.1.5", Collections.singletonList("vendorVersion"),
                EMPTY_STRING, false, null, EMR_CASE_EXACT_IA5_OID, null, null, null,
                SYNTAX_DIRECTORY_STRING_OID, true, false, true, AttributeUsage.DSA_OPERATION,
                RFC3045_ORIGIN, false);
    }

    private static void addRFC3112(final SchemaBuilder builder) {
        builder.buildSyntax(SYNTAX_AUTH_PASSWORD_OID).description(SYNTAX_AUTH_PASSWORD_DESCRIPTION)
                .extraProperties(RFC3112_ORIGIN).implementation(new AuthPasswordSyntaxImpl()).addToSchema();
        builder.buildMatchingRule(EMR_AUTH_PASSWORD_EXACT_OID)
                .names(EMR_AUTH_PASSWORD_EXACT_NAME)
                .description(EMR_AUTH_PASSWORD_EXACT_DESCRIPTION).syntaxOID(SYNTAX_AUTH_PASSWORD_OID)
                .extraProperties(RFC3112_ORIGIN).implementation(new AuthPasswordExactEqualityMatchingRuleImpl())
                .addToSchema();
        builder.addAttributeType("1.3.6.1.4.1.4203.1.3.3", Collections
                .singletonList("supportedAuthPasswordSchemes"),
                "supported password storage schemes", false, null, EMR_CASE_EXACT_IA5_OID, null,
                null, null, SYNTAX_IA5_STRING_OID, false, false, false,
                AttributeUsage.DSA_OPERATION, RFC3112_ORIGIN, false);
        builder.addAttributeType("1.3.6.1.4.1.4203.1.3.4", Collections
                .singletonList("authPassword"), "password authentication information", false, null,
                EMR_AUTH_PASSWORD_EXACT_OID, null, null, null, SYNTAX_AUTH_PASSWORD_OID, false,
                false, false, AttributeUsage.USER_APPLICATIONS, RFC3112_ORIGIN, false);
        builder.addObjectClass("1.3.6.1.4.1.4203.1.4.7", Collections
                .singletonList("authPasswordObject"), "authentication password mix in class",
                false, EMPTY_STRING_SET, EMPTY_STRING_SET, Collections.singleton("authPassword"),
                ObjectClassType.AUXILIARY, RFC3112_ORIGIN, false);
    }

    private static void addRFC4519(final SchemaBuilder builder) {
        builder.addAttributeType("2.5.4.15", Collections.singletonList("businessCategory"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.41", Collections.singletonList("name"), EMPTY_STRING,
                false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.6", Arrays.asList("c", "countryName"), EMPTY_STRING, false,
                "name", null, null, null, null, SYNTAX_COUNTRY_STRING_OID, true, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.3", Arrays.asList("cn", "commonName"), EMPTY_STRING, false,
                "name", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("0.9.2342.19200300.100.1.25", Arrays.asList("dc",
                "domainComponent"), EMPTY_STRING, false, null, EMR_CASE_IGNORE_IA5_OID, null,
                SMR_CASE_IGNORE_IA5_OID, null, SYNTAX_IA5_STRING_OID, true, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.13", Collections.singletonList("description"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.27", Collections.singletonList("destinationIndicator"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_PRINTABLE_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.49", Collections.singletonList("distinguishedName"),
                EMPTY_STRING, false, null, EMR_DN_OID, null, null, null, SYNTAX_DN_OID, false,
                false, false, AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.46", Collections.singletonList("dnQualifier"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, OMR_CASE_IGNORE_OID,
                SMR_CASE_IGNORE_OID, null, SYNTAX_PRINTABLE_STRING_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.47", Collections.singletonList("enhancedSearchGuide"),
                EMPTY_STRING, false, null, null, null, null, null, SYNTAX_ENHANCED_GUIDE_OID,
                false, false, false, AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.23", Collections.singletonList("facsimileTelephoneNumber"),
                EMPTY_STRING, false, null, null, null, null, null, SYNTAX_FAXNUMBER_OID, false,
                false, false, AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.44", Collections.singletonList("generationQualifier"),
                EMPTY_STRING, false, "name", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.42", Collections.singletonList("givenName"), EMPTY_STRING,
                false, "name", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.51", Collections.singletonList("houseIdentifier"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.43", Collections.singletonList("initials"), EMPTY_STRING,
                false, "name", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.25", Collections.singletonList("internationalISDNNumber"),
                EMPTY_STRING, false, null, EMR_NUMERIC_STRING_OID, null, SMR_NUMERIC_STRING_OID,
                null, SYNTAX_NUMERIC_STRING_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.7", Arrays.asList("l", "localityName"), EMPTY_STRING,
                false, "name", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.31", Collections.singletonList("member"), EMPTY_STRING,
                false, "distinguishedName", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.10", Arrays.asList("o", "organizationName"), EMPTY_STRING,
                false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.11", Arrays.asList("ou", "organizationalUnitName"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.32", Collections.singletonList("owner"), EMPTY_STRING,
                false, "distinguishedName", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.19", Collections
                .singletonList("physicalDeliveryOfficeName"), EMPTY_STRING, false, null,
                EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null, SYNTAX_DIRECTORY_STRING_OID,
                false, false, false, AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.16", Collections.singletonList("postalAddress"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.17", Collections.singletonList("postalCode"), EMPTY_STRING,
                false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.18", Collections.singletonList("postOfficeBox"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.28", Collections.singletonList("preferredDeliveryMethod"),
                EMPTY_STRING, false, null, null, null, null, null, SYNTAX_DELIVERY_METHOD_OID,
                true, false, false, AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.26", Collections.singletonList("registeredAddress"),
                EMPTY_STRING, false, "postalAddress", null, null, null, null,
                SYNTAX_POSTAL_ADDRESS_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.33", Collections.singletonList("roleOccupant"),
                EMPTY_STRING, false, "distinguishedName", null, null, null, null, null, false,
                false, false, AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.14", Collections.singletonList("searchGuide"),
                EMPTY_STRING, false, null, null, null, null, null, SYNTAX_GUIDE_OID, false, false,
                false, AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.34", Collections.singletonList("seeAlso"), EMPTY_STRING,
                false, "distinguishedName", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.5", Collections.singletonList("serialNumber"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_PRINTABLE_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.4", Arrays.asList("sn", "surname"), EMPTY_STRING, false,
                "name", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.8", Arrays.asList("st", "stateOrProvinceName"),
                EMPTY_STRING, false, "name", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.9", Arrays.asList("street", "streetAddress"), EMPTY_STRING,
                false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.20", Collections.singletonList("telephoneNumber"),
                EMPTY_STRING, false, null, EMR_TELEPHONE_OID, null, SMR_TELEPHONE_OID, null,
                SYNTAX_TELEPHONE_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.22",
                Collections.singletonList("teletexTerminalIdentifier"), EMPTY_STRING, false, null,
                null, null, null, null, SYNTAX_TELETEX_TERM_ID_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.21", Collections.singletonList("telexNumber"),
                EMPTY_STRING, false, null, null, null, null, null, SYNTAX_TELEX_OID, false, false,
                false, AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.12", Collections.singletonList("title"), EMPTY_STRING,
                false, "name", null, null, null, null, null, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("0.9.2342.19200300.100.1.1", Arrays.asList("uid", "userid"),
                EMPTY_STRING, false, null, EMR_CASE_IGNORE_OID, null, SMR_CASE_IGNORE_OID, null,
                SYNTAX_DIRECTORY_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.50", Collections.singletonList("uniqueMember"),
                EMPTY_STRING, false, null, EMR_UNIQUE_MEMBER_OID, null, null, null,
                SYNTAX_NAME_AND_OPTIONAL_UID_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.35", Collections.singletonList("userPassword"),
                EMPTY_STRING, false, null, EMR_OCTET_STRING_OID, null, null, null,
                SYNTAX_OCTET_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.24", Collections.singletonList("x121Address"),
                EMPTY_STRING, false, null, EMR_NUMERIC_STRING_OID, null, SMR_NUMERIC_STRING_OID,
                null, SYNTAX_NUMERIC_STRING_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4519_ORIGIN, false);

        builder.addAttributeType("2.5.4.45", Collections.singletonList("x500UniqueIdentifier"),
                EMPTY_STRING, false, null, EMR_BIT_STRING_OID, null, null, null,
                SYNTAX_BIT_STRING_OID, false, false, false, AttributeUsage.USER_APPLICATIONS,
                RFC4519_ORIGIN, false);

        Set<String> attrs = new HashSet<String>();
        attrs.add("seeAlso");
        attrs.add("ou");
        attrs.add("l");
        attrs.add("description");

        builder.addObjectClass("2.5.6.11", Collections.singletonList("applicationProcess"),
                EMPTY_STRING, false, Collections.singleton(TOP_OBJECTCLASS_NAME), Collections
                        .singleton("cn"), attrs, ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("searchGuide");
        attrs.add("description");

        builder.addObjectClass("2.5.6.2", Collections.singletonList("country"), EMPTY_STRING,
                false, Collections.singleton(TOP_OBJECTCLASS_NAME), Collections.singleton("c"),
                attrs, ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        builder.addObjectClass("1.3.6.1.4.1.1466.344", Collections.singletonList("dcObject"),
                EMPTY_STRING, false, Collections.singleton(TOP_OBJECTCLASS_NAME), Collections
                        .singleton("dc"), EMPTY_STRING_SET, ObjectClassType.AUXILIARY,
                RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("serialNumber");
        attrs.add("seeAlso");
        attrs.add("owner");
        attrs.add("ou");
        attrs.add("o");
        attrs.add("l");
        attrs.add("description");

        builder.addObjectClass("2.5.6.14", Collections.singletonList("device"), EMPTY_STRING,
                false, Collections.singleton(TOP_OBJECTCLASS_NAME), Collections.singleton("cn"),
                attrs, ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        Set<String> must = new HashSet<String>();
        must.add("member");
        must.add("cn");

        attrs = new HashSet<String>();
        attrs.add("businessCategory");
        attrs.add("seeAlso");
        attrs.add("owner");
        attrs.add("ou");
        attrs.add("o");
        attrs.add("description");

        builder.addObjectClass("2.5.6.9", Collections.singletonList("groupOfNames"), EMPTY_STRING,
                false, Collections.singleton(TOP_OBJECTCLASS_NAME), must, attrs,
                ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("businessCategory");
        attrs.add("seeAlso");
        attrs.add("owner");
        attrs.add("ou");
        attrs.add("o");
        attrs.add("description");

        builder.addObjectClass("2.5.6.17", Collections.singletonList("groupOfUniqueNames"),
                EMPTY_STRING, false, Collections.singleton(TOP_OBJECTCLASS_NAME), must, attrs,
                ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("street");
        attrs.add("seeAlso");
        attrs.add("searchGuide");
        attrs.add("st");
        attrs.add("l");
        attrs.add("description");

        builder.addObjectClass("2.5.6.3", Collections.singletonList("locality"), EMPTY_STRING,
                false, Collections.singleton(TOP_OBJECTCLASS_NAME), EMPTY_STRING_SET, attrs,
                ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("userPassword");
        attrs.add("searchGuide");
        attrs.add("seeAlso");
        attrs.add("businessCategory");
        attrs.add("x121Address");
        attrs.add("registeredAddress");
        attrs.add("destinationIndicator");
        attrs.add("preferredDeliveryMethod");
        attrs.add("telexNumber");
        attrs.add("teletexTerminalIdentifier");
        attrs.add("telephoneNumber");
        attrs.add("internationalISDNNumber");
        attrs.add("facsimileTelephoneNumber");
        attrs.add("street");
        attrs.add("postOfficeBox");
        attrs.add("postalCode");
        attrs.add("postalAddress");
        attrs.add("physicalDeliveryOfficeName");
        attrs.add("st");
        attrs.add("l");
        attrs.add("description");

        builder.addObjectClass("2.5.6.4", Collections.singletonList("organization"), EMPTY_STRING,
                false, Collections.singleton(TOP_OBJECTCLASS_NAME), Collections.singleton("o"),
                attrs, ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("title");
        attrs.add("x121Address");
        attrs.add("registeredAddress");
        attrs.add("destinationIndicator");
        attrs.add("preferredDeliveryMethod");
        attrs.add("telexNumber");
        attrs.add("teletexTerminalIdentifier");
        attrs.add("telephoneNumber");
        attrs.add("internationalISDNNumber");
        attrs.add("facsimileTelephoneNumber");
        attrs.add("street");
        attrs.add("postOfficeBox");
        attrs.add("postalCode");
        attrs.add("postalAddress");
        attrs.add("physicalDeliveryOfficeName");
        attrs.add("ou");
        attrs.add("st");
        attrs.add("l");

        builder.addObjectClass("2.5.6.7", Collections.singletonList("organizationalPerson"),
                EMPTY_STRING, false, Collections.singleton("person"), EMPTY_STRING_SET, attrs,
                ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("x121Address");
        attrs.add("registeredAddress");
        attrs.add("destinationIndicator");
        attrs.add("preferredDeliveryMethod");
        attrs.add("telexNumber");
        attrs.add("teletexTerminalIdentifier");
        attrs.add("telephoneNumber");
        attrs.add("internationalISDNNumber");
        attrs.add("facsimileTelephoneNumber");
        attrs.add("seeAlso");
        attrs.add("roleOccupant");
        attrs.add("preferredDeliveryMethod");
        attrs.add("street");
        attrs.add("postOfficeBox");
        attrs.add("postalCode");
        attrs.add("postalAddress");
        attrs.add("physicalDeliveryOfficeName");
        attrs.add("ou");
        attrs.add("st");
        attrs.add("l");
        attrs.add("description");

        builder.addObjectClass("2.5.6.8", Collections.singletonList("organizationalRole"),
                EMPTY_STRING, false, Collections.singleton(TOP_OBJECTCLASS_NAME), Collections
                        .singleton("cn"), attrs, ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("businessCategory");
        attrs.add("description");
        attrs.add("destinationIndicator");
        attrs.add("facsimileTelephoneNumber");
        attrs.add("internationalISDNNumber");
        attrs.add("l");
        attrs.add("physicalDeliveryOfficeName");
        attrs.add("postalAddress");
        attrs.add("postalCode");
        attrs.add("postOfficeBox");
        attrs.add("preferredDeliveryMethod");
        attrs.add("registeredAddress");
        attrs.add("searchGuide");
        attrs.add("seeAlso");
        attrs.add("st");
        attrs.add("street");
        attrs.add("telephoneNumber");
        attrs.add("teletexTerminalIdentifier");
        attrs.add("telexNumber");
        attrs.add("userPassword");
        attrs.add("x121Address");

        builder.addObjectClass("2.5.6.5", Collections.singletonList("organizationalUnit"),
                EMPTY_STRING, false, Collections.singleton(TOP_OBJECTCLASS_NAME), Collections
                        .singleton("ou"), attrs, ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        must = new HashSet<String>();
        must.add("sn");
        must.add("cn");

        attrs = new HashSet<String>();
        attrs.add("userPassword");
        attrs.add("telephoneNumber");
        attrs.add("destinationIndicator");
        attrs.add("seeAlso");
        attrs.add("description");

        builder.addObjectClass("2.5.6.6", Collections.singletonList("person"), EMPTY_STRING, false,
                Collections.singleton(TOP_OBJECTCLASS_NAME), must, attrs,
                ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("businessCategory");
        attrs.add("x121Address");
        attrs.add("registeredAddress");
        attrs.add("destinationIndicator");
        attrs.add("preferredDeliveryMethod");
        attrs.add("telexNumber");
        attrs.add("teletexTerminalIdentifier");
        attrs.add("telephoneNumber");
        attrs.add("internationalISDNNumber");
        attrs.add("facsimileTelephoneNumber");
        attrs.add("preferredDeliveryMethod");
        attrs.add("street");
        attrs.add("postOfficeBox");
        attrs.add("postalCode");
        attrs.add("postalAddress");
        attrs.add("physicalDeliveryOfficeName");
        attrs.add("st");
        attrs.add("l");

        builder.addObjectClass("2.5.6.10", Collections.singletonList("residentialPerson"),
                EMPTY_STRING, false, Collections.singleton("person"), Collections.singleton("l"),
                attrs, ObjectClassType.STRUCTURAL, RFC4519_ORIGIN, false);

        builder.addObjectClass("1.3.6.1.1.3.1", Collections.singletonList("uidObject"),
                EMPTY_STRING, false, Collections.singleton(TOP_OBJECTCLASS_NAME), Collections
                        .singleton("uid"), attrs, ObjectClassType.AUXILIARY, RFC4519_ORIGIN, false);
    }

    private static void addRFC4523(final SchemaBuilder builder) {
        builder.buildSyntax(SYNTAX_CERTLIST_OID).description(SYNTAX_CERTLIST_DESCRIPTION)
                .extraProperties(RFC4523_ORIGIN).implementation(new CertificateListSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_CERTPAIR_OID).description(SYNTAX_CERTPAIR_DESCRIPTION)
                .extraProperties(RFC4523_ORIGIN).implementation(new CertificatePairSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_CERTIFICATE_OID).description(SYNTAX_CERTIFICATE_DESCRIPTION)
                .extraProperties(RFC4523_ORIGIN).implementation(new CertificateSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_CERTIFICATE_EXACT_ASSERTION_OID)
                .description(SYNTAX_CERTIFICATE_EXACT_ASSERTION_DESCRIPTION).extraProperties(RFC4523_ORIGIN)
                .implementation(new CertificateExactAssertionSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_SUPPORTED_ALGORITHM_OID).description(SYNTAX_SUPPORTED_ALGORITHM_DESCRIPTION)
                .extraProperties(RFC4523_ORIGIN).implementation(new SupportedAlgorithmSyntaxImpl()).addToSchema();

        builder.buildMatchingRule(EMR_CERTIFICATE_EXACT_OID).names(EMR_CERTIFICATE_EXACT_NAME)
                .syntaxOID(SYNTAX_CERTIFICATE_EXACT_ASSERTION_OID).extraProperties(RFC4523_ORIGIN)
                .implementation(new CertificateExactMatchingRuleImpl()).addToSchema();

        builder.addAttributeType("2.5.4.36", Collections.singletonList("userCertificate"),
                "X.509 user certificate", false, null, EMR_CERTIFICATE_EXACT_OID, null,
                null, null, SYNTAX_CERTIFICATE_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4523_ORIGIN, false);
        builder.addAttributeType("2.5.4.37", Collections.singletonList("cACertificate"),
                "X.509 CA certificate", false, null, EMR_CERTIFICATE_EXACT_OID, null,
                null, null, SYNTAX_CERTIFICATE_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4523_ORIGIN, false);
        builder.addAttributeType("2.5.4.38", Collections.singletonList("authorityRevocationList"),
                "X.509 authority revocation list", false, null, EMR_OCTET_STRING_OID, null,
                null, null, SYNTAX_CERTLIST_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4523_ORIGIN, false);
        builder.addAttributeType("2.5.4.39", Collections.singletonList("certificateRevocationList"),
                "X.509 certificate revocation list", false, null, EMR_OCTET_STRING_OID, null,
                null, null, SYNTAX_CERTLIST_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4523_ORIGIN, false);
        builder.addAttributeType("2.5.4.40", Collections.singletonList("crossCertificatePair"),
                "X.509 cross certificate pair", false, null, EMR_OCTET_STRING_OID, null,
                null, null, SYNTAX_CERTPAIR_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4523_ORIGIN, false);
        builder.addAttributeType("2.5.4.52", Collections.singletonList("supportedAlgorithms"),
                "X.509 supported algorithms", false, null, EMR_OCTET_STRING_OID, null,
                null, null, SYNTAX_SUPPORTED_ALGORITHM_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4523_ORIGIN, false);
        builder.addAttributeType("2.5.4.53", Collections.singletonList("deltaRevocationList"),
                "X.509 delta revocation list", false, null, EMR_OCTET_STRING_OID, null,
                null, null, SYNTAX_CERTLIST_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4523_ORIGIN, false);

        builder.addObjectClass("2.5.6.21", Collections.singletonList("pkiUser"),
                "X.509 PKI User", false, Collections.singleton(TOP_OBJECTCLASS_NAME), EMPTY_STRING_SET,
                Collections.singleton("userCertificate"), ObjectClassType.AUXILIARY, RFC4523_ORIGIN, false);

        Set<String> attrs = new HashSet<String>();
        attrs.add("cACertificate");
        attrs.add("certificateRevocationList");
        attrs.add("authorityRevocationList");
        attrs.add("crossCertificatePair");

        builder.addObjectClass("2.5.6.22", Collections.singletonList("pkiCA"),
                "X.509 PKI Certificate Authority", false, Collections.singleton(TOP_OBJECTCLASS_NAME),
                EMPTY_STRING_SET, attrs, ObjectClassType.AUXILIARY, RFC4523_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("certificateRevocationList");
        attrs.add("authorityRevocationList");
        attrs.add("deltaRevocationList");

        builder.addObjectClass("2.5.6.19", Collections.singletonList("cRLDistributionPoint"),
                "X.509 CRL distribution point", false, Collections.singleton(TOP_OBJECTCLASS_NAME),
                Collections.singleton("cn"), attrs, ObjectClassType.STRUCTURAL, RFC4523_ORIGIN, false);

        builder.addObjectClass("2.5.6.23", Collections.singletonList("deltaCRL"),
                "X.509 delta CRL", false, Collections.singleton(TOP_OBJECTCLASS_NAME), EMPTY_STRING_SET,
                Collections.singleton("deltaRevocationList"), ObjectClassType.AUXILIARY, RFC4523_ORIGIN, false);
        builder.addObjectClass("2.5.6.15", Collections.singletonList("strongAuthenticationUser"),
                "X.521 strong authentication user", false, Collections.singleton(TOP_OBJECTCLASS_NAME),
                Collections.singleton("userCertificate"), EMPTY_STRING_SET, ObjectClassType.AUXILIARY,
                RFC4523_ORIGIN, false);
        builder.addObjectClass("2.5.6.18", Collections.singletonList("userSecurityInformation"),
                "X.521 user security information", false, Collections.singleton(TOP_OBJECTCLASS_NAME), EMPTY_STRING_SET,
                Collections.singleton("supportedAlgorithms"), ObjectClassType.AUXILIARY, RFC4523_ORIGIN, false);

        attrs = new HashSet<String>();
        attrs.add("authorityRevocationList");
        attrs.add("certificateRevocationList");
        attrs.add("cACertificate");

        builder.addObjectClass("2.5.6.16", Collections.singletonList("certificationAuthority"),
                "X.509 certificate authority", false, Collections.singleton(TOP_OBJECTCLASS_NAME), attrs,
                Collections.singleton("crossCertificatePair"), ObjectClassType.AUXILIARY, RFC4523_ORIGIN, false);

        builder.addObjectClass("2.5.6.16.2", Collections.singletonList("certificationAuthority-V2"),
                "X.509 certificate authority, version 2", false, Collections.singleton("certificationAuthority"),
                EMPTY_STRING_SET, Collections.singleton("deltaRevocationList"), ObjectClassType.AUXILIARY,
                RFC4523_ORIGIN, false);
    }

    private static void addRFC4530(final SchemaBuilder builder) {
        builder.buildSyntax(SYNTAX_UUID_OID).description(SYNTAX_UUID_DESCRIPTION).extraProperties(RFC4530_ORIGIN)
                .implementation(new UUIDSyntaxImpl()).addToSchema();
        builder.buildMatchingRule(EMR_UUID_OID).names(EMR_UUID_NAME).syntaxOID(SYNTAX_UUID_OID)
                .extraProperties(RFC4530_ORIGIN).implementation(new UUIDEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(OMR_UUID_OID).names(OMR_UUID_NAME).syntaxOID(SYNTAX_UUID_OID)
                .extraProperties(RFC4530_ORIGIN).implementation(new UUIDOrderingMatchingRuleImpl())
                .addToSchema();
        builder.addAttributeType("1.3.6.1.1.16.4", Collections.singletonList("entryUUID"),
                "UUID of the entry", false, null, EMR_UUID_OID, OMR_UUID_OID, null, null,
                SYNTAX_UUID_OID, true, false, true, AttributeUsage.DIRECTORY_OPERATION,
                RFC4530_ORIGIN, false);
    }

    private static void addRFC5020(final SchemaBuilder builder) {
        builder.addAttributeType("1.3.6.1.1.20", Collections.singletonList("entryDN"),
                "DN of the entry", false, null, EMR_DN_OID, null, null, null,
                SYNTAX_DN_OID, true, false, true, AttributeUsage.DIRECTORY_OPERATION,
                RFC5020_ORIGIN, false);
    }

    private static void addSunProprietary(final SchemaBuilder builder) {
        builder.buildSyntax(SYNTAX_USER_PASSWORD_OID).description(SYNTAX_USER_PASSWORD_DESCRIPTION)
                .extraProperties(OPENDJ_ORIGIN).implementation(new UserPasswordSyntaxImpl()).addToSchema();
        builder.buildMatchingRule(EMR_USER_PASSWORD_EXACT_OID)
                .names(Collections.singletonList(EMR_USER_PASSWORD_EXACT_NAME))
                .description(EMR_USER_PASSWORD_EXACT_DESCRIPTION).syntaxOID(SYNTAX_USER_PASSWORD_OID)
                .extraProperties(OPENDJ_ORIGIN).implementation(new UserPasswordExactEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(AMR_DOUBLE_METAPHONE_OID).names(Collections.singletonList(AMR_DOUBLE_METAPHONE_NAME))
                .description(AMR_DOUBLE_METAPHONE_DESCRIPTION).syntaxOID(SYNTAX_DIRECTORY_STRING_OID)
                .extraProperties(OPENDJ_ORIGIN).implementation(new DoubleMetaphoneApproximateMatchingRuleImpl())
                .addToSchema();
    }

    private static void defaultAttributeTypes(final SchemaBuilder builder) {
        builder.addAttributeType("2.5.4.0", Collections.singletonList("objectClass"), EMPTY_STRING,
                false, null, EMR_OID_NAME, null, null, null, SYNTAX_OID_OID, false, false, false,
                AttributeUsage.USER_APPLICATIONS, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.4.1", Collections.singletonList("aliasedObjectName"),
                EMPTY_STRING, false, null, EMR_DN_NAME, null, null, null, SYNTAX_DN_OID, true,
                false, false, AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.18.1", Collections.singletonList("createTimestamp"),
                EMPTY_STRING, false, null, EMR_GENERALIZED_TIME_NAME, OMR_GENERALIZED_TIME_NAME,
                null, null, SYNTAX_GENERALIZED_TIME_OID, true, false, true,
                AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.18.2", Collections.singletonList("modifyTimestamp"),
                EMPTY_STRING, false, null, EMR_GENERALIZED_TIME_NAME, OMR_GENERALIZED_TIME_NAME,
                null, null, SYNTAX_GENERALIZED_TIME_OID, true, false, true,
                AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.18.3", Collections.singletonList("creatorsName"),
                EMPTY_STRING, false, null, EMR_DN_NAME, null, null, null, SYNTAX_DN_OID, true,
                false, true, AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.18.4", Collections.singletonList("modifiersName"),
                EMPTY_STRING, false, null, EMR_DN_NAME, null, null, null, SYNTAX_DN_OID, true,
                false, true, AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.18.10", Collections.singletonList("subschemaSubentry"),
                EMPTY_STRING, false, null, EMR_DN_NAME, null, null, null, SYNTAX_DN_OID, true,
                false, true, AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.5", Collections.singletonList("attributeTypes"),
                EMPTY_STRING, false, null, EMR_OID_FIRST_COMPONENT_NAME, null, null, null,
                SYNTAX_ATTRIBUTE_TYPE_OID, false, false, false, AttributeUsage.DIRECTORY_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.6", Collections.singletonList("objectClasses"),
                EMPTY_STRING, false, null, EMR_OID_FIRST_COMPONENT_NAME, null, null, null,
                SYNTAX_OBJECTCLASS_OID, false, false, false, AttributeUsage.DIRECTORY_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.4", Collections.singletonList("matchingRules"),
                EMPTY_STRING, false, null, EMR_OID_FIRST_COMPONENT_NAME, null, null, null,
                SYNTAX_MATCHING_RULE_OID, false, false, false, AttributeUsage.DIRECTORY_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.8", Collections.singletonList("matchingRuleUse"),
                EMPTY_STRING, false, null, EMR_OID_FIRST_COMPONENT_NAME, null, null, null,
                SYNTAX_MATCHING_RULE_USE_OID, false, false, false,
                AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.9", Collections.singletonList("structuralObjectClass"),
                EMPTY_STRING, false, null, EMR_OID_NAME, null, null, null, SYNTAX_OID_OID, true,
                false, true, AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.10", Collections.singletonList("governingStructureRule"),
                EMPTY_STRING, false, null, EMR_INTEGER_NAME, null, null, null, SYNTAX_INTEGER_OID,
                true, false, true, AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.4.1.1466.101.120.5", Collections
                .singletonList("namingContexts"), EMPTY_STRING, false, null, null, null, null,
                null, SYNTAX_DN_OID, false, false, false, AttributeUsage.DSA_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.4.1.1466.101.120.6", Collections
                .singletonList("altServer"), EMPTY_STRING, false, null, null, null, null, null,
                SYNTAX_IA5_STRING_OID, false, false, false, AttributeUsage.DSA_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.4.1.1466.101.120.7", Collections
                .singletonList("supportedExtension"), EMPTY_STRING, false, null, null, null, null,
                null, SYNTAX_OID_OID, false, false, false, AttributeUsage.DSA_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.4.1.1466.101.120.13", Collections
                .singletonList("supportedControl"), EMPTY_STRING, false, null, null, null, null,
                null, SYNTAX_OID_OID, false, false, false, AttributeUsage.DSA_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.4.1.1466.101.120.14", Collections
                .singletonList("supportedSASLMechanisms"), EMPTY_STRING, false, null, null, null,
                null, null, SYNTAX_DIRECTORY_STRING_OID, false, false, false,
                AttributeUsage.DSA_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.4.1.4203.1.3.5", Collections
                .singletonList("supportedFeatures"), EMPTY_STRING, false, null, EMR_OID_NAME, null,
                null, null, SYNTAX_OID_OID, false, false, false, AttributeUsage.DSA_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.4.1.1466.101.120.15", Collections
                .singletonList("supportedLDAPVersion"), EMPTY_STRING, false, null, null, null,
                null, null, SYNTAX_INTEGER_OID, false, false, false, AttributeUsage.DSA_OPERATION,
                RFC4512_ORIGIN, false);

        builder.addAttributeType("1.3.6.1.4.1.1466.101.120.16", Collections
                .singletonList("ldapSyntaxes"), EMPTY_STRING, false, null,
                EMR_OID_FIRST_COMPONENT_NAME, null, null, null, SYNTAX_LDAP_SYNTAX_OID, false,
                false, false, AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.1", Collections.singletonList("ditStructureRules"),
                EMPTY_STRING, false, null, EMR_INTEGER_FIRST_COMPONENT_NAME, null, null, null,
                SYNTAX_DIT_STRUCTURE_RULE_OID, false, false, false,
                AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.7", Collections.singletonList("nameForms"), EMPTY_STRING,
                false, null, EMR_OID_FIRST_COMPONENT_NAME, null, null, null, SYNTAX_NAME_FORM_OID,
                false, false, false, AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);

        builder.addAttributeType("2.5.21.2", Collections.singletonList("ditContentRules"),
                EMPTY_STRING, false, null, EMR_OID_FIRST_COMPONENT_NAME, null, null, null,
                SYNTAX_DIT_CONTENT_RULE_OID, false, false, false,
                AttributeUsage.DIRECTORY_OPERATION, RFC4512_ORIGIN, false);
    }

    private static void defaultMatchingRules(final SchemaBuilder builder) {
        builder.buildMatchingRule(EMR_BIT_STRING_OID).names(EMR_BIT_STRING_NAME).syntaxOID(SYNTAX_BIT_STRING_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new BitStringEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(EMR_BOOLEAN_OID).names(EMR_BOOLEAN_NAME).syntaxOID(SYNTAX_BOOLEAN_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new BooleanEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(EMR_CASE_EXACT_IA5_OID).names(EMR_CASE_EXACT_IA5_NAME)
                .syntaxOID(SYNTAX_IA5_STRING_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseExactIA5EqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(SMR_CASE_EXACT_IA5_OID).names(SMR_CASE_EXACT_IA5_NAME)
                .syntaxOID(SYNTAX_SUBSTRING_ASSERTION_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseExactIA5SubstringMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_CASE_EXACT_OID).names(EMR_CASE_EXACT_NAME).syntaxOID(SYNTAX_DIRECTORY_STRING_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new CaseExactEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(OMR_CASE_EXACT_OID).names(OMR_CASE_EXACT_NAME).syntaxOID(SYNTAX_DIRECTORY_STRING_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new CaseExactOrderingMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(SMR_CASE_EXACT_OID).names(SMR_CASE_EXACT_NAME)
                .syntaxOID(SYNTAX_SUBSTRING_ASSERTION_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseExactSubstringMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_CASE_IGNORE_IA5_OID).names(EMR_CASE_IGNORE_IA5_NAME)
                .syntaxOID(SYNTAX_IA5_STRING_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseIgnoreIA5EqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(SMR_CASE_IGNORE_IA5_OID).names(SMR_CASE_IGNORE_IA5_NAME)
                .syntaxOID(SYNTAX_SUBSTRING_ASSERTION_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseIgnoreIA5SubstringMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_CASE_IGNORE_LIST_OID).names(EMR_CASE_IGNORE_LIST_NAME)
                .syntaxOID(SYNTAX_POSTAL_ADDRESS_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseIgnoreListEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(SMR_CASE_IGNORE_LIST_OID).names(SMR_CASE_IGNORE_LIST_NAME)
                .syntaxOID(SYNTAX_SUBSTRING_ASSERTION_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseIgnoreListSubstringMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_CASE_IGNORE_OID).names(EMR_CASE_IGNORE_NAME)
                .syntaxOID(SYNTAX_DIRECTORY_STRING_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseIgnoreEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(OMR_CASE_IGNORE_OID).names(OMR_CASE_IGNORE_NAME)
                .syntaxOID(SYNTAX_DIRECTORY_STRING_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseIgnoreOrderingMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(SMR_CASE_IGNORE_OID).names(SMR_CASE_IGNORE_NAME)
                .syntaxOID(SYNTAX_SUBSTRING_ASSERTION_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new CaseIgnoreSubstringMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_DIRECTORY_STRING_FIRST_COMPONENT_OID)
                .names(Collections.singletonList(EMR_DIRECTORY_STRING_FIRST_COMPONENT_NAME))
                .syntaxOID(SYNTAX_DIRECTORY_STRING_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new DirectoryStringFirstComponentEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_DN_OID).names(EMR_DN_NAME).syntaxOID(SYNTAX_DN_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new DistinguishedNameEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(EMR_GENERALIZED_TIME_OID).names(EMR_GENERALIZED_TIME_NAME)
                .syntaxOID(SYNTAX_GENERALIZED_TIME_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new GeneralizedTimeEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(OMR_GENERALIZED_TIME_OID).names(OMR_GENERALIZED_TIME_NAME)
                .syntaxOID(SYNTAX_GENERALIZED_TIME_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new GeneralizedTimeOrderingMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_INTEGER_FIRST_COMPONENT_OID).names(EMR_INTEGER_FIRST_COMPONENT_NAME)
                .syntaxOID(SYNTAX_INTEGER_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new IntegerFirstComponentEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_INTEGER_OID).names(EMR_INTEGER_NAME).syntaxOID(SYNTAX_INTEGER_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new IntegerEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(OMR_INTEGER_OID).names(OMR_INTEGER_NAME).syntaxOID(SYNTAX_INTEGER_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new IntegerOrderingMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(EMR_KEYWORD_OID).names(EMR_KEYWORD_NAME).syntaxOID(SYNTAX_DIRECTORY_STRING_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new KeywordEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(EMR_NUMERIC_STRING_OID).names(EMR_NUMERIC_STRING_NAME)
                .syntaxOID(SYNTAX_NUMERIC_STRING_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new NumericStringEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(OMR_NUMERIC_STRING_OID).names(OMR_NUMERIC_STRING_NAME)
                .syntaxOID(SYNTAX_NUMERIC_STRING_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new NumericStringOrderingMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(SMR_NUMERIC_STRING_OID).names(SMR_NUMERIC_STRING_NAME)
                .syntaxOID(SYNTAX_SUBSTRING_ASSERTION_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new NumericStringSubstringMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_OID_FIRST_COMPONENT_OID).names(EMR_OID_FIRST_COMPONENT_NAME)
                .syntaxOID(SYNTAX_OID_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new ObjectIdentifierFirstComponentEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_OID_OID).names(EMR_OID_NAME).syntaxOID(SYNTAX_OID_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new ObjectIdentifierEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(EMR_OCTET_STRING_OID).names(EMR_OCTET_STRING_NAME).syntaxOID(SYNTAX_OCTET_STRING_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new OctetStringEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(OMR_OCTET_STRING_OID).names(OMR_OCTET_STRING_NAME).syntaxOID(SYNTAX_OCTET_STRING_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new OctetStringOrderingMatchingRuleImpl())
                .addToSchema();
        // SMR octet string is not in any LDAP RFC and its from X.500
        builder.buildMatchingRule(SMR_OCTET_STRING_OID).names(SMR_OCTET_STRING_NAME).syntaxOID(SYNTAX_OCTET_STRING_OID)
                .extraProperties(X500_ORIGIN).implementation(new OctetStringSubstringMatchingRuleImpl())
                .addToSchema();
        // Depreciated in RFC 4512
        builder.buildMatchingRule(EMR_PROTOCOL_INFORMATION_OID).names(EMR_PROTOCOL_INFORMATION_NAME)
                .syntaxOID(SYNTAX_PROTOCOL_INFORMATION_OID).extraProperties(RFC2252_ORIGIN)
                .implementation(new ProtocolInformationEqualityMatchingRuleImpl()).addToSchema();
        // Depreciated in RFC 4512
        builder.buildMatchingRule(EMR_PRESENTATION_ADDRESS_OID).names(EMR_PRESENTATION_ADDRESS_NAME)
                .syntaxOID(SYNTAX_PRESENTATION_ADDRESS_OID).extraProperties(RFC2252_ORIGIN)
                .implementation(new PresentationAddressEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_TELEPHONE_OID).names(EMR_TELEPHONE_NAME).syntaxOID(SYNTAX_TELEPHONE_OID)
                .extraProperties(RFC2252_ORIGIN).implementation(new TelephoneNumberEqualityMatchingRuleImpl())
                .addToSchema();
        builder.buildMatchingRule(SMR_TELEPHONE_OID).names(SMR_TELEPHONE_NAME)
                .syntaxOID(SYNTAX_SUBSTRING_ASSERTION_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new TelephoneNumberSubstringMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_UNIQUE_MEMBER_OID).names(EMR_UNIQUE_MEMBER_NAME)
                .syntaxOID(SYNTAX_NAME_AND_OPTIONAL_UID_OID).extraProperties(RFC4512_ORIGIN)
                .implementation(new UniqueMemberEqualityMatchingRuleImpl()).addToSchema();
        builder.buildMatchingRule(EMR_WORD_OID).names(EMR_WORD_NAME).syntaxOID(SYNTAX_DIRECTORY_STRING_OID)
                .extraProperties(RFC4512_ORIGIN).implementation(new KeywordEqualityMatchingRuleImpl())
                .addToSchema();
    }

    private static void defaultObjectClasses(final SchemaBuilder builder) {
        builder.addObjectClass(TOP_OBJECTCLASS_OID,
                Collections.singletonList(TOP_OBJECTCLASS_NAME), TOP_OBJECTCLASS_DESCRIPTION,
                false, EMPTY_STRING_SET, Collections.singleton("objectClass"), EMPTY_STRING_SET,
                ObjectClassType.ABSTRACT, RFC4512_ORIGIN, false);

        builder.addObjectClass("2.5.6.1", Collections.singletonList("alias"), EMPTY_STRING, false,
                Collections.singleton("top"), Collections.singleton("aliasedObjectName"),
                EMPTY_STRING_SET, ObjectClassType.STRUCTURAL, RFC4512_ORIGIN, false);

        builder.addObjectClass(EXTENSIBLE_OBJECT_OBJECTCLASS_OID, Collections
                .singletonList(EXTENSIBLE_OBJECT_OBJECTCLASS_NAME), EMPTY_STRING, false,
                Collections.singleton(TOP_OBJECTCLASS_NAME), EMPTY_STRING_SET, EMPTY_STRING_SET,
                ObjectClassType.AUXILIARY, RFC4512_ORIGIN, false);

        final Set<String> subschemaAttrs = new HashSet<String>();
        subschemaAttrs.add("dITStructureRules");
        subschemaAttrs.add("nameForms");
        subschemaAttrs.add("ditContentRules");
        subschemaAttrs.add("objectClasses");
        subschemaAttrs.add("attributeTypes");
        subschemaAttrs.add("matchingRules");
        subschemaAttrs.add("matchingRuleUse");

        builder.addObjectClass("2.5.20.1", Collections.singletonList("subschema"), EMPTY_STRING,
                false, Collections.singleton(TOP_OBJECTCLASS_NAME), EMPTY_STRING_SET,
                subschemaAttrs, ObjectClassType.AUXILIARY, RFC4512_ORIGIN, false);
    }

    private static void defaultSyntaxes(final SchemaBuilder builder) {
        // All RFC 4512 / 4517
        builder.buildSyntax(SYNTAX_ATTRIBUTE_TYPE_OID).description(SYNTAX_ATTRIBUTE_TYPE_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new AttributeTypeSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_BINARY_OID).description(SYNTAX_BINARY_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new BinarySyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_BIT_STRING_OID).description(SYNTAX_BIT_STRING_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new BitStringSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_BOOLEAN_OID).description(SYNTAX_BOOLEAN_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new BooleanSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_COUNTRY_STRING_OID).description(SYNTAX_COUNTRY_STRING_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new CountryStringSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_DELIVERY_METHOD_OID).description(SYNTAX_DELIVERY_METHOD_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new DeliveryMethodSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_DIRECTORY_STRING_OID).description(SYNTAX_DIRECTORY_STRING_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new DirectoryStringSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_DIT_CONTENT_RULE_OID).description(SYNTAX_DIT_CONTENT_RULE_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new DITContentRuleSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_DIT_STRUCTURE_RULE_OID).description(SYNTAX_DIT_STRUCTURE_RULE_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new DITStructureRuleSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_DN_OID).description(SYNTAX_DN_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new DistinguishedNameSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_ENHANCED_GUIDE_OID).description(SYNTAX_ENHANCED_GUIDE_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new EnhancedGuideSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_FAXNUMBER_OID).description(SYNTAX_FAXNUMBER_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new FacsimileNumberSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_FAX_OID).description(SYNTAX_FAX_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new FaxSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_GENERALIZED_TIME_OID).description(SYNTAX_GENERALIZED_TIME_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new GeneralizedTimeSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_GUIDE_OID).description(SYNTAX_GUIDE_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new GuideSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_IA5_STRING_OID).description(SYNTAX_IA5_STRING_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new IA5StringSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_INTEGER_OID).description(SYNTAX_INTEGER_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new IntegerSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_JPEG_OID).description(SYNTAX_JPEG_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new JPEGSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_MATCHING_RULE_OID).description(SYNTAX_MATCHING_RULE_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new MatchingRuleSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_MATCHING_RULE_USE_OID).description(SYNTAX_MATCHING_RULE_USE_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new MatchingRuleUseSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_LDAP_SYNTAX_OID).description(SYNTAX_LDAP_SYNTAX_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new LDAPSyntaxDescriptionSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_NAME_AND_OPTIONAL_UID_OID).description(SYNTAX_NAME_AND_OPTIONAL_UID_DESCRIPTION)
                .extraProperties(RFC4517_ORIGIN).implementation(new NameAndOptionalUIDSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_NAME_FORM_OID).description(SYNTAX_NAME_FORM_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new NameFormSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_NUMERIC_STRING_OID).description(SYNTAX_NUMERIC_STRING_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new NumericStringSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_OBJECTCLASS_OID).description(SYNTAX_OBJECTCLASS_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new ObjectClassSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_OCTET_STRING_OID).description(SYNTAX_OCTET_STRING_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new OctetStringSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_OID_OID).description(SYNTAX_OID_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new OIDSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_OTHER_MAILBOX_OID).description(SYNTAX_OTHER_MAILBOX_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new OtherMailboxSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_POSTAL_ADDRESS_OID).description(SYNTAX_POSTAL_ADDRESS_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new PostalAddressSyntaxImpl()).addToSchema();
        // Depreciated in RFC 4512
        builder.buildSyntax(SYNTAX_PRESENTATION_ADDRESS_OID).description(SYNTAX_PRESENTATION_ADDRESS_DESCRIPTION)
                .extraProperties(RFC2252_ORIGIN).implementation(new PresentationAddressSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_PRINTABLE_STRING_OID).description(SYNTAX_PRINTABLE_STRING_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new PrintableStringSyntaxImpl()).addToSchema();
        // Depreciated in RFC 4512
        builder.buildSyntax(SYNTAX_PROTOCOL_INFORMATION_OID).description(SYNTAX_PROTOCOL_INFORMATION_DESCRIPTION)
                .extraProperties(RFC2252_ORIGIN).implementation(new ProtocolInformationSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_SUBSTRING_ASSERTION_OID).description(SYNTAX_SUBSTRING_ASSERTION_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new SubstringAssertionSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_TELEPHONE_OID).description(SYNTAX_TELEPHONE_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new TelephoneNumberSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_TELETEX_TERM_ID_OID).description(SYNTAX_TELETEX_TERM_ID_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new TeletexTerminalIdentifierSyntaxImpl())
                .addToSchema();
        builder.buildSyntax(SYNTAX_TELEX_OID).description(SYNTAX_TELEX_DESCRIPTION).extraProperties(RFC4512_ORIGIN)
                .implementation(new TelexNumberSyntaxImpl()).addToSchema();
        builder.buildSyntax(SYNTAX_UTC_TIME_OID).description(SYNTAX_UTC_TIME_DESCRIPTION)
                .extraProperties(RFC4512_ORIGIN).implementation(new UTCTimeSyntaxImpl()).addToSchema();
    }

    private CoreSchemaImpl() {
        // Prevent instantiation.
    }
}
