/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at
 * trunk/opendj3/legal-notices/CDDLv1_0.txt
 * or http://forgerock.org/license/CDDLv1.0.html.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at
 * trunk/opendj3/legal-notices/CDDLv1_0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the fields enclosed
 * by brackets "[]" replaced with your own identifying information:
 *      Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 *
 *
 *      Copyright 2010 Sun Microsystems, Inc.
 */

package org.forgerock.opendj.ldap.requests;



import org.forgerock.opendj.ldap.requests.AbandonRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.testng.annotations.DataProvider;



/**
 * Tests Abandon requests.
 */
public class AbandonRequestTestCase extends RequestTestCase
{
  @DataProvider(name = "abandonRequests")
  public Object[][] getAbandonRequests() throws Exception
  {
    final AbandonRequest[] requests = { Requests.newAbandonRequest(-1),
        Requests.newAbandonRequest(0), Requests.newAbandonRequest(1) };
    final Object[][] objArray = new Object[requests.length][1];
    for (int i = 0; i < requests.length; i++)
    {
      objArray[i][0] = requests[i];
    }
    return objArray;
  }



  @Override
  protected AbandonRequest[] createTestRequests() throws Exception
  {
    final Object[][] objs = getAbandonRequests();
    final AbandonRequest[] ops = new AbandonRequest[objs.length];
    for (int i = 0; i < objs.length; i++)
    {
      ops[i] = (AbandonRequest) objs[i][0];
    }
    return ops;
  }
}
