/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.sonar.api.batch.fs.internal.InputModuleHierarchy;
import org.sonar.core.util.FileUtils;
import org.sonar.home.cache.DirectoryLock;

public class WorkDirectoryCleaner {
  private final Path workDir;

  public WorkDirectoryCleaner(InputModuleHierarchy moduleHierarchy) {
    workDir = moduleHierarchy.root().getWorkDir().toPath();
  }

  public void execute() {
    if (!workDir.toFile().exists()) {
      return;
    }

    try (DirectoryStream<Path> stream = list()) {

      Iterator<Path> it = stream.iterator();
      while (it.hasNext()) {
        FileUtils.deleteQuietly(it.next().toFile());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to clean working directory: " + workDir.toString(), e);
    }
  }

  private DirectoryStream<Path> list() throws IOException {
    return Files.newDirectoryStream(workDir, entry -> !DirectoryLock.LOCK_FILE_NAME.equals(entry.getFileName().toString()));
  }
}
