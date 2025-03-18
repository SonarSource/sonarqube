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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.Git;
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
    Git.init().setDirectory(rootModuleDir.toFile()).call();
    Files.createDirectories(rootModuleDir.resolve("directory1"));
    Files.createDirectories(rootModuleDir.resolve("directory2"));
    Files.createDirectories(rootModuleDir.resolve("directory3"));
    Files.write(rootModuleDir.resolve("directory1/file_a.txt"), "content".getBytes());
    Files.write(rootModuleDir.resolve("directory1/file_b.txt"), "content".getBytes());
    Files.write(rootModuleDir.resolve("directory2/file_a.txt"), "content".getBytes());
    Files.write(rootModuleDir.resolve("directory2/file_b.txt"), "content".getBytes());
    Files.write(rootModuleDir.resolve("directory3/file_a.txt"), "content".getBytes());
    Files.write(rootModuleDir.resolve("directory3/file_b.txt"), "content".getBytes());
    Files.write(rootModuleDir.resolve(".gitignore"), "ignored.txt\ndirectory1\ndirectory2/file_a.txt".getBytes());
    Files.write(rootModuleDir.resolve("directory3/.gitignore"), "file_b.txt".getBytes());

    List<String> result = JGitUtils.getAllIgnoredPaths(rootModuleDir);

    // in directory1, the entire directory is ignored without listing each file
    // in directory2, specific files are ignored, so those files are listed
    // in directory3, specific files are ignored via a separate .gitignore file
    assertThat(result).isEqualTo(List.of("directory1", "directory2/file_a.txt", "directory3/file_b.txt"));
  }

  @Test
  void getIgnoredPaths_WithNonGitDirectory_ThrowsException() {
    assertThatThrownBy(() -> JGitUtils.getAllIgnoredPaths(rootModuleDir))
      .isInstanceOf(MessageException.class)
      .hasMessageStartingWith("Not inside a Git work tree: ");
  }
}
