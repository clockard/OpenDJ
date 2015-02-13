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
 *      Copyright 2015 ForgeRock AS
 */

package org.opends.server.backends.persistit;

import static org.opends.server.util.StaticUtils.getFileForPath;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.forgerock.opendj.config.server.ConfigException;
import org.opends.server.admin.std.server.PersistitBackendCfg;
import org.opends.server.admin.std.server.PluggableBackendCfg;
import org.opends.server.backends.pluggable.BackendImpl;
import org.opends.server.backends.pluggable.spi.Storage;
import org.opends.server.extensions.DiskSpaceMonitor;
import org.opends.server.types.InitializationException;

/**
 * Class defined in the configuration for this backend type.
 */
public class PitBackend extends BackendImpl
{
  /** {@inheritDoc} */
  @Override
  protected Storage newStorageInstance()
  {
    return new PersistItStorage();
  }

  /** {@inheritDoc} */
  @Override
  public DiskSpaceMonitor newDiskMonitor(PluggableBackendCfg cfg) throws ConfigException, InitializationException
  {
    PersistitBackendCfg config = (PersistitBackendCfg) cfg;
    File parentDirectory = getFileForPath(config.getDBDirectory());
    File backendDirectory =
        new File(parentDirectory, config.getBackendId());
    DiskSpaceMonitor dm = new DiskSpaceMonitor(getBackendID() + " backend",
        backendDirectory, config.getDiskLowThreshold(), config.getDiskFullThreshold(),
        5, TimeUnit.SECONDS, this);
    dm.initializeMonitorProvider(null);
    return dm;
  }

  /** {@inheritDoc} */
  @Override
  public void updateDiskMonitor(DiskSpaceMonitor dm, PluggableBackendCfg newConfig)
  {
    PersistitBackendCfg newCfg = (PersistitBackendCfg) newConfig;
    dm.setFullThreshold(newCfg.getDiskFullThreshold());
    dm.setLowThreshold(newCfg.getDiskLowThreshold());
  }

  /** {@inheritDoc} */
  protected File getBackupDirectory(PluggableBackendCfg cfg)
  {
    PersistitBackendCfg config = (PersistitBackendCfg) cfg;
    File parentDir = getFileForPath(config.getDBDirectory());
    return new File(parentDir, config.getBackendId());
  }
}
