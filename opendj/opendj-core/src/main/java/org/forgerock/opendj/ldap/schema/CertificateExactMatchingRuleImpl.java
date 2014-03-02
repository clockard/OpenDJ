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
 *      Copyright 2006-2009 Sun Microsystems, Inc.
 *      Portions Copyright 2013-2014 Manuel Gaupp
 */
package org.forgerock.opendj.ldap.schema;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500Principal;

import com.forgerock.opendj.util.StaticUtils;
import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.i18n.slf4j.LocalizedLogger;
import org.forgerock.opendj.ldap.Assertion;
import org.forgerock.opendj.ldap.ByteSequence;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.ByteStringBuilder;
import org.forgerock.opendj.ldap.DecodeException;
import org.forgerock.opendj.ldap.GSERParser;
import org.forgerock.opendj.ldap.DN;

import static com.forgerock.opendj.ldap.CoreMessages.ERR_MR_CERTIFICATE_MATCH_EXPECTED_END;
import static com.forgerock.opendj.ldap.CoreMessages.ERR_MR_CERTIFICATE_MATCH_GSER_INVALID;
import static com.forgerock.opendj.ldap.CoreMessages.ERR_MR_CERTIFICATE_MATCH_INVALID_DN;
import static com.forgerock.opendj.ldap.CoreMessages.ERR_MR_CERTIFICATE_MATCH_IDENTIFIER_NOT_FOUND;
import static com.forgerock.opendj.ldap.CoreMessages.ERR_MR_CERTIFICATE_MATCH_PARSE_ERROR;

/**
 * This class implements the certificateExactMatch matching rule defined in
 * X.509 and referenced in RFC 4523.
 */
