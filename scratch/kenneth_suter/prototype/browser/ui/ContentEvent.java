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

package org.opends.statuspanel.browser.ui;

import org.opends.statuspanel.browser.ldap.Entry;

import java.util.Set;
import java.util.Map;

/**
 *
 */
public class ContentEvent {

  Entry currentEntry;
  Map<String, Set<String>> attrs;
  String action;

  public ContentEvent(Entry currentEntry, String action) {
    this(currentEntry, null, action);
  }

  public ContentEvent(Entry currentEntry,
                           Map<String, Set<String>> attrs,
                           String action) {
    this.currentEntry = currentEntry;
    this.attrs = attrs;
    this.action = action;
  }

  public Entry getCurrentEntry() {
    return this.currentEntry;
  }

  public Map<String, Set<String>> getCurrentAttributes() {
    return this.attrs;
  }

  public String getAction() {
    return this.action;
  }

}
