/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.api.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @since 1.10
 */
public final class ZipUtils {

  private ZipUtils() {
    // only static methods
  }

  /**
   * Unzip a file into a new temporary directory. The directory is not deleted on JVM exit, so it
   * must be explicitely deleted. 
   * @return the temporary directory
   * @since 2.2
   */
  public static File unzipToTempDir(File zip) throws IOException {
    File toDir = TempFileUtils.createTempDirectory();
    unzip (zip, toDir);
    return toDir;
  }

  /**
   * Unzip a file into a directory. The directory is created if it does not exist.
   * @return the target directory
   */
  public static File unzip(File zip, File toDir) throws IOException {
    unzip(zip, toDir, new ZipEntryFilter() {
      public boolean accept(ZipEntry entry) {
        return true;
      }
    });
    return toDir;
  }

  public static void unzip(File zip, File toDir, ZipEntryFilter filter) throws IOException {
    if (!toDir.exists()) {
      FileUtils.forceMkdir(toDir);
    }

    ZipFile zipFile = new ZipFile(zip);
    try {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (filter.accept(entry)) {
          File to = new File(toDir, entry.getName());
          if (entry.isDirectory()) {
            if (!to.exists() && !to.mkdirs()) {
              throw new IOException("Error creating directory: " + to);
            }
          } else {
            File parent = to.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
              throw new IOException("Error creating directory: " + parent);
            }

            FileOutputStream fos = new FileOutputStream(to);
            try {
              IOUtils.copy(zipFile.getInputStream(entry), fos);
            } finally {
              IOUtils.closeQuietly(fos);
            }
          }
        }
      }
    } finally {
      zipFile.close();
    }
  }

  public static void zipDir(File dir, File zip) throws IOException {
    OutputStream out = null;
    ZipOutputStream zout = null;
    try {
      out = FileUtils.openOutputStream(zip);
      zout = new ZipOutputStream(out);
      zip(dir, zout, null);

    } finally {
      IOUtils.closeQuietly(zout);
      IOUtils.closeQuietly(out);
    }
  }


  private static void _zip(String entryName, InputStream in, ZipOutputStream out) throws IOException {
    ZipEntry zentry = new ZipEntry(entryName);
    out.putNextEntry(zentry);
    IOUtils.copy(in, out);
    out.closeEntry();
  }

  private static void _zip(String entryName, File file, ZipOutputStream out) throws IOException {
    if (file.isDirectory()) {
      entryName += '/';
      ZipEntry zentry = new ZipEntry(entryName);
      out.putNextEntry(zentry);
      out.closeEntry();
      File[] files = file.listFiles();
      for (int i = 0, len = files.length; i < len; i++) {
        _zip(entryName + files[i].getName(), files[i], out);
      }

    } else {
      InputStream in = null;
      try {
        in = new BufferedInputStream(new FileInputStream(file));
        _zip(entryName, in, out);
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
  }

  private static void zip(File file, ZipOutputStream out, String prefix) throws IOException {
    if (prefix != null) {
      int len = prefix.length();
      if (len == 0) {
        prefix = null;
      } else if (prefix.charAt(len - 1) != '/') {
        prefix += '/';
      }
    }
    for (File child : file.listFiles()) {
      String name = prefix != null ? prefix + child.getName() : child.getName();
      _zip(name, child, out);
    }
  }

  public static interface ZipEntryFilter {
    boolean accept(ZipEntry entry);
  }

}

