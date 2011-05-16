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
 *      Copyright 2010 Sun Microsystems, Inc.
 */

package org.opends.sdk;



import java.util.LinkedHashMap;

import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.opends.sdk.requests.Requests;

import com.sun.opends.sdk.util.Validator;



/**
 * An implementation of the {@code Entry} interface which uses a
 * {@code LinkedHashMap} for storing attributes. Attributes are returned in the
 * same order that they were added to the entry. All operations are supported by
 * this implementation.
 * <p>
 * A {@code LinkedHashMapEntry} stores references to attributes which have been
 * added using the {@link #addAttribute} methods. Attributes sharing the same
 * attribute description are merged by adding the values of the new attribute to
 * the existing attribute. More specifically, the existing attribute must be
 * modifiable for the merge to succeed. Similarly, the {@link #removeAttribute}
 * methods remove the specified values from the existing attribute. The
 * {@link #replaceAttribute} methods remove the existing attribute (if present)
 * and store a reference to the new attribute - neither the new or existing
 * attribute need to be modifiable in this case.
 */
public final class LinkedHashMapEntry extends AbstractMapEntry
{
  /**
   * An entry factory which can be used to create new linked hash map entries.
   */
  public static final EntryFactory FACTORY = new EntryFactory()
  {
    public Entry newEntry(final DN name) throws NullPointerException
    {
      return new LinkedHashMapEntry(name);
    }
  };



  /**
   * Creates an entry having the same distinguished name, attributes, and object
   * classes of the provided entry. This constructor performs a deep copy of
   * {@code entry} and will copy each attribute as a {@link LinkedAttribute}.
   * <p>
   * A shallow copy constructor is provided by
   * {@link #LinkedHashMapEntry(Entry)}.
   *
   * @param entry
   *          The entry to be copied.
   * @return A deep copy of {@code entry}.
   * @throws NullPointerException
   *           If {@code entry} was {@code null}.
   * @see #LinkedHashMapEntry(Entry)
   */
  public static LinkedHashMapEntry deepCopyOfEntry(final Entry entry)
      throws NullPointerException
  {
    LinkedHashMapEntry copy = new LinkedHashMapEntry(entry.getName());
    for (final Attribute attribute : entry.getAllAttributes())
    {
      copy.addAttribute(new LinkedAttribute(attribute));
    }
    return copy;
  }



  /**
   * Creates an entry with an empty (root) distinguished name and no attributes.
   */
  public LinkedHashMapEntry()
  {
    this(DN.rootDN());
  }



  /**
   * Creates an empty entry using the provided distinguished name and no
   * attributes.
   *
   * @param name
   *          The distinguished name of this entry.
   * @throws NullPointerException
   *           If {@code name} was {@code null}.
   */
  public LinkedHashMapEntry(final DN name) throws NullPointerException
  {
    super(Validator.ensureNotNull(name),
        new LinkedHashMap<AttributeDescription, Attribute>());
  }



  /**
   * Creates an entry having the same distinguished name, attributes, and object
   * classes of the provided entry. This constructor performs a shallow copy of
   * {@code entry} and will not copy the attributes contained in {@code entry}.
   * <p>
   * A deep copy constructor is provided by {@link #deepCopyOfEntry(Entry)}
   *
   * @param entry
   *          The entry to be copied.
   * @throws NullPointerException
   *           If {@code entry} was {@code null}.
   * @see #deepCopyOfEntry(Entry)
   */
  public LinkedHashMapEntry(final Entry entry) throws NullPointerException
  {
    this(entry.getName());
    for (final Attribute attribute : entry.getAllAttributes())
    {
      addAttribute(attribute);
    }
  }



  /**
   * Creates an empty entry using the provided distinguished name decoded using
   * the default schema.
   *
   * @param name
   *          The distinguished name of this entry.
   * @throws LocalizedIllegalArgumentException
   *           If {@code name} could not be decoded using the default schema.
   * @throws NullPointerException
   *           If {@code name} was {@code null}.
   */
  public LinkedHashMapEntry(final String name)
      throws LocalizedIllegalArgumentException, NullPointerException
  {
    this(DN.valueOf(name));
  }



  /**
   * Creates a new entry using the provided lines of LDIF decoded using the
   * default schema.
   *
   * @param ldifLines
   *          Lines of LDIF containing the an LDIF add change record or an LDIF
   *          entry record.
   * @throws LocalizedIllegalArgumentException
   *           If {@code ldifLines} was empty, or contained invalid LDIF, or
   *           could not be decoded using the default schema.
   * @throws NullPointerException
   *           If {@code ldifLines} was {@code null} .
   */
  public LinkedHashMapEntry(final String... ldifLines)
      throws LocalizedIllegalArgumentException, NullPointerException
  {
    this(Requests.newAddRequest(ldifLines));
  }

}
