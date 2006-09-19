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
 * by brackets "[]" replaced with your own identifying * information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Portions Copyright 2006 Sun Microsystems, Inc.
 */
package org.opends.server.protocols.ldap ;

import org.opends.server.DirectoryServerTestCase;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * An abstract class that all types  unit test should extend.
 */

@Test(groups = { "precommit", "ldap" })
public abstract class LdapTestCase extends DirectoryServerTestCase
{
  /**
   * Determine whether one LDAPAttribute is equal to another.
   * The values of the attribute must be identical and in the same order.
   * @param a1 The first LDAPAttribute.
   * @param a2 The second LDAPAttribute.
   * @return true if the first LDAPAttribute is equal to the second.
   */
  static boolean testEqual(LDAPAttribute a1, LDAPAttribute a2)
  {
    if (!a1.getAttributeType().equals(a2.getAttributeType()))
    {
      return false;
    }
    return a1.getValues().equals(a2.getValues());
  }

  /**
   * Determine whether one list of LDAPAttribute is equal to another.
   * @param list1 The first list of LDAPAttribute.
   * @param list2 The second list of LDAPAttribute.
   * @return true if the first list of LDAPAttribute is equal to the second.
   */
  static boolean testEqual(LinkedList<LDAPAttribute> list1,
                           LinkedList<LDAPAttribute> list2)
  {
    ListIterator<LDAPAttribute> e1 = list1.listIterator();
    ListIterator<LDAPAttribute> e2 = list2.listIterator();
    while(e1.hasNext() && e2.hasNext()) {
      LDAPAttribute o1 = e1.next();
      LDAPAttribute o2 = e2.next();
      if (!(o1==null ? o2==null : testEqual(o1, o2)))
        return false;
    }
    return !(e1.hasNext() || e2.hasNext());
  }



}
