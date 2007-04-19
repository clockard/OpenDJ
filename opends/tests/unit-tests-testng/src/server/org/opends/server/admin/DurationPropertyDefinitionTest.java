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
 *      Portions Copyright 2007 Sun Microsystems, Inc.
 */

package org.opends.server.admin;

import static org.testng.Assert.*;

import org.opends.server.admin.std.meta.RootCfgDefn;
import org.testng.annotations.*;

import java.util.EnumSet;

/**
 * DurationPropertyDefinition Tester.
 */
public class DurationPropertyDefinitionTest {

  /**
   * Tests creation of builder succeeds
   */
  @Test
  public void testCreateBuilder() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    assertNotNull(builder);
  }

  /**
   * Tests setting/getting of lower limit as long
   */
  @Test
  public void testLowerLimit1() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setLowerLimit((long) 1);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    assert spd.getLowerLimit() == 1;
  }

  /**
   * Creates data for testing string-based limit values
   * @return data
   */
  @DataProvider(name = "longLimitData")
  public Object[][] createlongLimitData() {
    return new Object[][]{
            {1L, 1L},
            // { null, 0 }
    };
  }

  /**
   * Creates data for testing limit values
   * @return data
   */
  @DataProvider(name = "illegalLimitData")
  public Object[][] createIllegalLimitData() {
    return new Object[][]{
            {-1L, 0L, true}, // lower, upper, lower first
            {0L, -1L, false},
            {2L, 1L, true},
            {2L, 1L, false}
    };
  }


  /**
   * Tests setting/getting of lower limit as String
   * @param limit unit limit
   * @param expectedValue to compare
   */
  @Test(dataProvider = "longLimitData")
  public void testLowerLimit2(long limit, Long expectedValue) {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setLowerLimit(limit);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    assert spd.getLowerLimit() == expectedValue;
  }

  /**
   * Tests setting/getting of lower limit as long
   */
  @Test
  public void testUpperLimit1() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setLowerLimit((long) 1);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    assert spd.getLowerLimit() == 1;
  }

  /**
   * Tests setting/getting of lower limit as String
   * @param limit upper limit
   * @param expectedValue to compare
   */
  @Test(dataProvider = "longLimitData")
  public void testUpperLimit2(long limit, long expectedValue) {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setUpperLimit(limit);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    assert spd.getUpperLimit().equals(expectedValue);
  }

  /**
   * Tests setting/getting of lower limit as String
   * @param upper upper limit
   * @param lower lower limit
   * @param lowerFirst when true sets the lower limit property first
   */
  @Test(dataProvider = "illegalLimitData", expectedExceptions = IllegalArgumentException.class)
  public void testIllegalLimits(long lower, long upper, boolean lowerFirst) {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    if (lowerFirst) {
      builder.setLowerLimit(lower);
      builder.setUpperLimit(upper);
    } else {
      builder.setUpperLimit(upper);
      builder.setLowerLimit(lower);
    }
  }

  /**
   * Tests the allowUnlimited property
   */
  @Test
  public void testIsAllowUnlimited1() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(true);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.decodeValue("unlimited");
  }

  /**
   * Tests the allowUnlimited property
   */
  @Test(expectedExceptions = IllegalPropertyValueStringException.class)
  public void testIsAllowUnlimited2() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(false);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.decodeValue("unlimited");
  }

  /**
   * Tests the allowUnlimited property
   */
  @Test(expectedExceptions = IllegalPropertyValueException.class)
  public void testIsAllowUnlimited3() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(false);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.validateValue(-1L);
  }

  /**
   * Creates illegal data for validate value
   * @return data
   */
  @DataProvider(name = "validateValueData")
  public Object[][] createvalidateValueData() {
    return new Object[][]{
            {5L, 10L, false, 7L},
            {5L, null, true, -1L},
            {5L, 10L, false, 5L},
            {5L, 10L, false, 10L},
            {5L, null, false, 10000L}
    };
  }

  /**
   * Tests that validateValue works
   * @param allowUnlimited when true allows unlimited
   * @param high upper limit
   * @param low lower limit
   * @param value to validate
   */
  @Test(dataProvider = "validateValueData")
  public void testValidateValue1(Long low, Long high, boolean allowUnlimited, Long value) {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setLowerLimit(low);
    builder.setUpperLimit(high);
    builder.setAllowUnlimited(allowUnlimited);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.validateValue(value);
  }

  /**
   * Creates illegal data for validate value
   * @return data
   */
  @DataProvider(name = "illegalValidateValueData")
  public Object[][] createIllegalValidateValueData() {
    return new Object[][]{
            {5L, 10L, false, null},
            {5L, 10L, false, 1L},
            {5L, 10L, false, 11L},
            {5L, 10L, false, -1L}
    };
  }

  /**
   * Tests that validateValue throws exceptions
   * @param low lower limit
   * @param high upper limit
   * @param allowUnlimited when true allows unlimited
   * @param value to validate
   */
  @Test(dataProvider = "illegalValidateValueData",
          expectedExceptions = {AssertionError.class,NullPointerException.class,IllegalPropertyValueException.class})
  public void testValidateValue2(Long low, Long high, boolean allowUnlimited, Long value) {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setLowerLimit(low);
    builder.setUpperLimit(high);
    builder.setAllowUnlimited(allowUnlimited);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.validateValue(value);
  }

  /**
   * Creates encode test data
   * @return data
   */
  @DataProvider(name = "encodeValueData")
  public Object[][] createEncodeValueData() {
    return new Object[][]{
            {-1L, "unlimited"},
            {0L, "0s"},
            {1L, "1s"},
            {2L, "2s"},
            {999L, "999s"},
            {1000L, "1000s"},
            {1001L, "1001s"},
            {1023L, "1023s"},
            {1024L, "1024s"},
            {1025L, "1025s"},
            {1000L * 1000L, "1000000s"},
    };
  }

  /**
   * Tests encode value
   * @param value to encode
   * @param expectedValue to compare
   */
  @Test(dataProvider = "encodeValueData")
  public void testEncodeValue(Long value, String expectedValue) {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(true);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    assertEquals(spd.encodeValue(value), expectedValue);
  }

  /**
   * Test that accept doesn't throw and exception
   */
  @Test
  public void testAccept() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(true);
    DurationPropertyDefinition spd = buildTestDefinition(builder);

    PropertyDefinitionVisitor<Boolean, Void> v = new AbstractPropertyDefinitionVisitor<Boolean, Void>() {

      public Boolean visitDuration(DurationPropertyDefinition d,
          Void o) {
        return true;
      }

      public Boolean visitUnknown(PropertyDefinition d, Void o)
          throws UnknownPropertyDefinitionException {
        return false;
      }

    };

    assertEquals((boolean) spd.accept(v, null), true);
  }

  /**
   * Make sure toString doesn't barf
   */
  @Test
  public void testToString() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(true);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.toString();
  }

  /**
   * Make sure toString doesn't barf
   */
  @Test
  public void testToString2() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setUpperLimit(10L);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.toString();
  }

  /**
   * Test value comparisons.
   */
  @Test
  public void testCompare() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(true);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.compare(1L, 2L);
  }

  /**
   * Test setting a default behavior provider.
   */
  @Test
  public void testSetDefaultBehaviorProvider() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(true);
    builder.setDefaultBehaviorProvider(new DefaultBehaviorProvider<Long>() {
      public <R, P> R accept(DefaultBehaviorProviderVisitor<Long, R, P> v, P p) {
        return null;
      }
    });
  }

  /**
   * Test setting a property option.
   */
  @Test
  public void testSetOption() {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setOption(PropertyOption.HIDDEN);
  }

  /**
   * Creates encode test data
   * @return data
   */
  @DataProvider(name = "decodeValueData")
  public Object[][] createDecodeValueData() {
    return new Object[][]{
            // syntax tests
            {"unlimited", -1L},
            {"0h", 0L},
            {"0.h", 0L},
            {"0.0h", 0L},
            {"0.00h", 0L},
            {"0 h", 0L},
            {"0. h", 0L},
            {"0.00 h", 0L},
            {"1h", 1L},
            {"1.h", 1L},
            {"1.1h", 1L},
            {"1 h", 1L},
            {"1. h", 1L},
            {"1.1 h", 1L},

            // conversion tests
            {"1 d", 24L},
            {"2 d", 48L},
            {"0.5 d", 12L}
    };
  }

  /**
   * Tests decodeValue()
   * @param value to decode
   * @param expectedValue for comparison
   */
  @Test(dataProvider = "decodeValueData")
  public void testDecodeValue(String value, Long expectedValue) {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(true);
    builder.setBaseUnit(DurationUnit.HOURS);
    builder.setMaximumUnit(DurationUnit.DAYS);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
//    if (spd.decodeValue(value) != expectedValue) {
//      System.out.println(spd.decodeValue(value) + "!=" + expectedValue);
//    }
    assert(spd.decodeValue(value) == expectedValue);
  }

  /**
   * Creates encode test data
   * @return data
   */
  @DataProvider(name = "decodeValueData2")
  public Object[][] createDecodeValueData2() {
    return new Object[][]{
            {"a s"},
            {"1 x"},
            {"30 m"}, // unit too small violation
            {"60 m"}, // unit too small violation
            {"1 w"},  // unit too big violation
            {"7 w"},  // unit too big violation
            {"1 x"},
            {"1 d"}, // upper limit violation
            {"2 h"}, // lower limit violation
            {"-1 h"} // unlimited violation
    };
  }

  /**
   * Tests decodeValue()
   * @param value to decode
   */
  @Test(dataProvider = "decodeValueData2",
          expectedExceptions = {IllegalPropertyValueStringException.class})
  public void testDecodeValue(String value) {
    DurationPropertyDefinition.Builder builder = createTestBuilder();
    builder.setAllowUnlimited(false);
    builder.setBaseUnit(DurationUnit.HOURS);
    builder.setMaximumUnit(DurationUnit.DAYS);
    builder.setLowerLimit(5L);
    builder.setUpperLimit(10L);
    DurationPropertyDefinition spd = buildTestDefinition(builder);
    spd.decodeValue(value);
  }

  private DurationPropertyDefinition.Builder createTestBuilder() {
    return DurationPropertyDefinition.createBuilder(
        RootCfgDefn.getInstance(), "test-property-name");
  }

  private DurationPropertyDefinition buildTestDefinition(DurationPropertyDefinition.Builder builder) {
    return builder.buildInstance(RootCfgDefn.getInstance(), "test-prop",
            EnumSet.noneOf(PropertyOption.class),
            new DefinedDefaultBehaviorProvider<Long>("0"));
  }

}
