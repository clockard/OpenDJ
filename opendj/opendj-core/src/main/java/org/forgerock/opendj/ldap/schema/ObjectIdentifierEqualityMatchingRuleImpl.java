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
 *      Copyright 2009 Sun Microsystems, Inc.
 *      Portions copyright 2011-2014 ForgeRock AS
 */
package org.forgerock.opendj.ldap.schema;

import org.forgerock.opendj.ldap.Assertion;
import org.forgerock.opendj.ldap.ByteSequence;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DecodeException;

import com.forgerock.opendj.util.StaticUtils;
import com.forgerock.opendj.util.SubstringReader;

/**
 * This class defines the objectIdentifierMatch matching rule defined in X.520
 * and referenced in RFC 2252. This expects to work on OIDs and will match
 * either an attribute/objectclass name or a numeric OID. NOTE: This matching
 * rule requires a schema to lookup object identifiers in the descriptor form.
 */
final class ObjectIdentifierEqualityMatchingRuleImpl extends AbstractEqualityMatchingRuleImpl {

    static String resolveNames(final Schema schema, final String oid) {
        if (!StaticUtils.isDigit(oid.charAt(0))) {
            // Do an best effort attempt to normalize names to OIDs.
            String schemaName = null;
            if (schema.hasAttributeType(oid)) {
                schemaName = schema.getAttributeType(oid).getOID();
            }
            if (schemaName == null && schema.hasDITContentRule(oid)) {
                schemaName = schema.getDITContentRule(oid).getStructuralClass().getOID();
            }
            if (schemaName == null && schema.hasSyntax(oid)) {
                schemaName = schema.getSyntax(oid).getOID();
            }
            if (schemaName == null && schema.hasObjectClass(oid)) {
                schemaName = schema.getObjectClass(oid).getOID();
            }
            if (schemaName == null && schema.hasMatchingRule(oid)) {
                schemaName = schema.getMatchingRule(oid).getOID();
            }
            if (schemaName == null && schema.hasMatchingRuleUse(oid)) {
                schemaName = schema.getMatchingRuleUse(oid).getMatchingRule().getOID();
            }
            if (schemaName == null && schema.hasNameForm(oid)) {
                schemaName = schema.getNameForm(oid).getOID();
            }

            if (schemaName != null) {
                return schemaName;
            }
            return StaticUtils.toLowerCase(oid);
        }
        return oid;
    }

    @Override
    public Assertion getAssertion(final Schema schema, final ByteSequence assertionValue)
            throws DecodeException {
        final String definition = assertionValue.toString();
        final SubstringReader reader = new SubstringReader(definition);
        final String oid = SchemaUtils.readOID(reader, schema.allowMalformedNamesAndOptions());
        final String normalized = resolveNames(schema, oid);
        return DefaultAssertion.equality(ByteString.valueOf(normalized));
    }

    public ByteString normalizeAttributeValue(final Schema schema, final ByteSequence value)
            throws DecodeException {
        final String definition = value.toString();
        final SubstringReader reader = new SubstringReader(definition);
        final String oid = SchemaUtils.readOID(reader, schema.allowMalformedNamesAndOptions());
        final String normalized = resolveNames(schema, oid);
        return ByteString.valueOf(normalized);
    }
}
