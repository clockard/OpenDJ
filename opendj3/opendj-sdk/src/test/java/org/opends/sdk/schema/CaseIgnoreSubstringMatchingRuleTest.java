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
package org.opends.sdk.schema;



import static org.opends.sdk.schema.SchemaConstants.SMR_CASE_IGNORE_OID;

import org.opends.sdk.ConditionResult;
import org.testng.annotations.DataProvider;



/**
 * Test the CaseIgnoreSubstringMatchingRule.
 */
public class CaseIgnoreSubstringMatchingRuleTest extends
    SubstringMatchingRuleTest
{

  @DataProvider(name = "substringInvalidAssertionValues")
  public Object[][] createMatchingRuleInvalidAssertionValues()
  {
    return new Object[][] {};
  }



  @DataProvider(name = "substringInvalidAttributeValues")
  public Object[][] createMatchingRuleInvalidAttributeValues()
  {
    return new Object[][] {};
  }



  /**
   * {@inheritDoc}
   */
  @Override
  @DataProvider(name = "substringFinalMatchData")
  public Object[][] createSubstringFinalMatchData()
  {
    return new Object[][] {
        { "this is a value", "value", ConditionResult.TRUE },
        { "this is a value", "alue", ConditionResult.TRUE },
        { "this is a value", "ue", ConditionResult.TRUE },
        { "this is a value", "e", ConditionResult.TRUE },
        { "this is a value", "valu", ConditionResult.FALSE },
        { "this is a value", "this", ConditionResult.FALSE },
        { "this is a value", "VALUE", ConditionResult.TRUE },
        { "this is a value", "AlUe", ConditionResult.TRUE },
        { "this is a value", "UE", ConditionResult.TRUE },
        { "this is a value", "E", ConditionResult.TRUE },
        { "this is a value", "valu", ConditionResult.FALSE },
        { "this is a value", "THIS", ConditionResult.FALSE },
        { "this is a value", " ", ConditionResult.FALSE },
        { "this is a VALUE", "value", ConditionResult.TRUE },
        { "end with space    ", " ", ConditionResult.FALSE },
        { "end with space    ", "space", ConditionResult.TRUE },
        { "end with space    ", "SPACE", ConditionResult.TRUE }, };
  }



  /**
   * {@inheritDoc}
   */
  @Override
  @DataProvider(name = "substringInitialMatchData")
  public Object[][] createSubstringInitialMatchData()
  {
    return new Object[][] {
        { "this is a value", "this", ConditionResult.TRUE },
        { "this is a value", "th", ConditionResult.TRUE },
        { "this is a value", "t", ConditionResult.TRUE },
        { "this is a value", "is", ConditionResult.FALSE },
        { "this is a value", "a", ConditionResult.FALSE },
        { "this is a value", "TH", ConditionResult.TRUE },
        { "this is a value", "T", ConditionResult.TRUE },
        { "this is a value", "IS", ConditionResult.FALSE },
        { "this is a value", "A", ConditionResult.FALSE },
        { "this is a value", "VALUE", ConditionResult.FALSE },
        { "this is a value", " ", ConditionResult.FALSE },
        { "this is a value", "NOT", ConditionResult.FALSE },
        { "this is a value", "THIS", ConditionResult.TRUE }, };
  }



  /**
   * {@inheritDoc}
   */
  @Override
  @DataProvider(name = "substringMiddleMatchData")
  public Object[][] createSubstringMiddleMatchData()
  {
    return new Object[][] {
        { "this is a value", new String[] { "this" }, ConditionResult.TRUE },
        { "this is a value", new String[] { "is" }, ConditionResult.TRUE },
        { "this is a value", new String[] { "a" }, ConditionResult.TRUE },
        { "this is a value", new String[] { "value" }, ConditionResult.TRUE },
        { "this is a value", new String[] { "THIS" }, ConditionResult.TRUE },
        { "this is a value", new String[] { "IS" }, ConditionResult.TRUE },
        { "this is a value", new String[] { "A" }, ConditionResult.TRUE },
        { "this is a value", new String[] { "VALUE" }, ConditionResult.TRUE },
        { "this is a value", new String[] { " " }, ConditionResult.TRUE },
        { "this is a value", new String[] { "this", "is", "a", "value" },
            ConditionResult.TRUE },
        // The matching rule requires ordered non overlapping
        // substrings.
        // Issue #730 was not valid.
        { "this is a value", new String[] { "value", "this" },
            ConditionResult.FALSE },
        { "this is a value", new String[] { "this", "this is" },
            ConditionResult.FALSE },
        { "this is a value", new String[] { "this", "IS", "a", "VALue" },
            ConditionResult.TRUE },
        { "this is a value", new String[] { "his IS", "A val", },
            ConditionResult.TRUE },
        { "this is a value", new String[] { "not", }, ConditionResult.FALSE },
        { "this is a value", new String[] { "this", "not" },
            ConditionResult.FALSE },
        { "this is a value", new String[] { "    " }, ConditionResult.TRUE }, };
  }



  /**
   * {@inheritDoc}
   */
  @Override
  protected MatchingRule getRule()
  {
    return Schema.getCoreSchema().getMatchingRule(SMR_CASE_IGNORE_OID);
  }
}
