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
package org.sonar.server.plugins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.MessageException;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.test.TestUtils;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerPluginJarsInstallerTest {

  @Rule
  public TestName name = new TestName();

  @Rule
  public ExpectedException exception = ExpectedException.none();

  ServerPluginJarInstaller jarInstaller;
  DefaultServerFileSystem fileSystem;
  File homeDir;
  File deployDir;
  ServerPluginJarsInstaller jarsInstaller;
  Server server = mock(Server.class);
  ServerUpgradeStatus serverUpgradeStatus;

  @Before
  public void before() {
    when(server.getVersion()).thenReturn("3.1");
    homeDir = TestUtils.getResource(ServerPluginJarsInstallerTest.class, name.getMethodName());
    deployDir = TestUtils.getTestTempDir(ServerPluginJarsInstallerTest.class, name.getMethodName() + "/deploy");
    fileSystem = new DefaultServerFileSystem(null, homeDir, deployDir);
    jarInstaller = new ServerPluginJarInstaller();
    serverUpgradeStatus = mock(ServerUpgradeStatus.class);
    jarsInstaller = new ServerPluginJarsInstaller(server, serverUpgradeStatus, fileSystem, jarInstaller);
  }

  @Test
  public void deployPlugin() {
    when(serverUpgradeStatus.isFreshInstall()).thenReturn(false);

    jarsInstaller.install();

    // check that the plugin is registered
    assertThat(jarsInstaller.getMetadata()).hasSize(1);

    PluginMetadata plugin = jarsInstaller.getMetadata("foo");
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
  public void deployBundledPluginsOnFreshInstall() {
    when(serverUpgradeStatus.isFreshInstall()).thenReturn(true);

    jarsInstaller.install();

    // check that the plugin is registered
    assertThat(jarsInstaller.getMetadata()).hasSize(2);

    PluginMetadata plugin = jarsInstaller.getMetadata("bar");
    assertThat(plugin.getName()).isEqualTo("Bar");
    assertThat(plugin.getDeployedFiles()).hasSize(1);
    assertThat(plugin.isCore()).isFalse();
    assertThat(plugin.isUseChildFirstClassLoader()).isFalse();

    // check that the file is deployed
    File deployedJar = new File(deployDir, "plugins/bar/bar-plugin.jar");
    assertThat(deployedJar).exists();
    assertThat(deployedJar).isFile();
  }

  @Test
  public void ignoreJarsWhichAreNotPlugins() {
    jarsInstaller.install();

    assertThat(jarsInstaller.getMetadata()).isEmpty();
  }

  @Test
  public void fail_if_require_greater_SQ_version() {
    when(server.getVersion()).thenReturn("2.0");

    exception.expect(IllegalStateException.class);
    exception.expectMessage("Plugin switchoffviolations needs a more recent version of SonarQube than 2.0. At least 2.5 is expected");

    jarsInstaller.install();
  }

  @Test
  public void failIfTwoPluginsWithSameKey() {
    try {
      jarsInstaller.install();
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage())
        .contains("Found two files for the same plugin 'foo'")
        .contains("foo-plugin1.jar")
        .contains("foo-plugin2.jar");
    }
  }
}
