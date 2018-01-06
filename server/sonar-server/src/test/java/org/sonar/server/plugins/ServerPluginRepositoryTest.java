/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.SonarRuntime;
import org.sonar.api.platform.ServerUpgradeStatus;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.core.platform.PluginInfo;
import org.sonar.core.platform.PluginLoader;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.updatecenter.common.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerPluginRepositoryTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logs = new LogTester();

  private SonarRuntime runtime = mock(SonarRuntime.class);
  ServerUpgradeStatus upgradeStatus = mock(ServerUpgradeStatus.class);
  ServerFileSystem fs = mock(ServerFileSystem.class, Mockito.RETURNS_DEEP_STUBS);
  PluginLoader pluginLoader = mock(PluginLoader.class);
  ServerPluginRepository underTest = new ServerPluginRepository(runtime, upgradeStatus, fs, pluginLoader);

  @Before
  public void setUp() throws IOException {
    when(fs.getBundledPluginsDir()).thenReturn(temp.newFolder());
    when(fs.getDeployedPluginsDir()).thenReturn(temp.newFolder());
    when(fs.getDownloadedPluginsDir()).thenReturn(temp.newFolder());
    when(fs.getHomeDir()).thenReturn(temp.newFolder());
    when(fs.getInstalledPluginsDir()).thenReturn(temp.newFolder());
    when(fs.getTempDir()).thenReturn(temp.newFolder());
    when(runtime.getApiVersion()).thenReturn(org.sonar.api.utils.Version.parse("5.2"));
  }

  @After
  public void tearDown() {
    underTest.stop();
  }

  /**
   * The first server startup (fresh db) installs bundled plugins and instantiates bundled plugins.
   */
  @Test
  public void first_startup_installs_bundled_plugins() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getBundledPluginsDir());
    when(upgradeStatus.isFreshInstall()).thenReturn(true);

    underTest.start();

    // both plugins are installed
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
  }

  @Test
  public void bundled_plugins_are_not_installed_if_not_fresh_server() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getBundledPluginsDir());
    when(upgradeStatus.isFreshInstall()).thenReturn(false);

    underTest.start();

    assertThat(underTest.getPluginInfos()).isEmpty();
  }

  @Test
  public void standard_startup_loads_installed_plugins() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());

    underTest.start();

    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
  }

  @Test
  public void no_plugins_at_all_on_startup() {
    underTest.start();

    assertThat(underTest.getPluginInfos()).isEmpty();
    assertThat(underTest.getPluginInfosByKeys()).isEmpty();
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
  public void fail_if_plugin_does_not_support_sq_version() throws Exception {
    when(runtime.getApiVersion()).thenReturn(org.sonar.api.utils.Version.parse("1.0"));
    copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());

    try {
      underTest.start();
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Plugin Base Plugin [testbase] requires at least SonarQube 4.5.4");
    }
  }

  @Test
  public void uninstall() throws Exception {
    File installedJar = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    File uninstallDir = temp.newFolder("uninstallDir");

    underTest.start();
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
    underTest.uninstall("testbase", uninstallDir);

    assertThat(installedJar).doesNotExist();
    // still up. Will be dropped after next startup
    assertThat(underTest.getPluginInfosByKeys()).containsOnlyKeys("testbase");
    assertThat(uninstallDir.list()).containsOnly(installedJar.getName());
  }

  @Test
  public void uninstall_dependents() throws Exception {
    File base = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    File extension = copyTestPluginTo("test-require-plugin", fs.getInstalledPluginsDir());
    File uninstallDir = temp.newFolder("uninstallDir");

    underTest.start();
    assertThat(underTest.getPluginInfos()).hasSize(2);
    underTest.uninstall("testbase", uninstallDir);
    assertThat(base).doesNotExist();
    assertThat(extension).doesNotExist();
    assertThat(uninstallDir.list()).containsOnly(base.getName(), extension.getName());
  }

  @Test
  public void dont_uninstall_non_existing_dependents() throws IOException {
    File base = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    File extension = copyTestPluginTo("test-require-plugin", fs.getInstalledPluginsDir());
    File uninstallDir = temp.newFolder("uninstallDir");

    underTest.start();
    assertThat(underTest.getPluginInfos()).hasSize(2);
    underTest.uninstall("testrequire", uninstallDir);
    assertThat(underTest.getPluginInfos()).hasSize(2);

    underTest.uninstall("testbase", uninstallDir);
    assertThat(base).doesNotExist();
    assertThat(extension).doesNotExist();
    assertThat(uninstallDir.list()).containsOnly(base.getName(), extension.getName());
  }

  @Test
  public void dont_uninstall_non_existing_files() throws IOException {
    File base = copyTestPluginTo("test-base-plugin", fs.getInstalledPluginsDir());
    File extension = copyTestPluginTo("test-require-plugin", fs.getInstalledPluginsDir());
    File uninstallDir = temp.newFolder("uninstallDir");

    underTest.start();
    assertThat(underTest.getPluginInfos()).hasSize(2);
    underTest.uninstall("testbase", uninstallDir);
    assertThat(underTest.getPluginInfos()).hasSize(2);

    underTest.uninstall("testbase", uninstallDir);
    assertThat(base).doesNotExist();
    assertThat(extension).doesNotExist();
    assertThat(uninstallDir.list()).containsOnly(base.getName(), extension.getName());
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
  public void fail_to_get_missing_plugins() {
    underTest.start();
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

  @Test
  public void plugin_is_incompatible_if_no_entry_point_class() {
    PluginInfo plugin = new PluginInfo("foo").setName("Foo");
    assertThat(ServerPluginRepository.isCompatible(plugin, runtime, Collections.emptyMap())).isFalse();
    assertThat(logs.logs()).contains("Plugin Foo [foo] is ignored because entry point class is not defined");
  }

  @Test
  public void fail_when_views_is_installed() throws Exception {
    copyTestPluginTo("fake-views-plugin", fs.getInstalledPluginsDir());

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Plugin 'views' is no more compatible with this version of SonarQube");
    underTest.start();
  }

  @Test
  public void fail_when_sqale_plugin_is_installed() throws Exception {
    copyTestPluginTo("fake-sqale-plugin", fs.getInstalledPluginsDir());

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Plugin 'sqale' is no more compatible with this version of SonarQube");
    underTest.start();
  }

  @Test
  public void fail_when_report_is_installed() throws Exception {
    copyTestPluginTo("fake-report-plugin", fs.getInstalledPluginsDir());

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Plugin 'report' is no more compatible with this version of SonarQube");
    underTest.start();
  }

  /**
   * Some plugins can only extend the classloader of base plugin, without declaring new extensions.
   */
  @Test
  public void plugin_is_compatible_if_no_entry_point_class_but_extend_other_plugin() {
    PluginInfo basePlugin = new PluginInfo("base").setMainClass("org.bar.Bar");
    PluginInfo plugin = new PluginInfo("foo").setBasePlugin("base");
    Map<String, PluginInfo> plugins = ImmutableMap.of("base", basePlugin, "foo", plugin);

    assertThat(ServerPluginRepository.isCompatible(plugin, runtime, plugins)).isTrue();
  }

  @Test
  public void getPluginInstance_throws_ISE_if_repo_is_not_started() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("not started yet");

    underTest.getPluginInstance("foo");
  }

  @Test
  public void getPluginInfo_throws_ISE_if_repo_is_not_started() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("not started yet");

    underTest.getPluginInfo("foo");
  }

  @Test
  public void hasPlugin_throws_ISE_if_repo_is_not_started() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("not started yet");

    underTest.hasPlugin("foo");
  }

  @Test
  public void getPluginInfos_throws_ISE_if_repo_is_not_started() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("not started yet");

    underTest.getPluginInfos();
  }

  private File copyTestPluginTo(String testPluginName, File toDir) throws IOException {
    File jar = TestProjectUtils.jarOf(testPluginName);
    // file is copied because it's supposed to be moved by the test
    FileUtils.copyFileToDirectory(jar, toDir);
    return new File(toDir, jar.getName());
  }
}
