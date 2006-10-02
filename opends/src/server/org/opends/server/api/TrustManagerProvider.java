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
package org.opends.server.api;



import javax.net.ssl.TrustManager;

import org.opends.server.config.ConfigEntry;
import org.opends.server.config.ConfigException;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.InitializationException;



/**
 * This class defines an API that may be used to obtain a set of
 * <CODE>javax.net.ssl.TrustManager</CODE> objects for use when
 * performing SSL communication.
 */
public abstract class TrustManagerProvider
{
  /**
   * Initializes this trust manager provider based on the information
   * in the provided configuration entry.
   *
   * @param  configEntry  The configuration entry that contains the
   *                      information to use to initialize this trust
   *                      manager provider.
   *
   * @throws  ConfigException  If an unrecoverable problem arises in
   *                           the process of performing the
   *                           initialization as a result of the
   *                           server configuration.
   *
   * @throws  InitializationException  If a problem occurs during
   *                                   initialization that is not
   *                                   related to the server
   *                                   configuration.
   */
  public abstract void initializeTrustManagerProvider(
                            ConfigEntry configEntry)
         throws ConfigException, InitializationException;



  /**
   * Performs any finalization that may be necessary for this trust
   * manager provider.
   */
  public abstract void finalizeTrustManagerProvider();



  /**
   * Retrieves a set of <CODE>TrustManager</CODE> objects that may be
   * used for interactions requiring access to a trust manager.
   *
   * @return  A set of <CODE>TrustManager</CODE> objects that may be
   *          used for interactions requiring access to a trust
   *          manager.
   *
   * @throws  DirectoryException  If a problem occurs while attempting
   *                              to obtain the set of trust managers.
   */
  public abstract TrustManager[] getTrustManagers()
         throws DirectoryException;
}

