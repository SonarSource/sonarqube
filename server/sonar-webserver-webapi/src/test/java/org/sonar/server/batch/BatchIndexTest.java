/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.batch;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.CharUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.platform.ServerFileSystem;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchIndexTest {

  @TempDir
  Path temp;

  private File jar;

  private final ServerFileSystem fs = mock(ServerFileSystem.class);

  @BeforeEach
  void prepare_fs() throws IOException {
    Path homeDir = Files.createTempDirectory(temp, "homeDir");
    when(fs.getHomeDir()).thenReturn(homeDir.toFile());

    Path batchDir = homeDir.resolve("lib/scanner");
    Files.createDirectories(batchDir);
    jar = batchDir.resolve("sonar-batch.jar").toFile();
    FileUtils.writeByteArrayToFile(batchDir.resolve("sonar-batch.jar").toFile(), "foo".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void get_index() {
    BatchIndex batchIndex = new BatchIndex(fs);
    batchIndex.start();

    String index = batchIndex.getIndex();
    assertThat(index).isEqualTo("sonar-batch.jar|acbd18db4cc2f85cedef654fccc4a4d8" + CharUtils.LF);

    batchIndex.stop();
  }

  @Test
  void get_file() {
    BatchIndex batchIndex = new BatchIndex(fs);
    batchIndex.start();

    File file = batchIndex.getFile("sonar-batch.jar");
    assertThat(file).isEqualTo(jar);
  }

  /**
   * Do not allow to download files located outside the directory lib/batch, for example
   * /etc/passwd
   */
  @Test
  void check_location_of_file() {
    BatchIndex batchIndex = new BatchIndex(fs);
    batchIndex.start();
    assertThatThrownBy(() -> batchIndex.getFile("../sonar-batch.jar"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Bad filename: ../sonar-batch.jar");
  }

  @Test
  void file_does_not_exist() {
    BatchIndex batchIndex = new BatchIndex(fs);
    batchIndex.start();
    assertThatThrownBy(() -> batchIndex.getFile("other.jar"))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Bad filename: other.jar");
  }

  @Test
  void start_whenBatchDirDoesntExist_shouldThrow() throws IOException {
    Path homeDir = Files.createTempDirectory(temp, "homeDir");
    when(fs.getHomeDir()).thenReturn(homeDir.toFile());

    BatchIndex batchIndex = new BatchIndex(fs);
    
    // Ensure that the file separator is correct based on the OS
    String expectedMessage = format("%s%slib%sscanner folder not found",
      homeDir.toFile().getAbsolutePath(), File.separator, File.separator);
    assertThatThrownBy(batchIndex::start)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage(expectedMessage);
  }
}
