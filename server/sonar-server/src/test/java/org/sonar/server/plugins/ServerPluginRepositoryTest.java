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

import com.google.common.collect.ImmutableSet;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.platform.Server;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.MessageException;
import org.sonar.core.platform.PluginLoader;
import org.sonar.server.platform.DefaultServerFileSystem;
import org.sonar.updatecenter.common.Version;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerPluginRepositoryTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Server server = mock(Server.class);
  ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
  DefaultServerFileSystem fs = mock(DefaultServerFileSystem.class, Mockito.RETURNS_DEEP_STUBS);
  PluginLoader pluginLoader = new PluginLoader(new ServerPluginUnzipper(fs));
  ServerPluginRepository underTest = new ServerPluginRepository(server, upgradeStatus, fs, pluginLoader);

  @Before
  public void setUp() throws IOException {
    when(fs.getBundledPluginsDir()).thenReturn(temp.newFolder());
    when(fs.getCorePluginsDir()).thenReturn(temp.newFolder());
    when(fs.getDeployedPluginsDir()).thenReturn(temp.newFolder());
    when(fs.getDownloadedPluginsDir()).thenReturn(temp.newFolder());
    when(fs.getHomeDir()).thenReturn(temp.newFolder());
    when(fs.getInstalledPluginsDir()).thenReturn(temp.newFolder());
    when(fs.getTempDir()).thenReturn(temp.newFolder());
    when(server.getVersion()).thenReturn("5.2");
  }

  @After
  public void tearDown() throws Exception {
    underTest.stop();
  }

  /**
   * The first server startup (fresh db) installs bundled plugins and instantiates bundled + core plugins.
   */
  @Test
  public void first_startup_installs_bundled_plugins() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getBundledPluginsDir());
    copyTestPluginTo("test-core-plugin", fs.getCorePluginsDir());
    when(upgradeStatus.isFreshInstall()).thenReturn(true);

    underTest.start();

    // both plugins are installed
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("core", "testbase");
    assertThat(underTest.getPluginInstance("core").getClass().getName()).isEqualTo("CorePlugin");
    assertThat(underTest.getPluginInstance("testbase").getClass().getName()).isEqualTo("BasePlugin");
    assertThat(underTest.hasPlugin("testbase")).isTrue();
  }

  @Test
  public void bundled_plugins_are_not_installed_if_not_fresh_server() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getBundledPluginsDir());
    when(upgradeStatus.isFreshInstall()).thenReturn(false);

    underTest.start();

    assertThat(underTest.getPluginInfos()).isEmpty();
  }

  @Test
  public void standard_startup_loads_core_and_installed_plugins() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    copyTestPluginTo("test-core-plugin", fs.getCorePluginsDir());

    underTest.start();

    // both plugins are installed
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("core", "testbase");
    assertThat(underTest.getPluginInstance("core").getClass().getName()).isEqualTo("CorePlugin");
    assertThat(underTest.getPluginInstance("testbase").getClass().getName()).isEqualTo("BasePlugin");
  }

  /**
   * That sounds impossible, there are still core plugins for now, but it's still valuable
   * to test sensibility to null values.
   */
  @Test
  public void no_plugins_at_all_on_startup() throws Exception {
    underTest.start();

    assertThat(underTest.getPluginInfos()).isEmpty();
    assertThat(underTest.getPluginInfosByKeys()).isEmpty();
    assertThat(underTest.getUninstalledPlugins()).isEmpty();
    assertThat(underTest.hasPlugin("testbase")).isFalse();
  }

  @Test
  public void fail_if_multiple_jars_for_same_installed_plugin_on_startup() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    copyTestPluginTo("test-base-plugin-v2", fs.getInstalledPluginsDir());

    try {
      underTest.start();
      fail();
    } catch (MessageException e) {
      assertThat(e)
        .hasMessageStartingWith("Found two files for the same plugin [testbase]: ")
        // order is not guaranteed, so assertion is split
        .hasMessageContaining("test-base-plugin-0.1-SNAPSHOT.jar")
        .hasMessageContaining("test-base-plugin-0.2-SNAPSHOT.jar");
    }
  }

  @Test
  public void install_downloaded_plugins_on_startup() throws Exception {
    File downloadedJar = copyTestPluginTo("test-base-plugin", fs.getDownloadedPluginsDir());

    underTest.start();

    // plugin is moved to extensions/plugins then loaded
    assertThat(downloadedJar).doesNotExist();
    assertThat(new File(fs.getInstalledPluginsDir(), downloadedJar.getName())).isFile().exists();
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
    assertThat(underTest.getPluginInstance("testbase").getClass().getName()).isEqualTo("BasePlugin");
  }

  @Test
  public void downloaded_file_overrides_existing_installed_file_on_startup() throws Exception {
    File installedV1 = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    File downloadedV2 = copyTestPluginTo("test-base-plugin-v2", fs.getDownloadedPluginsDir());

    underTest.start();

    // plugin is moved to extensions/plugins and replaces v1
    assertThat(downloadedV2).doesNotExist();
    assertThat(installedV1).doesNotExist();
    assertThat(new File(fs.getInstalledPluginsDir(), downloadedV2.getName())).exists();
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
    assertThat(underTest.getPluginInfo("testbase").getVersion()).isEqualTo(Version.create("0.2-SNAPSHOT"));
  }

  @Test
  public void blacklisted_plugin_is_automatically_uninstalled_on_startup() throws Exception {
    underTest.setBlacklistedPluginKeys(ImmutableSet.of("testbase", "issuesreport"));
    File jar = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());

    underTest.start();

    // plugin is not installed and file is deleted
    assertThat(underTest.getPluginInfos()).isEmpty();
    assertThat(jar).doesNotExist();
  }

  @Test
  public void test_plugin_requirements_at_startup() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    copyTestPluginTo("test-require-plugin", fs.getInstalledPluginsDir());

    underTest.start();

    // both plugins are installed
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase", "testrequire");
  }

  @Test
  public void plugin_is_ignored_if_required_plugin_is_missing_at_startup() throws Exception {
    copyTestPluginTo("test-require-plugin", fs.getInstalledPluginsDir());

    underTest.start();

    // plugin is not installed as test-base-plugin is missing
    assertThat(underTest.getPluginInfosByKeys()).isEmpty();
  }

  @Test
  public void plugin_is_ignored_if_required_plugin_is_too_old_at_startup() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    copyTestPluginTo("test-requirenew-plugin", fs.getInstalledPluginsDir());

    underTest.start();

    // the plugin "requirenew" is not installed as it requires base 0.2+ to be installed.
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
  }

  @Test
  public void plugin_is_ignored_at_startup_if_unsupported_sq() throws Exception {
    when(server.getVersion()).thenReturn("1.0");
    copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());

    underTest.start();

    // plugin requires SQ 4.5.1 but SQ 1.0 is installed
    assertThat(underTest.getPluginInfos()).isEmpty();
  }

  @Test
  public void uninstall() throws Exception {
    File installedJar = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());

    underTest.start();
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
    underTest.uninstall("testbase");

    assertThat(installedJar).doesNotExist();
    // still up. Will be dropped after next startup
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
    assertThat(underTest.getUninstalledPluginFilenames()).containsOnly(installedJar.getName());
    assertThat(underTest.getUninstalledPlugins()).extracting("key").containsOnly("testbase");
  }

  @Test
  public void uninstall_dependents() throws Exception {
    File base = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    File extension = copyTestPluginTo("test-require-plugin", fs.getInstalledPluginsDir());

    underTest.start();
    assertThat(underTest.getPluginInfos()).hasSize(2);
    underTest.uninstall("testbase");

    assertThat(base).doesNotExist();
    assertThat(extension).doesNotExist();
    assertThat(underTest.getUninstalledPluginFilenames()).containsOnly(base.getName(), extension.getName());
    assertThat(underTest.getUninstalledPlugins()).extracting("key").containsOnly("testbase", "testrequire");
  }

  @Test
  public void cancel_uninstall() throws Exception {
    File base = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    underTest.start();

    underTest.uninstall("testbase");
    assertThat(base).doesNotExist();

    underTest.cancelUninstalls();
    assertThat(base).exists();
    assertThat(underTest.getUninstalledPluginFilenames()).isEmpty();
    assertThat(underTest.getUninstalledPlugins()).isEmpty();
  }

  @Test
  public void install_plugin_and_its_extension_plugins_at_startup() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    copyTestPluginTo("test-extend-plugin", fs.getInstalledPluginsDir());

    underTest.start();

    // both plugins are installed
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase", "testextend");
  }

  @Test
  public void extension_plugin_is_ignored_if_base_plugin_is_missing_at_startup() throws Exception {
    copyTestPluginTo("test-extend-plugin", fs.getInstalledPluginsDir());

    underTest.start();

    // plugin is not installed as its base plugin is not installed
    assertThat(underTest.getPluginInfos()).isEmpty();
  }

  @Test
  public void fail_is_missing_required_plugin() throws Exception {
    try {
      underTest.getPluginInfo("unknown");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Plugin [unknown] does not exist");
    }

    try {
      underTest.getPluginInstance("unknown");
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Plugin [unknown] does not exist");
    }
  }

  private File copyTestPluginTo(String testPluginName, File toDir) throws IOException {
    File jar = TestProjectUtils.jarOf(testPluginName);
    // file is copied because it's supposed to be moved by the test
    FileUtils.copyFileToDirectory(jar, toDir);
    return new File(toDir, jar.getName());
  }
}
