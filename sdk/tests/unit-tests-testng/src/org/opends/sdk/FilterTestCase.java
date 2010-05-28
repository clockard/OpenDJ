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



import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;



public class FilterTestCase extends SdkTestCase
{
  @DataProvider(name = "badfilterstrings")
  public Object[][] getBadFilterStrings() throws Exception
  {
    return new Object[][] { { null, null }, { "", null }, { "=", null },
        { "()", null }, { "(&(objectClass=*)(sn=s*s)", null },
        { "(dob>12221)", null }, { "(cn=bob\\2 doe)", null },
        { "(cn=\\4j\\w2\\yu)", null }, { "(cn=ds\\2)", null },
        { "(&(givenname=bob)|(sn=pep)dob=12))", null }, { "(:=bob)", null },
        { "(=sally)", null }, { "(cn=billy bob", null },
        { "(|(!(title=sweep*)(l=Paris*)))", null }, { "(|(!))", null },
        { "((uid=user.0))", null }, { "(&&(uid=user.0))", null },
        { "!uid=user.0", null }, { "(:dn:=Sally)", null }, };
  }



  @DataProvider(name = "filterstrings")
  public Object[][] getFilterStrings() throws Exception
  {
    final Filter equal = Filter.newEqualityMatchFilter("objectClass",
        ByteString.valueOf("\\test*(Value)"));
    final Filter equal2 = Filter.newEqualityMatchFilter("objectClass",
        ByteString.valueOf(""));
    final Filter approx = Filter.newApproxMatchFilter("sn", ByteString
        .valueOf("\\test*(Value)"));
    final Filter greater = Filter.newGreaterOrEqualFilter("employeeNumber",
        ByteString.valueOf("\\test*(Value)"));
    final Filter less = Filter.newLessOrEqualFilter("dob", ByteString
        .valueOf("\\test*(Value)"));
    final Filter presense = Filter.newPresentFilter("login");

    final ArrayList<ByteString> any = new ArrayList<ByteString>(0);
    final ArrayList<ByteString> multiAny = new ArrayList<ByteString>(1);
    multiAny.add(ByteString.valueOf("\\wid*(get)"));
    multiAny.add(ByteString.valueOf("*"));

    final Filter substring1 = Filter.newSubstringsFilter("givenName",
        ByteString.valueOf("\\Jo*()"), any, ByteString.valueOf("\\n*()"));
    final Filter substring2 = Filter.newSubstringsFilter("givenName",
        ByteString.valueOf("\\Jo*()"), multiAny, ByteString.valueOf("\\n*()"));
    final Filter substring3 = Filter.newSubstringsFilter("givenName",
        ByteString.valueOf(""), any, ByteString.valueOf("\\n*()"));
    final Filter substring4 = Filter.newSubstringsFilter("givenName",
        ByteString.valueOf("\\Jo*()"), any, ByteString.valueOf(""));
    final Filter substring5 = Filter.newSubstringsFilter("givenName",
        ByteString.valueOf(""), multiAny, ByteString.valueOf(""));
    final Filter extensible1 = Filter.newExtensibleMatchFilter("2.4.6.8.19",
        "cn", ByteString.valueOf("\\John* (Doe)"), false);
    final Filter extensible2 = Filter.newExtensibleMatchFilter("2.4.6.8.19",
        "cn", ByteString.valueOf("\\John* (Doe)"), true);
    final Filter extensible3 = Filter.newExtensibleMatchFilter("2.4.6.8.19",
        null, ByteString.valueOf("\\John* (Doe)"), true);
    final Filter extensible4 = Filter.newExtensibleMatchFilter(null, "cn",
        ByteString.valueOf("\\John* (Doe)"), true);
    final Filter extensible5 = Filter.newExtensibleMatchFilter("2.4.6.8.19",
        null, ByteString.valueOf("\\John* (Doe)"), false);

    final ArrayList<Filter> list1 = new ArrayList<Filter>();
    list1.add(equal);
    list1.add(approx);

    final Filter and = Filter.newAndFilter(list1);

    final ArrayList<Filter> list2 = new ArrayList<Filter>();
    list2.add(substring1);
    list2.add(extensible1);
    list2.add(and);

    return new Object[][] {
        { "(objectClass=\\5ctest\\2a\\28Value\\29)", equal },

        { "(objectClass=)", equal2 },

        { "(sn~=\\5ctest\\2a\\28Value\\29)", approx },

        { "(employeeNumber>=\\5ctest\\2a\\28Value\\29)", greater },

        { "(dob<=\\5ctest\\2a\\28Value\\29)", less },

        { "(login=*)", presense },

        { "(givenName=\\5cJo\\2a\\28\\29*\\5cn\\2a\\28\\29)", substring1 },

        {
            "(givenName=\\5cJo\\2a\\28\\29*\\5cwid\\2a\\28get\\29*\\2a*\\5cn\\2a\\28\\29)",
            substring2 },

        { "(givenName=*\\5cn\\2a\\28\\29)", substring3 },

        { "(givenName=\\5cJo\\2a\\28\\29*)", substring4 },

        { "(givenName=*\\5cwid\\2a\\28get\\29*\\2a*)", substring5 },

        { "(cn:2.4.6.8.19:=\\5cJohn\\2a \\28Doe\\29)", extensible1 },

        { "(cn:dn:2.4.6.8.19:=\\5cJohn\\2a \\28Doe\\29)", extensible2 },

        { "(:dn:2.4.6.8.19:=\\5cJohn\\2a \\28Doe\\29)", extensible3 },

        { "(cn:dn:=\\5cJohn\\2a \\28Doe\\29)", extensible4 },

        { "(:2.4.6.8.19:=\\5cJohn\\2a \\28Doe\\29)", extensible5 },

        {
            "(&(objectClass=\\5ctest\\2a\\28Value\\29)(sn~=\\5ctest\\2a\\28Value\\29))",
            Filter.newAndFilter(list1) },

        {
            "(|(objectClass=\\5ctest\\2a\\28Value\\29)(sn~=\\5ctest\\2a\\28Value\\29))",
            Filter.newOrFilter(list1) },

        { "(!(objectClass=\\5ctest\\2a\\28Value\\29))",
            Filter.newNotFilter(equal) },

        {
            "(|(givenName=\\5cJo\\2a\\28\\29*\\5cn\\2a\\28\\29)(cn:2.4.6.8.19:=\\5cJohn\\2a \\28Doe\\29)"
                + "(&(objectClass=\\5ctest\\2a\\28Value\\29)(sn~=\\5ctest\\2a\\28Value\\29)))",
            Filter.newOrFilter(list2) }

    };
  }



