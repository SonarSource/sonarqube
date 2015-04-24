/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.home.cache;

import org.apache.commons.io.FileUtils;
import org.sonar.home.log.Log;

import javax.annotation.CheckForNull;

import java.io.File;
import java.io.IOException;

/**
 * This class is responsible for managing Sonar batch file cache. You can put file into cache and
 * later try to retrieve them. MD5 is used to differentiate files (name is not secure as files may come
 * from different Sonar servers and have same name but be actually different, and same for SNAPSHOTs).
 */
public class FileCache {

  /** Maximum loop count when creating temp directories. */
  private static final int TEMP_DIR_ATTEMPTS = 10000;

  private final File dir, tmpDir;
  private final FileHashes hashes;
  private final Log log;

  FileCache(File dir, Log log, FileHashes fileHashes) {
    this.hashes = fileHashes;
    this.log = log;
    this.dir = createDir(dir, log, "user cache");
    log.info(String.format("User cache: %s", dir.getAbsolutePath()));
    this.tmpDir = createDir(new File(dir, "_tmp"), log, "temp dir");
  }

  public static FileCache create(File dir, Log log) {
    return new FileCache(dir, log, new FileHashes());
  }

  public File getDir() {
    return dir;
  }

  /**
   * Look for a file in the cache by its filename and md5 checksum. If the file is not
   * present then return null.
   */
  @CheckForNull
  public File get(String filename, String hash) {
    File cachedFile = new File(new File(dir, hash), filename);
    if (cachedFile.exists()) {
      return cachedFile;
    }
    log.debug(String.format("No file found in the cache with name %s and hash %s", filename, hash));
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
      File tempFile = newTempFile();
      download(downloader, filename, tempFile);
      String downloadedHash = hashes.of(tempFile);
      if (!hash.equals(downloadedHash)) {
        throw new IllegalStateException("INVALID HASH: File " + tempFile.getAbsolutePath() + " was expected to have hash " + hash
          + " but was downloaded with hash " + downloadedHash);
      }
      mkdirQuietly(hashDir);
      renameQuietly(tempFile, targetFile);
    }
    return targetFile;
  }

  private void download(Downloader downloader, String filename, File tempFile) {
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
      log.warn(String.format("Unable to rename %s to %s", sourceFile.getAbsolutePath(), targetFile.getAbsolutePath()));
      log.warn(String.format("A copy/delete will be tempted but with no garantee of atomicity"));
      try {
        FileUtils.moveFile(sourceFile, targetFile);
      } catch (IOException e) {
        throw new IllegalStateException("Fail to move " + sourceFile.getAbsolutePath() + " to " + targetFile, e);
      }
    }
  }

  private File hashDir(String hash) {
    return new File(dir, hash);
  }

  private void mkdirQuietly(File hashDir) {
    try {
      FileUtils.forceMkdir(hashDir);
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

  private File createDir(File dir, Log log, String debugTitle) {
    if (!dir.isDirectory() || !dir.exists()) {
      log.debug("Create : " + dir.getAbsolutePath());
      try {
        FileUtils.forceMkdir(dir);
      } catch (IOException e) {
        throw new IllegalStateException("Unable to create " + debugTitle + dir.getAbsolutePath(), e);
      }
    }
    return dir;
  }
}
