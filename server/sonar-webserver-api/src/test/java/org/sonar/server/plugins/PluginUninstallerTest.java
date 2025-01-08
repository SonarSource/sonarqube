/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.Plugin;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.plugin.PluginType;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.plugin.PluginType.BUNDLED;
import static org.sonar.core.plugin.PluginType.EXTERNAL;

public class PluginUninstallerTest {
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  @Rule
  public LogTester logs = new LogTester();

  private File uninstallDir;
  private ServerFileSystem fs = mock(ServerFileSystem.class);
  private ServerPluginRepository serverPluginRepository = new ServerPluginRepository();
  private PluginUninstaller underTest = new PluginUninstaller(fs, serverPluginRepository);

  @Before
  public void setUp() throws IOException {
    uninstallDir = testFolder.newFolder("uninstall");
    when(fs.getUninstalledPluginsDir()).thenReturn(uninstallDir);
    when(fs.getInstalledExternalPluginsDir()).thenReturn(testFolder.newFolder("external"));
  }

  @Test
  public void create_uninstall_dir() {
    File dir = new File(testFolder.getRoot(), "dir");
    when(fs.getUninstalledPluginsDir()).thenReturn(dir);

    assertThat(dir).doesNotExist();
    underTest.start();
    assertThat(dir).isDirectory();
  }

  @Test
  public void fail_uninstall_if_plugin_doesnt_exist() {
    underTest.start();
    assertThatThrownBy(() -> underTest.uninstall("plugin"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Plugin [plugin] is not installed");
  }

  @Test
  public void fail_uninstall_if_plugin_is_bundled() {
    underTest.start();
    serverPluginRepository.addPlugin(newPlugin("plugin", BUNDLED, "plugin.jar"));
    assertThatThrownBy(() -> underTest.uninstall("plugin"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Plugin [plugin] is not installed");
  }

  @Test
  public void uninstall() throws Exception {
    File installedJar = copyTestPluginTo("test-base-plugin", fs.getInstalledExternalPluginsDir());
    serverPluginRepository.addPlugin(newPlugin("testbase", EXTERNAL, installedJar.getName()));

    underTest.start();
    assertThat(installedJar).exists();

    underTest.uninstall("testbase");

    assertThat(installedJar).doesNotExist();
    assertThat(uninstallDir.list()).containsOnly(installedJar.getName());
  }

  @Test
  public void uninstall_ignores_non_existing_files() {
    underTest.start();
    serverPluginRepository.addPlugin(newPlugin("test", EXTERNAL, "nonexisting.jar"));
    underTest.uninstall("test");
    assertThat(uninstallDir).isEmptyDirectory();
    assertThat(logs.logs()).contains("Plugin already uninstalled: test [test]");
  }

  @Test
  public void uninstall_dependents() throws IOException {
    File baseJar = copyTestPluginTo("test-base-plugin", fs.getInstalledExternalPluginsDir());
    File requirejar = copyTestPluginTo("test-require-plugin", fs.getInstalledExternalPluginsDir());

    ServerPlugin base = newPlugin("test-base-plugin", EXTERNAL, baseJar.getName());
    ServerPlugin extension = newPlugin("test-require-plugin", EXTERNAL, requirejar.getName(), new PluginInfo.RequiredPlugin("test-base-plugin", Version.create("1.0")));

    serverPluginRepository.addPlugins(Arrays.asList(base, extension));

    underTest.start();
    underTest.uninstall("test-base-plugin");
    assertThat(Files.list(uninstallDir.toPath())).extracting(p -> p.getFileName().toString()).containsOnly(baseJar.getName(), requirejar.getName());
    assertThat(fs.getInstalledExternalPluginsDir()).isEmptyDirectory();
  }

  @Test
  public void cancel() throws IOException {
    File file = copyTestPluginTo("test-base-plugin", uninstallDir);
    assertThat(Files.list(uninstallDir.toPath())).extracting(p -> p.getFileName().toString()).containsOnly(file.getName());
    underTest.cancelUninstalls();
  }

  @Test
  public void list_uninstalled_plugins() throws IOException {
    new File(uninstallDir, "file1").createNewFile();
    copyTestPluginTo("test-base-plugin", uninstallDir);
    assertThat(underTest.getUninstalledPlugins()).extracting("key").containsOnly("testbase");
  }

  private static ServerPlugin newPlugin(String key, PluginType type, String jarFile, PluginInfo.RequiredPlugin requiredPlugin) {
    ServerPluginInfo pluginInfo = newPluginInfo(key, type, jarFile);
    when(pluginInfo.getRequiredPlugins()).thenReturn(Collections.singleton(requiredPlugin));
    return newPlugin(pluginInfo);
  }

  private static ServerPlugin newPlugin(String key, PluginType type, String jarFile) {
    return newPlugin(newPluginInfo(key, type, jarFile));
  }

  private static ServerPluginInfo newPluginInfo(String key, PluginType type, String jarFile) {
    ServerPluginInfo pluginInfo = mock(ServerPluginInfo.class);
    when(pluginInfo.getKey()).thenReturn(key);
    when(pluginInfo.getName()).thenReturn(key);
    when(pluginInfo.getType()).thenReturn(type);
    when(pluginInfo.getNonNullJarFile()).thenReturn(new File(jarFile));
    return pluginInfo;
  }

  private static ServerPlugin newPlugin(ServerPluginInfo pluginInfo) {
    return new ServerPlugin(pluginInfo, pluginInfo.getType(), mock(Plugin.class),
      mock(PluginFilesAndMd5.FileAndMd5.class), mock(ClassLoader.class));
  }

  private File copyTestPluginTo(String testPluginName, File toDir) throws IOException {
    File jar = TestProjectUtils.jarOf(testPluginName);
    // file is copied because it's supposed to be moved by the test
    FileUtils.copyFileToDirectory(jar, toDir);
    return new File(toDir, jar.getName());
  }
}
