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
 */

package com.forgerock.opendj.util;

import org.forgerock.opendj.ldap.AttributeDescription;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.schema.Schema;

/**
 * Common {@link Function} implementations.
 */
public final class Functions {

    private static final class FixedFunction<M, N, P> implements Function<M, N, Void> {
        private final Function<M, N, P> function;

        private final P parameter;

        private FixedFunction(final Function<M, N, P> function, final P p) {
            this.function = function;
            this.parameter = p;
        }

        /**
         * {@inheritDoc}
         */
        public N apply(final M value, final Void p) {
            return function.apply(value, parameter);
        }

    }

    private static final Function<ByteString, AttributeDescription, Schema> BYTESTRING_TO_ATTRIBUTE_DESCRIPTION =
            new Function<ByteString, AttributeDescription, Schema>() {

                public AttributeDescription apply(final ByteString value, final Schema p) {
                    // FIXME: what should we do if parsing fails?
                    return AttributeDescription.valueOf(value.toString(), p);
                }
            };

    private static final Function<ByteString, Boolean, Void> BYTESTRING_TO_BOOLEAN =
            new Function<ByteString, Boolean, Void>() {

                public Boolean apply(final ByteString value, final Void p) {
                    final String valueString = StaticUtils.toLowerCase(value.toString());

                    if (valueString.equals("true") || valueString.equals("yes")
                            || valueString.equals("on") || valueString.equals("1")) {
                        return Boolean.TRUE;
                    } else if (valueString.equals("false") || valueString.equals("no")
                            || valueString.equals("off") || valueString.equals("0")) {
                        return Boolean.FALSE;
                    } else {
                        throw new NumberFormatException("Invalid boolean value \"" + valueString
                                + "\"");
                    }
                }
            };

    private static final Function<ByteString, DN, Schema> BYTESTRING_TO_DN =
            new Function<ByteString, DN, Schema>() {

                public DN apply(final ByteString value, final Schema p) {
                    // FIXME: what should we do if parsing fails?

                    // FIXME: we should have a ByteString valueOf
                    // implementation.
                    return DN.valueOf(value.toString(), p);
                }
            };

    private static final Function<ByteString, Integer, Void> BYTESTRING_TO_INTEGER =
            new Function<ByteString, Integer, Void>() {

                public Integer apply(final ByteString value, final Void p) {
                    // We do not use ByteString.toInt() as we are string based.
                    return Integer.valueOf(value.toString());
                }
            };

    private static final Function<ByteString, Long, Void> BYTESTRING_TO_LONG =
            new Function<ByteString, Long, Void>() {

                public Long apply(final ByteString value, final Void p) {
                    // We do not use ByteString.toLong() as we are string based.
                    return Long.valueOf(value.toString());
                }
            };

    private static final Function<ByteString, String, Void> BYTESTRING_TO_STRING =
            new Function<ByteString, String, Void>() {

                public String apply(final ByteString value, final Void p) {
                    return value.toString();
                }
            };

    private static final Function<Object, ByteString, Void> OBJECT_TO_BYTESTRING =
            new Function<Object, ByteString, Void>() {

                public ByteString apply(final Object value, final Void p) {
                    return ByteString.valueOf(value);
                }
            };

    private static final Function<String, String, Void> NORMALIZE_STRING =
            new Function<String, String, Void>() {

                public String apply(final String value, final Void p) {
                    return StaticUtils.toLowerCase(value).trim();
                }
            };

    private static final Function<Object, Object, Void> IDENTITY =
            new Function<Object, Object, Void>() {

                public Object apply(Object value, Void p) {
                    return value;
                }

            };

    /**
     * Returns a function which which always invokes {@code function} with
     * {@code p}.
     *
     * @param <M>
     *            The type of input values transformed by this function.
     * @param <N>
     *            The type of output values return by this function.
     * @param <P>
     *            The type of the additional parameter to this function's
     *            {@code apply} method. Use {@link java.lang.Void} for functions
     *            that do not need an additional parameter.
     * @param function
     *            The function to wrap.
     * @param p
     *            The parameter which will always be passed to {@code function}.
     * @return A function which which always invokes {@code function} with
     *         {@code p}.
     */
    public static <M, N, P> Function<M, N, Void> fixedFunction(final Function<M, N, P> function,
            final P p) {
        return new FixedFunction<M, N, P>(function, p);
    }

    /**
     * Returns a function which always returns the value that it was provided
     * with.
     *
     * @param <M>
     *            The type of values transformed by this function.
     * @return A function which always returns the value that it was provided
     *         with.
     */
    @SuppressWarnings("unchecked")
    public static <M> Function<M, M, Void> identityFunction() {
        return (Function<M, M, Void>) IDENTITY;
    }

