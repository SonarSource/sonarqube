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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarRuntime;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.MessageException;
import org.sonar.core.platform.PluginInfo;
import org.sonar.server.platform.ServerFileSystem;
import org.sonar.updatecenter.common.PluginManifest;

import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluginJarLoaderTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public LogTester logs = new LogTester();

  private final ServerFileSystem fs = mock(ServerFileSystem.class);
  private final Set<String> blacklisted = new HashSet<>();
  private final SonarRuntime sonarRuntime = mock(SonarRuntime.class);
  private final PluginJarLoader underTest = new PluginJarLoader(fs, sonarRuntime, blacklisted);

  @Before
  public void setUp() throws IOException {
    when(sonarRuntime.getApiVersion()).thenReturn(org.sonar.api.utils.Version.parse("5.2"));
    when(fs.getDeployedPluginsDir()).thenReturn(temp.newFolder("deployed"));
    when(fs.getDownloadedPluginsDir()).thenReturn(temp.newFolder("downloaded"));
    when(fs.getHomeDir()).thenReturn(temp.newFolder("home"));
    when(fs.getInstalledExternalPluginsDir()).thenReturn(temp.newFolder("external"));
    when(fs.getInstalledBundledPluginsDir()).thenReturn(temp.newFolder("bundled"));
    when(fs.getTempDir()).thenReturn(temp.newFolder("temp"));
  }

  @Test
  public void load_installed_bundled_and_external_plugins() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledExternalPluginsDir());
    copyTestPluginTo("test-extend-plugin", fs.getInstalledBundledPluginsDir());

    Collection<ServerPluginInfo> loadedPlugins = underTest.loadPlugins();

    assertThat(loadedPlugins).extracting(PluginInfo::getKey).containsOnly("testbase", "testextend");
  }

  @Test
  public void dont_fail_if_directories_dont_exist() {
    FileUtils.deleteQuietly(fs.getInstalledExternalPluginsDir());
    FileUtils.deleteQuietly(fs.getInstalledBundledPluginsDir());
    FileUtils.deleteQuietly(fs.getDownloadedPluginsDir());
    Collection<ServerPluginInfo> loadedPlugins = underTest.loadPlugins();
    assertThat(loadedPlugins).extracting(PluginInfo::getKey).isEmpty();
  }

  @Test
  public void update_downloaded_plugin() throws IOException {
    File jar = createJar(fs.getDownloadedPluginsDir(), "plugin1", "main", null, "2.0");
    createJar(fs.getInstalledExternalPluginsDir(), "plugin1", "main", null, "1.0");

    underTest.loadPlugins();

    assertThat(logs.logs()).contains("Plugin plugin1 [plugin1] updated to version 2.0");
    assertThat(Files.list(fs.getInstalledExternalPluginsDir().toPath())).extracting(Path::getFileName).containsOnly(jar.toPath().getFileName());
  }

  @Test
  public void move_downloaded_plugins_to_external() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getDownloadedPluginsDir());
    copyTestPluginTo("test-extend-plugin", fs.getInstalledExternalPluginsDir());
    assertThat(Files.list(fs.getInstalledExternalPluginsDir().toPath())).hasSize(1);

    Collection<ServerPluginInfo> loadedPlugins = underTest.loadPlugins();

    assertThat(loadedPlugins).extracting(PluginInfo::getKey).containsOnly("testbase", "testextend");
    assertThat(fs.getDownloadedPluginsDir()).isEmptyDirectory();
    assertThat(Files.list(fs.getInstalledExternalPluginsDir().toPath())).hasSize(2);
  }

  @Test
  public void no_plugins_at_startup() {
    assertThat(underTest.loadPlugins()).isEmpty();
  }

  @Test
  public void test_plugin_requirements_at_startup() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledExternalPluginsDir());
    copyTestPluginTo("test-require-plugin", fs.getInstalledExternalPluginsDir());

    assertThat(underTest.loadPlugins()).extracting(PluginInfo::getKey).containsOnly("testbase", "testrequire");
  }

  @Test
  public void plugin_is_ignored_if_required_plugin_is_missing_at_startup() throws Exception {
    copyTestPluginTo("test-require-plugin", fs.getInstalledExternalPluginsDir());

    // plugin is not installed as test-base-plugin is missing
    assertThat(underTest.loadPlugins()).isEmpty();
    assertThat(logs.logs()).contains("Plugin Test Require Plugin [testrequire] is ignored because the required plugin [testbase] is not installed");
  }

  @Test
  public void install_plugin_and_its_extension_plugins_at_startup() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledExternalPluginsDir());
    copyTestPluginTo("test-extend-plugin", fs.getInstalledExternalPluginsDir());

    // both plugins are installed
    assertThat(underTest.loadPlugins()).extracting(PluginInfo::getKey).containsOnly("testbase", "testextend");
  }

  /**
   * Some plugins can only extend the classloader of base plugin, without declaring new extensions.
   */
  @Test
  public void plugin_is_compatible_if_no_entry_point_class_but_extend_other_plugin() throws IOException {
    createJar(fs.getInstalledExternalPluginsDir(), "base", "org.bar.Bar", null);
    createJar(fs.getInstalledExternalPluginsDir(), "foo", null, "base");

    assertThat(underTest.loadPlugins()).extracting(PluginInfo::getKey).containsOnly("base", "foo");
  }

  @Test
  public void extension_plugin_is_ignored_if_base_plugin_is_missing_at_startup() throws Exception {
    copyTestPluginTo("test-extend-plugin", fs.getInstalledExternalPluginsDir());

    assertThat(underTest.loadPlugins()).isEmpty();
    assertThat(logs.logs()).contains("Plugin Test Extend Plugin [testextend] is ignored because its base plugin [testbase] is not installed");
  }

  @Test
  public void plugin_is_ignored_if_required_plugin_is_too_old_at_startup() throws Exception {
    copyTestPluginTo("test-base-plugin", fs.getInstalledExternalPluginsDir());
    copyTestPluginTo("test-requirenew-plugin", fs.getInstalledExternalPluginsDir());

    // the plugin "requirenew" is not installed as it requires base 0.2+ to be installed.
    assertThat(underTest.loadPlugins()).extracting(PluginInfo::getKey).containsOnly("testbase");
    assertThat(logs.logs()).contains("Plugin Test Require New Plugin [testrequire] is ignored because the version 0.2 of required plugin [testbase] is not installed");
  }

  @Test
  public void blacklisted_plugin_is_automatically_deleted() throws Exception {
    blacklisted.add("testbase");
    blacklisted.add("issuesreport");

    File jar = copyTestPluginTo("test-base-plugin", fs.getInstalledExternalPluginsDir());

    Collection<ServerPluginInfo> loadedPlugins = underTest.loadPlugins();

    // plugin is not installed and file is deleted
    assertThat(loadedPlugins).isEmpty();
    assertThat(jar).doesNotExist();
  }

  @Test
  public void warn_if_plugin_has_no_entry_point_class() throws IOException {
    createJar(fs.getInstalledExternalPluginsDir(), "test", null, null);
    assertThat(underTest.loadPlugins()).isEmpty();
    assertThat(logs.logs()).contains("Plugin test [test] is ignored because entry point class is not defined");
  }

  @Test
  public void fail_if_external_plugin_has_same_key_has_bundled_plugin() throws IOException {
    File jar = createJar(fs.getInstalledExternalPluginsDir(), "plugin1", "main", null);
    createJar(fs.getInstalledBundledPluginsDir(), "plugin1", "main", null);

    String dir = getDirName(fs.getInstalledExternalPluginsDir());

    assertThatThrownBy(underTest::loadPlugins)
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("Found a plugin 'plugin1' in the directory '" + dir + "' with the same key [plugin1] as a built-in feature 'plugin1'. "
        + "Please remove '" + new File(dir, jar.getName()) + "'");
  }

  @Test
  public void fail_if_downloaded_plugin_has_same_key_has_bundled() throws IOException {
    File downloaded = createJar(fs.getDownloadedPluginsDir(), "plugin1", "main", null);
    createJar(fs.getInstalledBundledPluginsDir(), "plugin1", "main", null);
    String dir = getDirName(fs.getDownloadedPluginsDir());

    assertThatThrownBy(underTest::loadPlugins)
      .isInstanceOf(MessageException.class)
      .hasMessage("Fail to update plugin: plugin1. Built-in feature with same key already exists: plugin1. "
        + "Move or delete plugin from " + dir + " directory");
  }

  @Test
  public void fail_if_external_plugins_have_same_key() throws IOException {
    File jar1 = createJar(fs.getInstalledExternalPluginsDir(), "plugin1", "main", null);
    File jar2 = createJar(fs.getInstalledExternalPluginsDir(), "plugin1", "main", null);

    String dir = getDirName(fs.getInstalledExternalPluginsDir());

    assertThatThrownBy(underTest::loadPlugins)
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("Found two versions of the plugin 'plugin1' [plugin1] in the directory '" + dir + "'. Please remove ")
      .hasMessageContaining(jar2.getName())
      .hasMessageContaining(jar1.getName());
  }

  @Test
  public void fail_if_bundled_plugins_have_same_key() throws IOException {
    File jar1 = createJar(fs.getInstalledBundledPluginsDir(), "plugin1", "main", null);
    File jar2 = createJar(fs.getInstalledBundledPluginsDir(), "plugin1", "main", null);
    String dir = getDirName(fs.getInstalledBundledPluginsDir());

    assertThatThrownBy(underTest::loadPlugins)
      .isInstanceOf(MessageException.class)
      .hasMessageContaining("Found two versions of the plugin plugin1 [plugin1] in the directory " + dir + ". Please remove one of ")
      .hasMessageContaining(jar2.getName())
      .hasMessageContaining(jar1.getName());
  }

  @Test
  public void fail_when_sqale_plugin_is_installed() throws Exception {
    copyTestPluginTo("fake-sqale-plugin", fs.getInstalledExternalPluginsDir());

    assertThatThrownBy(underTest::loadPlugins)
      .isInstanceOf(MessageException.class)
      .hasMessage("The following plugin is no longer compatible with this version of SonarQube: 'sqale'");
  }

  @Test
  public void fail_when_incompatible_plugins_are_installed() throws Exception {
    createJar(fs.getInstalledExternalPluginsDir(), "sqale", "main", null);
    createJar(fs.getInstalledExternalPluginsDir(), "scmgit", "main", null);
    createJar(fs.getInstalledExternalPluginsDir(), "scmsvn", "main", null);

    assertThatThrownBy(underTest::loadPlugins)
      .isInstanceOf(MessageException.class)
      .hasMessage("The following plugins are no longer compatible with this version of SonarQube: 'scmgit', 'scmsvn', 'sqale'");
  }

  @Test
  public void fail_when_report_is_installed() throws Exception {
    copyTestPluginTo("fake-report-plugin", fs.getInstalledExternalPluginsDir());

    assertThatThrownBy(underTest::loadPlugins)
      .isInstanceOf(MessageException.class)
      .hasMessage("The following plugin is no longer compatible with this version of SonarQube: 'report'");
  }

  @Test
  public void fail_when_views_is_installed() throws Exception {
    copyTestPluginTo("fake-views-plugin", fs.getInstalledExternalPluginsDir());

    assertThatThrownBy(underTest::loadPlugins)
      .isInstanceOf(MessageException.class)
      .hasMessage("The following plugin is no longer compatible with this version of SonarQube: 'views'");
  }

  @Test
  public void fail_if_plugin_does_not_support_plugin_api_version() throws Exception {
    when(sonarRuntime.getApiVersion()).thenReturn(org.sonar.api.utils.Version.parse("1.0"));
    copyTestPluginTo("test-base-plugin", fs.getInstalledExternalPluginsDir());

    assertThatThrownBy(underTest::loadPlugins)
      .hasMessage("Plugin Base Plugin [testbase] requires at least Sonar Plugin API version 4.5.4 (current: 1.0)");
  }

  private static File copyTestPluginTo(String testPluginName, File toDir) throws IOException {
    File jar = TestProjectUtils.jarOf(testPluginName);
    // file is copied because it's supposed to be moved by the test
    FileUtils.copyFileToDirectory(jar, toDir);
    return new File(toDir, jar.getName());
  }

  private static String getDirName(File dir) {
    Path path = dir.toPath();
    return new File(path.getName(path.getNameCount() - 2).toString(), path.getName(path.getNameCount() - 1).toString()).toString();
  }

  private static File createJar(File dir, String key, @Nullable String mainClass, @Nullable String basePlugin) throws IOException {
    return createJar(dir, key, mainClass, basePlugin, null);
  }

  private static File createJar(File dir, String key, @Nullable String mainClass, @Nullable String basePlugin, @Nullable String version) throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().putValue(PluginManifest.KEY, key);
    manifest.getMainAttributes().putValue(PluginManifest.NAME, key);
    if (version != null) {
      manifest.getMainAttributes().putValue(PluginManifest.VERSION, version);
    }
    if (mainClass != null) {
      manifest.getMainAttributes().putValue(PluginManifest.MAIN_CLASS, mainClass);
    }
    if (basePlugin != null) {
      manifest.getMainAttributes().putValue(PluginManifest.BASE_PLUGIN, basePlugin);
    }
    manifest.getMainAttributes().putValue(MANIFEST_VERSION.toString(), "1.0");
    File jarFile = File.createTempFile(key, ".jar", dir);
    try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
      // nothing else to add
    }
    return jarFile;
  }
}
