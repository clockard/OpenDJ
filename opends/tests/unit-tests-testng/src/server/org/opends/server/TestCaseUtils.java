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

import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.util.LDIFReader;

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.InetSocketAddress;

import org.opends.server.backends.MemoryBackend;
import org.opends.server.config.ConfigException;
import org.opends.server.config.ConfigFileHandler;
import org.opends.server.core.DirectoryServer;
import org.opends.server.core.InitializationException;
import org.opends.server.loggers.Error;
import org.opends.server.loggers.Debug;
import org.opends.server.types.DN;
import org.opends.server.types.FilePermission;
import org.opends.server.types.OperatingSystem;

import static org.opends.server.util.ServerConstants.*;
import static org.opends.server.util.StaticUtils.*;

/**
 * This class defines some utility functions which can be used by test
 * cases.
 */
public final class TestCaseUtils {
  /**
   * The name of the system property that specifies the server build root.
   */
  public static final String PROPERTY_BUILD_ROOT =
       "org.opends.server.BuildRoot";

  /**
   * The string representation of the DN that will be used as the base entry for
   * the test backend.  This must not be changed, as there are a number of test
   * cases that depend on this specific value of "o=test".
   */
  public static final String TEST_ROOT_DN_STRING = "o=test";

  /**
   * Indicates whether the server has already been started.  The value of this
   * constant must not be altered by anything outside the
   * <CODE>startServer</CODE> method.
   */
  public static boolean SERVER_STARTED = false;

  /**
   * The memory-based backend configured for use in the server.
   */
  private static MemoryBackend memoryBackend = null;

  /**
   * The LDAP port the server is bound to on start.
   */
  private static int serverLdapPort;

  /**
   * The JMX port the server is bound to on start.
   */
  private static int serverJmxPort;

  /**
   * The LDAPS port the server is bound to on start.
   */
  private static int serverLdapsPort;

