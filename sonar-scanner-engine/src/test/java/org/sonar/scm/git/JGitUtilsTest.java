/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scm.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JGitUtilsTest {

  @TempDir
  Path rootModuleDir;

  @Test
  void getAllIgnoredPaths_ReturnsIgnoredFiles() throws Exception {
    setupTestDirectory();

    List<String> result = JGitUtils.getAllIgnoredPaths(rootModuleDir);

    // in directory1, the entire directory is ignored without listing each file
    // in directory2, specific files are ignored, so those files are listed
    // in directory3, specific files are ignored via a separate .gitignore file
    if (SystemUtils.IS_OS_WINDOWS) {
      assertThat(result).isEqualTo(List.of("directory1", "directory2\\file_a.txt", "directory3\\file_b.txt"));
    } else {
      assertThat(result).isEqualTo(List.of("directory1", "directory2/file_a.txt", "directory3/file_b.txt"));
    }
  }

  @Test
  void getIgnoredPaths_WithNonGitDirectory_ThrowsException() {
    assertThatThrownBy(() -> JGitUtils.getAllIgnoredPaths(rootModuleDir))
      .isInstanceOf(MessageException.class)
      .hasMessageStartingWith("Not inside a Git work tree: ");
  }

  @Test
  void getIgnoredPaths_WithDifferentBaseDir_ReturnsIgnoredFilesRelativeToBaseDir() throws Exception {
    Path baseDir = rootModuleDir.resolve("directory2");
    setupTestDirectory();

    List<String> result = JGitUtils.getAllIgnoredPaths(baseDir);

    assertThat(result).isEqualTo(List.of("file_a.txt"));
  }

  @Test
  void getIgnoredPaths_WithSubDirBaseDirContainingGitIgnore_ReturnsIgnoredFilesRelativeToBaseDir() throws Exception {
    Path baseDir = rootModuleDir.resolve("directory3");
    setupTestDirectory();

    List<String> result = JGitUtils.getAllIgnoredPaths(baseDir);

    assertThat(result).isEqualTo(List.of("file_b.txt"));
  }

  private void setupTestDirectory() throws GitAPIException, IOException {
    Git.init().setDirectory(rootModuleDir.toFile()).call();

    var directories = List.of("directory1", "directory2", "directory3");
    var fileNames = List.of("file_a.txt", "file_b.txt");

    for (String dir : directories) {
      Path directoryPath = rootModuleDir.resolve(dir);
      Files.createDirectories(directoryPath);
      for (String fileName : fileNames) {
        Files.write(directoryPath.resolve(fileName), "content".getBytes());
      }
    }

    Files.write(rootModuleDir.resolve(".gitignore"), "ignored.txt\ndirectory1\ndirectory2/file_a.txt".getBytes());
    Files.write(rootModuleDir.resolve("directory3/.gitignore"), "file_b.txt".getBytes());
  }
}
