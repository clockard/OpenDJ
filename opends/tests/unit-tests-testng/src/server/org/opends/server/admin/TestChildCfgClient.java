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



import org.opends.server.admin.ConfigurationClient;
import org.opends.server.admin.ManagedObjectDefinition;



/**
 * A sample client-side configuration interface for testing.
 */
public interface TestChildCfgClient extends ConfigurationClient {

  /**
   * {@inheritDoc}
   */
  ManagedObjectDefinition<? extends TestChildCfgClient, ? extends TestChildCfg> definition();



  /**
   * Get the "heartbeat-interval" property.
   *
   * @return Returns the value of the "heartbeat-interval" property.
   */
  long getHeartbeatInterval();



  /**
   * Set the "heartbeat-interval" property.
   *
   * @param value
   *          The value of the "heartbeat-interval" property.
   * @throws IllegalPropertyValueException
   *           If the new value is invalid.
   */
  void setHeartbeatInterval(Long value) throws IllegalPropertyValueException;



  /**
   * Get the "maximum-length" property.
   *
   * @return Returns the value of the "maximum-length" property.
   */
  int getMaximumLength();



  /**
   * Set the "maximum-length" property.
   *
   * @param value
   *          The value of the "maximum-length" property.
   * @throws IllegalPropertyValueException
   *           If the new value is invalid.
   */
  void setMaximumLength(Integer value) throws IllegalPropertyValueException;



  /**
   * Get the "minimum-length" property.
   *
   * @return Returns the value of the "minimum-length" property.
   */
  int getMinimumLength();



  /**
   * Set the "minimum-length" property.
   *
   * @param value
   *          The value of the "minimum-length" property.
   * @throws IllegalPropertyValueException
   *           If the new value is invalid.
   */
  void setMinimumLength(Integer value) throws IllegalPropertyValueException;
}