  /**
   * Starts the Directory Server so that it will be available for use while
   * running the unit tests.  This will only actually start the server once, so
   * subsequent attempts to start it will be ignored because it will already be
   * available.
   *
   * @throws  IOException  If a problem occurs while interacting with the
   *                       filesystem to prepare the test package root.
   *
   * @throws  InitializationException  If a problem occurs while starting the
   *                                   server.
   *
   * @throws  ConfigException  If there is a problem with the server
   *                           configuration.
   */
  public static void startServer()
         throws IOException, InitializationException, ConfigException
  {
    if (SERVER_STARTED)
    {
      return;
    }

    // Get the build root and use it to create a test package directory.
    String buildRoot = System.getProperty(PROPERTY_BUILD_ROOT);
    File   testRoot  = new File(buildRoot + File.separator + "build" +
                                File.separator + "unit-tests" + File.separator +
                                "package");
    File   testSrcRoot = new File(buildRoot + File.separator + "tests" +
                                  File.separator + "unit-tests-testng");

    if (testRoot.exists())
    {
      deleteDirectory(testRoot);
    }
    testRoot.mkdirs();

    String[] subDirectories = { "bak", "bin", "changelogDb", "classes",
                                "config", "db", "ldif", "lib", "locks",
                                "logs" };
    for (String s : subDirectories)
    {
      new File(testRoot, s).mkdir();
    }


    // Copy the configuration, schema, and MakeLDIF resources into the
    // appropriate place under the test package.
    File resourceDir   = new File(buildRoot, "resource");
    File testResourceDir = new File(testSrcRoot, "resource");
    File testConfigDir = new File(testRoot, "config");
    File testBinDir = new File(testRoot, "bin");

    copyDirectory(new File(resourceDir, "bin"), testBinDir);
    copyDirectory(new File(resourceDir, "config"), testConfigDir);
    copyDirectory(new File(resourceDir, "schema"),
                  new File(testConfigDir, "schema"));
    copyDirectory(new File(resourceDir, "MakeLDIF"),
                  new File(testConfigDir, "MakeLDIF"));
    copyFile(new File(testResourceDir, "jmxkeystore"),
             new File(testRoot, "jmxkeystore"));
    copyFile(new File(testResourceDir, "server.keystore"),
             new File(testConfigDir, "server.keystore"));
    copyFile(new File(testResourceDir, "server.truststore"),
             new File(testConfigDir, "server.truststore"));
    copyFile(new File(testResourceDir, "client.keystore"),
             new File(testConfigDir, "client.keystore"));
    copyFile(new File(testResourceDir, "client.truststore"),
             new File(testConfigDir, "client.truststore"));
    copyFile(new File(testResourceDir, "server-cert.p12"),
             new File(testConfigDir, "server-cert.p12"));
    copyFile(new File(testResourceDir, "client-cert.p12"),
             new File(testConfigDir, "client-cert.p12"));


    // Make the shell scripts in the bin directory executable, if possible.
    OperatingSystem os = DirectoryServer.getOperatingSystem();
    if ((os != null) && OperatingSystem.isUNIXBased(os) &&
        FilePermission.canSetPermissions())
    {
      try
      {
        FilePermission perm = FilePermission.decodeUNIXMode("755");
        for (File f : testBinDir.listFiles())
        {
          if (f.getName().endsWith(".sh"))
          {
            FilePermission.setPermissions(f, perm);
          }
        }
      } catch (Exception e) {}
    }


    // Find some free ports for the listeners and write them to the
    // config-chamges.ldif file.
    ServerSocket serverLdapSocket  = null;
    ServerSocket serverJmxSocket   = null;
    ServerSocket serverLdapsSocket = null;

    serverLdapSocket = new ServerSocket();
    serverLdapSocket.setReuseAddress(true);
    serverLdapSocket.bind(new InetSocketAddress("127.0.0.1", 0));
    serverLdapPort = serverLdapSocket.getLocalPort();

    serverJmxSocket = new ServerSocket();
    serverJmxSocket.setReuseAddress(true);
    serverJmxSocket.bind(new InetSocketAddress("127.0.0.1", 0));
    serverJmxPort = serverJmxSocket.getLocalPort();

    serverLdapsSocket = new ServerSocket();
    serverLdapsSocket.setReuseAddress(true);
    serverLdapsSocket.bind(new InetSocketAddress("127.0.0.1", 0));
    serverLdapsPort = serverLdapsSocket.getLocalPort();

    BufferedReader reader = new BufferedReader(new FileReader(
                                               new File(testResourceDir,
                                                        "config-changes.ldif")
                                              ));
    FileOutputStream outFile = new FileOutputStream(
        new File(testConfigDir, "config-changes.ldif"));
    PrintStream writer = new PrintStream(outFile);

    String line = reader.readLine();

    while(line != null)
    {
      line = line.replaceAll("#ldapport#", String.valueOf(serverLdapPort));
      line = line.replaceAll("#jmxport#", String.valueOf(serverJmxPort));
      line = line.replaceAll("#ldapsport#", String.valueOf(serverLdapsPort));

      writer.println(line);
      line = reader.readLine();
    }

    writer.close();
    outFile.close();

    serverLdapSocket.close();
    serverJmxSocket.close();
    serverLdapsSocket.close();

    // Actually start the server and set a variable that will prevent us from
    // needing to do it again.
    System.setProperty(PROPERTY_SERVER_ROOT, testRoot.getAbsolutePath());
    System.setProperty(PROPERTY_FORCE_DAEMON_THREADS, "true");

    String configClass = ConfigFileHandler.class.getName();
    String configFile  = testConfigDir.getAbsolutePath() + File.separator +
                         "config.ldif";

    DirectoryServer directoryServer = DirectoryServer.getInstance();
    directoryServer.bootstrapServer();
    directoryServer.initializeConfiguration(configClass, configFile);
    Error.removeAllErrorLoggers(false);
    Debug.removeAllDebugLoggers(false);
    directoryServer.startServer();
    SERVER_STARTED = true;
  }

  /**
   * Shut down the server, if it has been started.
   * @param reason The reason for the shutdown.
   */
  public static void shutdownServer(String reason)
  {
    if (SERVER_STARTED)
    {
      DirectoryServer.shutDown("org.opends.server.TestCaseUtils", reason);
      SERVER_STARTED = false;
    }
  }

