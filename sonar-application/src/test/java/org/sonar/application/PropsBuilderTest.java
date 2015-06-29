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
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class PropsBuilderTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File homeDir;
  File dataDir;
  File webDir;
  File logsDir;
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

    assertThat(props.nonNullValueAsFile("sonar.path.logs")).isEqualTo(logsDir);
    assertThat(props.nonNullValueAsFile("sonar.path.home")).isEqualTo(homeDir);

    // create <HOME>/temp
    File tempDir = props.nonNullValueAsFile("sonar.path.temp");
    assertThat(tempDir).isDirectory().exists();
    assertThat(tempDir.getName()).isEqualTo("temp");
    assertThat(tempDir.getParentFile()).isEqualTo(homeDir);

    assertThat(props.value("foo")).isEqualTo("bar");
    assertThat(props.value("unknown")).isNull();

    // default properties
    assertThat(props.valueAsInt("sonar.search.port")).isEqualTo(9001);
  }

  @Test
  public void create_missing_required_directory() throws Exception {
    // <home>/data is missing
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);

    File dataDir = new File(homeDir, "data");
    new PropsBuilder(new Properties(), jdbcSettings, homeDir).build();
    assertThat(dataDir).isDirectory().exists();
  }

  @Test
  public void fail_if_required_directory_is_a_file() throws Exception {
    // <home>/data is missing
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);
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

    // properties loaded from file
    assertThat(props.value("sonar.jdbc.username")).isEqualTo("angela");

    // command-line arguments override sonar.properties file
    assertThat(props.value("sonar.origin")).isEqualTo("raw");
  }

  @Test
  public void utf8_file_encoding() throws Exception {
    FileUtils.write(new File(homeDir, "conf/sonar.properties"), "utf8prop=Thônes", StandardCharsets.UTF_8);
    Props props = new PropsBuilder(new Properties(), jdbcSettings, homeDir).build();
    assertThat(props.value("utf8prop")).isEqualTo("Thônes");
  }

  @Test
  public void do_not_load_properties_file_if_not_exists() throws Exception {
    FileUtils.forceMkdir(dataDir);
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);

    Properties rawProperties = new Properties();
    rawProperties.setProperty("sonar.origin", "raw");
    Props props = new PropsBuilder(rawProperties, jdbcSettings, homeDir).build();

    assertThat(props.value("sonar.origin")).isEqualTo("raw");
  }

  @Test
  public void detectHomeDir() throws Exception {
    assertThat(PropsBuilder.detectHomeDir()).isDirectory().exists();

  }
}
