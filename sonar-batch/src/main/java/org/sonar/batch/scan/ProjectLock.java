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

import org.sonar.batch.bootstrap.Slf4jLogger;

import org.sonar.home.cache.DirectoryLock;
import org.picocontainer.Startable;
import org.sonar.api.batch.bootstrap.ProjectReactor;

import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;

public class ProjectLock implements Startable {
  static final String LOCK_FILE_NAME = ".sonar_lock";

  private DirectoryLock lock;

  public ProjectLock(ProjectReactor projectReactor) {
    Path directory = projectReactor.getRoot().getBaseDir().toPath();
    this.lock = new DirectoryLock(directory.toAbsolutePath(), new Slf4jLogger());
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
