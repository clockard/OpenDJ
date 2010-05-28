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

package com.sun.opends.sdk.util;



import java.util.Iterator;
import java.util.NoSuchElementException;



/**
 * Utility methods for manipulating {@link Iterator}s.
 */
public final class Iterators
{
  private static final class ArrayIterator<M> implements Iterator<M>
  {
    private int i = 0;
    private final M[] a;



    // Constructed via factory methods.
    private ArrayIterator(final M[] a)
    {
      this.a = a;
    }



    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
    {
      return i < a.length;
    }



    /**
     * {@inheritDoc}
     */
    public M next()
    {
      if (hasNext())
      {
        return a[i++];
      }
      else
      {
        throw new NoSuchElementException();
      }
    }



    /**
     * {@inheritDoc}
     */
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

  };



  private static final class EmptyIterator<M> implements Iterator<M>
  {
    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
    {
      return false;
    }



    /**
     * {@inheritDoc}
     */
    public M next()
    {
      throw new NoSuchElementException();
    }



    /**
     * {@inheritDoc}
     */
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }



  private static final class FilteredIterator<M, P> implements Iterator<M>
  {

    private boolean hasNextMustIterate = true;
    private final Iterator<M> iterator;
    private M next = null;

    private final P parameter;
    private final Predicate<? super M, P> predicate;



    // Constructed via factory methods.
    private FilteredIterator(final Iterator<M> iterator,
        final Predicate<? super M, P> predicate, final P p)
    {
      this.iterator = iterator;
      this.predicate = predicate;
      this.parameter = p;
    }



    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
    {
      if (hasNextMustIterate)
      {
        hasNextMustIterate = false;
        while (iterator.hasNext())
        {
          next = iterator.next();
          if (predicate.matches(next, parameter))
          {
            return true;
          }
        }
        next = null;
        return false;
      }
      else
      {
        return next != null;
      }
    }



    /**
     * {@inheritDoc}
     */
    public M next()
    {
      if (!hasNext())
      {
        throw new NoSuchElementException();
      }
      hasNextMustIterate = true;
      return next;
    }



    /**
     * {@inheritDoc}
     */
    public void remove()
    {
      iterator.remove();
    }

  }



  private static final class SingletonIterator<M> implements Iterator<M>
  {
    private M value;



    // Constructed via factory methods.
    private SingletonIterator(final M value)
    {
      this.value = value;
    }



    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
    {
      return value != null;
    }



    /**
     * {@inheritDoc}
     */
    public M next()
    {
      if (value != null)
      {
        final M tmp = value;
        value = null;
        return tmp;
      }
      else
      {
        throw new NoSuchElementException();
      }
    }



    /**
     * {@inheritDoc}
     */
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

  }



  private static final class TransformedIterator<M, N, P> implements
      Iterator<N>
  {

    private final Function<? super M, ? extends N, P> function;
    private final Iterator<M> iterator;
    private final P parameter;



    // Constructed via factory methods.
    private TransformedIterator(final Iterator<M> iterator,
        final Function<? super M, ? extends N, P> function, final P p)
    {
      this.iterator = iterator;
      this.function = function;
      this.parameter = p;
    }



    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
    {
      return iterator.hasNext();
    }



    /**
     * {@inheritDoc}
     */
    public N next()
    {
      return function.apply(iterator.next(), parameter);
    }



    /**
     * {@inheritDoc}
     */
    public void remove()
    {
      iterator.remove();
    }

  }



  private static final class UnmodifiableIterator<M> implements Iterator<M>
  {
    private final Iterator<M> iterator;



    private UnmodifiableIterator(final Iterator<M> iterator)
    {
      this.iterator = iterator;
    }



    /**
     * {@inheritDoc}
     */
    public boolean hasNext()
    {
      return iterator.hasNext();
    }



    /**
     * {@inheritDoc}
     */
    public M next()
    {
      return iterator.next();
    }



    /**
     * {@inheritDoc}
     */
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }



  private static final Iterator<Object> EMPTY_ITERATOR = new EmptyIterator<Object>();



  /**
   * Returns an iterator over the elements contained in {@code a}. The returned
   * iterator does not support element removal via the {@code remove()} method.
   *
   * @param <M>
   *          The type of elements contained in {@code a}.
   * @param a
   *          The array of elements to be returned by the iterator.
   * @return An iterator over the elements contained in {@code a}.
   */
  public static <M> Iterator<M> arrayIterator(final M[] a)
  {
    return new ArrayIterator<M>(a);
  }



  /**
   * Returns an immutable empty iterator.
   *
   * @param <M>
   *          The required type of the empty iterator.
   * @return An immutable empty iterator.
   */
  @SuppressWarnings("unchecked")
  public static <M> Iterator<M> empty()
  {
    return (Iterator<M>) EMPTY_ITERATOR;
  }



