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
 * BooleanPropertyDefinition Tester.
 */
public class BooleanPropertyDefinitionTest {

  BooleanPropertyDefinition.Builder builder = null;

  /**
   * Sets up tests
   */
  @BeforeClass
  public void setUp() {
    builder = BooleanPropertyDefinition.createBuilder(
        RootCfgDefn.getInstance(), "test-property");
  }

  /**
   * Tests validateValue() with valid data
   */
  @Test
  public void testValidateValue1() {
    BooleanPropertyDefinition d = createPropertyDefinition();
    d.validateValue(Boolean.TRUE);
  }

  /**
   * Tests validateValue() with illegal data
   */
  @Test(expectedExceptions = AssertionError.class)
  public void testValidateValue2() {
    BooleanPropertyDefinition d = createPropertyDefinition();
    d.validateValue(null);
  }

  /**
   * @return data for testing
   */
  @DataProvider(name = "testDecodeValueData")
  public Object[][] createvalidateValueData() {
    return new Object[][]{
            {"0", Boolean.FALSE},
            {"no", Boolean.FALSE},
            {"off", Boolean.FALSE},
            {"false", Boolean.FALSE},
            {"disable", Boolean.FALSE},
            {"disabled", Boolean.FALSE},
            {"1", Boolean.TRUE},
            {"yes", Boolean.TRUE},
            {"on", Boolean.TRUE},
            {"true", Boolean.TRUE},
            {"enable", Boolean.TRUE},
            {"enabled", Boolean.TRUE},
    };
  }

  /**
   * Tests decodeValue()
   * @param value to decode
   * @param expected value
   */
  @Test(dataProvider = "testDecodeValueData")
  public void testDecodeValue(String value, Boolean expected) {
    BooleanPropertyDefinition d = createPropertyDefinition();
    assertEquals(d.decodeValue(value), expected);
  }

  /**
   * @return data for testing illegal values
   */
  @DataProvider(name = "testDecodeValueData2")
  public Object[][] createvalidateValueData2() {
    return new Object[][]{
            {null},{"abc"}
    };
  }

  /**
   * Tests decodeValue() with illegal data
   * @param value to decode
   */
  @Test(dataProvider = "testDecodeValueData2",
          expectedExceptions = {AssertionError.class,IllegalPropertyValueStringException.class})
  public void testDecodeValue2(String value) {
    BooleanPropertyDefinition d = createPropertyDefinition();
    d.decodeValue(value);
  }

  private BooleanPropertyDefinition createPropertyDefinition() {
    return builder.buildInstance(RootCfgDefn.getInstance(), "test-property",
            EnumSet.noneOf(PropertyOption.class),
            new UndefinedDefaultBehaviorProvider<Boolean>());
  }

}
