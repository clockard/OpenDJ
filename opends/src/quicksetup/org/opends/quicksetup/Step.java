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

package org.opends.quicksetup;

/**
 * This enumeration just represents the different steps that we can have in
 * the installation and uninstallation wizards.
 *
 */
public enum Step

{
  /**
   * Welcome step for the installation.
   */
  WELCOME("welcome-step"),
  /**
   * Server Settings step (path, port, etc.).
   */
  SERVER_SETTINGS("server-settings-step"),
  /**
   * Data Options panel (suffix dn, LDIF path, etc.).
   */
  DATA_OPTIONS("data-options-step"),
  /**
   * Review panel for the install.
   */
  REVIEW("review-step"),
  /**
   * Progress panel.
   */
  PROGRESS("progress-step"),
  /**
   * Confirmation panel for the uninstallation.
   */
  CONFIRM_UNINSTALL("confirm-uninstall-step");

  private String msgKey;

  /**
   * Creates a step.
   * @param msgKey the message key used to access a message catalog to
   * retreive this step's display name
   */
  Step(String msgKey) {
    this.msgKey = msgKey;
  }

  /**
   * Gets this steps message key.
   * @return String message key used to access a message catalog to
   * retreive this step's display name
   */
  public String getMessageKey() {
    return msgKey;
  }
}
