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
package org.sonar.batch.scan;

import org.picocontainer.Startable;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProjectLock implements Startable {
  private static final Logger LOG = LoggerFactory.getLogger(ProjectLock.class);
  static final String LOCK_FILE_NAME = ".sonar_lock";
  private final Path lockFilePath;

  private RandomAccessFile lockRandomAccessFile;
  private FileChannel lockChannel;
  private FileLock lockFile;

  public ProjectLock(ProjectReactor projectReactor) {
    Path directory = projectReactor.getRoot().getBaseDir().toPath();
    this.lockFilePath = directory.resolve(LOCK_FILE_NAME).toAbsolutePath();
  }

  public void tryLock() {
    try {
      lockRandomAccessFile = new RandomAccessFile(lockFilePath.toFile(), "rw");
      lockChannel = lockRandomAccessFile.getChannel();
      lockFile = lockChannel.tryLock(0, 1024, false);

      if (lockFile == null) {
        failAlreadyInProgress(null);
      }
    } catch (OverlappingFileLockException e) {
      failAlreadyInProgress(e);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create project lock in " + lockFilePath.toString(), e);
    }
  }
  
  private static void failAlreadyInProgress(Exception e) {
    throw new IllegalStateException("Another SonarQube analysis is already in progress for this project", e);
  }

  @Override
  public void stop() {
    if (lockFile != null) {
      try {
        lockFile.release();
        lockFile = null;
      } catch (IOException e) {
        LOG.error("Error releasing lock", e);
      }
    }
    if (lockChannel != null) {
      try {
        lockChannel.close();
        lockChannel = null;
      } catch (IOException e) {
        LOG.error("Error closing file channel", e);
      }
    }
    if (lockRandomAccessFile != null) {
      try {
        lockRandomAccessFile.close();
        lockRandomAccessFile = null;
      } catch (IOException e) {
        LOG.error("Error closing file", e);
      }
    }
    
    try {
      Files.delete(lockFilePath);
    } catch (IOException e) {
      //ignore, as an error happens if another process just started to acquire the same lock
      LOG.debug("Couldn't delete lock file: " + lockFilePath.toString(), e);
    }
  }

  @Override
  public void start() {
    // nothing to do
  }

}
