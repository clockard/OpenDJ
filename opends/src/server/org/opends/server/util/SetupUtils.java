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
 *      Copyright 2006-2008 Sun Microsystems, Inc.
 */
package org.opends.server.util;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.LinkedList;

import java.util.Random;
import org.opends.server.types.OperatingSystem;


/**
 * This class provides a number of utility methods that may be used during the
 * graphical or command-line setup process.
 */
@org.opends.server.types.PublicAPI(
     stability=org.opends.server.types.StabilityLevel.VOLATILE,
     mayInstantiate=false,
     mayExtend=false,
     mayInvoke=true)
public class SetupUtils
{
  /**
   * Java property used to known if we are using web start or not.
   */
  public static final String IS_WEBSTART = "org.opends.quicksetup.iswebstart";

  /**
   * Specific environment variable used by the scripts to find java.
   */
  public static final String OPENDS_JAVA_HOME = "OPENDS_JAVA_HOME";

  /**
   * Specific environment variable used by the scripts to set java arguments.
   */
  public static final String OPENDS_JAVA_ARGS = "OPENDS_JAVA_ARGS";

  /**
   * Java property used to know which are the jar files that must be downloaded
   * lazily.  The current code in WebStartDownloader that uses this property
   * assumes that the URL are separated with an space.
   */
  public static final String LAZY_JAR_URLS =
      "org.opends.quicksetup.lazyjarurls";

  /**
   * Java property used to know which is the name of the zip file that must
   * be unzipped and whose contents must be extracted during the Web Start
   * based setup.
   */
  public static final String ZIP_FILE_NAME =
      "org.opends.quicksetup.zipfilename";

  /**
   * The relative path where all the libraries (jar files) are.
   */
  public static final String LIBRARIES_PATH_RELATIVE = "lib";

  /* These string values must be synchronized with Directory Server's main
   * method.  These string values are considered stable by the server team and
   * not candidates for internationalization. */
  /** Product name. */
  public static final String NAME = "Name";
  /** Build ID. */
  public static final String BUILD_ID = "Build ID";
  /** Major version. */
  public static final String MAJOR_VERSION = "Major Version";
  /** Minor version. */
  public static final String MINOR_VERSION = "Minor Version";
  /** Point version of the product. */
  public static final String POINT_VERSION = "Point Version";
  /** Revision number in SVN. */
  public static final String REVISION_NUMBER = "Revision Number";
  /** The version qualifier. */
  public static final String VERSION_QUALIFIER = "Version Qualifier";
  /** Incompatibilities found between builds (used by the upgrade tool). */
  public static final String INCOMPATIBILITY_EVENTS = "Upgrade Event IDs";
  /** Fix IDs associated with the build. */
  public static final String FIX_IDS = "Fix IDs";
  /** Debug build identifier. */
  public static final String DEBUG_BUILD = "Debug Build";
  /** The OS used during the build. */
  public static final String BUILD_OS = "Build OS";
  /** The user that generated the build. */
  public static final String BUILD_USER = "Build User";
  /** The java version used to generate the build. */
  public static final String BUILD_JAVA_VERSION = "Build Java Version";
  /** The java vendor of the JVM used to build. */
  public static final String BUILD_JAVA_VENDOR = "Build Java Vendor";
  /** The version of the JVM used to create the build. */
  public static final String BUILD_JVM_VERSION = "Build JVM Version";
  /** The vendor of the JVM used to create the build. */
  public static final String BUILD_JVM_VENDOR = "Build JVM Vendor";
  /** The build number. */
  public static final String BUILD_NUMBER = "Build Number";

