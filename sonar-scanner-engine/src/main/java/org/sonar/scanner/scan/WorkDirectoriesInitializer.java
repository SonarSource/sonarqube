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
package org.sonar.scanner.scan;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.core.util.FileUtils;
import org.sonar.home.cache.DirectoryLock;

/**
 * Clean and create working directories of each module.
 * Be careful that sub module work dir might be nested in parent working directory.
 */
public class WorkDirectoriesInitializer {

  private InputModuleHierarchy moduleHierarchy;

  public WorkDirectoriesInitializer(InputModuleHierarchy moduleHierarchy) {
    this.moduleHierarchy = moduleHierarchy;
  }

  public void execute() {
    cleanAllWorkingDirs(moduleHierarchy.root());
    mkdirsAllWorkingDirs(moduleHierarchy.root());
  }

  private void cleanAllWorkingDirs(DefaultInputModule module) {
    for (DefaultInputModule sub : moduleHierarchy.children(module)) {
      cleanAllWorkingDirs(sub);
    }
    if (Files.exists(module.getWorkDir())) {
      deleteAllRecursivelyExceptLockFile(module.getWorkDir());
    }
  }

  private void mkdirsAllWorkingDirs(DefaultInputModule module) {
    for (DefaultInputModule sub : moduleHierarchy.children(module)) {
      mkdirsAllWorkingDirs(sub);
    }
    try {
      Files.createDirectories(module.getWorkDir());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create working dir: " + module.getWorkDir(), e);
    }
  }

  private static void deleteAllRecursivelyExceptLockFile(Path dirToDelete) {
    try (DirectoryStream<Path> stream = list(dirToDelete)) {

      Iterator<Path> it = stream.iterator();
      while (it.hasNext()) {
        FileUtils.deleteQuietly(it.next().toFile());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to clean working directory: " + dirToDelete.toString(), e);
    }
  }

  private static DirectoryStream<Path> list(Path dir) throws IOException {
    return Files.newDirectoryStream(dir, entry -> !DirectoryLock.LOCK_FILE_NAME.equals(entry.getFileName().toString()));
  }
}
