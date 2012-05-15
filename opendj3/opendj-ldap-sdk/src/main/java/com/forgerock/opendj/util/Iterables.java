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

import java.util.Iterator;

import org.forgerock.opendj.ldap.Function;

/**
 * Utility methods for manipulating {@link Iterable}s.
 */
public final class Iterables {
    private static final class ArrayIterable<M> implements Iterable<M> {

        private final M[] a;

        // Constructed via factory methods.
        private ArrayIterable(final M[] a) {
            this.a = a;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<M> iterator() {
            return Iterators.arrayIterator(a);
        }

    }

    private static final class EmptyIterable<M> implements Iterable<M> {

        /**
         * {@inheritDoc}
         */
        public Iterator<M> iterator() {
            return Iterators.emptyIterator();
        }

    }

    private static final class FilteredIterable<M, P> implements Iterable<M> {

        private final Iterable<M> iterable;
        private final P parameter;
        private final Predicate<? super M, P> predicate;

        // Constructed via factory methods.
        private FilteredIterable(final Iterable<M> iterable,
                final Predicate<? super M, P> predicate, final P p) {
            this.iterable = iterable;
            this.predicate = predicate;
            this.parameter = p;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<M> iterator() {
            return Iterators.filteredIterator(iterable.iterator(), predicate, parameter);
        }

    }

    private static final class SingletonIterable<M> implements Iterable<M> {

        private final M value;

        // Constructed via factory methods.
        private SingletonIterable(final M value) {
            this.value = value;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<M> iterator() {
            return Iterators.singletonIterator(value);
        }

    }

    private static final class TransformedIterable<M, N, P> implements Iterable<N> {

        private final Function<? super M, ? extends N, P> function;
        private final Iterable<M> iterable;
        private final P parameter;

        // Constructed via factory methods.
        private TransformedIterable(final Iterable<M> iterable,
                final Function<? super M, ? extends N, P> function, final P p) {
            this.iterable = iterable;
            this.function = function;
            this.parameter = p;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<N> iterator() {
            return Iterators.transformedIterator(iterable.iterator(), function, parameter);
        }

    }

    private static final class UnmodifiableIterable<M> implements Iterable<M> {

        private final Iterable<M> iterable;

        // Constructed via factory methods.
        private UnmodifiableIterable(final Iterable<M> iterable) {
            this.iterable = iterable;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<M> iterator() {
            return Iterators.unmodifiableIterator(iterable.iterator());
        }

    }

    private static final Iterable<Object> EMPTY_ITERABLE = new EmptyIterable<Object>();

    /**
     * Returns an iterable containing the elements of {@code a}. The returned
     * iterable's iterator does not support element removal via the
     * {@code remove()} method.
     *
     * @param <M>
     *            The type of elements contained in {@code a}.
     * @param a
     *            The array of elements.
     * @return An iterable containing the elements of {@code a}.
     */
    public static <M> Iterable<M> arrayIterable(final M[] a) {
        return new ArrayIterable<M>(a);
    }

    /**
     * Returns an immutable empty iterable.
     *
     * @param <M>
     *            The required type of the empty iterable.
     * @return An immutable empty iterable.
     */
    @SuppressWarnings("unchecked")
    public static <M> Iterable<M> emptyIterable() {
        return (Iterable<M>) EMPTY_ITERABLE;
    }

    /**
     * Returns a filtered view of {@code iterable} containing only those
     * elements which match {@code predicate}. The returned iterable's iterator
     * supports element removal via the {@code remove()} method subject to any
     * constraints imposed by {@code iterable}.
     *
     * @param <M>
     *            The type of elements contained in {@code iterable}.
     * @param <P>
     *            The type of the additional parameter to the predicate's
     *            {@code matches} method. Use {@link java.lang.Void} for
     *            predicates that do not need an additional parameter.
     * @param iterable
     *            The iterable to be filtered.
     * @param predicate
     *            The predicate.
     * @param p
     *            A predicate specified parameter.
     * @return A filtered view of {@code iterable} containing only those
     *         elements which match {@code predicate}.
     */
    public static <M, P> Iterable<M> filteredIterable(final Iterable<M> iterable,
            final Predicate<? super M, P> predicate, final P p) {
        return new FilteredIterable<M, P>(iterable, predicate, p);
    }

