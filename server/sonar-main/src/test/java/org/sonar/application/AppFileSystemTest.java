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
package org.sonar.application;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.sharedmemoryfile.AllProcessesCommands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessProperties.Property.PATH_DATA;
import static org.sonar.process.ProcessProperties.Property.PATH_HOME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.ProcessProperties.Property.PATH_TEMP;
import static org.sonar.process.ProcessProperties.Property.PATH_WEB;
import static org.sonar.process.sharedmemoryfile.ProcessCommands.MAX_PROCESSES;

public class AppFileSystemTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private File homeDir;
  private File dataDir;
  private File tempDir;
  private File logsDir;
  private File webDir;
  private TestAppSettings settings = new TestAppSettings();
  private AppFileSystem underTest = new AppFileSystem(settings);

  @Before
  public void before() throws IOException {
    homeDir = temp.newFolder();
    dataDir = new File(homeDir, "data");
    tempDir = new File(homeDir, "temp");
    logsDir = new File(homeDir, "logs");
    webDir = new File(homeDir, "web");

    settings.getProps().set(PATH_HOME.getKey(), homeDir.getAbsolutePath());
    settings.getProps().set(PATH_DATA.getKey(), dataDir.getAbsolutePath());
    settings.getProps().set(PATH_TEMP.getKey(), tempDir.getAbsolutePath());
    settings.getProps().set(PATH_LOGS.getKey(), logsDir.getAbsolutePath());
    settings.getProps().set(PATH_WEB.getKey(), webDir.getAbsolutePath());
  }

  @Test
  public void reset_creates_dirs_if_they_don_t_exist() throws Exception {
    assertThat(dataDir).doesNotExist();

    underTest.reset();

    assertThat(dataDir).exists().isDirectory();
    assertThat(logsDir).exists().isDirectory();
    assertThat(tempDir).exists().isDirectory();
    assertThat(webDir).exists().isDirectory();

    underTest.reset();

    assertThat(dataDir).exists().isDirectory();
    assertThat(logsDir).exists().isDirectory();
    assertThat(tempDir).exists().isDirectory();
    assertThat(webDir).exists().isDirectory();
  }

  @Test
  public void reset_deletes_content_of_temp_dir_but_not_temp_dir_itself_if_it_already_exists() throws Exception {
    assertThat(tempDir.mkdir()).isTrue();
    Object tempDirKey = getFileKey(tempDir);
    File fileInTempDir = new File(tempDir, "someFile.txt");
    assertThat(fileInTempDir.createNewFile()).isTrue();
    File subDirInTempDir = new File(tempDir, "subDir");
    assertThat(subDirInTempDir.mkdir()).isTrue();

    underTest.reset();

    assertThat(tempDir).exists();
    assertThat(fileInTempDir).doesNotExist();
    assertThat(subDirInTempDir).doesNotExist();
    assertThat(getFileKey(tempDir)).isEqualTo(tempDirKey);
  }

  @Test
  public void reset_deletes_content_of_temp_dir_but_not_sharedmemory_file() throws Exception {
    assertThat(tempDir.mkdir()).isTrue();
    File sharedmemory = new File(tempDir, "sharedmemory");
    assertThat(sharedmemory.createNewFile()).isTrue();
    FileUtils.write(sharedmemory, "toto");
    Object fileKey = getFileKey(sharedmemory);

    Object tempDirKey = getFileKey(tempDir);
    File fileInTempDir = new File(tempDir, "someFile.txt");
    assertThat(fileInTempDir.createNewFile()).isTrue();

    underTest.reset();

    assertThat(tempDir).exists();
    assertThat(fileInTempDir).doesNotExist();
    assertThat(getFileKey(tempDir)).isEqualTo(tempDirKey);
    assertThat(getFileKey(sharedmemory)).isEqualTo(fileKey);
    // content of sharedMemory file is reset
    assertThat(FileUtils.readFileToString(sharedmemory)).isNotEqualTo("toto");
  }

  @Test
  public void reset_cleans_the_sharedmemory_file() throws IOException {
    assertThat(tempDir.mkdir()).isTrue();
    try (AllProcessesCommands commands = new AllProcessesCommands(tempDir)) {
      for (int i = 0; i < MAX_PROCESSES; i++) {
        commands.create(i).setUp();
      }

      underTest.reset();

      for (int i = 0; i < MAX_PROCESSES; i++) {
        assertThat(commands.create(i).isUp()).isFalse();
      }
    }
  }

  @CheckForNull
  private static Object getFileKey(File fileInTempDir) throws IOException {
    Path path = Paths.get(fileInTempDir.toURI());
    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
    return attrs.fileKey();
  }

  @Test
  public void reset_throws_ISE_if_data_dir_is_a_file() throws Exception {
    resetThrowsISEIfDirIsAFile(PATH_DATA.getKey());
  }

  @Test
  public void reset_throws_ISE_if_web_dir_is_a_file() throws Exception {
    resetThrowsISEIfDirIsAFile(PATH_WEB.getKey());
  }

  @Test
  public void reset_throws_ISE_if_logs_dir_is_a_file() throws Exception {
    resetThrowsISEIfDirIsAFile(PATH_LOGS.getKey());
  }

  @Test
  public void reset_throws_ISE_if_temp_dir_is_a_file() throws Exception {
    resetThrowsISEIfDirIsAFile(PATH_TEMP.getKey());
  }

  private void resetThrowsISEIfDirIsAFile(String property) throws IOException {
    File file = new File(homeDir, "zoom.store");
    assertThat(file.createNewFile()).isTrue();
    settings.getProps().set(property, file.getAbsolutePath());

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property '" + property + "' is not valid, not a directory: " + file.getAbsolutePath());

    underTest.reset();
  }

  @Test
  public void fail_if_required_directory_is_a_file() throws Exception {
    // <home>/data is missing
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);
    FileUtils.touch(dataDir);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property 'sonar.path.data' is not valid, not a directory: " + dataDir.getAbsolutePath());

    underTest.reset();
  }

}