  /**
   * Initializes a memory-based backend that may be used to perform operations
   * while testing the server.  This will ensure that the memory backend is
   * created in the server if it does not yet exist, and that it is empty.  Note
   * that the base DN for the test backend will always be "o=test", and it must
   * not be changed.  It is acceptable for test cases using this backend to
   * hard-code their sample data to use this base DN, although they may still
   * reference the <CODE>TEST_ROOT_DN_STRING</CODE> constant if they wish.
   *
   * @param  createBaseEntry  Indicate whether to automatically create the base
   *                          entry and add it to the backend.
   *
   * @throws  Exception  If an unexpected problem occurs.
   */
  public static void initializeTestBackend(boolean createBaseEntry)
         throws Exception
  {
    startServer();

    DN baseDN = DN.decode(TEST_ROOT_DN_STRING);
    if (memoryBackend == null)
    {
      memoryBackend = new MemoryBackend();
      memoryBackend.initializeBackend(null, new DN[] { baseDN });
      DirectoryServer.registerBackend(memoryBackend);
    }

    memoryBackend.clearMemoryBackend();

    if (createBaseEntry)
    {
      Entry e = createEntry(baseDN);
      memoryBackend.addEntry(e, null);
    }
  }

  /**
   * Create a temporary directory with the specified prefix.
   *
   * @param prefix
   *          The directory prefix.
   * @return The temporary directory.
   * @throws IOException
   *           If the temporary directory could not be created.
   */
  public static File createTemporaryDirectory(String prefix)
      throws IOException {
    File tempDirectory = File.createTempFile(prefix, null);

    if (!tempDirectory.delete()) {
      throw new IOException("Unable to delete temporary file: "
          + tempDirectory);
    }

    if (!tempDirectory.mkdir()) {
      throw new IOException("Unable to create temporary directory: "
          + tempDirectory);
    }

    return tempDirectory;
  }

  /**
   * Copy a directory and its contents.
   *
   * @param src
   *          The name of the directory to copy.
   * @param dst
   *          The name of the destination directory.
   * @throws IOException
   *           If the directory could not be copied.
   */
  public static void copyDirectory(File src, File dst) throws IOException {
    if (src.isDirectory()) {
      // Create the destination directory if it does not exist.
      if (!dst.exists()) {
        dst.mkdirs();
      }

      // Recursively copy sub-directories and files.
      for (String child : src.list()) {
        copyDirectory(new File(src, child), new File(dst, child));
      }
    } else {
      copyFile(src, dst);
    }
  }

  /**
   * Delete a directory and its contents.
   *
   * @param dir
   *          The name of the directory to delete.
   * @throws IOException
   *           If the directory could not be deleted.
   */
  public static void deleteDirectory(File dir) throws IOException {
    if (dir.isDirectory()) {
      // Recursively delete sub-directories and files.
      for (String child : dir.list()) {
        deleteDirectory(new File(dir, child));
      }
    }

    dir.delete();
  }

