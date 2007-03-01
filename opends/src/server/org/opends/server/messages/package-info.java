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
 *      Portions Copyright 2006-2007 Sun Microsystems, Inc.
 */



/**
 * Defines all the messages that may be used in the Directory Server,
 * particularly those that may appear in error messages and/or are included in
 * responses to clients.  Each message definition consists of two parts.  The
 * first part is an integer value that uniquely identifies that message among
 * all other messages in the server (information about the message category and
 * severity are encoded in that identifier).  The second element is a default
 * format string that will be used to generate the message associated with a
 * particular message ID.  These format strings may contain tags that will be
 * replaced with additional arguments provided at the time the message is
 * generated.  The default format strings may be overridden using properties
 * files that associated different format strings with message IDs, which can be
 * used to help internationalize the messages that are generated.
 */
package org.opends.server.messages;

