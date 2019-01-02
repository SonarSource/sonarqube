/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.core.platform;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.ZipUtils;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.Version;

import static com.google.common.collect.Ordering.natural;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@RunWith(DataProviderRunner.class)
public class PluginInfoTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void test_RequiredPlugin() {
    PluginInfo.RequiredPlugin plugin = PluginInfo.RequiredPlugin.parse("java:1.1");
    assertThat(plugin.getKey()).isEqualTo("java");
    assertThat(plugin.getMinimalVersion().getName()).isEqualTo("1.1");
    assertThat(plugin.toString()).isEqualTo("java:1.1");
    assertThat(plugin.equals(PluginInfo.RequiredPlugin.parse("java:1.2"))).isTrue();
    assertThat(plugin.equals(PluginInfo.RequiredPlugin.parse("php:1.2"))).isFalse();

    try {
      PluginInfo.RequiredPlugin.parse("java");
      fail();
    } catch (IllegalArgumentException expected) {
      // ok
    }
  }

  @Test
  public void test_comparison() {
    PluginInfo java1 = new PluginInfo("java").setVersion(Version.create("1.0"));
    PluginInfo java2 = new PluginInfo("java").setVersion(Version.create("2.0"));
    PluginInfo javaNoVersion = new PluginInfo("java");
    PluginInfo cobol = new PluginInfo("cobol").setVersion(Version.create("1.0"));
    PluginInfo noVersion = new PluginInfo("noVersion");
    List<PluginInfo> plugins = Arrays.asList(java1, cobol, javaNoVersion, noVersion, java2);

    List<PluginInfo> ordered = natural().sortedCopy(plugins);
    Assertions.assertThat(ordered.get(0)).isSameAs(cobol);
    Assertions.assertThat(ordered.get(1)).isSameAs(javaNoVersion);
    Assertions.assertThat(ordered.get(2)).isSameAs(java1);
    Assertions.assertThat(ordered.get(3)).isSameAs(java2);
    Assertions.assertThat(ordered.get(4)).isSameAs(noVersion);
  }

  @Test
  public void test_equals() {
    PluginInfo java1 = new PluginInfo("java").setVersion(Version.create("1.0"));
    PluginInfo java2 = new PluginInfo("java").setVersion(Version.create("2.0"));
    PluginInfo javaNoVersion = new PluginInfo("java");
    PluginInfo cobol = new PluginInfo("cobol").setVersion(Version.create("1.0"));

    assertThat(java1.equals(java1)).isTrue();
    assertThat(java1.equals(java2)).isFalse();
    assertThat(java1.equals(javaNoVersion)).isFalse();
    assertThat(java1.equals(cobol)).isFalse();
    assertThat(java1.equals("java:1.0")).isFalse();
    assertThat(java1.equals(null)).isFalse();
    assertThat(javaNoVersion.equals(javaNoVersion)).isTrue();

    assertThat(java1.hashCode()).isEqualTo(java1.hashCode());
    assertThat(javaNoVersion.hashCode()).isEqualTo(javaNoVersion.hashCode());
  }

  /**
   * SNAPSHOT versions of SonarQube are built on local developer machines only.
   * All other build environments have unique release versions (6.3.0.12345).
   */
  @Test
  public void test_compatibility_with_snapshot_version_of_sonarqube() {
    // plugins compatible with 5.6 LTS
    assertThat(withMinSqVersion("5.6").isCompatibleWith("6.3-SNAPSHOT")).isTrue();
    assertThat(withMinSqVersion("5.6.1").isCompatibleWith("6.3-SNAPSHOT")).isTrue();

    // plugin build with old release candidates of SonarQube (RC technical versions have been removed
    // in SonarQube 6.3)
    assertThat(withMinSqVersion("5.6-RC1").isCompatibleWith("6.3-SNAPSHOT")).isTrue();
    assertThat(withMinSqVersion("6.2-RC1").isCompatibleWith("6.3-SNAPSHOT")).isTrue();

    // plugin built with snapshot version of SonarQube
    assertThat(withMinSqVersion("5.6-SNAPSHOT").isCompatibleWith("6.3-SNAPSHOT")).isTrue();
    assertThat(withMinSqVersion("6.3-SNAPSHOT").isCompatibleWith("6.3-SNAPSHOT")).isTrue();
    assertThat(withMinSqVersion("6.4-SNAPSHOT").isCompatibleWith("6.3-SNAPSHOT")).isFalse();

    // plugin built with SonarQube releases
    assertThat(withMinSqVersion("6.3.0.5000").isCompatibleWith("6.3-SNAPSHOT")).isTrue();
    assertThat(withMinSqVersion("6.3.1.5000").isCompatibleWith("6.3-SNAPSHOT")).isTrue();
    assertThat(withMinSqVersion("6.3.1.5000").isCompatibleWith("6.4-SNAPSHOT")).isTrue();
    assertThat(withMinSqVersion("6.4.0.5000").isCompatibleWith("6.3-SNAPSHOT")).isFalse();

    // no constraint
    assertThat(withMinSqVersion(null).isCompatibleWith("6.3-SNAPSHOT")).isTrue();
  }

  /**
   * @see #test_compatibility_with_snapshot_version_of_sonarqube
   */
  @Test
  public void test_compatibility_with_release_version_of_sonarqube() {
    // plugins compatible with 5.6 LTS
    assertThat(withMinSqVersion("5.6").isCompatibleWith("6.3.0.5000")).isTrue();
    assertThat(withMinSqVersion("5.6.1").isCompatibleWith("6.3.0.5000")).isTrue();

    // plugin build with old release candidates of SonarQube (RC technical versions have been removed
    // in SonarQube 6.3)
    assertThat(withMinSqVersion("5.6-RC1").isCompatibleWith("6.3.0.5000")).isTrue();
    assertThat(withMinSqVersion("6.2-RC1").isCompatibleWith("6.3.0.5000")).isTrue();

    // plugin built with snapshot version of SonarQube
    assertThat(withMinSqVersion("5.6-SNAPSHOT").isCompatibleWith("6.3.0.5000")).isTrue();
    assertThat(withMinSqVersion("6.3-SNAPSHOT").isCompatibleWith("6.3.0.5000")).isTrue();
    assertThat(withMinSqVersion("6.3-SNAPSHOT").isCompatibleWith("6.3.1.6000")).isTrue();
    assertThat(withMinSqVersion("6.4-SNAPSHOT").isCompatibleWith("6.3.0.5000")).isFalse();

    // plugin built with SonarQube releases
    assertThat(withMinSqVersion("6.3.0.5000").isCompatibleWith("6.3.0.4000")).isFalse();
    assertThat(withMinSqVersion("6.3.0.5000").isCompatibleWith("6.3.0.5000")).isTrue();
    assertThat(withMinSqVersion("6.3.0.5000").isCompatibleWith("6.3.1.6000")).isTrue();
    assertThat(withMinSqVersion("6.4.0.7000").isCompatibleWith("6.3.0.5000")).isFalse();

    // no constraint
    assertThat(withMinSqVersion(null).isCompatibleWith("6.3.0.5000")).isTrue();
  }

  @Test
  public void create_from_minimal_manifest() throws Exception {
    PluginManifest manifest = new PluginManifest();
    manifest.setKey("java");
    manifest.setVersion("1.0");
    manifest.setName("Java");
    manifest.setMainClass("org.foo.FooPlugin");

    File jarFile = temp.newFile();
    PluginInfo pluginInfo = PluginInfo.create(jarFile, manifest);

    assertThat(pluginInfo.getKey()).isEqualTo("java");
    assertThat(pluginInfo.getName()).isEqualTo("Java");
    assertThat(pluginInfo.getVersion().getName()).isEqualTo("1.0");
    assertThat(pluginInfo.getJarFile()).isSameAs(jarFile);
    assertThat(pluginInfo.getMainClass()).isEqualTo("org.foo.FooPlugin");
    assertThat(pluginInfo.getBasePlugin()).isNull();
    assertThat(pluginInfo.getDescription()).isNull();
    assertThat(pluginInfo.getHomepageUrl()).isNull();
    assertThat(pluginInfo.getImplementationBuild()).isNull();
    assertThat(pluginInfo.getIssueTrackerUrl()).isNull();
    assertThat(pluginInfo.getLicense()).isNull();
    assertThat(pluginInfo.getOrganizationName()).isNull();
    assertThat(pluginInfo.getOrganizationUrl()).isNull();
    assertThat(pluginInfo.getMinimalSqVersion()).isNull();
    assertThat(pluginInfo.getRequiredPlugins()).isEmpty();
    assertThat(pluginInfo.isSonarLintSupported()).isFalse();
  }

  @Test
  public void create_from_complete_manifest() throws Exception {
    PluginManifest manifest = new PluginManifest();
    manifest.setKey("fbcontrib");
    manifest.setVersion("2.0");
    manifest.setName("Java");
    manifest.setMainClass("org.fb.FindbugsPlugin");
    manifest.setBasePlugin("findbugs");
    manifest.setSonarVersion("4.5.1");
    manifest.setDescription("the desc");
    manifest.setHomepage("http://fbcontrib.org");
    manifest.setImplementationBuild("SHA1");
    manifest.setLicense("LGPL");
    manifest.setOrganization("SonarSource");
    manifest.setOrganizationUrl("http://sonarsource.com");
    manifest.setIssueTrackerUrl("http://jira.com");
    manifest.setRequirePlugins(new String[] {"java:2.0", "pmd:1.3"});
    manifest.setSonarLintSupported(true);

    File jarFile = temp.newFile();
    PluginInfo pluginInfo = PluginInfo.create(jarFile, manifest);

    assertThat(pluginInfo.getBasePlugin()).isEqualTo("findbugs");
    assertThat(pluginInfo.getDescription()).isEqualTo("the desc");
    assertThat(pluginInfo.getHomepageUrl()).isEqualTo("http://fbcontrib.org");
    assertThat(pluginInfo.getImplementationBuild()).isEqualTo("SHA1");
    assertThat(pluginInfo.getIssueTrackerUrl()).isEqualTo("http://jira.com");
    assertThat(pluginInfo.getLicense()).isEqualTo("LGPL");
    assertThat(pluginInfo.getOrganizationName()).isEqualTo("SonarSource");
    assertThat(pluginInfo.getOrganizationUrl()).isEqualTo("http://sonarsource.com");
    assertThat(pluginInfo.getMinimalSqVersion().getName()).isEqualTo("4.5.1");
    assertThat(pluginInfo.getRequiredPlugins()).extracting("key").containsOnly("java", "pmd");
    assertThat(pluginInfo.isSonarLintSupported()).isTrue();
  }

  @Test
  @UseDataProvider("licenseVersions")
  public void requiredPlugin_license_is_ignored_when_reading_manifest(String version) throws IOException {
    PluginManifest manifest = new PluginManifest();
    manifest.setKey("java");
    manifest.setVersion("1.0");
    manifest.setName("Java");
    manifest.setMainClass("org.foo.FooPlugin");
    manifest.setRequirePlugins(new String[] {"license:" + version});

    File jarFile = temp.newFile();
    PluginInfo pluginInfo = PluginInfo.create(jarFile, manifest);
    assertThat(pluginInfo.getRequiredPlugins()).isEmpty();
  }

  @Test
  @UseDataProvider("licenseVersions")
  public void requiredPlugin_license_among_others_is_ignored_when_reading_manifest(String version) throws IOException {
    PluginManifest manifest = new PluginManifest();
    manifest.setKey("java");
    manifest.setVersion("1.0");
    manifest.setName("Java");
    manifest.setMainClass("org.foo.FooPlugin");
    manifest.setRequirePlugins(new String[] {"java:2.0", "license:" + version, "pmd:1.3"});

    File jarFile = temp.newFile();
    PluginInfo pluginInfo = PluginInfo.create(jarFile, manifest);
    assertThat(pluginInfo.getRequiredPlugins()).extracting("key").containsOnly("java", "pmd");
  }

  @DataProvider
  public static Object[][] licenseVersions() {
    return new Object[][] {
      {"0.3"},
      {"7.2.0.1253"}
    };
  }

  @Test
  public void create_from_file() {
    File checkstyleJar = FileUtils.toFile(getClass().getResource("/org/sonar/core/platform/sonar-checkstyle-plugin-2.8.jar"));
    PluginInfo checkstyleInfo = PluginInfo.create(checkstyleJar);

    assertThat(checkstyleInfo.getName()).isEqualTo("Checkstyle");
    assertThat(checkstyleInfo.getMinimalSqVersion()).isEqualTo(Version.create("2.8"));
  }

  @Test
  public void test_toString() throws Exception {
    PluginInfo pluginInfo = new PluginInfo("java").setVersion(Version.create("1.1"));
    assertThat(pluginInfo.toString()).isEqualTo("[java / 1.1]");

    pluginInfo.setImplementationBuild("SHA1");
    assertThat(pluginInfo.toString()).isEqualTo("[java / 1.1 / SHA1]");
  }

  /**
   * The English bundle plugin was removed in 5.2. L10n plugins do not need to declare
   * it as base plugin anymore
   */
  @Test
  public void l10n_plugins_should_not_extend_english_plugin() {
    PluginInfo pluginInfo = new PluginInfo("l10nfr").setBasePlugin("l10nen");
    assertThat(pluginInfo.getBasePlugin()).isNull();
  }

  @Test
  public void fail_when_jar_is_not_a_plugin() throws IOException {
    // this JAR has a manifest but is not a plugin
    File jarRootDir = temp.newFolder();
    FileUtils.write(new File(jarRootDir, "META-INF/MANIFEST.MF"), "Build-Jdk: 1.6.0_15");
    File jar = temp.newFile();
    ZipUtils.zipDir(jarRootDir, jar);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("File is not a plugin. Please delete it and restart: " + jar.getAbsolutePath());

    PluginInfo.create(jar);
  }

  PluginInfo withMinSqVersion(@Nullable String version) {
    PluginInfo pluginInfo = new PluginInfo("foo");
    if (version != null) {
      pluginInfo.setMinimalSqVersion(Version.create(version));
    }
    return pluginInfo;
  }
}
