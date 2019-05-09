/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

public class ShutdownerTest {
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  
  private File srcFile;

  @Before
  public void setUp() throws Exception {
    srcFile = new File(Shutdowner.class.getProtectionDomain().getCodeSource().getLocation().toURI());
  }

  @Test
  public void detectHomeDir_assumes_Shutdowner_jar_is_in_lib_dir_root() {
    assertThat(Shutdowner.detectHomeDir())
      .isEqualTo(srcFile.getParentFile().getParentFile());
  }

  @Test
  public void loadPropertiesFile_fails_with_ISE_if_sonar_properties_not_in_conf_dir() throws IOException {
    File homeDir = temporaryFolder.newFolder();

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Configuration file not found: " + new File(new File(homeDir, "conf"), "sonar.properties").getAbsolutePath());

    Shutdowner.loadPropertiesFile(homeDir);
  }

  @Test
  public void loadPropertiesFile_reads_sonar_properties_content() throws IOException {
    File homeDir = temporaryFolder.newFolder();
    File confDir = new File(homeDir, "conf");
    confDir.mkdirs();
    File sonarProperties = new File(confDir, "sonar.properties");
    sonarProperties.createNewFile();
    Files.write(sonarProperties.toPath(), Arrays.asList("foo=bar"));

    Properties properties = Shutdowner.loadPropertiesFile(homeDir);

    assertThat(properties.getProperty("foo")).isEqualTo("bar");
  }

  @Test
  public void resolveTempDir_defaults_to_temp() {
    File file = Shutdowner.resolveTempDir(new Properties());

    assertThat(file).isEqualTo(new File("temp"));
  }

  @Test
  public void resolveTempDir_reads_relative_temp_dir_location_from_sonar_path_temp() {
    String tempDirPath = "blablabl";
    Properties properties = new Properties();
    properties.put("sonar.path.temp", tempDirPath);
    File file = Shutdowner.resolveTempDir(properties);

    assertThat(file).isEqualTo(new File(tempDirPath));
  }

  @Test
  public void resolveTempDir_reads_absolute_temp_dir_location_from_sonar_path_temp() throws IOException {
    File tempDirLocation = temporaryFolder.newFolder();
    Properties properties = new Properties();
    properties.put("sonar.path.temp", tempDirLocation.getAbsolutePath());
    File file = Shutdowner.resolveTempDir(properties);

    assertThat(file).isEqualTo(tempDirLocation);
  }

  @Test
  public void askForHardStop_write_right_bit_with_right_value_in_right_file() throws Exception {
    File tempFolder = temporaryFolder.newFolder();

    Shutdowner.askForHardStop(tempFolder);
    try (RandomAccessFile sharedMemory = new RandomAccessFile(new File(tempFolder, "sharedmemory"), "r")) {
      // Using values from org.sonar.process.ProcessCommands
      MappedByteBuffer mappedByteBuffer = sharedMemory.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, 50L * 10);

      assertThat(mappedByteBuffer.get(1)).isEqualTo((byte) 0xFF);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