final class CertificateExactMatchingRuleImpl
        extends AbstractMatchingRuleImpl {

    private static final LocalizedLogger logger = LocalizedLogger.getLoggerForThisClass();

    /**
     * The GSER identifier for the serialNumber named value.
     */
    private static final String GSER_ID_SERIALNUMBER = "serialNumber";

    /**
     * The GSER identifier for the issuer named value.
     */
    private static final String GSER_ID_ISSUER = "issuer";

    /**
     * The GSER identifier for the rdnSequence IdentifiedChoiceValue.
     */
    private static final String GSER_ID_RDNSEQUENCE = "rdnSequence";

    /**
     * Retrieves the normalized form of the provided value, which is best suited
     * for efficiently performing matching operations on that value.
     *
     * @param value The value to be normalized.
     *
     * @return The normalized version of the provided value.
     *
     * @throws DirectoryException If the provided value is invalid according to
     * the associated attribute syntax.
     */
    public ByteString normalizeAttributeValue(final Schema schema, final ByteSequence value)
            throws DecodeException {
        final BigInteger serialNumber;
        final String dnstring;
        String certificateIssuer;

        // Read the X.509 Certificate and extract serialNumber and issuerDN
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream inputStream = new ByteArrayInputStream(value.toByteArray());
            X509Certificate certValue = (X509Certificate) certFactory
                    .generateCertificate(inputStream);

            serialNumber = certValue.getSerialNumber();
            X500Principal issuer = certValue.getIssuerX500Principal();
            dnstring = issuer.getName(X500Principal.RFC2253);
        } catch (CertificateException ce) {
            // There seems to be a problem while parsing the certificate.
            final LocalizableMessage message =
                    ERR_MR_CERTIFICATE_MATCH_PARSE_ERROR.get(ce.getMessage());
            logger.trace(message);

            // return the raw bytes as a fall back
            return value.toByteString();
        }

        // Normalize the DN
        try {
            DN dn = DN.valueOf(dnstring, schema.asNonStrictSchema());
            certificateIssuer = dn.toNormalizedString();
        } catch (Exception e) {
            // We couldn't normalize the DN for some reason.
            final LocalizableMessage message
                = ERR_MR_CERTIFICATE_MATCH_INVALID_DN.get(dnstring,
                    StaticUtils.getExceptionMessage(e));
            throw DecodeException.error(message);
        }

        // Create the encoded value
        return createEncodedValue(serialNumber, certificateIssuer);
    }

    /**
     * {@inheritDoc}
     */
    public Assertion getAssertion(final Schema schema, final ByteSequence value)
            throws DecodeException {
        // validate and normalize the GSER structure
        // according to the definitions from RFC 4523, Appendix A.1
        final BigInteger serialNumber;
        final String dnstring;
        String certificateIssuer;

        final GSERParser parser;
        String identifier;

        parser = new GSERParser(value.toString());

        try {
            // the String starts with a sequence
            parser.readStartSequence();
        } catch (DecodeException e) {
            logger.traceException(e);
            // Assume the assertion value is a certificate and parse issuer and
            // serial number. If the value is not even a certificate then the
            // raw bytes will be returned.
            return new DefaultEqualityAssertion(normalizeAttributeValue(schema, value));
        }

        try {
            // the first namedValue is serialNumber
            identifier = parser.nextNamedValueIdentifier();
            if (!identifier.equals(GSER_ID_SERIALNUMBER)) {
                LocalizableMessage message =
                        ERR_MR_CERTIFICATE_MATCH_IDENTIFIER_NOT_FOUND.get(GSER_ID_SERIALNUMBER);
                throw DecodeException.error(message);
            }

            // The value for the serialNumber
            serialNumber = parser.nextBigInteger();

            // separator
            parser.skipSeparator();

            // the next namedValue is issuer
            identifier = parser.nextNamedValueIdentifier();
            if (!identifier.equals(GSER_ID_ISSUER)) {
                LocalizableMessage message =
                        ERR_MR_CERTIFICATE_MATCH_IDENTIFIER_NOT_FOUND.get(GSER_ID_ISSUER);
                throw DecodeException.error(message);
            }

            // expecting "rdnSequence:"
            identifier = parser.nextChoiceValueIdentifier();
            if (!identifier.equals(GSER_ID_RDNSEQUENCE)) {
                LocalizableMessage message =
                        ERR_MR_CERTIFICATE_MATCH_IDENTIFIER_NOT_FOUND.get(GSER_ID_RDNSEQUENCE);
                throw DecodeException.error(message);
            }

            // now the issuer dn
            dnstring = parser.nextString();

            // Closing the Sequence
            parser.readEndSequence();

            // There should not be additional characters
            if (parser.hasNext()) {
                LocalizableMessage message = ERR_MR_CERTIFICATE_MATCH_EXPECTED_END.get();
                throw DecodeException.error(message);
            }
        } catch (DecodeException e) {
            LocalizableMessage message =
                    ERR_MR_CERTIFICATE_MATCH_GSER_INVALID.get(StaticUtils.getExceptionMessage(e));
            throw DecodeException.error(message);
        }

        // Normalize the DN
        try {
            DN dn = DN.valueOf(dnstring, schema.asNonStrictSchema());
            certificateIssuer = dn.toNormalizedString();
        } catch (Exception e) {
            logger.traceException(e);

            // We couldn't normalize the DN for some reason.
            LocalizableMessage message =
                    ERR_MR_CERTIFICATE_MATCH_INVALID_DN.get(dnstring,
                            StaticUtils.getExceptionMessage(e));
            throw DecodeException.error(message);
        }

        // Create the encoded value
        return new DefaultEqualityAssertion(createEncodedValue(serialNumber, certificateIssuer));
    }

    /**
     * Creates the value containing serialNumber and issuer DN.
     *
     * @param serial the serialNumber
     * @param issuerDN the issuer DN String
     *
     * @return the encoded ByteString
     */
    private static ByteString createEncodedValue(BigInteger serial,
            String issuerDN) {
        ByteStringBuilder builder = new ByteStringBuilder();
        builder.append(StaticUtils.getBytes(issuerDN));
        builder.append((byte) 0); // Separator
        builder.append(serial.toByteArray());
        return builder.toByteString();
    }

}
