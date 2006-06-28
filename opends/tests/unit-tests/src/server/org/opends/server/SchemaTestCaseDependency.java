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
package org.opends.server;

import static org.opends.server.TestCaseUtils.copyDirectory;

import java.io.File;

import org.opends.server.core.DirectoryServer;

/**
 * This dependency makes sure that a directory server instance is
 * available with the core schema files loaded from the source tree's
 * resource directory.
 * <p>
 * This dependency should be used by test cases which need a directory
 * server instance with core schema files loaded.
 * <p>
 * The dependency requires the
 * {@link org.opends.server.ConfigurationTestCaseDependency} dependency.
 *
 * @author Matthew Swift
 */
public final class SchemaTestCaseDependency extends TestCaseDependency {

  // Flag used to prevent multiple initialization.
  private boolean isInitialized = false;

  // The configuration dependency (required by this dependency).
  private ConfigurationTestCaseDependency dependency;

  /**
   * Create a dependency which will make sure that the core schema files
   * are loaded.
   *
   * @param dependency
   *          The configuration dependency which this dependency
   *          requires.
   */
  public SchemaTestCaseDependency(ConfigurationTestCaseDependency dependency) {
    this.dependency = dependency;
  }

  /**
   * {@inheritDoc}
   */
  public void setUp() throws Exception {
    if (isInitialized == false) {
      // Make sure that the core configuration is available.
      dependency.setUp();

      // Copy over the schema files.
      File tempDirectory = dependency.getTempDirectory();
      File configDirectory = new File(tempDirectory, "config");
      File schemaDirectory = new File(configDirectory, "schema");
      File resourceDirectory = new File("resource");
      copyDirectory(new File(resourceDirectory, "schema"), schemaDirectory);

      // Initialize and load the schema files.
      DirectoryServer.getInstance().initializeSchema();

      // Prevent multiple initialization.
      isInitialized = true;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void tearDown() throws Exception {
    // TODO: clean up the schema?

    isInitialized = false;
  }
}
