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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.SubmoduleAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.scm.git.Utils.javaUnzip;

public class GitIgnoreCommandTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void ignored_files_should_match_files_ignored_by_git() throws IOException {
    Path projectDir = temp.newFolder().toPath();
    javaUnzip("ignore-git.zip", projectDir.toFile());

    Path baseDir = projectDir.resolve("ignore-git");
    GitIgnoreCommand underTest = new GitIgnoreCommand();
    underTest.init(baseDir);

    assertThat(underTest.isIgnored(baseDir.resolve(".gitignore"))).isFalse();
    assertThat(underTest.isIgnored(baseDir.resolve("pom.xml"))).isFalse();
    assertThat(underTest.isIgnored(baseDir.resolve("src/main/java/org/dummy/.gitignore"))).isFalse();
    assertThat(underTest.isIgnored(baseDir.resolve("src/main/java/org/dummy/AnotherDummy.java"))).isFalse();
    assertThat(underTest.isIgnored(baseDir.resolve("src/test/java/org/dummy/AnotherDummyTest.java"))).isFalse();

    assertThat(underTest.isIgnored(baseDir.resolve("src/main/java/org/dummy/Dummy.java"))).isTrue();
    assertThat(underTest.isIgnored(baseDir.resolve("target"))).isTrue();
  }

  @Test
  public void test_pattern_on_deep_repo() throws Exception {
    Path projectDir = createGitRepoWithIgnore();
    int child_folders_per_folder = 2;
    int folder_depth = 10;
    createFolderStructure(projectDir, child_folders_per_folder, 0, folder_depth);

    logTester.setLevel(LoggerLevel.DEBUG);

    GitIgnoreCommand underTest = new GitIgnoreCommand();
    underTest.init(projectDir);

    assertThat(underTest
      .isIgnored(projectDir.resolve("folder_0_0/folder_1_0/folder_2_0/folder_3_0/folder_4_0/folder_5_0/folder_6_0/folder_7_0/folder_8_0/folder_9_0/Foo.java")))
      .isTrue();
    assertThat(underTest
      .isIgnored(projectDir.resolve("folder_0_0/folder_1_0/folder_2_0/folder_3_0/folder_4_0/folder_5_0/folder_6_0/folder_7_0/folder_8_0/folder_9_0/Foo.php")))
      .isFalse();

    int expectedIncludedFiles = (int) Math.pow(child_folders_per_folder, folder_depth) + 1; // The .gitignore file is indexed
    assertThat(logTester.logs(Level.DEBUG)).contains(expectedIncludedFiles + " non excluded files in this Git repository");
  }

  @Test
  public void include_submodules() throws IOException, GitAPIException {
    Path projectDir = temp.newFolder().toPath();
    Git git = Git.init().setDirectory(projectDir.toFile()).call();

    createSubmoduleWithFiles(git, "module1");

    Files.write(projectDir.resolve(".gitignore"), Arrays.asList("**/*.java"), UTF_8, TRUNCATE_EXISTING, CREATE);
    createFolderStructure(projectDir, 1, 0, 1);

    logTester.setLevel(LoggerLevel.DEBUG);

    GitIgnoreCommand underTest = new GitIgnoreCommand();
    underTest.init(projectDir);

    assertThat(underTest.isIgnored(projectDir.resolve("folder_0_0/Foo.java"))).isTrue();
    assertThat(underTest.isIgnored(projectDir.resolve("folder_0_0/Foo.php"))).isFalse();

    // also applies to files in submodule
    assertThat(underTest.isIgnored(projectDir.resolve("module1/folder_0_0/Foo.java"))).isTrue();
    assertThat(underTest.isIgnored(projectDir.resolve("module1/folder_0_0/Foo.php"))).isFalse();

    int expectedIncludedFiles = 6;
    assertThat(logTester.logs(Level.DEBUG)).contains(expectedIncludedFiles + " non excluded files in this Git repository");
  }

  @Test
  public void skip_submodules_if_not_cloned() throws IOException, GitAPIException {
    Path projectDir = temp.newFolder().toPath();
    Git git = Git.init().setDirectory(projectDir.toFile()).call();

    createSubmoduleWithFiles(git, "module1");

    Files.write(projectDir.resolve(".gitignore"), Arrays.asList("**/*.java"), UTF_8, TRUNCATE_EXISTING, CREATE);
    createFolderStructure(projectDir, 1, 0, 1);

    //clean submodule
    FileUtils.cleanDirectory(new File(projectDir.toString(), "module1"));

    logTester.setLevel(LoggerLevel.DEBUG);

    GitIgnoreCommand underTest = new GitIgnoreCommand();
    underTest.init(projectDir);

    assertThat(underTest.isIgnored(projectDir.resolve("folder_0_0/Foo.java"))).isTrue();
    assertThat(underTest.isIgnored(projectDir.resolve("folder_0_0/Foo.php"))).isFalse();

    // ignoring not cloned submodules
    assertThat(logTester.logs(Level.DEBUG)).contains("Git submodule [module1] found, but has not been cloned, skipping.");

    int expectedIncludedFiles = 3;
    assertThat(logTester.logs(Level.DEBUG)).contains(expectedIncludedFiles + " non excluded files in this Git repository");
  }

  @Test
  public void dont_index_files_outside_basedir() throws Exception {
    Path repoRoot = createGitRepoWithIgnore();

    int child_folders_per_folder = 2;
    int folder_depth = 10;
    createFolderStructure(repoRoot, child_folders_per_folder, 0, folder_depth);

    logTester.setLevel(LoggerLevel.DEBUG);

    GitIgnoreCommand underTest = new GitIgnoreCommand();
    // Define project baseDir as folder_0_0 so that folder_0_1 is excluded
    Path projectBasedir = repoRoot.resolve("folder_0_0");
    underTest.init(projectBasedir);

    assertThat(underTest
      .isIgnored(projectBasedir.resolve("folder_1_0/folder_2_0/folder_3_0/folder_4_0/folder_5_0/folder_6_0/folder_7_0/folder_8_0/folder_9_0/Foo.php")))
      .isFalse();
    assertThat(underTest
      .isIgnored(repoRoot.resolve("folder_0_1/folder_1_0/folder_2_0/folder_3_0/folder_4_0/folder_5_0/folder_6_0/folder_7_0/folder_8_0/folder_9_0/Foo.php")))
      .isTrue();

    int expectedIncludedFiles = (int) Math.pow(child_folders_per_folder, folder_depth - 1);
    assertThat(logTester.logs(Level.DEBUG)).contains(expectedIncludedFiles + " non excluded files in this Git repository");
  }

  private Path createGitRepoWithIgnore() throws IOException, GitAPIException {
    Path repoRoot = temp.newFolder().toPath();
    try (Git git = Git.init().setDirectory(repoRoot.toFile()).call()) {
    }
    Files.write(repoRoot.resolve(".gitignore"), Arrays.asList("**/*.java"), UTF_8, TRUNCATE_EXISTING, CREATE);
    return repoRoot;
  }

  private void createSubmoduleWithFiles(Git git, String path) throws IOException, GitAPIException {
    // create the other git repository
    Path subRoot = temp.newFolder().toPath();
    Files.write(subRoot.resolve(".gitignore"), Arrays.asList("**/*.java"), UTF_8, TRUNCATE_EXISTING, CREATE);
    createFolderStructure(subRoot, 1, 0, 1);

    try (Git subGit = Git.init().setDirectory(subRoot.toFile()).call()) {
      subGit.add().addFilepattern(".").call();
      subGit.commit().setMessage("first").call();
    }

    // add the other git repo as a submodule
    SubmoduleAddCommand addCommand = git.submoduleAdd()
      .setURI(subRoot.toUri().toString())
      .setPath(path);
    try (Repository module = addCommand.call()) {

    }
    git.submoduleUpdate().call();
  }

  private void createFolderStructure(Path current, int childCount, int currentDepth, int maxDepth) throws IOException {
    if (currentDepth >= maxDepth) {
      Path javaFile = current.resolve("Foo.java");
      Path phpFile = current.resolve("Foo.php");
      if (!Files.exists(phpFile)) {
        Files.createFile(phpFile);
      }
      if (!Files.exists(javaFile)) {
        Files.createFile(javaFile);
      }
      return;
    }
    for (int j = 0; j < childCount; j++) {
      Path newPath = current.resolve("folder_" + currentDepth + "_" + j);
      if (!Files.exists(newPath)) {
        Files.createDirectory(newPath);
      }
      createFolderStructure(newPath, childCount, currentDepth + 1, maxDepth);
    }
  }

}
