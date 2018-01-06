/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.home.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;
import javax.annotation.CheckForNull;

/**
 * This class is responsible for managing Sonar batch file cache. You can put file into cache and
 * later try to retrieve them. MD5 is used to differentiate files (name is not secure as files may come
 * from different Sonar servers and have same name but be actually different, and same for SNAPSHOTs).
 */
public class FileCache {

  /** Maximum loop count when creating temp directories. */
  private static final int TEMP_DIR_ATTEMPTS = 10_000;

  private final File cacheDir;
  private final File tmpDir;
  private final FileHashes hashes;
  private final Logger logger;

  FileCache(File dir, FileHashes fileHashes, Logger logger) {
    this.hashes = fileHashes;
    this.logger = logger;
    this.cacheDir = createDir(dir, "user cache: ");
    logger.info(String.format("User cache: %s", dir.getAbsolutePath()));
    this.tmpDir = createDir(new File(dir, "_tmp"), "temp dir");
  }

  public static FileCache create(File dir, Logger logger) {
    return new FileCache(dir, new FileHashes(), logger);
  }

  public File getDir() {
    return cacheDir;
  }

  /**
   * Look for a file in the cache by its filename and md5 checksum. If the file is not
   * present then return null.
   */
  @CheckForNull
  public File get(String filename, String hash) {
    File cachedFile = new File(new File(cacheDir, hash), filename);
    if (cachedFile.exists()) {
      return cachedFile;
    }
    logger.debug(String.format("No file found in the cache with name %s and hash %s", filename, hash));
    return null;
  }

  public interface Downloader {
    void download(String filename, File toFile) throws IOException;
  }

  public File get(String filename, String hash, Downloader downloader) {
    // Does not fail if another process tries to create the directory at the same time.
    File hashDir = hashDir(hash);
    File targetFile = new File(hashDir, filename);
    if (!targetFile.exists()) {
      cacheMiss(targetFile, hash, downloader);
    }
    return targetFile;
  }

  private void cacheMiss(File targetFile, String expectedHash, Downloader downloader) {
    File tempFile = newTempFile();
    download(downloader, targetFile.getName(), tempFile);
    String downloadedHash = hashes.of(tempFile);
    if (!expectedHash.equals(downloadedHash)) {
      throw new IllegalStateException("INVALID HASH: File " + tempFile.getAbsolutePath() + " was expected to have hash " + expectedHash
        + " but was downloaded with hash " + downloadedHash);
    }
    mkdirQuietly(targetFile.getParentFile());
    renameQuietly(tempFile, targetFile);
  }

  public File getCompressed(String filename, String hash, Downloader downloader) {
    File hashDir = hashDir(hash);
    File compressedFile = new File(hashDir, filename);
    File jarFile = new File(compressedFile.getParentFile(), getUnpackedFileName(compressedFile.getName()));

    if (!jarFile.exists()) {
      if (!compressedFile.exists()) {
        cacheMiss(compressedFile, hash, downloader);
      }
      File tempFile = newTempFile();
      unpack200(compressedFile.toPath(), tempFile.toPath());
      renameQuietly(tempFile, jarFile);
    }
    return jarFile;
  }

  private static String getUnpackedFileName(String packedName) {
    return packedName.substring(0, packedName.length() - 7) + "jar";
  }

  private void unpack200(Path compressedFile, Path jarFile) {
    logger.debug("Unpacking plugin " + compressedFile);
    Pack200.Unpacker unpacker = Pack200.newUnpacker();
    try {
      try (JarOutputStream jarStream = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(jarFile)));
        InputStream in = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(compressedFile)))) {
        unpacker.unpack(in, jarStream);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static void download(Downloader downloader, String filename, File tempFile) {
    try {
      downloader.download(filename, tempFile);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to download " + filename + " to " + tempFile, e);
    }
  }

  private void renameQuietly(File sourceFile, File targetFile) {
    boolean rename = sourceFile.renameTo(targetFile);
    // Check if the file was cached by another process during download
    if (!rename && !targetFile.exists()) {
      logger.warn(String.format("Unable to rename %s to %s", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath()));
      logger.warn("A copy/delete will be tempted but with no guarantee of atomicity");
      try {
        Files.move(sourceFile.toPath(), targetFile.toPath());
      } catch (IOException e) {
        throw new IllegalStateException("Fail to move " + sourceFile.getAbsolutePath() + " to " + targetFile, e);
      }
    }
  }

  private File hashDir(String hash) {
    return new File(cacheDir, hash);
  }

  private static void mkdirQuietly(File hashDir) {
    try {
      Files.createDirectories(hashDir.toPath());
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create cache directory: " + hashDir, e);
    }
  }

  private File newTempFile() {
    try {
      return File.createTempFile("fileCache", null, tmpDir);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to create temp file in " + tmpDir, e);
    }
  }

  public File createTempDir() {
    String baseName = System.currentTimeMillis() + "-";

    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(tmpDir, baseName + counter);
      if (tempDir.mkdir()) {
        return tempDir;
      }
    }
    throw new IllegalStateException("Failed to create directory in " + tmpDir);
  }

  private File createDir(File dir, String debugTitle) {
    if (!dir.isDirectory() || !dir.exists()) {
      logger.debug("Create : " + dir.getAbsolutePath());
      try {
        Files.createDirectories(dir.toPath());
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create " + debugTitle + dir.getAbsolutePath(), e);
      }
    }
    return dir;
  }
}