  /**
   * Returns a filtered view of {@code iterator} containing only those elements
   * which match {@code predicate}. The returned iterator supports element
   * removal via the {@code remove()} method subject to any constraints imposed
   * by {@code iterator}.
   *
   * @param <M>
   *          The type of elements contained in {@code iterator}.
   * @param <P>
   *          The type of the additional parameter to the predicate's {@code
   *          matches} method. Use {@link java.lang.Void} for predicates that do
   *          not need an additional parameter.
   * @param iterator
   *          The iterator to be filtered.
   * @param predicate
   *          The predicate.
   * @param p
   *          A predicate specified parameter.
   * @return A filtered view of {@code iterator} containing only those elements
   *         which match {@code predicate}.
   */
  public static <M, P> Iterator<M> filter(final Iterator<M> iterator,
      final Predicate<? super M, P> predicate, final P p)
  {
    return new FilteredIterator<M, P>(iterator, predicate, p);
  }



  /**
   * Returns a filtered view of {@code iterator} containing only those elements
   * which match {@code predicate}. The returned iterator supports element
   * removal via the {@code remove()} method subject to any constraints imposed
   * by {@code iterator}.
   *
   * @param <M>
   *          The type of elements contained in {@code iterator}.
   * @param iterator
   *          The iterator to be filtered.
   * @param predicate
   *          The predicate.
   * @return A filtered view of {@code iterator} containing only those elements
   *         which match {@code predicate}.
   */
  public static <M> Iterator<M> filter(final Iterator<M> iterator,
      final Predicate<? super M, Void> predicate)
  {
    return new FilteredIterator<M, Void>(iterator, predicate, null);
  }



  /**
   * Returns an iterator containing the single element {@code value}. The
   * returned iterator does not support element removal via the {@code remove()}
   * method.
   *
   * @param <M>
   *          The type of the single element {@code value}.
   * @param value
   *          The single element to be returned by the iterator.
   * @return An iterator containing the single element {@code value}.
   */
  public static <M> Iterator<M> singleton(final M value)
  {
    return new SingletonIterator<M>(value);
  }



  /**
   * Returns a view of {@code iterator} whose values have been mapped to
   * elements of type {@code N} using {@code function}. The returned iterator
   * supports element removal via the {@code remove()} method subject to any
   * constraints imposed by {@code iterator}.
   *
   * @param <M>
   *          The type of elements contained in {@code iterator}.
   * @param <N>
   *          The type of elements contained in the returned iterator.
   * @param <P>
   *          The type of the additional parameter to the function's {@code
   *          apply} method. Use {@link java.lang.Void} for functions that do
   *          not need an additional parameter.
   * @param iterator
   *          The iterator to be transformed.
   * @param function
   *          The function.
   * @param p
   *          A predicate specified parameter.
   * @return A view of {@code iterator} whose values have been mapped to
   *         elements of type {@code N} using {@code function}.
   */
  public static <M, N, P> Iterator<N> transform(final Iterator<M> iterator,
      final Function<? super M, ? extends N, P> function, final P p)
  {
    return new TransformedIterator<M, N, P>(iterator, function, p);
  }



  /**
   * Returns a view of {@code iterator} whose values have been mapped to
   * elements of type {@code N} using {@code function}. The returned iterator
   * supports element removal via the {@code remove()} method subject to any
   * constraints imposed by {@code iterator}.
   *
   * @param <M>
   *          The type of elements contained in {@code iterator}.
   * @param <N>
   *          The type of elements contained in the returned iterator.
   * @param iterator
   *          The iterator to be transformed.
   * @param function
   *          The function.
   * @return A view of {@code iterator} whose values have been mapped to
   *         elements of type {@code N} using {@code function}.
   */
  public static <M, N> Iterator<N> transform(final Iterator<M> iterator,
      final Function<? super M, ? extends N, Void> function)
  {
    return new TransformedIterator<M, N, Void>(iterator, function, null);
  }



  /**
   * Returns a read-only view of {@code iterator} which does not support element
   * removal via the {@code remove()}. Attempts to use the {@code remove()}
   * method will result in a {@code UnsupportedOperationException}.
   *
   * @param <M>
   *          The type of elements contained in {@code iterator}.
   * @param iterator
   *          The iterator to be made read-only.
   * @return A read-only view of {@code iterator} which does not support element
   *         removal via the {@code remove()}.
   */
  public static <M> Iterator<M> unmodifiable(final Iterator<M> iterator)
  {
    return new UnmodifiableIterator<M>(iterator);
  }



  // Prevent instantiation
  private Iterators()
  {
    // Do nothing.
  }

}
