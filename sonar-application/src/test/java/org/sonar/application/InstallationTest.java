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
  public void check_minimum_viable_environment() throws Exception {
    FileUtils.forceMkdir(dataDir);
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);

    Properties initialProps = new Properties();
    initialProps.setProperty("foo", "bar");
    Installation installation = new Installation(new MinimumViableEnvironment(), homeDir, initialProps);
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
    assertThat(installation.prop("default", "value")).isEqualTo("value");
    assertThat(installation.prop("sonar.path.home")).isEqualTo(homeDir.getAbsolutePath());
  }

  @Test
  public void fail_if_missing_required_directory() throws Exception {
    // <home>/data is missing
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);

    File dataDir = new File(homeDir, "data");
    try {
      new Installation(new MinimumViableEnvironment(), homeDir, new Properties());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Property 'sonar.path.data' is not valid, directory does not exist: " + dataDir.getAbsolutePath());
    }

    try {
      FileUtils.touch(dataDir);
      new Installation(new MinimumViableEnvironment(), homeDir, new Properties());
      fail();
    } catch (IllegalStateException e) {
      assertThat(e.getMessage()).startsWith("Property 'sonar.path.data' is not valid, not a directory: " + dataDir.getAbsolutePath());
    }
  }

  @Test
  public void load_properties_file_if_exists() throws Exception {
    FileUtils.write(new File(homeDir, "conf/sonar.properties"), "sonar.jdbc.username=angela");
    FileUtils.forceMkdir(dataDir);
    FileUtils.forceMkdir(webDir);
    FileUtils.forceMkdir(logsDir);

    Installation installation = new Installation(new MinimumViableEnvironment(), homeDir, new Properties());
    assertThat(installation.prop("sonar.jdbc.username")).isEqualTo("angela");
  }
}