  /**
   * Creates a MakeLDIF template file using the provided information.
   *
   * @param  baseDN      The base DN for the data in the template file.
   * @param  numEntries  The number of user entries the template file should
   *                     create.
   *
   * @return  The {@code File} object that references the created template file.
   *
   * @throws  IOException  If a problem occurs while writing the template file.
   */
  public static File createTemplateFile(String baseDN, int numEntries)
         throws IOException
  {
    File templateFile = File.createTempFile("opends-install", ".template");
    templateFile.deleteOnExit();

    LinkedList<String> lines = new LinkedList<String>();
    lines.add("define suffix=" + baseDN);

    if (numEntries > 0)
    {
      lines.add("define numusers=" + numEntries);
    }

    lines.add("");
    lines.add("branch: [suffix]");
    lines.add("");
    lines.add("branch: ou=People,[suffix]");

    if (numEntries > 0)
    {
      lines.add("subordinateTemplate: person:[numusers]");
      lines.add("");
      lines.add("template: person");
      lines.add("rdnAttr: uid");
      lines.add("objectClass: top");
      lines.add("objectClass: person");
      lines.add("objectClass: organizationalPerson");
      lines.add("objectClass: inetOrgPerson");
      lines.add("givenName: <first>");
      lines.add("sn: <last>");
      lines.add("cn: {givenName} {sn}");
      lines.add("initials: {givenName:1}" +
                "<random:chars:ABCDEFGHIJKLMNOPQRSTUVWXYZ:1>{sn:1}");
      lines.add("employeeNumber: <sequential:0>");
      lines.add("uid: user.{employeeNumber}");
      lines.add("mail: {uid}@maildomain.net");
      lines.add("userPassword: password");
      lines.add("telephoneNumber: <random:telephone>");
      lines.add("homePhone: <random:telephone>");
      lines.add("pager: <random:telephone>");
      lines.add("mobile: <random:telephone>");
      lines.add("street: <random:numeric:5> <file:streets> Street");
      lines.add("l: <file:cities>");
      lines.add("st: <file:states>");
      lines.add("postalCode: <random:numeric:5>");
      lines.add("postalAddress: {cn}${street}${l}, {st}  {postalCode}");
      lines.add("description: This is the description for {cn}.");
    }

    BufferedWriter writer = new BufferedWriter(new FileWriter(templateFile));
    for (String line : lines)
    {
      writer.write(line);
      writer.newLine();
    }

    writer.flush();
    writer.close();

    return templateFile;
  }

  /**
   * Returns {@code true} if we are running under Mac OS and
   * {@code false} otherwise.
   * @return {@code true} if we are running under Mac OS and
   * {@code false} otherwise.
   */
  public static boolean isMacOS()
  {
    return OperatingSystem.MACOS == getOperatingSystem();
  }

  /**
   * Returns {@code true} if we are running under Unix and
   * {@code false} otherwise.
   * @return {@code true} if we are running under Unix and
   * {@code false} otherwise.
   */
  public static boolean isUnix()
  {
    return OperatingSystem.isUNIXBased(getOperatingSystem());
  }

  /**
   * Indicates whether the underlying operating system is a Windows variant.
   *
   * @return  {@code true} if the underlying operating system is a Windows
   *          variant, or {@code false} if not.
   */
  public static boolean isWindows()
  {
      return OperatingSystem.WINDOWS == getOperatingSystem();
  }

  /**
   * Indicates whether the underlying operating system is Windows Vista.
   *
   * @return  {@code true} if the underlying operating system is Windows
   *          Vista, or {@code false} if not.
   */
  public static boolean isVista()
  {
    boolean isVista;
    String os = System.getProperty("os.name");
    if (os != null)
    {
     isVista = isWindows() && (os.toLowerCase().indexOf("vista") != -1);
    }
    else
    {
      isVista = false;
    }
    return isVista;
  }
  /**
   * Returns a String representation of the OS we are running.
   * @return a String representation of the OS we are running.
   */
  public static String getOSString()
  {
    return getOperatingSystem().toString();
  }

  /**
   * Commodity method to help identifying the OS we are running on.
   * @return the OperatingSystem we are running on.
   */
  public static OperatingSystem getOperatingSystem()
  {
    return OperatingSystem.forName(System.getProperty("os.name"));
  }

  /**
   * Returns {@code true} if the provided port is free and we can use it,
   * {@code false} otherwise.
   * @param hostname the host name we are analyzing.
   * @param port the port we are analyzing.
   * @return {@code true} if the provided port is free and we can use it,
   * {@code false} otherwise.
   */
  public static boolean canUseAsPort(String hostname, int port)
  {
    boolean canUseAsPort = false;
    ServerSocket serverSocket = null;
    try
    {
      InetSocketAddress socketAddress = new InetSocketAddress(hostname, port);
      serverSocket = new ServerSocket();
      if (!isWindows())
      {
        serverSocket.setReuseAddress(true);
      }
      serverSocket.bind(socketAddress);
      canUseAsPort = true;

      serverSocket.close();

      /* Try to create a socket because sometimes even if we can create a server
       * socket there is already someone listening to the port (is the case
       * of products as Sun DS 6.0).
       */
      Socket s = null;
      try
      {
        s = new Socket();
        s.connect(socketAddress, 1000);
        canUseAsPort = false;

      } catch (Throwable t)
      {
      }
      finally
      {
        if (s != null)
        {
          try
          {
            s.close();
          }
          catch (Throwable t)
          {
          }
        }
      }


    } catch (IOException ex)
    {
      canUseAsPort = false;
    } finally
    {
      try
      {
        if (serverSocket != null)
        {
          serverSocket.close();
        }
      } catch (Exception ex)
      {
      }
    }

    return canUseAsPort;
  }