  /**
   * Copy a file.
   *
   * @param src
   *          The name of the source file.
   * @param dst
   *          The name of the destination file.
   * @throws IOException
   *           If the file could not be copied.
   */
  public static void copyFile(File src, File dst) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dst);

    // Transfer bytes from in to out
    byte[] buf = new byte[8192];
    int len;
    while ((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  /**
   * Get the LDAP port the test environment Directory Server instance is
   * running on.
   *
   * @return The port number.
   */
  public static long getServerLdapPort()
  {
    return serverLdapPort;
  }

  /**
   * Get the JMX port the test environment Directory Server instance is
   * running on.
   *
   * @return The port number.
   */
  public static long getServerJmxPort()
  {
    return serverJmxPort;
  }

  /**
   * Get teh LDAPS port the test environment Directory Server instance is
   * running on
   *
   * @return The port number.
   */
  public static long getServerLdapsPort()
  {
    return serverLdapsPort;
  }

  /**
   * Method for getting a file from the test resources directory.
   *
   * @return The directory as a File
   */
  public static File getTestResource(String filename)
  {
    String buildRoot = System.getProperty(PROPERTY_BUILD_ROOT);
    File   testResourceDir = new File(buildRoot + File.separator + "tests" +
                                      File.separator + "unit-tests-testng" +
                                      File.separator + "resource");

    return new File(testResourceDir, filename);
  }

  /**
   * Prevent instantiation.
   */
  private TestCaseUtils() {
    // No implementation.
  }


  ////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////
  //
  // Various methods for converting LDIF Strings to Entries
  //
  ////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////


  /**
   * Returns a modifiable List of entries parsed from the provided LDIF.
   * It's best to call this after the server has been initialized so
   * that schema checking happens.
   * <p>
   * Also take a look at the makeLdif method below since this makes
   * expressing LDIF a little bit cleaner.
   *
   * @param ldif of the entries to parse.
   * @return a List of EntryS parsed from the ldif string.
   * @see #makeLdif
   */
  public static List<Entry> entriesFromLdifString(String ldif) throws Exception {
    LDIFImportConfig ldifImportConfig = new LDIFImportConfig(new StringReader(ldif));
    LDIFReader reader = new LDIFReader(ldifImportConfig);

    List<Entry> entries = new ArrayList<Entry>();
    Entry entry = null;
    while ((entry = reader.readEntry()) != null) {
      entries.add(entry);
    }

    return entries;
  }

  /**
   * This is used as a convenience when and LDIF string only includes a single
   * entry. It's best to call this after the server has been initialized so
   * that schema checking happens.
   * <p>
   * Also take a look at the makeLdif method below since this makes
   * expressing LDIF a little bit cleaner.
   *
   * @return the first Entry parsed from the ldif String
   * @see #makeLdif
   */
  public static Entry entryFromLdifString(String ldif) throws Exception {
    return entriesFromLdifString(ldif).get(0);
  }

  /**
   * This method provides the minor convenience of not having to specify the
   * newline character at the end of every line of LDIF in test code.
   * This is an admittedly small advantage, but it does make things a little
   * easier and less error prone.  For example, this
   *
     <pre>
       private static final String JOHN_SMITH_LDIF = TestCaseUtils.makeLdif(
          "dn: cn=John Smith,dc=example,dc=com",
          "objectclass: inetorgperson",
          "cn: John Smith",
          "sn: Smith",
          "givenname: John");

     </pre>

   is a <bold>little</bold> easier to work with than

     <pre>
       private static final String JOHN_SMITH_LDIF =
          "dn: cn=John Smith,dc=example,dc=com\n" +
          "objectclass: inetorgperson\n" +
          "cn: John Smith\n" +
          "sn: Smith\n" +
          "givenname: John\n";

     </pre>
   *
   * @return the concatenation of each line followed by a newline character
   */
  public static String makeLdif(String... lines) {
    StringBuilder buffer = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      buffer.append(lines[i]).append("\n");
    }
    return buffer.toString();
  }

  /**
   * This is a convience method that constructs an Entry from the specified
   * lines of LDIF.  Here's a sample usage
   *
   <pre>
   Entry john = TestCaseUtils.makeEntry(
      "dn: cn=John Smith,dc=example,dc=com",
      "objectclass: inetorgperson",
      "cn: John Smith",
      "sn: Smith",
      "givenname: John");
   </pre>
   * @see #makeLdif
   */
  public static Entry makeEntry(String... lines) throws Exception {
     return entryFromLdifString(makeLdif(lines));
  }

  /**
   * This is a convience method that constructs an List of EntryS from the
   * specified lines of LDIF.  Here's a sample usage
   *
   <pre>
   List<Entry> smiths = TestCaseUtils.makeEntries(
      "dn: cn=John Smith,dc=example,dc=com",
      "objectclass: inetorgperson",
      "cn: John Smith",
      "sn: Smith",
      "givenname: John",
      "",
      "dn: cn=Jane Smith,dc=example,dc=com",
      "objectclass: inetorgperson",
      "cn: Jane Smith",
      "sn: Smith",
      "givenname: Jane");
   </pre>
   * @see #makeLdif
   */
  public static List<Entry> makeEntries(String... lines) throws Exception {
     return entriesFromLdifString(makeLdif(lines));
  }
}
