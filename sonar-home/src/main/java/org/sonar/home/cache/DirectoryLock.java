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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

public class DirectoryLock {
  static final String LOCK_FILE_NAME = ".sonar_lock";
  private final Path lockFilePath;
  private final Logger logger;

  private RandomAccessFile lockRandomAccessFile;
  private FileChannel lockChannel;
  private FileLock lockFile;

  public DirectoryLock(Path directory, Logger logger) {
    this.logger = logger;
    this.lockFilePath = directory.resolve(LOCK_FILE_NAME).toAbsolutePath();
  }

  public String getFileLockName() {
    return LOCK_FILE_NAME;
  }
  
  public void lock() {
    try {
      lockRandomAccessFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
      lockChannel = lockRandomAccessFile.getChannel();
      lockFile = lockChannel.lock(0, 1024, false);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create lock in " + lockFilePath.toString(), e);
    }
  }
  
  public boolean tryLock() {
    try {
      lockRandomAccessFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
      lockChannel = lockRandomAccessFile.getChannel();
      lockFile = lockChannel.tryLock(0, 1024, false);

      return lockFile != null;
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create lock in " + lockFilePath.toString(), e);
    }
  }

  public void unlock() {
    if (lockFile != null) {
      try {
        lockFile.release();
        lockFile = null;
      } catch (IOException e) {
        logger.error("Error releasing lock", e);
      }
    }
    if (lockChannel != null) {
      try {
        lockChannel.close();
        lockChannel = null;
      } catch (IOException e) {
        logger.error("Error closing file channel", e);
      }
    }
    if (lockRandomAccessFile != null) {
      try {
        lockRandomAccessFile.close();
        lockRandomAccessFile = null;
      } catch (IOException e) {
        logger.error("Error closing file", e);
      }
    }

    try {
      Files.delete(lockFilePath);
    } catch (IOException e) {
      // ignore, as an error happens if another process just started to acquire the same lock
      StringWriter errors = new StringWriter();
      e.printStackTrace(new PrintWriter(errors));
      logger.debug("Couldn't delete lock file: " + lockFilePath.toString() + " " + errors.toString());
    }
  }
}
