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

import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.platform.PluginMetadata;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.MessageException;
import org.sonar.core.plugins.DefaultPluginMetadata;
import org.sonar.server.platform.DefaultServerFileSystem;

import java.io.File;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerPluginJarsInstallerTest {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  DefaultServerFileSystem fileSystem;
  File homeDir, pluginsDir, downloadsDir, bundledDir, trashDir, coreDir;
  ServerPluginJarInstaller jarInstaller;
  ServerPluginJarsInstaller jarsInstaller;
  Server server = mock(Server.class);
  ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);

  @Before
  public void before() throws Exception {
    when(server.getVersion()).thenReturn("3.1");
    when(server.getDeployDir()).thenReturn(temp.newFolder("deploy"));

    homeDir = temp.newFolder("home");
    pluginsDir = new File(homeDir, "extensions/plugins");
    FileUtils.forceMkdir(pluginsDir);
    downloadsDir = new File(homeDir, "extensions/downloads");
    trashDir = new File(homeDir, "extensions/trash");
    bundledDir = new File(homeDir, "lib/bundled-plugins");
    coreDir = new File(homeDir, "lib/core-plugins");
    FileUtils.forceMkdir(bundledDir);

    fileSystem = new DefaultServerFileSystem(homeDir, temp.newFolder(), server);
    jarInstaller = new ServerPluginJarInstaller();
    jarsInstaller = new ServerPluginJarsInstaller(server, upgradeStatus, fileSystem, jarInstaller);
  }

  private File jar(String name) throws Exception {
    return new File(Resources.getResource(getClass(), "ServerPluginJarsInstallerTest/" + name).toURI());
  }

  @Test
  public void copy_bundled_plugin_on_fresh_install() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(true);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), bundledDir);

    jarsInstaller.install();

    assertThat(FileUtils.listFiles(pluginsDir, new String[] {"jar"}, false)).hasSize(1);
    assertThat(new File(pluginsDir, "foo-plugin-1.0.jar")).exists().isFile();
    PluginMetadata plugin = jarsInstaller.getMetadata("foo");
    assertThat(plugin.getName()).isEqualTo("Foo");
    assertThat(plugin.getDeployedFiles()).hasSize(1);
    assertThat(plugin.isCore()).isFalse();
    assertThat(plugin.isUseChildFirstClassLoader()).isFalse();
  }

  @Test
  public void do_not_copy_bundled_plugin_on_non_fresh_install() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), bundledDir);

    jarsInstaller.install();

    assertThat(FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false)).isEmpty();
  }

  @Test
  public void do_not_copy_bundled_plugin_if_already_installed() throws Exception {
    // fresh install but plugins are already packaged in extensions/plugins
    when(upgradeStatus.isFreshInstall()).thenReturn(true);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), bundledDir);
    FileUtils.copyFileToDirectory(jar("foo-plugin-2.0.jar"), pluginsDir);
    FileUtils.copyFileToDirectory(jar("bar-plugin-1.0.jar"), pluginsDir);

    jarsInstaller.install();

    // do not copy foo 1.0
    assertThat(FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false)).hasSize(2);
    assertThat(new File(pluginsDir, "foo-plugin-2.0.jar")).exists().isFile();
    assertThat(new File(pluginsDir, "bar-plugin-1.0.jar")).exists().isFile();
    PluginMetadata plugin = jarsInstaller.getMetadata("foo");
    assertThat(plugin.getVersion()).isEqualTo("2.0");
  }

  @Test
  public void deploy_installed_plugin() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), pluginsDir);

    jarsInstaller.install();

    // check that the plugin is registered
    assertThat(jarsInstaller.getMetadata()).hasSize(1);
    PluginMetadata plugin = jarsInstaller.getMetadata("foo");
    assertThat(plugin.getName()).isEqualTo("Foo");
    assertThat(plugin.getDeployedFiles()).hasSize(1);
    assertThat(plugin.isCore()).isFalse();
    assertThat(plugin.isUseChildFirstClassLoader()).isFalse();

    // check that the file is still present in extensions/plugins
    assertThat(FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false)).hasSize(1);
    assertThat(new File(pluginsDir, "foo-plugin-1.0.jar")).exists().isFile();
  }

  @Test
  public void ignore_non_plugin_jars() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    FileUtils.copyFileToDirectory(jar("not-a-plugin.jar"), pluginsDir);

    jarsInstaller.install();

    // nothing to install but keep the file
    assertThat(jarsInstaller.getMetadata()).isEmpty();
    assertThat(FileUtils.listFiles(pluginsDir, new String[] {"jar"}, false)).hasSize(1);
    assertThat(new File(pluginsDir, "not-a-plugin.jar")).exists().isFile();
  }

  @Test
  public void fail_if_plugin_requires_greater_SQ_version() throws Exception {
    exception.expect(MessageException.class);
    exception.expectMessage("Plugin switchoffviolations needs a more recent version of SonarQube than 2.0. At least 2.5 is expected");

    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    when(server.getVersion()).thenReturn("2.0");
    FileUtils.copyFileToDirectory(jar("require-sq-2.5.jar"), pluginsDir);

    jarsInstaller.install();
  }

  @Test
  public void move_downloaded_plugins() throws Exception {
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), downloadsDir);
    when(upgradeStatus.isFreshInstall()).thenReturn(false);

    jarsInstaller.install();

    assertThat(FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false)).hasSize(1);
    assertThat(FileUtils.listFiles(downloadsDir, new String[] {"jar"}, false)).isEmpty();
    assertThat(new File(pluginsDir, "foo-plugin-1.0.jar")).exists().isFile();
  }

  @Test
  public void downloaded_plugins_overrides_existing_plugin() throws Exception {
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), pluginsDir);
    FileUtils.copyFileToDirectory(jar("foo-plugin-2.0.jar"), downloadsDir);
    when(upgradeStatus.isFreshInstall()).thenReturn(false);

    jarsInstaller.install();

    assertThat(FileUtils.listFiles(pluginsDir, new String[] {"jar"}, false)).hasSize(1);
    assertThat(FileUtils.listFiles(downloadsDir, new String[] {"jar"}, false)).isEmpty();
    assertThat(new File(pluginsDir, "foo-plugin-2.0.jar")).exists().isFile();
  }

  @Test
  public void downloaded_plugins_overrides_existing_plugin_even_if_same_filename() throws Exception {
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), pluginsDir, true);
    // foo-plugin-1.0.jar in extensions/downloads is in fact version 2.0. It's used to verify
    // that it has correctly overridden extensions/plugins/foo-plugin-1.0.jar
    FileUtils.copyFile(jar("foo-plugin-2.0.jar"), new File(downloadsDir, "foo-plugin-1.0.jar"));
    when(upgradeStatus.isFreshInstall()).thenReturn(false);

    jarsInstaller.install();

    PluginMetadata plugin = jarsInstaller.getMetadata("foo");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getVersion()).isEqualTo("2.0");
    assertThat(FileUtils.listFiles(pluginsDir, new String[] {"jar"}, false)).hasSize(1);
    assertThat(FileUtils.listFiles(downloadsDir, new String[] {"jar"}, false)).isEmpty();
    File installed = new File(pluginsDir, "foo-plugin-1.0.jar");
    assertThat(installed).exists().isFile();
  }

  @Test
  public void delete_trash() throws Exception {
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), trashDir);
    when(upgradeStatus.isFreshInstall()).thenReturn(false);

    jarsInstaller.install();

    assertThat(FileUtils.listFiles(pluginsDir, new String[] {"jar"}, false)).isEmpty();
    assertThat(trashDir).doesNotExist();
  }

  @Test
  public void fail_if_two_installed_plugins_with_same_key() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), pluginsDir);
    FileUtils.copyFileToDirectory(jar("foo-plugin-2.0.jar"), pluginsDir);

    try {
      jarsInstaller.install();
      fail();
    } catch (MessageException e) {
      assertThat(e.getMessage())
        .contains("Found two files for the same plugin 'foo'")
        .contains("foo-plugin-1.0.jar")
        .contains("foo-plugin-2.0.jar");
    }
  }

  @Test
  public void uninstall_plugin() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), pluginsDir);

    jarsInstaller.install();
    jarsInstaller.uninstall("foo");

    assertThat(FileUtils.listFiles(pluginsDir, new String[]{"jar"}, false)).isEmpty();
    assertThat(FileUtils.listFiles(trashDir, new String[] {"jar"}, false)).hasSize(1);
    assertThat(jarsInstaller.getUninstalledPluginFilenames()).containsOnly("foo-plugin-1.0.jar");
  }

  @Test
  public void pending_removals_reads_metadata() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), pluginsDir);

    jarsInstaller.install();
    jarsInstaller.uninstall("foo");

    assertThat(FileUtils.listFiles(pluginsDir, new String[] {"jar"}, false)).isEmpty();
    assertThat(FileUtils.listFiles(trashDir, new String[] {"jar"}, false)).hasSize(1);
    Collection<DefaultPluginMetadata> removals = jarsInstaller.getUninstalledPlugins();
    assertThat(removals).hasSize(1);
    PluginMetadata metadata = removals.iterator().next();
    assertThat(metadata.getKey()).isEqualTo("foo");
    assertThat(metadata.getName()).isEqualTo("Foo");
    assertThat(metadata.getVersion()).isEqualTo("1.0");
    assertThat(metadata.getOrganization()).isEqualTo("SonarSource");
    assertThat(metadata.getOrganizationUrl()).isEqualTo("http://www.sonarsource.org");
    assertThat(metadata.getLicense()).isEqualTo("LGPL 3");
    assertThat(metadata.getMainClass()).isEqualTo("foo.Main");
  }

  @Test
  public void cancel_uninstallation() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), pluginsDir);

    jarsInstaller.install();
    jarsInstaller.uninstall("foo");
    jarsInstaller.cancelUninstalls();

    assertThat(FileUtils.listFiles(pluginsDir, new String[] {"jar"}, false)).hasSize(1);
    assertThat(FileUtils.listFiles(trashDir, new String[] {"jar"}, false)).hasSize(0);
    assertThat(jarsInstaller.getUninstalledPluginFilenames()).isEmpty();
  }

  @Test
  public void deploy_core_plugins() throws Exception {
    when(upgradeStatus.isFreshInstall()).thenReturn(false);
    FileUtils.copyFileToDirectory(jar("foo-plugin-1.0.jar"), coreDir);

    jarsInstaller.install();

    // do not deploy in extensions/plugins
    assertThat(FileUtils.listFiles(pluginsDir, new String[] {"jar"}, false)).hasSize(0);

    // do not remove from lib/core-plugins
    assertThat(FileUtils.listFiles(coreDir, new String[] {"jar"}, false)).hasSize(1);

    PluginMetadata plugin = jarsInstaller.getMetadata("foo");
    assertThat(plugin).isNotNull();
    assertThat(plugin.isCore()).isTrue();
  }
}
