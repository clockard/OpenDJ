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

package org.opends.quicksetup.util;

import org.opends.quicksetup.QuickSetupException;
import org.opends.quicksetup.Application;
import org.opends.quicksetup.i18n.ResourceProvider;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Class for extracting the contents of a zip file and managing
 * the reporting of progress during extraction.
 */
public class ZipExtractor {

  private InputStream is;
  private int minRatio;
  private int maxRatio;
  private String basePath;
  private int numberZipEntries;
  private String zipFileName;
  private Application application;

  /**
   * Creates an instance of an ZipExtractor.
   * @param is InputStream of zip file content
   * @param minRatio int indicating the max ration
   * @param maxRatio int indicating the min ration
   * @param basePath filesystem location where content will be unzipped
   * @param numberZipEntries number of entries in the input stream
   * @param zipFileName name of the input zip file
   * @param app application to be notified about progress
   */
  public ZipExtractor(InputStream is, int minRatio, int maxRatio,
                                      String basePath, int numberZipEntries,
                                      String zipFileName,
                                      Application app) {
    this.is = is;
    this.minRatio = minRatio;
    this.maxRatio = maxRatio;
    this.basePath = basePath;
    this.numberZipEntries = numberZipEntries;
    this.zipFileName = zipFileName;
    this.application = app;
  }

  /**
   * Performs the zip extraction.
   * @throws QuickSetupException if something goes wrong
   */
  public void extract() throws QuickSetupException {

    ZipInputStream zipIn = new ZipInputStream(is);
    int nEntries = 1;

    /* This map is updated in the copyZipEntry method with the permissions
     * of the files that have been copied.  Once all the files have
     * been copied to the file system we will update the file permissions of
     * these files.  This is done this way to group the number of calls to
     * Runtime.exec (which is required to update the file system permissions).
     */
    Map<String, ArrayList<String>> permissions =
        new HashMap<String, ArrayList<String>>();

    String zipFirstPath = null;
    try
    {
      ZipEntry entry = zipIn.getNextEntry();
      while (entry != null)
      {
        int ratioBeforeCompleted = minRatio
        + ((nEntries - 1) * (maxRatio - minRatio) / numberZipEntries);
        int ratioWhenCompleted =
          minRatio + (nEntries * (maxRatio - minRatio) / numberZipEntries);

        if (nEntries == 1)
        {
          zipFirstPath = entry.getName();
        } else
        {
          try
          {
            copyZipEntry(entry, basePath, zipFirstPath, zipIn,
            ratioBeforeCompleted, ratioWhenCompleted, permissions, application);

          } catch (IOException ioe)
          {
            String[] arg =
              { entry.getName() };
            String errorMsg =
                    Utils.getThrowableMsg(ResourceProvider.getInstance(),
                      "error-copying", arg, ioe);

            throw new QuickSetupException(
                    QuickSetupException.Type.FILE_SYSTEM_ERROR,
                    errorMsg, ioe);
          }
        }

        zipIn.closeEntry();
        entry = zipIn.getNextEntry();
        nEntries++;
      }

      if (Utils.isUnix())
      {
        // Change the permissions for UNIX systems
        for (String perm : permissions.keySet())
        {
          ArrayList<String> paths = permissions.get(perm);
          try
          {
            int result = Utils.setPermissionsUnix(paths, perm);
            if (result != 0)
            {
              throw new IOException("Could not set permissions on files "
                  + paths + ".  The chmod error code was: " + result);
            }
          } catch (InterruptedException ie)
          {
            IOException ioe =
                new IOException("Could not set permissions on files " + paths
                    + ".  The chmod call returned an InterruptedException.");
            ioe.initCause(ie);
            throw ioe;
          }
        }
      }

    } catch (IOException ioe)
    {
      String[] arg =
        { zipFileName };
      String errorMsg =
              Utils.getThrowableMsg(ResourceProvider.getInstance(),
                      "error-zip-stream", arg, ioe);
      throw new QuickSetupException(QuickSetupException.Type.FILE_SYSTEM_ERROR,
          errorMsg, ioe);
    }
  }

  /**
   * Copies a zip entry in the file system.
   * @param entry the ZipEntry object.
   * @param basePath the basePath (the installation path)
   * @param zipFirstPath the first zip file path.  This is required because the
   * zip file contain a directory of type
   * 'OpenDS-(major version).(minor version)' that we want to get rid of.  The
   * first zip file path corresponds to this path.
   * @param is the ZipInputStream that contains the contents to be copied.
   * @param ratioBeforeCompleted the progress ratio before the zip file is
   * copied.
   * @param ratioWhenCompleted the progress ratio after the zip file is
   * copied.
   * @param permissions an ArrayList with permissions whose contents will be
   * updated.
   * @param app Application to be notified about progress
   * @throws IOException if an error occurs.
   */
  private void copyZipEntry(ZipEntry entry, String basePath,
      String zipFirstPath, ZipInputStream is, int ratioBeforeCompleted,
      int ratioWhenCompleted, Map<String, ArrayList<String>> permissions,
      Application app)
      throws IOException
  {
    String entryName = entry.getName();
    // Get rid of the zipFirstPath
    if (entryName.startsWith(zipFirstPath))
    {
      entryName = entryName.substring(zipFirstPath.length());
    }
    String path = Utils.getPath(basePath, entryName);

    String progressSummary =
            ResourceProvider.getInstance().getMsg("progress-extracting",
                    new String[]{ path });
    app.notifyListeners(ratioBeforeCompleted, progressSummary);
    if (Utils.createParentPath(path))
    {
      if (entry.isDirectory())
      {
        String perm = getDirectoryFileSystemPermissions(path);
        ArrayList<String> list = permissions.get(perm);
        if (list == null)
        {
          list = new ArrayList<String>();
        }
        list.add(path);
        permissions.put(perm, list);

        if (!Utils.createDirectory(path))
        {
          throw new IOException("Could not create path: " + path);
        }
      } else
      {
        String perm = getFileSystemPermissions(path);
        ArrayList<String> list = permissions.get(perm);
        if (list == null)
        {
          list = new ArrayList<String>();
        }
        list.add(path);
        Utils.createFile(path, is);
      }
    } else
    {
      throw new IOException("Could not create parent path: " + path);
    }
    application.notifyListenersDone(ratioWhenCompleted);
  }

  /**
   * Returns the file system permissions for a directory.
   * @param path the directory for which we want the file permissions.
   * @return the file system permissions for the directory.
   */
  private String getDirectoryFileSystemPermissions(String path)
  {
    // TODO We should get this dynamically during build?
    return "755";
  }

  /**
   * Returns the file system permissions for a file.
   * @param path the file for which we want the file permissions.
   * @return the file system permissions for the file.
   */
  private String getFileSystemPermissions(String path)
  {
    // TODO We should get this dynamically during build?
    String perm;

    File file = new File(path);
    if (file.getParent().endsWith(
        File.separator + Utils.getWindowsBinariesRelativePath()) ||
        file.getParent().endsWith(
        File.separator + Utils.getUNIXBinariesRelativePath()))
    {
      if (path.endsWith(".bat"))
      {
        perm = "644";
      }
      else
      {
        perm = "755";
      }
    }
    else if (path.endsWith(".sh"))
    {
      perm = "755";
    } else if (path.endsWith(Utils.getUnixSetupFileName()) ||
            path.endsWith(Utils.getUnixUninstallFileName()) ||
            path.endsWith(Utils.getUnixUpgradeFileName()))
    {
      perm = "755";
    } else
    {
      perm = "644";
    }
    return perm;
  }

}
