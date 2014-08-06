/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.application;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.Props;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;

public class PropsBuilderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File homeDir, dataDir, webDir, logsDir;
  JdbcSettings jdbcSettings = mock(JdbcSettings.class);

  @Before
  public void before() throws IOException {
    homeDir = temp.newFolder();
    dataDir = new File(homeDir, "data");
    webDir = new File(homeDir, "web");
    logsDir = new File(homeDir, "logs");
  }

  @Test
  public void build_props() throws Exception {
    FileUtils.forceMkdir(dataDir);
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);
    Properties rawProperties = new Properties();
    rawProperties.setProperty("foo", "bar");

    Props props = new PropsBuilder(rawProperties, jdbcSettings, homeDir).build();

    assertThat(props.fileOf("sonar.path.logs")).isEqualTo(logsDir);
    assertThat(props.fileOf("sonar.path.home")).isEqualTo(homeDir);

    // create <HOME>/temp
    File tempDir = props.fileOf("sonar.path.temp");
    assertThat(tempDir).isDirectory().exists();
    assertThat(tempDir.getName()).isEqualTo("temp");
    assertThat(tempDir.getParentFile()).isEqualTo(homeDir);

    assertThat(props.of("foo")).isEqualTo("bar");
    assertThat(props.of("unknown")).isNull();

    // default properties
    assertThat(props.intOf("sonar.search.port")).isEqualTo(9001);
  }

  @Test
  public void fail_if_missing_required_directory() throws Exception {
    // <home>/data is missing
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);

    File dataDir = new File(homeDir, "data");
    try {
      new PropsBuilder(new Properties(), jdbcSettings, homeDir).build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Property 'sonar.path.data' is not valid, directory does not exist: " + dataDir.getAbsolutePath());
    }

    try {
      FileUtils.touch(dataDir);
      new PropsBuilder(new Properties(), jdbcSettings, homeDir).build();
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Property 'sonar.path.data' is not valid, not a directory: " + dataDir.getAbsolutePath());
    }
  }

  @Test
  public void load_properties_file_if_exists() throws Exception {
    FileUtils.write(new File(homeDir, "conf/sonar.properties"), "sonar.jdbc.username=angela\nsonar.origin=file");
    FileUtils.forceMkdir(dataDir);
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);

    Properties rawProperties = new Properties();
    rawProperties.setProperty("sonar.origin", "raw");
    Props props = new PropsBuilder(rawProperties, jdbcSettings, homeDir).build();

    assertThat(props.of("sonar.jdbc.username")).isEqualTo("angela");
    // command-line arguments override sonar.properties file
    assertThat(props.of("sonar.origin")).isEqualTo("raw");
  }
}
