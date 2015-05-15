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
package org.sonar.server.startup;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JdbcDriverDeployerTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_deploy() throws Exception {
    DefaultServerFileSystem fs = mock(DefaultServerFileSystem.class);
    File driver = new File(Resources.getResource(getClass(), "JdbcDriverDeployerTest/deploy/my-driver.jar").toURI());
    Settings settings = new Settings();
    settings.setProperty("sonar.jdbc.driverPath", driver.getAbsolutePath());

    File deployDir = temp.newFolder("deploy");
    when(fs.getDeployDir()).thenReturn(deployDir);
    File deployedIndex = new File(deployDir, "jdbc-driver.txt");
    File deployedFile = new File(deployDir, "my-driver.jar");
    assertThat(deployedIndex).doesNotExist();
    assertThat(deployedFile).doesNotExist();
    when(fs.getDeployedJdbcDriverIndex()).thenReturn(deployedIndex);

    new JdbcDriverDeployer(fs, settings).start();

    assertThat(deployedIndex).exists();
    assertThat(deployedFile).exists();
    assertThat(deployedFile).hasContentEqualTo(driver);

    assertThat(Files.toString(deployedIndex, StandardCharsets.UTF_8)).isEqualTo("my-driver.jar|02b97f7bc37b2b68fc847fcc3fc1c156");
  }

  @Test
  public void dont_fail_when_medium_test() {
    Settings settings = new Settings();
    DefaultServerFileSystem fs = mock(DefaultServerFileSystem.class);

    // No sonar.jdbc.driverPath property

    new JdbcDriverDeployer(fs, settings).start();
  }
}
