/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Objects;
import org.sonar.api.batch.fs.internal.AbstractProjectOrModule;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.core.util.FileUtils;
import org.sonar.scanner.fs.InputModuleHierarchy;

/**
 * Clean and create working directories of each module, except the root.
 * Be careful that sub module work dir might be nested in parent working directory.
 */
public class WorkDirectoriesInitializer {
  public void execute(InputModuleHierarchy moduleHierarchy) {
    DefaultInputModule root = moduleHierarchy.root();
    // dont apply to root. Root is done by InputProjectProvider
    for (DefaultInputModule sub : moduleHierarchy.children(root)) {
      if (!Objects.equals(root.getWorkDir(), sub.getWorkDir())) {
        cleanAllWorkingDirs(moduleHierarchy, sub);
        mkdirsAllWorkingDirs(moduleHierarchy, sub);
      }
    }
  }

  public void execute(DefaultInputProject project) {
    cleanWorkingDir(project);
    mkdirWorkingDir(project);
  }

  private static void cleanAllWorkingDirs(InputModuleHierarchy moduleHierarchy, DefaultInputModule module) {
    for (DefaultInputModule sub : moduleHierarchy.children(module)) {
      cleanAllWorkingDirs(moduleHierarchy, sub);
    }
    cleanWorkingDir(module);
  }

  private static void cleanWorkingDir(AbstractProjectOrModule projectOrModule) {
    if (Files.exists(projectOrModule.getWorkDir())) {
      deleteAllRecursivelyExceptLockFile(projectOrModule.getWorkDir());
    }
  }

  private static void mkdirsAllWorkingDirs(InputModuleHierarchy moduleHierarchy, DefaultInputModule module) {
    for (DefaultInputModule sub : moduleHierarchy.children(module)) {
      mkdirsAllWorkingDirs(moduleHierarchy, sub);
    }
    mkdirWorkingDir(module);
  }

  private static void mkdirWorkingDir(AbstractProjectOrModule projectOrModule) {
    try {
      Files.createDirectories(projectOrModule.getWorkDir());
    } catch (Exception e) {
      throw new IllegalStateException("Fail to create working dir: " + projectOrModule.getWorkDir(), e);
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