    /**
     * Returns a filtered view of {@code iterable} containing only those
     * elements which match {@code predicate}. The returned iterable's iterator
     * supports element removal via the {@code remove()} method subject to any
     * constraints imposed by {@code iterable}.
     *
     * @param <M>
     *            The type of elements contained in {@code iterable}.
     * @param iterable
     *            The iterable to be filtered.
     * @param predicate
     *            The predicate.
     * @return A filtered view of {@code iterable} containing only those
     *         elements which match {@code predicate}.
     */
    public static <M> Iterable<M> filteredIterable(final Iterable<M> iterable,
            final Predicate<? super M, Void> predicate) {
        return new FilteredIterable<M, Void>(iterable, predicate, null);
    }

    /**
     * Returns an iterable containing the single element {@code value}. The
     * returned iterable's iterator does not support element removal via the
     * {@code remove()} method.
     *
     * @param <M>
     *            The type of the single element {@code value}.
     * @param value
     *            The single element.
     * @return An iterable containing the single element {@code value}.
     */
    public static <M> Iterable<M> singletonIterable(final M value) {
        return new SingletonIterable<M>(value);
    }

    /**
     * Returns a view of {@code iterable} whose values have been mapped to
     * elements of type {@code N} using {@code function}. The returned
     * iterable's iterator supports element removal via the {@code remove()}
     * method subject to any constraints imposed by {@code iterable}.
     *
     * @param <M>
     *            The type of elements contained in {@code iterable}.
     * @param <N>
     *            The type of elements contained in the returned iterable.
     * @param <P>
     *            The type of the additional parameter to the function's
     *            {@code apply} method. Use {@link java.lang.Void} for functions
     *            that do not need an additional parameter.
     * @param iterable
     *            The iterable to be transformed.
     * @param function
     *            The function.
     * @param p
     *            A predicate specified parameter.
     * @return A view of {@code iterable} whose values have been mapped to
     *         elements of type {@code N} using {@code function}.
     */
    public static <M, N, P> Iterable<N> transformedIterable(final Iterable<M> iterable,
            final Function<? super M, ? extends N, P> function, final P p) {
        return new TransformedIterable<M, N, P>(iterable, function, p);
    }

    /**
     * Returns a view of {@code iterable} whose values have been mapped to
     * elements of type {@code N} using {@code function}. The returned
     * iterable's iterator supports element removal via the {@code remove()}
     * method subject to any constraints imposed by {@code iterable}.
     *
     * @param <M>
     *            The type of elements contained in {@code iterable}.
     * @param <N>
     *            The type of elements contained in the returned iterable.
     * @param iterable
     *            The iterable to be transformed.
     * @param function
     *            The function.
     * @return A view of {@code iterable} whose values have been mapped to
     *         elements of type {@code N} using {@code function}.
     */
    public static <M, N> Iterable<N> transformedIterable(final Iterable<M> iterable,
            final Function<? super M, ? extends N, Void> function) {
        return new TransformedIterable<M, N, Void>(iterable, function, null);
    }

    /**
     * Returns a read-only view of {@code iterable} whose iterator does not
     * support element removal via the {@code remove()}. Attempts to use the
     * {@code remove()} method will result in a
     * {@code UnsupportedOperationException}.
     *
     * @param <M>
     *            The type of elements contained in {@code iterable}.
     * @param iterable
     *            The iterable to be made read-only.
     * @return A read-only view of {@code iterable} whose iterator does not
     *         support element removal via the {@code remove()}.
     */
    public static <M> Iterable<M> unmodifiableIterable(final Iterable<M> iterable) {
        return new UnmodifiableIterable<M>(iterable);
    }

    // Prevent instantiation
    private Iterables() {
        // Do nothing.
    }

}