    /**
     * Returns a function which converts a {@code String} to lower case using
     * {@link StaticUtils#toLowerCase} and then trims it.
     *
     * @return A function which converts a {@code String} to lower case using
     *         {@link StaticUtils#toLowerCase} and then trims it.
     */
    public static Function<String, String, Void> normalizeString() {
        return NORMALIZE_STRING;
    }

    /**
     * Returns a function which converts an {@code Object} to a
     * {@code ByteString} using the {@link ByteString#valueOf(Object)} method.
     *
     * @return A function which converts an {@code Object} to a
     *         {@code ByteString} .
     */
    public static Function<Object, ByteString, Void> objectToByteString() {
        return OBJECT_TO_BYTESTRING;
    }

    /**
     * Returns a function which parses the string representation of a
     * {@code ByteString} as an {@code AttributeDescription} using the default
     * schema. Invalid values will result in a
     * {@code LocalizedIllegalArgumentException}.
     *
     * @return A function which parses the string representation of a
     *         {@code ByteString} as an {@code AttributeDescription}.
     */
    public static Function<ByteString, AttributeDescription, Void> valueToAttributeDescription() {
        return fixedFunction(BYTESTRING_TO_ATTRIBUTE_DESCRIPTION, Schema.getDefaultSchema());
    }

    /**
     * Returns a function which parses the string representation of a
     * {@code ByteString} as an {@code AttributeDescription} using the provided
     * schema. Invalid values will result in a
     * {@code LocalizedIllegalArgumentException}.
     *
     * @param schema
     *            The schema to use for decoding attribute descriptions.
     * @return A function which parses the string representation of a
     *         {@code ByteString} as an {@code AttributeDescription}.
     */
    public static Function<ByteString, AttributeDescription, Void> valueToAttributeDescription(
            final Schema schema) {
        return fixedFunction(BYTESTRING_TO_ATTRIBUTE_DESCRIPTION, schema);
    }

    /**
     * Returns a function which parses the string representation of a
     * {@code ByteString} to a {@code Boolean}. The function will accept the
     * values {@code 0}, {@code false}, {@code no}, {@code off}, {@code 1},
     * {@code true}, {@code yes}, {@code on}. All other values will result in a
     * {@code NumberFormatException}.
     *
     * @return A function which transforms a {@code ByteString} to a
     *         {@code Boolean}.
     */
    public static Function<ByteString, Boolean, Void> valueToBoolean() {
        return BYTESTRING_TO_BOOLEAN;
    }

    /**
     * Returns a function which parses the string representation of a
     * {@code ByteString} as a {@code DN} using the default schema. Invalid
     * values will result in a {@code LocalizedIllegalArgumentException}.
     *
     * @return A function which parses the string representation of a
     *         {@code ByteString} as an {@code DN}.
     */
    public static Function<ByteString, DN, Void> valueToDN() {
        return fixedFunction(BYTESTRING_TO_DN, Schema.getDefaultSchema());
    }

    /**
     * Returns a function which parses the string representation of a
     * {@code ByteString} as a {@code DN} using the provided schema. Invalid
     * values will result in a {@code LocalizedIllegalArgumentException}.
     *
     * @param schema
     *            The schema to use for decoding DNs.
     * @return A function which parses the string representation of a
     *         {@code ByteString} as an {@code DN}.
     */
    public static Function<ByteString, DN, Void> valueToDN(final Schema schema) {
        return fixedFunction(BYTESTRING_TO_DN, schema);
    }

    /**
     * Returns a function which parses the string representation of a
     * {@code ByteString} as an {@code Integer}. Invalid values will result in a
     * {@code NumberFormatException}.
     *
     * @return A function which parses the string representation of a
     *         {@code ByteString} as an {@code Integer}.
     */
    public static Function<ByteString, Integer, Void> valueToInteger() {
        return BYTESTRING_TO_INTEGER;
    }

    /**
     * Returns a function which parses the string representation of a
     * {@code ByteString} as a {@code Long}. Invalid values will result in a
     * {@code NumberFormatException}.
     *
     * @return A function which parses the string representation of a
     *         {@code ByteString} as a {@code Long}.
     */
    public static Function<ByteString, Long, Void> valueToLong() {
        return BYTESTRING_TO_LONG;
    }

    /**
     * Returns a function which parses a {@code ByteString} as a UTF-8 encoded
     * {@code String}.
     *
     * @return A function which parses the string representation of a
     *         {@code ByteString} as a UTF-8 encoded {@code String}.
     */
    public static Function<ByteString, String, Void> valueToString() {
        return BYTESTRING_TO_STRING;
    }

    // Prevent instantiation
    private Functions() {
        // Do nothing.
    }

}