  /**
   * Decodes the specified filter strings.
   *
   * @param filterStr
   * @param filter
   * @throws Exception
   */
  @Test(dataProvider = "filterstrings")
  public void testDecode(final String filterStr, final Filter filter)
      throws Exception
  {
    final Filter decoded = Filter.valueOf(filterStr);
    assertEquals(decoded.toString(), filter.toString());
  }



  /**
   * Decodes the erroneous filter strings.
   *
   * @param filterStr
   * @param filter
   * @throws Exception
   */
  @Test(dataProvider = "badfilterstrings", expectedExceptions = {
      LocalizedIllegalArgumentException.class, NullPointerException.class })
  public void testDecodeException(final String filterStr, final Filter filter)
      throws Exception
  {
    Filter.valueOf(filterStr);
  }



  /**
   * Tests the matcher.
   *
   * @throws Exception
   */
  @Test
  public void testMatcher() throws Exception
  {
    final Filter equal = Filter.newEqualityMatchFilter("cn", ByteString
        .valueOf("\\test*(Value)"));
    final LinkedHashMapEntry entry = new LinkedHashMapEntry(DN
        .valueOf("cn=\\test*(Value),dc=org"));
    entry.addAttribute("cn", "\\test*(Value)");
    entry.addAttribute("objectclass", "top,person");
    final Matcher matcher = equal.matcher();
    assertTrue(matcher.matches(entry).toBoolean());
  }
}
