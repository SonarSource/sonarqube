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
package org.sonar.scanner.scan;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryLock {
  private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryLock.class);
  public static final String LOCK_FILE_NAME = ".sonar_lock";

  private final Path lockFilePath;

  private RandomAccessFile lockRandomAccessFile;
  private FileChannel lockChannel;
  private FileLock lockFile;

  public DirectoryLock(Path directory) {
    this.lockFilePath = directory.resolve(LOCK_FILE_NAME).toAbsolutePath();
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
        LOGGER.error("Error releasing lock", e);
      }
    }
    if (lockChannel != null) {
      try {
        lockChannel.close();
        lockChannel = null;
      } catch (IOException e) {
        LOGGER.error("Error closing file channel", e);
      }
    }
    if (lockRandomAccessFile != null) {
      try {
        lockRandomAccessFile.close();
        lockRandomAccessFile = null;
      } catch (IOException e) {
        LOGGER.error("Error closing file", e);
      }
    }
  }
}
