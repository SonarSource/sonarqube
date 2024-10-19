/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

public class Files2Test {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Files2 underTest = spy(Files2.FILES2);

  @Test
  public void deleteIfExists_does_nothing_if_file_does_not_exist() throws Exception {
    File file = temp.newFile();
    assertThat(file.delete()).isTrue();

    underTest.deleteIfExists(file);
    assertThat(file).doesNotExist();
  }

  @Test
  public void deleteIfExists_deletes_directory() throws Exception {
    File dir = temp.newFolder();

    underTest.deleteIfExists(dir);
    assertThat(dir).doesNotExist();
  }

  @Test
  public void deleteIfExists_deletes_file() throws Exception {
    File file = temp.newFile();

    underTest.deleteIfExists(file);
    assertThat(file).doesNotExist();
  }

  @Test
  public void deleteIfExists_throws_ISE_on_error() throws Exception {
    File file = temp.newFile();
    doThrow(new IOException("failure")).when(underTest).deleteIfExistsOrThrowIOE(file);

    assertThatThrownBy(() -> underTest.deleteIfExists(file))
      .isInstanceOf(IllegalStateException.class);
  }

  @Test
  public void openInputStream_opens_existing_file() throws Exception {
    File file = temp.newFile();
    FileUtils.write(file, "foo");

    try (FileInputStream input = underTest.openInputStream(file)) {
      assertThat(IOUtils.toString(input)).isEqualTo("foo");
    }
  }

  @Test
  public void openInputStream_throws_ISE_if_file_does_not_exist() throws Exception {
    final File file = temp.newFile();
    assertThat(file.delete()).isTrue();

    assertThatThrownBy(() -> underTest.openInputStream(file))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Can not open file " + file)
      .hasRootCauseMessage("File " + file + " does not exist");
  }

  @Test
  public void openInputStream_throws_ISE_if_file_is_a_directory() throws Exception {
    File dir = temp.newFolder();

    assertThatThrownBy(() -> underTest.openInputStream(dir))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Can not open file " + dir)
      .hasRootCauseMessage("File " + dir + " exists but is a directory");
  }

  @Test
  public void openOutputStream_creates_file() throws Exception {
    File file = temp.newFile();
    assertThat(file.delete()).isTrue();

    try (FileOutputStream outputStream = underTest.openOutputStream(file, false)) {
      IOUtils.write("foo", outputStream);
    }
    assertThat(FileUtils.readFileToString(file)).isEqualTo("foo");
  }

  @Test
  public void openOutputStream_appends_bytes_to_existing_file() throws Exception {
    File file = temp.newFile();
    FileUtils.write(file, "foo");

    try (FileOutputStream outputStream = underTest.openOutputStream(file, true)) {
      IOUtils.write("bar", outputStream);
    }
    assertThat(FileUtils.readFileToString(file)).isEqualTo("foobar");
  }

  @Test
  public void openOutputStream_overwrites_existing_file() throws Exception {
    File file = temp.newFile();
    FileUtils.write(file, "foo");

    try (FileOutputStream outputStream = underTest.openOutputStream(file, false)) {
      IOUtils.write("bar", outputStream);
    }
    assertThat(FileUtils.readFileToString(file)).isEqualTo("bar");
  }

  @Test
  public void openOutputStream_throws_ISE_if_file_is_a_directory() throws Exception {
    File dir = temp.newFolder();

    assertThatThrownBy(() -> underTest.openOutputStream(dir, false))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Can not open file " + dir)
      .hasRootCauseMessage("File " + dir + " exists but is a directory");
  }

  @Test
  public void zipDir_an_existing_directory_then_unzipToDir() throws Exception {
    File dir = temp.newFolder();
    FileUtils.write(new File(dir, "foo.txt"), "foo");

    File zipFile = temp.newFile();
    underTest.zipDir(dir, zipFile);
    assertThat(zipFile).exists().isFile();

    File unzippedDir = temp.newFolder();
    underTest.unzipToDir(zipFile, unzippedDir);
    assertThat(FileUtils.readFileToString(new File(unzippedDir, "foo.txt"))).isEqualTo("foo");
  }

  @Test
  public void zipDir_throws_ISE_if_directory_does_not_exist() throws Exception {
    File dir = temp.newFolder();
    underTest.deleteIfExists(dir);

    assertThatThrownBy(() -> underTest.zipDir(dir, temp.newFile()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Can not zip directory " + dir)
      .hasRootCauseMessage("Directory " + dir + " does not exist");
  }

  @Test
  public void zipDir_throws_ISE_if_directory_is_a_file() throws Exception {
    File file = temp.newFile();

    assertThatThrownBy(() -> underTest.zipDir(file, temp.newFile()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Can not zip directory " + file)
      .hasRootCauseMessage("File " + file + " exists but is not a directory");
  }

  @Test
  public void createDir_creates_specified_directory() throws IOException {
    File dir = new File(temp.newFolder(), "someDir");

    assertThat(dir).doesNotExist();

    underTest.createDir(dir);

    assertThat(dir).exists();
  }

  @Test
  public void createDir_creates_specified_directory_and_missing_parents() throws IOException {
    File dir1 = new File(temp.newFolder(), "dir1");
    File dir2 = new File(dir1, "dir2");
    File dir = new File(dir2, "someDir");

    assertThat(dir1).doesNotExist();
    assertThat(dir2).doesNotExist();
    assertThat(dir).doesNotExist();

    underTest.createDir(dir);

    assertThat(dir1).exists();
    assertThat(dir2).exists();
    assertThat(dir).exists();
  }

  @Test
  public void createDir_throws_ISE_if_File_is_an_existing_file() throws IOException {
    File file = temp.newFile();

    assertThatThrownBy(() -> underTest.createDir(file))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(file.toPath() + " is not a directory");
  }

  @Test
  public void createDir_throws_ISE_if_File_is_an_existing_link() throws IOException {
    File file = Files.createLink(new File(temp.newFolder(), "toto.lnk").toPath(), temp.newFile().toPath()).toFile();

    assertThatThrownBy(() -> underTest.createDir(file))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(file.toPath() + " is not a directory");
  }
}
