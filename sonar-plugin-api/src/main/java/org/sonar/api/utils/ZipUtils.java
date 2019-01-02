/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * Utility to zip directories and unzip files.
 *
 * @since 1.10
 */
public final class ZipUtils {

  private static final String ERROR_CREATING_DIRECTORY = "Error creating directory: ";

  private ZipUtils() {
    // only static methods
  }

  /**
   * Unzip a file into a directory. The directory is created if it does not exist.
   *
   * @return the target directory
   */
  public static File unzip(File zip, File toDir) throws IOException {
    return unzip(zip, toDir, (Predicate<ZipEntry>) ze -> true);
  }

  public static File unzip(InputStream zip, File toDir) throws IOException {
    return unzip(zip, toDir, (Predicate<ZipEntry>) ze -> true);
  }

  /**
   * @deprecated replaced by {@link #unzip(InputStream, File, Predicate)} in 6.2.
   */
  @Deprecated
  public static File unzip(InputStream stream, File toDir, ZipEntryFilter filter) throws IOException {
    return unzip(stream, toDir, new ZipEntryFilterDelegate(filter));
  }

  /**
   * Unzip a file to a directory.
   *
   * @param stream the zip input file
   * @param toDir  the target directory. It is created if needed.
   * @param filter filter zip entries so that only a subset of directories/files can be
   *               extracted to target directory.
   * @return the parameter {@code toDir}
   * @since 6.2
   */
  public static File unzip(InputStream stream, File toDir, Predicate<ZipEntry> filter) throws IOException {
    if (!toDir.exists()) {
      FileUtils.forceMkdir(toDir);
    }

    Path targetDirNormalizedPath = toDir.toPath().normalize();
    try (ZipInputStream zipStream = new ZipInputStream(stream)) {
      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        if (filter.test(entry)) {
          unzipEntry(entry, zipStream, targetDirNormalizedPath);
        }
      }
      return toDir;
    }
  }

  private static void unzipEntry(ZipEntry entry, ZipInputStream zipStream, Path targetDirNormalized) throws IOException {
    File to = targetDirNormalized.resolve(entry.getName()).toFile();
    verifyInsideTargetDirectory(entry, to.toPath(), targetDirNormalized);

    if (entry.isDirectory()) {
      throwExceptionIfDirectoryIsNotCreatable(to);
    } else {
      File parent = to.getParentFile();
      throwExceptionIfDirectoryIsNotCreatable(parent);
      copy(zipStream, to);
    }
  }

  private static void throwExceptionIfDirectoryIsNotCreatable(File to) throws IOException {
    if (!to.exists() && !to.mkdirs()) {
      throw new IOException(ERROR_CREATING_DIRECTORY + to);
    }
  }

  /**
   * @deprecated replaced by {@link #unzip(File, File, Predicate)} in 6.2.
   */
  @Deprecated
  public static File unzip(File zip, File toDir, ZipEntryFilter filter) throws IOException {
    return unzip(zip, toDir, new ZipEntryFilterDelegate(filter));
  }

  /**
   * Unzip a file to a directory.
   *
   * @param zip    the zip file. It must exist.
   * @param toDir  the target directory. It is created if needed.
   * @param filter filter zip entries so that only a subset of directories/files can be
   *               extracted to target directory.
   * @return the parameter {@code toDir}
   * @since 6.2
   */
  public static File unzip(File zip, File toDir, Predicate<ZipEntry> filter) throws IOException {
    if (!toDir.exists()) {
      FileUtils.forceMkdir(toDir);
    }

    Path targetDirNormalizedPath = toDir.toPath().normalize();
    try (ZipFile zipFile = new ZipFile(zip)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (filter.test(entry)) {
          File target = new File(toDir, entry.getName());

          verifyInsideTargetDirectory(entry, target.toPath(), targetDirNormalizedPath);

          if (entry.isDirectory()) {
            throwExceptionIfDirectoryIsNotCreatable(target);
          } else {
            File parent = target.getParentFile();
            throwExceptionIfDirectoryIsNotCreatable(parent);
            copy(zipFile, entry, target);
          }
        }
      }
      return toDir;
    }
  }

  private static void copy(ZipInputStream zipStream, File to) throws IOException {
    try (OutputStream fos = new FileOutputStream(to)) {
      IOUtils.copy(zipStream, fos);
    }
  }

  private static void copy(ZipFile zipFile, ZipEntry entry, File to) throws IOException {
    try (InputStream input = zipFile.getInputStream(entry); OutputStream fos = new FileOutputStream(to)) {
      IOUtils.copy(input, fos);
    }
  }

  public static void zipDir(File dir, File zip) throws IOException {
    try (OutputStream out = FileUtils.openOutputStream(zip);
      ZipOutputStream zout = new ZipOutputStream(out)) {
      doZipDir(dir, zout);
    }
  }

  private static void doZip(String entryName, InputStream in, ZipOutputStream out) throws IOException {
    ZipEntry entry = new ZipEntry(entryName);
    out.putNextEntry(entry);
    IOUtils.copy(in, out);
    out.closeEntry();
  }

  private static void doZip(String entryName, File file, ZipOutputStream out) throws IOException {
    if (file.isDirectory()) {
      entryName += "/";
      ZipEntry entry = new ZipEntry(entryName);
      out.putNextEntry(entry);
      out.closeEntry();
      File[] files = file.listFiles();
      // java.io.File#listFiles() returns null if object is a directory (not possible here) or if
      // an I/O error occurs (weird!)
      if (files == null) {
        throw new IllegalStateException("Fail to list files of directory " + file.getAbsolutePath());
      }
      for (File f : files) {
        doZip(entryName + f.getName(), f, out);
      }

    } else {
      try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
        doZip(entryName, in, out);
      }
    }
  }

  private static void doZipDir(File dir, ZipOutputStream out) throws IOException {
    File[] children = dir.listFiles();
    if (children == null) {
      throw new IllegalStateException("Fail to list files of directory " + dir.getAbsolutePath());
    }
    for (File child : children) {
      doZip(child.getName(), child, out);
    }
  }

  private static void verifyInsideTargetDirectory(ZipEntry entry, Path entryPath, Path targetDirNormalizedPath) {
    if (!entryPath.normalize().startsWith(targetDirNormalizedPath)) {
      // vulnerability - trying to create a file outside the target directory
      throw new IllegalStateException("Unzipping an entry outside the target directory is not allowed: " + entry.getName());
    }
  }

  /**
   * @see #unzip(File, File, Predicate)
   * @deprecated replaced by {@link Predicate<ZipEntry>} in 6.2.
   */
  @Deprecated
  @FunctionalInterface
  public interface ZipEntryFilter {
    boolean accept(ZipEntry entry);
  }

  private static class ZipEntryFilterDelegate implements Predicate<ZipEntry> {
    private final ZipEntryFilter delegate;

    private ZipEntryFilterDelegate(ZipEntryFilter delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean test(ZipEntry zipEntry) {
      return delegate.accept(zipEntry);
    }
  }
}