  /**
   * Returns {@code true} if the provided port is free and we can use it,
   * {@code false} otherwise.
   * @param port the port we are analyzing.
   * @return {@code true} if the provided port is free and we can use it,
   * {@code false} otherwise.
   */
  public static boolean canUseAsPort(int port)
  {
    return canUseAsPort("localhost", port);
  }

  /**
   * Returns {@code true} if the provided port is a priviledged port,
   * {@code false} otherwise.
   * @param port the port we are analyzing.
   * @return {@code true} if the provided port is a priviledged port,
   * {@code false} otherwise.
   */
  public static boolean isPriviledgedPort(int port)
  {
    return (port <= 1024) && !isWindows();
  }

  /**
   * Returns the default value for the JMX Port.
   * @return the default value for the JMX Port.
   */
  public static int getDefaultJMXPort()
  {
    return 1689;
  }

  /**
   * Indicates whether we are in a web start installation or not.
   *
   * @return <CODE>true</CODE> if we are in a web start installation and
   *         <CODE>false</CODE> if not.
   */
  public static boolean isWebStart()
  {
    return "true".equals(System.getProperty(IS_WEBSTART));
  }

  /**
   * Returns the String that can be used to launch an script using Runtime.exec.
   * This method is required because in Windows the script that contain a "="
   * in their path must be quoted.
   * @param script the script name
   * @return the absolute path for the given parentPath and relativePath.
   */
  public static String getScriptPath(String script)
  {
    String s = script;
    if (isWindows())
    {
      if (s != null)
      {
        if (!s.startsWith("\"") || !s.endsWith("\""))
        {
          s = "\""+script+"\"";
        }
      }
    }
    return s;
  }

  /**
   * Returns a randomly generated password for a self-signed certificate
   * keystore.
   * @return a randomly generated password for a self-signed certificate
   * keystore.
   */
  public static char[] createSelfSignedCertificatePwd() {
    int pwdLength = 50;
    char[] pwd = new char[pwdLength];
    Random random = new Random();
    for (int pos=0; pos < pwdLength; pos++) {
        int type = getRandomInt(random,3);
        char nextChar = getRandomChar(random,type);
        pwd[pos] = nextChar;
    }
    return pwd;
  }


  /**
   * Export a certificate in a file.
   *
   * @param certManager Certificate manager to use.
   * @param alias Certificate alias to export.
   * @param path Path of the output file.
   *
   * @throws CertificateEncodingException If the certificate manager cannot
   * encode the certificate.
   * @throws IOException If a problem occurs while creating or writing in the
   * output file.
   * @throws KeyStoreException If the certificate manager cannot retrieve the
   * certificate to be exported.
   */
  public static void exportCertificate(
    CertificateManager certManager, String alias, String path)
    throws CertificateEncodingException, IOException, KeyStoreException
  {
    Certificate certificate = certManager.getCertificate(alias);

    byte[] certificateBytes = certificate.getEncoded();

    FileOutputStream outputStream = new FileOutputStream(path, false);
    outputStream.write(certificateBytes);
    outputStream.close();
  }

  /* The next two methods are used to generate the random password for the
   * self-signed certificate. */
  private static char getRandomChar(Random random, int type)
  {
    char generatedChar;
    int next = random.nextInt();
    int d;

    switch (type)
    {
    case 0:
      // Will return a digit
      d = next % 10;
      if (d < 0)
      {
        d = d * (-1);
      }
      generatedChar = (char) (d+48);
      break;
    case 1:
      // Will return a lower case letter
      d = next % 26;
      if (d < 0)
      {
        d = d * (-1);
      }
      generatedChar =  (char) (d + 97);
      break;
    default:
      // Will return a capital letter
      d = (next % 26);
      if (d < 0)
      {
        d = d * (-1);
      }
      generatedChar = (char) (d + 65) ;
    }

    return generatedChar;
  }

  private static int getRandomInt(Random random,int modulo)
  {
    return (random.nextInt() & modulo);
  }

}

