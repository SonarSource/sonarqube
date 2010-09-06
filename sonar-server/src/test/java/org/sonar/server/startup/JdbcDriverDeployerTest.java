/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.junit.Test;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.test.TestUtils;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JdbcDriverDeployerTest {

  @Test
  public void testDeploy() {
    DefaultServerFileSystem fs = mock(DefaultServerFileSystem.class);
    File initialDriver = TestUtils.getResource(getClass(), "deploy/my-driver.jar");
    when(fs.getJdbcDriver()).thenReturn(initialDriver);


    File deployed = new File(TestUtils.getTestTempDir(getClass(), "deploy", true), "copy.jar");
    assertThat(deployed.exists(), is(false));
    when(fs.getDeployedJdbcDriver()).thenReturn(deployed);

    JdbcDriverDeployer deployer = new JdbcDriverDeployer(fs);
    deployer.start();

    assertThat(deployed.exists(), is(true));
    assertThat(deployed.length(), is(initialDriver.length()));
  }
}
