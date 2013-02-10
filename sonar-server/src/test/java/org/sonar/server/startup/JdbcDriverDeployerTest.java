/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.startup;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.test.TestUtils;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JdbcDriverDeployerTest {

  @Test
  public void test_deploy() throws Exception {
    DefaultServerFileSystem fs = mock(DefaultServerFileSystem.class);
    File initialDriver = TestUtils.getResource(getClass(), "deploy/my-driver.jar");
    when(fs.getJdbcDriver()).thenReturn(initialDriver);

    File deployDir = TestUtils.getTestTempDir(getClass(), "deploy", true);
    when(fs.getDeployDir()).thenReturn(deployDir);
    File deployedIndex = new File(deployDir, "jdbc-driver.txt");
    File deployedFile = new File(deployDir, "my-driver.jar");
    assertThat(deployedIndex).doesNotExist();
    assertThat(deployedFile).doesNotExist();
    when(fs.getDeployedJdbcDriverIndex()).thenReturn(deployedIndex);

    JdbcDriverDeployer deployer = new JdbcDriverDeployer(fs);
    deployer.start();

    assertThat(deployedIndex).exists();
    assertThat(deployedFile).exists();
    assertThat(deployedFile).hasSize(initialDriver.length());
    assertThat(FileUtils.readFileToString(deployedIndex)).isEqualTo("my-driver.jar|02b97f7bc37b2b68fc847fcc3fc1c156");
  }
}
