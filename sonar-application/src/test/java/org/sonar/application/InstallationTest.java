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
import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.MinimumViableEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class InstallationTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File homeDir, dataDir, webDir, logsDir;

  @Before
  public void before() throws IOException {
    homeDir = temp.newFolder();
    dataDir = new File(homeDir, "data");
    webDir = new File(homeDir, "web");
    logsDir = new File(homeDir, "logs");
  }

  @Test
  public void create_installation() throws Exception {
    FileUtils.forceMkdir(dataDir);
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);
    Properties rawProperties = new Properties();
    rawProperties.setProperty("foo", "bar");
    MinimumViableEnvironment mve = mock(MinimumViableEnvironment.class);

    Installation installation = new Installation(rawProperties, mve, homeDir);

    verify(mve).check();
    assertThat(installation.logsDir()).isEqualTo(logsDir);
    assertThat(installation.homeDir()).isEqualTo(homeDir);

    // create <HOME>/temp
    assertThat(installation.tempDir()).isDirectory().exists();
    assertThat(installation.tempDir().getName()).isEqualTo("temp");
    assertThat(installation.tempDir().getParentFile()).isEqualTo(homeDir);

    String startPath = installation.starPath("lib/search");
    assertThat(FilenameUtils.normalize(startPath, true))
      .endsWith("*")
      .startsWith(FilenameUtils.normalize(homeDir.getAbsolutePath(), true));
    assertThat(installation.props()).isNotNull();
    assertThat(installation.prop("foo")).isEqualTo("bar");
    assertThat(installation.prop("unknown")).isNull();

    // hardcoded property
    assertThat(installation.prop("sonar.path.home")).isEqualTo(homeDir.getAbsolutePath());

    // default properties
    assertThat(installation.prop("sonar.search.port")).isEqualTo("9001");
  }

  @Test
  public void fail_if_missing_required_directory() throws Exception {
    // <home>/data is missing
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);

    File dataDir = new File(homeDir, "data");
    try {
      new Installation(new Properties(), mock(MinimumViableEnvironment.class), homeDir);
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Property 'sonar.path.data' is not valid, directory does not exist: " + dataDir.getAbsolutePath());
    }

    try {
      FileUtils.touch(dataDir);
      new Installation(new Properties(), mock(MinimumViableEnvironment.class), homeDir);
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
    Installation installation = new Installation(rawProperties, mock(MinimumViableEnvironment.class), homeDir);
    assertThat(installation.prop("sonar.jdbc.username")).isEqualTo("angela");
    // command-line arguments override sonar.properties file
    assertThat(installation.prop("sonar.origin")).isEqualTo("raw");
  }

  @Test
  public void parse_arguments() throws Exception {
    String[] args = new String[] {"-Dsonar.foo=bar", "-Dsonar.whitespace=foo bar"};

    Properties p = Installation.argumentsToProperties(args);
    assertThat(p).hasSize(2);
    assertThat(p.getProperty("sonar.foo")).isEqualTo("bar");
    assertThat(p.getProperty("sonar.whitespace")).isEqualTo("foo bar");
  }

  @Test
  public void fail_to_parse_arguments_if_bad_format() throws Exception {
    String[] args = new String[] {"-Dsonar.foo=bar", "sonar.bad=true"};

    try {
      Installation.argumentsToProperties(args);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Command-line argument must start with -D, for example -Dsonar.jdbc.username=sonar. Got: sonar.bad=true");
    }
  }
}
