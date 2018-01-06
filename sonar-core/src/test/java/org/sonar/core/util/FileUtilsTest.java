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
package org.sonar.core.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.SystemUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class FileUtilsTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void cleanDirectory_throws_NPE_if_file_is_null() throws IOException {
    expectDirectoryCanNotBeNullNPE();

    FileUtils.cleanDirectory(null);
  }

  @Test
  public void cleanDirectory_does_nothing_if_argument_does_not_exist() throws IOException {
    FileUtils.cleanDirectory(new File("/a/b/ToDoSSS"));
  }

  @Test
  public void cleanDirectory_throws_IAE_if_argument_is_a_file() throws IOException {
    File file = temporaryFolder.newFile();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("'" + file.getAbsolutePath() + "' is not a directory");

    FileUtils.cleanDirectory(file);
  }

  @Test
  public void cleanDirectory_removes_directories_and_files_in_target_directory_but_not_target_directory() throws IOException {
    Path target = temporaryFolder.newFolder().toPath();
    Path childFile1 = Files.createFile(target.resolve("file1.txt"));
    Path childDir1 = Files.createDirectory(target.resolve("subDir1"));
    Path childFile2 = Files.createFile(childDir1.resolve("file2.txt"));
    Path childDir2 = Files.createDirectory(childDir1.resolve("subDir2"));

    assertThat(target).isDirectory();
    assertThat(childFile1).isRegularFile();
    assertThat(childDir1).isDirectory();
    assertThat(childFile2).isRegularFile();
    assertThat(childDir2).isDirectory();

    // on supporting FileSystem, target will change if directory is recreated
    Object targetKey = getFileKey(target);

    FileUtils.cleanDirectory(target.toFile());

    assertThat(target).isDirectory();
    assertThat(childFile1).doesNotExist();
    assertThat(childDir1).doesNotExist();
    assertThat(childFile2).doesNotExist();
    assertThat(childDir2).doesNotExist();
    assertThat(getFileKey(target)).isEqualTo(targetKey);
  }

  @Test
  public void cleanDirectory_follows_symlink_to_target_directory() throws IOException {
    assumeTrue(SystemUtils.IS_OS_UNIX);
    Path target = temporaryFolder.newFolder().toPath();
    Path symToDir = Files.createSymbolicLink(temporaryFolder.newFolder().toPath().resolve("sym_to_dir"), target);
    Path childFile1 = Files.createFile(target.resolve("file1.txt"));
    Path childDir1 = Files.createDirectory(target.resolve("subDir1"));
    Path childFile2 = Files.createFile(childDir1.resolve("file2.txt"));
    Path childDir2 = Files.createDirectory(childDir1.resolve("subDir2"));

    assertThat(target).isDirectory();
    assertThat(symToDir).isSymbolicLink();
    assertThat(childFile1).isRegularFile();
    assertThat(childDir1).isDirectory();
    assertThat(childFile2).isRegularFile();
    assertThat(childDir2).isDirectory();

    // on supporting FileSystem, target will change if directory is recreated
    Object targetKey = getFileKey(target);
    Object symLinkKey = getFileKey(symToDir);

    FileUtils.cleanDirectory(symToDir.toFile());

    assertThat(target).isDirectory();
    assertThat(symToDir).isSymbolicLink();
    assertThat(childFile1).doesNotExist();
    assertThat(childDir1).doesNotExist();
    assertThat(childFile2).doesNotExist();
    assertThat(childDir2).doesNotExist();
    assertThat(getFileKey(target)).isEqualTo(targetKey);
    assertThat(getFileKey(symToDir)).isEqualTo(symLinkKey);
  }

  @Test
  public void deleteQuietly_does_not_fail_if_argument_is_null() {
    FileUtils.deleteQuietly((File) null);
    FileUtils.deleteQuietly((Path) null);
  }

  @Test
  public void deleteQuietly_does_not_fail_if_file_does_not_exist() throws IOException {
    File file = new File(temporaryFolder.newFolder(), "blablabl");
    assertThat(file).doesNotExist();

    FileUtils.deleteQuietly(file);
  }

  @Test
  public void deleteQuietly_deletes_directory_and_content() throws IOException {
    Path target = temporaryFolder.newFolder().toPath();
    Path childFile1 = Files.createFile(target.resolve("file1.txt"));
    Path childDir1 = Files.createDirectory(target.resolve("subDir1"));
    Path childFile2 = Files.createFile(childDir1.resolve("file2.txt"));
    Path childDir2 = Files.createDirectory(childDir1.resolve("subDir2"));

    assertThat(target).isDirectory();
    assertThat(childFile1).isRegularFile();
    assertThat(childDir1).isDirectory();
    assertThat(childFile2).isRegularFile();
    assertThat(childDir2).isDirectory();

    FileUtils.deleteQuietly(target.toFile());

    assertThat(target).doesNotExist();
    assertThat(childFile1).doesNotExist();
    assertThat(childDir1).doesNotExist();
    assertThat(childFile2).doesNotExist();
    assertThat(childDir2).doesNotExist();
  }

  @Test
  public void deleteQuietly_deletes_symbolicLink() throws IOException {
    assumeTrue(SystemUtils.IS_OS_UNIX);
    Path folder = temporaryFolder.newFolder().toPath();
    Path file1 = Files.createFile(folder.resolve("file1.txt"));
    Path symLink = Files.createSymbolicLink(folder.resolve("link1"), file1);

    assertThat(file1).isRegularFile();
    assertThat(symLink).isSymbolicLink();

    FileUtils.deleteQuietly(symLink.toFile());

    assertThat(symLink).doesNotExist();
    assertThat(file1).isRegularFile();
  }

  @Test
  public void deleteDirectory_throws_NPE_if_argument_is_null() throws IOException {
    expectDirectoryCanNotBeNullNPE();

    FileUtils.deleteDirectory((File) null);
  }

  @Test
  public void deleteDirectory_does_not_fail_if_file_does_not_exist() throws IOException {
    File file = new File(temporaryFolder.newFolder(), "foo.d");

    FileUtils.deleteDirectory(file);
  }

  @Test
  public void deleteDirectory_throws_IOE_if_argument_is_a_file() throws IOException {
    File file = temporaryFolder.newFile();

    expectedException.expect(IOException.class);
    expectedException.expectMessage("Directory '" + file.getAbsolutePath() + "' is a file");

    FileUtils.deleteDirectory(file);
  }

  @Test
  public void deleteDirectory_throws_IOE_if_file_is_symbolicLink() throws IOException {
    assumeTrue(SystemUtils.IS_OS_UNIX);
    Path folder = temporaryFolder.newFolder().toPath();
    Path file1 = Files.createFile(folder.resolve("file1.txt"));
    Path symLink = Files.createSymbolicLink(folder.resolve("link1"), file1);

    assertThat(file1).isRegularFile();
    assertThat(symLink).isSymbolicLink();

    expectedException.expect(IOException.class);
    expectedException.expectMessage("Directory '" + symLink.toFile().getAbsolutePath() + "' is a symbolic link");

    FileUtils.deleteDirectory(symLink.toFile());
  }

  @Test
  public void deleteDirectory_deletes_directory_and_content() throws IOException {
    Path target = temporaryFolder.newFolder().toPath();
    Path childFile1 = Files.createFile(target.resolve("file1.txt"));
    Path childDir1 = Files.createDirectory(target.resolve("subDir1"));
    Path childFile2 = Files.createFile(childDir1.resolve("file2.txt"));
    Path childDir2 = Files.createDirectory(childDir1.resolve("subDir2"));

    assertThat(target).isDirectory();
    assertThat(childFile1).isRegularFile();
    assertThat(childDir1).isDirectory();
    assertThat(childFile2).isRegularFile();
    assertThat(childDir2).isDirectory();

    FileUtils.deleteQuietly(target.toFile());

    assertThat(target).doesNotExist();
    assertThat(childFile1).doesNotExist();
    assertThat(childDir1).doesNotExist();
    assertThat(childFile2).doesNotExist();
    assertThat(childDir2).doesNotExist();
  }

  @Test
  public void getPack200FilePath_transforms_jar_name() {
    Path jarPath = Paths.get("plugin.jar");
    assertThat(FileUtils.getPack200FilePath(jarPath)).isEqualTo(Paths.get("plugin.pack.gz"));
  }

  private void expectDirectoryCanNotBeNullNPE() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Directory can not be null");
  }

  @CheckForNull
  private static Object getFileKey(Path path) throws IOException {
    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
    return attrs.fileKey();
  }
}
