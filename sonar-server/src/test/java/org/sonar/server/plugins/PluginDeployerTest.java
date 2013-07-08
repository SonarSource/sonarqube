/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.plugins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.Server;
import org.sonar.core.plugins.PluginInstaller;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.test.TestUtils;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginDeployerTest {

  @Rule
  public TestName name = new TestName();
  @Rule
  public ExpectedException exception = ExpectedException.none();
  private PluginInstaller extractor;
  private DefaultServerFileSystem fileSystem;
  private File homeDir;
  private File deployDir;
  private PluginDeployer deployer;
  private Server server = mock(Server.class);
  private UpdateCenterMatrixFactory updateCenterMatrixFactory;

  @Before
  public void start() {
    when(server.getVersion()).thenReturn("3.1");
    homeDir = TestUtils.getResource(PluginDeployerTest.class, name.getMethodName());
    deployDir = TestUtils.getTestTempDir(PluginDeployerTest.class, name.getMethodName() + "/deploy");
    fileSystem = new DefaultServerFileSystem(null, homeDir, deployDir);
    extractor = new PluginInstaller();
    deployer = new PluginDeployer(server, fileSystem, extractor);
  }

  @Test
  public void deployPlugin() {
    deployer.start();

    // check that the plugin is registered
    assertThat(deployer.getMetadata()).hasSize(1); // no more checkstyle

    PluginMetadata plugin = deployer.getMetadata("foo");
    assertThat(plugin.getName()).isEqualTo("Foo");
    assertThat(plugin.getDeployedFiles()).hasSize(1);
    assertThat(plugin.isCore()).isFalse();
    assertThat(plugin.isUseChildFirstClassLoader()).isFalse();

    // check that the file is deployed
    File deployedJar = new File(deployDir, "plugins/foo/foo-plugin.jar");
    assertThat(deployedJar).exists();
    assertThat(deployedJar).isFile();
  }

  @Test
  public void deployPluginExtensions() {
    deployer.start();

    // check that the plugin is registered
    assertThat(deployer.getMetadata()).hasSize(1); // no more checkstyle

    PluginMetadata plugin = deployer.getMetadata("foo");
    assertThat(plugin.getDeployedFiles()).hasSize(2);
    File extFile = plugin.getDeployedFiles().get(1);
    assertThat(extFile.getName()).isEqualTo("foo-extension.txt");

    // check that the extension file is deployed
    File deployedJar = new File(deployDir, "plugins/foo/foo-extension.txt");
    assertThat(deployedJar.exists()).isTrue();
    assertThat(deployedJar.isFile()).isTrue();
  }

  @Test
  public void ignoreJarsWhichAreNotPlugins() {
    deployer.start();

    assertThat(deployer.getMetadata()).isEmpty();
  }

  @Test
  public void should_fail_on_plugin_depending_on_more_recent_sonar() {
    when(server.getVersion()).thenReturn("2.0");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Plugin switchoffviolations needs a more recent version of SonarQube than 2.0. At least 2.5 is expected");

    deployer.start();
  }

  @Test(expected = IllegalStateException.class)
  public void failIfTwoPluginsWithSameKey() {
    deployer.start();
  }
}
