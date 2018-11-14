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
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.picocontainer.Startable;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;

public class ProjectLock implements Startable {
  private final DirectoryLock lock;

  public ProjectLock(AbstractProjectOrModule project) {
    Path directory = project.getWorkDir();
    try {
      if (!directory.toFile().exists()) {
        Files.createDirectories(directory);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create work directory", e);
    }
    this.lock = new DirectoryLock(directory.toAbsolutePath());
  }

  public void tryLock() {
    try {
      if (!lock.tryLock()) {
        failAlreadyInProgress(null);
      }
    } catch (OverlappingFileLockException e) {
      failAlreadyInProgress(e);
    }
  }

  private static void failAlreadyInProgress(Exception e) {
    throw new IllegalStateException("Another SonarQube analysis is already in progress for this project", e);
  }

  @Override
  public void stop() {
    lock.unlock();
  }

  @Override
  public void start() {
    // nothing to do
  }
}
