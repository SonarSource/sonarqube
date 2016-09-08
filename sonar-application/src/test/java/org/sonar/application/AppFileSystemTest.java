/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import java.util.Properties;
import javax.annotation.CheckForNull;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.AllProcessesCommands;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessCommands.MAX_PROCESSES;

public class AppFileSystemTest {

  private static final String PROPERTY_SONAR_PATH_WEB = "sonar.path.web";
  private static final String PROPERTY_SONAR_PATH_DATA = "sonar.path.data";
  private static final String PROPERTY_SONAR_PATH_LOGS = "sonar.path.logs";
  private static final String PROPERTY_SONAR_PATH_TEMP = "sonar.path.temp";
  private static final String NON_DEFAULT_DATA_DIR_NAME = "toto";
  private static final String NON_DEFAULT_WEB_DIR_NAME = "tutu";
  private static final String NON_DEFAULT_LOGS_DIR_NAME = "titi";
  private static final String NON_DEFAULT_TEMP_DIR_NAME = "tatta";
  private static final String DEFAULT_DATA_DIR_NAME = "data";
  private static final String DEFAULT_WEB_DIR_NAME = "web";
  private static final String DEFAULT_LOGS_DIR_NAME = "logs";
  private static final String DEFAULT_TEMP_DIR_NAME = "temp";

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private File homeDir;
  private Properties properties;

  @Before
  public void before() throws IOException {
    homeDir = temp.newFolder();

    properties = new Properties();
    properties.setProperty("sonar.path.home", homeDir.getAbsolutePath());
  }

  @Test
  public void verifyProps_set_dir_path_absolute_based_on_home_dir_and_default_names_when_no_property() {
    Props props = new Props(properties);
    AppFileSystem underTest = new AppFileSystem(props);

    underTest.verifyProps();

    assertThat(props.nonNullValue(PROPERTY_SONAR_PATH_DATA)).isEqualTo(new File(homeDir, DEFAULT_DATA_DIR_NAME).getAbsolutePath());
    assertThat(props.nonNullValue(PROPERTY_SONAR_PATH_WEB)).isEqualTo(new File(homeDir, DEFAULT_WEB_DIR_NAME).getAbsolutePath());
    assertThat(props.nonNullValue(PROPERTY_SONAR_PATH_LOGS)).isEqualTo(new File(homeDir, DEFAULT_LOGS_DIR_NAME).getAbsolutePath());
    assertThat(props.nonNullValue(PROPERTY_SONAR_PATH_TEMP)).isEqualTo(new File(homeDir, DEFAULT_TEMP_DIR_NAME).getAbsolutePath());
  }

  @Test
  public void verifyProps_can_be_called_multiple_times() {
    AppFileSystem underTest = new AppFileSystem(new Props(properties));

    underTest.verifyProps();
    underTest.verifyProps();
  }

  @Test
  public void reset_throws_ISE_if_verifyProps_not_called_first() throws Exception {
    AppFileSystem underTest = new AppFileSystem(new Props(properties));

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("method verifyProps must be called first");

    underTest.reset();
  }

  @Test
  public void verifyProps_makes_dir_path_absolute_based_on_home_dir_when_relative() throws Exception {
    properties.setProperty(PROPERTY_SONAR_PATH_WEB, NON_DEFAULT_WEB_DIR_NAME);
    properties.setProperty(PROPERTY_SONAR_PATH_DATA, NON_DEFAULT_DATA_DIR_NAME);
    properties.setProperty(PROPERTY_SONAR_PATH_LOGS, NON_DEFAULT_LOGS_DIR_NAME);
    properties.setProperty(PROPERTY_SONAR_PATH_TEMP, NON_DEFAULT_TEMP_DIR_NAME);

    Props props = new Props(properties);
    AppFileSystem underTest = new AppFileSystem(props);

    underTest.verifyProps();

    assertThat(props.nonNullValue(PROPERTY_SONAR_PATH_DATA)).isEqualTo(new File(homeDir, NON_DEFAULT_DATA_DIR_NAME).getAbsolutePath());
    assertThat(props.nonNullValue(PROPERTY_SONAR_PATH_WEB)).isEqualTo(new File(homeDir, NON_DEFAULT_WEB_DIR_NAME).getAbsolutePath());
    assertThat(props.nonNullValue(PROPERTY_SONAR_PATH_LOGS)).isEqualTo(new File(homeDir, NON_DEFAULT_LOGS_DIR_NAME).getAbsolutePath());
    assertThat(props.nonNullValue(PROPERTY_SONAR_PATH_TEMP)).isEqualTo(new File(homeDir, NON_DEFAULT_TEMP_DIR_NAME).getAbsolutePath());
  }

