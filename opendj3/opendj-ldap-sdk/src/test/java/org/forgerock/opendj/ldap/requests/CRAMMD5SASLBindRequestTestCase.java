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
 *      Copyright 2010 Sun Microsystems, Inc.
 *      Portions copyright 2011-2013 ForgeRock AS
 */

package org.forgerock.opendj.ldap.requests;

import static com.forgerock.opendj.util.StaticUtils.EMPTY_BYTES;
import static com.forgerock.opendj.util.StaticUtils.getBytes;

import org.testng.annotations.DataProvider;

/**
 * Tests CRAM MD5 SASL bind requests.
 */
@SuppressWarnings("javadoc")
public class CRAMMD5SASLBindRequestTestCase extends BindRequestTestCase {
    @DataProvider(name = "CRAMMD5SASLBindRequests")
    public Object[][] getCRAMMD5SASLBindRequests() throws Exception {
        return getTestRequests();
    }

    @Override
    protected CRAMMD5SASLBindRequest[] createTestRequests() throws Exception {
        return new CRAMMD5SASLBindRequest[] {
                Requests.newCRAMMD5SASLBindRequest("id1", EMPTY_BYTES),
                Requests.newCRAMMD5SASLBindRequest("id2", getBytes("test"))
        };
    }
}
