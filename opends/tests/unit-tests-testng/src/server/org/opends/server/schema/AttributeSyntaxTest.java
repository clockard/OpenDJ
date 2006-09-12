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
package org.opends.server.schema;

import static org.testng.Assert.assertEquals;
import static org.opends.server.schema.SchemaConstants.*;

import org.opends.server.TestCaseUtils;
import org.opends.server.api.AttributeSyntax;
import org.opends.server.core.DirectoryServer;
import org.opends.server.protocols.asn1.ASN1OctetString;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AttributeSyntaxTest extends SchemaTestCase
{
  @DataProvider(name="acceptableValues")
  public Object[][] createapproximateMatchingRuleTest()
  {
    // fill this table with tables containing :
    // - the name of the Syntax rule to test
    // - a value that must be tested for correctness
    // - a boolean indicating if the value is correct.
    return new Object[][] {
        
        // tests for the UTC time syntax
        // Some values are commented because these values are not
        // accepted. I think they should.
        //{SYNTAX_UTC_TIME_OID,"20060906135000.000Z", true},
        //{SYNTAX_UTC_TIME_OID,"20060906135030.3Z", true},
        //{SYNTAX_UTC_TIME_OID,"20060906135030.30Z", true},
        //{SYNTAX_UTC_TIME_OID,"20060906135030.Z", true},
        //{SYNTAX_UTC_TIME_OID,"20060906135030.0118Z", true},
        {SYNTAX_UTC_TIME_OID,"20060906135030+01", true},
        {SYNTAX_UTC_TIME_OID,"200609061350Z", true},
        {SYNTAX_UTC_TIME_OID,"20060906135030Z", true},
        {SYNTAX_UTC_TIME_OID,"20061116135030Z", true},
        {SYNTAX_UTC_TIME_OID,"20061126135030Z", true},
        {SYNTAX_UTC_TIME_OID,"20061231235959Z", true},
        {SYNTAX_UTC_TIME_OID,"20060906135030+0101", true},
        {SYNTAX_UTC_TIME_OID,"20060906135030+2359", true},
        {SYNTAX_UTC_TIME_OID,"20060906135030+3359", false},
        {SYNTAX_UTC_TIME_OID,"20060906135030+2389", false},
        {SYNTAX_UTC_TIME_OID,"20062231235959Z", false},
        {SYNTAX_UTC_TIME_OID,"20061232235959Z", false},
        {SYNTAX_UTC_TIME_OID,"2006123123595aZ", false},
        {SYNTAX_UTC_TIME_OID,"200a1231235959Z", false},
        {SYNTAX_UTC_TIME_OID,"2006j231235959Z", false},
        {SYNTAX_UTC_TIME_OID,"200612-1235959Z", false},
        {SYNTAX_UTC_TIME_OID,"20061231#35959Z", false},
        {SYNTAX_UTC_TIME_OID,"2006", false},
        
        // generalized time. Not much different from UTC time.
        {SYNTAX_GENERALIZED_TIME_OID,"20060906135030+01", true},
        {SYNTAX_GENERALIZED_TIME_OID,"200609061350Z", true},
        {SYNTAX_GENERALIZED_TIME_OID,"20060906135030Z", true},
        {SYNTAX_GENERALIZED_TIME_OID,"20061116135030Z", true},
        {SYNTAX_GENERALIZED_TIME_OID,"20061126135030Z", true},
        {SYNTAX_GENERALIZED_TIME_OID,"20061231235959Z", true},
        {SYNTAX_GENERALIZED_TIME_OID,"20060906135030+0101", true},
        {SYNTAX_GENERALIZED_TIME_OID,"20060906135030+2359", true},
        {SYNTAX_GENERALIZED_TIME_OID,"20060906135030+3359", false},
        {SYNTAX_GENERALIZED_TIME_OID,"20060906135030+2389", false},
        {SYNTAX_GENERALIZED_TIME_OID,"20062231235959Z", false},
        {SYNTAX_GENERALIZED_TIME_OID,"20061232235959Z", false},
        {SYNTAX_GENERALIZED_TIME_OID,"2006123123595aZ", false},
        {SYNTAX_GENERALIZED_TIME_OID,"200a1231235959Z", false},
        {SYNTAX_GENERALIZED_TIME_OID,"2006j231235959Z", false},
        {SYNTAX_GENERALIZED_TIME_OID,"200612-1235959Z", false},
        {SYNTAX_GENERALIZED_TIME_OID,"20061231#35959Z", false},
        {SYNTAX_GENERALIZED_TIME_OID,"2006", false},
        
        // here starts the data for the tests of the Content rule syntax
        {SYNTAX_DIT_CONTENT_RULE_OID,
          "( 2.5.6.4 DESC 'content rule for organization' NOT "
             + "( x121Address $ telexNumber ) )", true},
        {SYNTAX_DIT_CONTENT_RULE_OID,
            "( 2.5.6.4 NAME 'full rule' DESC 'rule with all possible fields' "
              + " OBSOLETE"
              + " AUX ( person )"
              + " MUST ( cn $ sn )"
              + " MAY ( dc )"
              + " NOT ( x121Address $ telexNumber ) )"
                , true},
        {SYNTAX_DIT_CONTENT_RULE_OID,
              "( 2.5.6.4 NAME 'full rule' DESC 'ommit parenthesis' "
                  + " OBSOLETE"
                  + " AUX person "
                  + " MUST cn "
                  + " MAY dc "
                  + " NOT x121Address )"
              , true},
         {SYNTAX_DIT_CONTENT_RULE_OID,
              "( 2.5.6.4 NAME 'full rule' DESC 'use numeric OIDs' "
                + " OBSOLETE"
                + " AUX 2.5.6.6"
                + " MUST cn "
                + " MAY dc "
                + " NOT x121Address )"
                   , true},
         {SYNTAX_DIT_CONTENT_RULE_OID,
               "( 2.5.6.4 NAME 'full rule' DESC 'illegal OIDs' "
               + " OBSOLETE"
               + " AUX 2.5.6.."
               + " MUST cn "
               + " MAY dc "
               + " NOT x121Address )"
               , false},
         {SYNTAX_DIT_CONTENT_RULE_OID,
               "( 2.5.6.4 NAME 'full rule' DESC 'illegal OIDs' "
                 + " OBSOLETE"
                 + " AUX 2.5.6.x"
                 + " MUST cn "
                 + " MAY dc "
                 + " NOT x121Address )"
                 , false},
         {SYNTAX_DIT_CONTENT_RULE_OID,
               "( 2.5.6.4 NAME 'full rule' DESC 'missing closing parenthesis' "
                 + " OBSOLETE"
                 + " AUX person "
                 + " MUST cn "
                 + " MAY dc "
                 + " NOT x121Address"
             , false},
         {SYNTAX_DIT_CONTENT_RULE_OID,
               "( 2.5.6.4 NAME 'full rule' DESC 'extra parameterss' "
                 + " MUST cn "
                 + "( this is an extra parameter )"
             , true},
         
         // Here start the data for the tests of the matching rule syntaxes 
         {SYNTAX_MATCHING_RULE_OID,
               "( 1.2.3.4 NAME 'full matching rule' "
               + " DESC 'description of matching rule' OBSOLETE "
               + " SYNTAX 1.3.6.1.4.1.1466.115.121.1.17 "
               + " ( this is an extension ) )", true},          
         {SYNTAX_MATCHING_RULE_OID,
               "( 1.2.3.4 NAME 'missing closing parenthesis' "
               + " DESC 'description of matching rule' "
               + " SYNTAX 1.3.6.1.4.1.1466.115.121.1.17 "
               + " ( this is an extension ) ", false},
               
         // Here start the data for the tests of the matching rule use syntaxes 
         {SYNTAX_MATCHING_RULE_USE_OID,
               "( 2.5.13.10 NAME 'full matching rule' "
               + " DESC 'description of matching rule' OBSOLETE "
               + " APPLIES 2.5.4.3 "
               + " ( this is an extension ) )", true},          
         {SYNTAX_MATCHING_RULE_USE_OID,
                     "( 2.5.13.10 NAME 'missing closing parenthesis' "
                     + " DESC 'description of matching rule' "
                     + " SYNTAX 2.5.4.3 "
                     + " ( this is an extension ) ", false},
               
        
    };
  }
  
  /**
   * Test the normalization and the approximate comparison.
   */
  @Test(dataProvider= "acceptableValues")
  public void testAcceptableValues(String oid, String value,
      Boolean result) throws Exception
  {
    // Make sure that the specified class can be instantiated as a task.
    AttributeSyntax rule = DirectoryServer.getAttributeSyntax(oid, false);

    // normalize the 2 provided values and check that they are equals
    Boolean liveResult = rule.valueIsAcceptable(
        new ASN1OctetString(value), new StringBuilder());
    assertEquals(result, liveResult);
    
    // call the getters to increase code coverage...
    rule.getApproximateMatchingRule();
    rule.getDescription();
    rule.getEqualityMatchingRule();
    rule.getOID();
    rule.getOrderingMatchingRule();
    rule.getSubstringMatchingRule();
    rule.getSyntaxName();
    rule.toString();
  }
  
  /**
   * Set up the environment for performing the tests in this suite.
   *
   * @throws Exception
   *           If the environment could not be set up.
   */
  @BeforeClass
  public void setUp() throws Exception {
    // This test suite depends on having the schema available, so we'll start
    // the server.
    TestCaseUtils.startServer();
  }
}