  @Test
  public void reset_creates_dir_all_dirs_if_they_don_t_exist() throws Exception {
    AppFileSystem underTest = new AppFileSystem(new Props(properties));

    underTest.verifyProps();

    File dataDir = new File(homeDir, DEFAULT_DATA_DIR_NAME);
    File webDir = new File(homeDir, DEFAULT_WEB_DIR_NAME);
    File logsDir = new File(homeDir, DEFAULT_LOGS_DIR_NAME);
    File tempDir = new File(homeDir, DEFAULT_TEMP_DIR_NAME);
    assertThat(dataDir).doesNotExist();
    assertThat(webDir).doesNotExist();
    assertThat(logsDir).doesNotExist();
    assertThat(tempDir).doesNotExist();

    underTest.reset();

    assertThat(dataDir).exists().isDirectory();
    assertThat(webDir).exists().isDirectory();
    assertThat(logsDir).exists().isDirectory();
    assertThat(tempDir).exists().isDirectory();
  }

  @Test
  public void reset_deletes_content_of_temp_dir_but_not_temp_dir_itself_if_it_already_exists() throws Exception {
    File tempDir = new File(homeDir, DEFAULT_TEMP_DIR_NAME);
    assertThat(tempDir.mkdir()).isTrue();
    Object tempDirKey = getFileKey(tempDir);
    File fileInTempDir = new File(tempDir, "someFile.txt");
    assertThat(fileInTempDir.createNewFile()).isTrue();

    AppFileSystem underTest = new AppFileSystem(new Props(properties));
    underTest.verifyProps();
    underTest.reset();

    assertThat(tempDir).exists();
    assertThat(fileInTempDir).doesNotExist();
    assertThat(getFileKey(tempDir)).isEqualTo(tempDirKey);
  }

  @Test
  public void reset_deletes_content_of_temp_dir_but_not_sharedmemory_file() throws Exception {
    File tempDir = new File(homeDir, DEFAULT_TEMP_DIR_NAME);
    assertThat(tempDir.mkdir()).isTrue();
    File sharedmemory = new File(tempDir, "sharedmemory");
    assertThat(sharedmemory.createNewFile()).isTrue();
    FileUtils.write(sharedmemory, "toto");
    Object fileKey = getFileKey(sharedmemory);

    Object tempDirKey = getFileKey(tempDir);
    File fileInTempDir = new File(tempDir, "someFile.txt");
    assertThat(fileInTempDir.createNewFile()).isTrue();

    AppFileSystem underTest = new AppFileSystem(new Props(properties));
    underTest.verifyProps();
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
    File tempDir = new File(homeDir, DEFAULT_TEMP_DIR_NAME);
    assertThat(tempDir.mkdir()).isTrue();
    try (AllProcessesCommands commands = new AllProcessesCommands(tempDir)) {
      for (int i = 0; i < MAX_PROCESSES; i++) {
        commands.create(i).setUp();
      }

      AppFileSystem underTest = new AppFileSystem(new Props(properties));
      underTest.verifyProps();
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
    resetThrowsISEIfDirIsAFile(PROPERTY_SONAR_PATH_DATA);
  }

  @Test
  public void reset_throws_ISE_if_web_dir_is_a_file() throws Exception {
    resetThrowsISEIfDirIsAFile(PROPERTY_SONAR_PATH_WEB);
  }

  @Test
  public void reset_throws_ISE_if_logs_dir_is_a_file() throws Exception {
    resetThrowsISEIfDirIsAFile(PROPERTY_SONAR_PATH_LOGS);
  }

  @Test
  public void reset_throws_ISE_if_temp_dir_is_a_file() throws Exception {
    resetThrowsISEIfDirIsAFile(PROPERTY_SONAR_PATH_TEMP);
  }

  private void resetThrowsISEIfDirIsAFile(String property) throws IOException {
    File file = new File(homeDir, "zoom.store");
    assertThat(file.createNewFile()).isTrue();

    properties.setProperty(property, "zoom.store");

    AppFileSystem underTest = new AppFileSystem(new Props(properties));

    underTest.verifyProps();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Property '" + property + "' is not valid, not a directory: " + file.getAbsolutePath());

    underTest.reset();
  }

  //  @Test
//  public void fail_if_required_directory_is_a_file() throws Exception {
//    // <home>/data is missing
//    FileUtils.forceMkdir(webDir);
//    FileUtils.forceMkdir(logsDir);
//    try {
//      FileUtils.touch(dataDir);
//      new PropsBuilder(new Properties(), jdbcSettings, homeDir).build();
//      fail();
//    } catch (IllegalStateException e) {
//      assertThat(e.getMessage()).startsWith("Property 'sonar.path.data' is not valid, not a directory: " + dataDir.getAbsolutePath());
//    }
//  }

}
