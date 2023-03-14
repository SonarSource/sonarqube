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
package org.sonar.scm.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitUtils {
  public static Git createRepository(Path worktree) throws IOException {
    Repository repo = FileRepositoryBuilder.create(worktree.resolve(".git").toFile());
    repo.create();
    return new Git(repo);
  }

  public static void createFile(String relativePath, String content, Path worktree) throws IOException {
    Path newFile = worktree.resolve(relativePath);
    Files.createDirectories(newFile.getParent());
    Files.write(newFile, content.getBytes(), StandardOpenOption.CREATE);
  }

  public static void createFile(Path worktree, String relativePath, String... lines) throws IOException {
    Path newFile = worktree.resolve(relativePath);
    Files.createDirectories(newFile.getParent());
    String content = String.join(System.lineSeparator(), lines) + System.lineSeparator();
    Files.write(newFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  public static void deleteFile(Path worktree, String relativePath) throws IOException {
    Path fileToDelete = worktree.resolve(relativePath);
    Files.delete(fileToDelete);
  }

  public static void copyFile(Path worktree, String origin, String dest) throws IOException {
    Path originPath = worktree.resolve(origin);
    Path destPath = worktree.resolve(dest);
    Files.copy(originPath, destPath);
  }

  public static void moveFile(Path worktree, String origin, String dest) throws IOException {
    Path originPath = worktree.resolve(origin);
    Path destPath = worktree.resolve(dest);
    Files.move(originPath, destPath);
  }


}
