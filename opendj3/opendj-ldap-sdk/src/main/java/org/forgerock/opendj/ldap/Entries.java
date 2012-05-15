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
 *      Copyright 2010 Sun Microsystems, Inc.
 *      Portions copyright 2011-2012 ForgeRock AS
 */

package org.forgerock.opendj.ldap;

import static org.forgerock.opendj.ldap.AttributeDescription.objectClass;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.forgerock.i18n.LocalizableMessage;
import org.forgerock.opendj.ldap.requests.ModifyRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.schema.ObjectClass;
import org.forgerock.opendj.ldap.schema.ObjectClassType;
import org.forgerock.opendj.ldap.schema.Schema;
import org.forgerock.opendj.ldap.schema.SchemaValidationPolicy;
import org.forgerock.opendj.ldap.schema.UnknownSchemaElementException;

import com.forgerock.opendj.util.Iterables;
import com.forgerock.opendj.util.Validator;

/**
 * This class contains methods for creating and manipulating entries.
 *
 * @see Entry
 */
public final class Entries {

    private static final class UnmodifiableEntry implements Entry {
        private final Entry entry;

        private UnmodifiableEntry(final Entry entry) {
            this.entry = entry;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean addAttribute(final Attribute attribute) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean addAttribute(final Attribute attribute,
                final Collection<ByteString> duplicateValues) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry addAttribute(final String attributeDescription, final Object... values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Entry clearAttributes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAttribute(final Attribute attribute,
                final Collection<ByteString> missingValues) {
            return entry.containsAttribute(attribute, missingValues);
        }

        @Override
        public boolean containsAttribute(final String attributeDescription, final Object... values) {
            return entry.containsAttribute(attributeDescription, values);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object object) {
            return (object == this || entry.equals(object));
        }

        @Override
        public Iterable<Attribute> getAllAttributes() {
            return Iterables.unmodifiableIterable(Iterables.transformedIterable(entry
                    .getAllAttributes(), UNMODIFIABLE_ATTRIBUTE_FUNCTION));
        }

        @Override
        public Iterable<Attribute> getAllAttributes(final AttributeDescription attributeDescription) {
            return Iterables.unmodifiableIterable(Iterables.transformedIterable(entry
                    .getAllAttributes(attributeDescription), UNMODIFIABLE_ATTRIBUTE_FUNCTION));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Iterable<Attribute> getAllAttributes(final String attributeDescription) {
            return Iterables.unmodifiableIterable(Iterables.transformedIterable(entry
                    .getAllAttributes(attributeDescription), UNMODIFIABLE_ATTRIBUTE_FUNCTION));
        }

        @Override
        public Attribute getAttribute(final AttributeDescription attributeDescription) {
            final Attribute attribute = entry.getAttribute(attributeDescription);
            if (attribute != null) {
                return Attributes.unmodifiableAttribute(attribute);
            } else {
                return null;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Attribute getAttribute(final String attributeDescription) {
            final Attribute attribute = entry.getAttribute(attributeDescription);
            if (attribute != null) {
                return Attributes.unmodifiableAttribute(attribute);
            } else {
                return null;
            }
        }

        @Override
        public int getAttributeCount() {
            return entry.getAttributeCount();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DN getName() {
            return entry.getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return entry.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        public AttributeParser parseAttribute(AttributeDescription attributeDescription) {
            return entry.parseAttribute(attributeDescription);
        }

        /**
         * {@inheritDoc}
         */
        public AttributeParser parseAttribute(String attributeDescription) {
            return entry.parseAttribute(attributeDescription);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean removeAttribute(final Attribute attribute,
                final Collection<ByteString> missingValues) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAttribute(final AttributeDescription attributeDescription) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry removeAttribute(final String attributeDescription, final Object... values) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean replaceAttribute(final Attribute attribute) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry replaceAttribute(final String attributeDescription, final Object... values) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Entry setName(final DN dn) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Entry setName(final String dn) {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return entry.toString();
        }

    }

    private static final Function<Attribute, Attribute, Void> UNMODIFIABLE_ATTRIBUTE_FUNCTION =
            new Function<Attribute, Attribute, Void>() {

                @Override
                public Attribute apply(final Attribute value, final Void p) {
                    return Attributes.unmodifiableAttribute(value);
                }

            };

    private static final Comparator<Entry> COMPARATOR = new Comparator<Entry>() {
        public int compare(Entry o1, Entry o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    /**
     * Returns a {@code Comparator} which can be used to compare entries by name
     * using the natural order for DN comparisons (parent before children).
     * <p>
     * In order to sort entries in reverse order (children first) use the
     * following code:
     *
     * <pre>
     * Collections.reverseOrder(Entries.compareByName());
     * </pre>
     *
     * For more complex sort orders involving one or more attributes refer to
     * the {@link SortKey} class.
     *
     * @return The {@code Comparator}.
     */
    public static Comparator<Entry> compareByName() {
        return COMPARATOR;
    }

    /**
     * Returns {@code true} if the provided entry is valid according to the
     * specified schema and schema validation policy.
     * <p>
     * If attribute value validation is enabled then following checks will be
     * performed:
     * <ul>
     * <li>checking that there is at least one value
     * <li>checking that single-valued attributes contain only a single value
     * </ul>
     * In particular, attribute values will not be checked for conformance to
     * their syntax since this is expected to have already been performed while
     * adding the values to the entry.
     *
     * @param entry
     *            The entry to be validated.
     * @param schema
     *            The schema against which the entry will be validated.
     * @param policy
     *            The schema validation policy.
     * @param errorMessages
     *            A collection into which any schema validation warnings or
     *            error messages can be placed, or {@code null} if they should
     *            not be saved.
     * @return {@code true} if the provided entry is valid according to the
     *         specified schema and schema validation policy.
     * @see Schema#validateEntry(Entry, SchemaValidationPolicy, Collection)
     */
    public static boolean conformsToSchema(final Entry entry, final Schema schema,
            final SchemaValidationPolicy policy, final Collection<LocalizableMessage> errorMessages) {
        return schema.validateEntry(entry, policy, errorMessages);
    }

    /**
     * Returns {@code true} if the provided entry is valid according to the
     * default schema and schema validation policy.
     * <p>
     * If attribute value validation is enabled then following checks will be
     * performed:
     * <ul>
     * <li>checking that there is at least one value
     * <li>checking that single-valued attributes contain only a single value
     * </ul>
     * In particular, attribute values will not be checked for conformance to
     * their syntax since this is expected to have already been performed while
     * adding the values to the entry.
     *
     * @param entry
     *            The entry to be validated.
     * @param policy
     *            The schema validation policy.
     * @param errorMessages
     *            A collection into which any schema validation warnings or
     *            error messages can be placed, or {@code null} if they should
     *            not be saved.
     * @return {@code true} if the provided entry is valid according to the
     *         default schema and schema validation policy.
     * @see Schema#validateEntry(Entry, SchemaValidationPolicy, Collection)
     */
    public static boolean conformsToSchema(final Entry entry, final SchemaValidationPolicy policy,
            final Collection<LocalizableMessage> errorMessages) {
        return conformsToSchema(entry, Schema.getDefaultSchema(), policy, errorMessages);
    }

    /**
     * Creates a new modify request containing a list of modifications which can
     * be used to transform {@code fromEntry} into entry {@code toEntry}.
     * <p>
     * The modify request is reversible: it will contain only modifications of
     * type {@link ModificationType#ADD ADD} and {@link ModificationType#DELETE
     * DELETE}.
     * <p>
     * Finally, the modify request will use the distinguished name taken from
     * {@code fromEntry}. Moreover, this method will not check to see if both
     * {@code fromEntry} and {@code toEntry} have the same distinguished name.
     * <p>
     * This method is equivalent to:
     *
     * <pre>
     * ModifyRequest request = Requests.newModifyRequest(fromEntry, toEntry);
     * </pre>
     *
     * @param fromEntry
     *            The source entry.
     * @param toEntry
     *            The destination entry.
     * @return A modify request containing a list of modifications which can be
     *         used to transform {@code fromEntry} into entry {@code toEntry}.
     * @throws NullPointerException
     *             If {@code fromEntry} or {@code toEntry} were {@code null}.
     * @see Requests#newModifyRequest(Entry, Entry)
     */
    public static ModifyRequest diffEntries(final Entry fromEntry, final Entry toEntry) {
        Validator.ensureNotNull(fromEntry, toEntry);

        final ModifyRequest request = Requests.newModifyRequest(fromEntry.getName());

        TreeMapEntry tfrom;
        if (fromEntry instanceof TreeMapEntry) {
            tfrom = (TreeMapEntry) fromEntry;
        } else {
            tfrom = new TreeMapEntry(fromEntry);
        }

        TreeMapEntry tto;
        if (toEntry instanceof TreeMapEntry) {
            tto = (TreeMapEntry) toEntry;
        } else {
            tto = new TreeMapEntry(toEntry);
        }

        final Iterator<Attribute> ifrom = tfrom.getAllAttributes().iterator();
        final Iterator<Attribute> ito = tto.getAllAttributes().iterator();

        Attribute afrom = ifrom.hasNext() ? ifrom.next() : null;
        Attribute ato = ito.hasNext() ? ito.next() : null;

        while (afrom != null && ato != null) {
            final AttributeDescription adfrom = afrom.getAttributeDescription();
            final AttributeDescription adto = ato.getAttributeDescription();

            final int cmp = adfrom.compareTo(adto);
            if (cmp == 0) {
                // Attribute is in both entries. Compute the set of values to be
                // added
                // and removed. We won't replace the attribute because this is
                // not
                // reversible.
                final Attribute addedValues = new LinkedAttribute(ato);
                addedValues.removeAll(afrom);
                if (!addedValues.isEmpty()) {
                    request.addModification(new Modification(ModificationType.ADD, addedValues));
                }

                final Attribute deletedValues = new LinkedAttribute(afrom);
                deletedValues.removeAll(ato);
                if (!deletedValues.isEmpty()) {
                    request.addModification(new Modification(ModificationType.DELETE, deletedValues));
                }

                afrom = ifrom.hasNext() ? ifrom.next() : null;
                ato = ito.hasNext() ? ito.next() : null;
            } else if (cmp < 0) {
                // afrom in source, but not destination.
                request.addModification(new Modification(ModificationType.DELETE, afrom));
                afrom = ifrom.hasNext() ? ifrom.next() : null;
            } else {
                // ato in destination, but not in source.
                request.addModification(new Modification(ModificationType.ADD, ato));
                ato = ito.hasNext() ? ito.next() : null;
            }
        }

        // Additional attributes in source entry: these must be deleted.
        if (afrom != null) {
            request.addModification(new Modification(ModificationType.DELETE, afrom));
        }

        while (ifrom.hasNext()) {
            final Attribute a = ifrom.next();
            request.addModification(new Modification(ModificationType.DELETE, a));
        }

        // Additional attributes in destination entry: these must be added.
        if (ato != null) {
            request.addModification(new Modification(ModificationType.ADD, ato));
        }

        while (ito.hasNext()) {
            final Attribute a = ito.next();
            request.addModification(new Modification(ModificationType.ADD, a));
        }

        return request;
    }

    /**
     * Returns an unmodifiable set containing the object classes associated with
     * the provided entry. This method will ignore unrecognized object classes.
     * <p>
     * This method uses the default schema for decoding the object class
     * attribute values.
     *
     * @param entry
     *            The entry whose object classes are required.
     * @return An unmodifiable set containing the object classes associated with
     *         the provided entry.
     */
    public static Set<ObjectClass> getObjectClasses(final Entry entry) {
        return getObjectClasses(entry, Schema.getDefaultSchema());
    }

    /**
     * Returns an unmodifiable set containing the object classes associated with
     * the provided entry. This method will ignore unrecognized object classes.
     *
     * @param entry
     *            The entry whose object classes are required.
     * @param schema
     *            The schema which should be used for decoding the object class
     *            attribute values.
     * @return An unmodifiable set containing the object classes associated with
     *         the provided entry.
     */
    public static Set<ObjectClass> getObjectClasses(final Entry entry, final Schema schema) {
        final Attribute objectClassAttribute =
                entry.getAttribute(AttributeDescription.objectClass());
        if (objectClassAttribute == null) {
            return Collections.emptySet();
        } else {
            final Set<ObjectClass> objectClasses =
                    new HashSet<ObjectClass>(objectClassAttribute.size());
            for (final ByteString v : objectClassAttribute) {
                final String objectClassName = v.toString();
                final ObjectClass objectClass;
                try {
                    objectClass = schema.getObjectClass(objectClassName);
                    objectClasses.add(objectClass);
                } catch (final UnknownSchemaElementException e) {
                    // Ignore.
                    continue;
                }
            }
            return Collections.unmodifiableSet(objectClasses);
        }
    }

    /**
     * Returns the structural object class associated with the provided entry,
     * or {@code null} if none was found. If the entry contains multiple
     * structural object classes then the first will be returned. This method
     * will ignore unrecognized object classes.
     * <p>
     * This method uses the default schema for decoding the object class
     * attribute values.
     *
     * @param entry
     *            The entry whose structural object class is required.
     * @return The structural object class associated with the provided entry,
     *         or {@code null} if none was found.
     */
    public static ObjectClass getStructuralObjectClass(final Entry entry) {
        return getStructuralObjectClass(entry, Schema.getDefaultSchema());
    }

    /**
     * Returns the structural object class associated with the provided entry,
     * or {@code null} if none was found. If the entry contains multiple
     * structural object classes then the first will be returned. This method
     * will ignore unrecognized object classes.
     *
     * @param entry
     *            The entry whose structural object class is required.
     * @param schema
     *            The schema which should be used for decoding the object class
     *            attribute values.
     * @return The structural object class associated with the provided entry,
     *         or {@code null} if none was found.
     */
    public static ObjectClass getStructuralObjectClass(final Entry entry, final Schema schema) {
        ObjectClass structuralObjectClass = null;
        final Attribute objectClassAttribute = entry.getAttribute(objectClass());

        if (objectClassAttribute == null) {
            return null;
        }

        for (final ByteString v : objectClassAttribute) {
            final String objectClassName = v.toString();
            final ObjectClass objectClass;
            try {
                objectClass = schema.getObjectClass(objectClassName);
            } catch (final UnknownSchemaElementException e) {
                // Ignore.
                continue;
            }

            if (objectClass.getObjectClassType() == ObjectClassType.STRUCTURAL) {
                if (structuralObjectClass == null
                        || objectClass.isDescendantOf(structuralObjectClass)) {
                    structuralObjectClass = objectClass;
                }
            }
        }

        return structuralObjectClass;
    }

    /**
     * Returns a read-only view of {@code entry} and its attributes. Query
     * operations on the returned entry and its attributes "read-through" to the
     * underlying entry or attribute, and attempts to modify the returned entry
     * and its attributes either directly or indirectly via an iterator result
     * in an {@code UnsupportedOperationException}.
     *
     * @param entry
     *            The entry for which a read-only view is to be returned.
     * @return A read-only view of {@code entry}.
     * @throws NullPointerException
     *             If {@code entry} was {@code null}.
     */
    public static Entry unmodifiableEntry(final Entry entry) {
        if (entry instanceof UnmodifiableEntry) {
            return entry;
        } else {
            return new UnmodifiableEntry(entry);
        }
    }

    // Prevent instantiation.
    private Entries() {
        // Nothing to do.
    }
}
