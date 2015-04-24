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
package org.sonar.core.platform;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.utils.ManifestUtils;
import org.sonar.updatecenter.common.PluginManifest;
import org.sonar.updatecenter.common.Version;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import static com.google.common.collect.Ordering.natural;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class PluginInfoTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void test_RequiredPlugin() throws Exception {
    PluginInfo.RequiredPlugin plugin = PluginInfo.RequiredPlugin.parse("java:1.1");
    assertThat(plugin.getKey()).isEqualTo("java");
    assertThat(plugin.getMinimalVersion().getName()).isEqualTo("1.1");

    try {
      PluginInfo.RequiredPlugin.parse("java");
      fail();
    } catch (IllegalArgumentException expected) {
      // ok
    }
  }

  @Test
  public void test_compare() {
    PluginInfo java1 = new PluginInfo("java").setName("Java").setVersion(Version.create("1.0"));
    PluginInfo java2 = new PluginInfo("java").setName("Java").setVersion(Version.create("2.0"));
    PluginInfo cobol = new PluginInfo("cobol").setName("Cobol").setVersion(Version.create("1.0"));
    List<PluginInfo> plugins = Arrays.asList(java1, java2, cobol);
    Collections.shuffle(plugins);

    List<PluginInfo> ordered = natural().sortedCopy(plugins);
    assertThat(ordered.get(0)).isSameAs(cobol);
    assertThat(ordered.get(1)).isSameAs(java1);
    assertThat(ordered.get(2)).isSameAs(java2);
  }

  @Test
  public void test_compatibility_with_sq_version() {
    assertThat(withMinSqVersion("1.1").isCompatibleWith("1.1")).isTrue();
    assertThat(withMinSqVersion("1.1").isCompatibleWith("1.1.0")).isTrue();
    assertThat(withMinSqVersion("1.0").isCompatibleWith("1.0.0")).isTrue();

    assertThat(withMinSqVersion("1.0").isCompatibleWith("1.1")).isTrue();
    assertThat(withMinSqVersion("1.1.1").isCompatibleWith("1.1.2")).isTrue();
    assertThat(withMinSqVersion("2.0").isCompatibleWith("2.1.0")).isTrue();
    assertThat(withMinSqVersion("3.2").isCompatibleWith("3.2-RC1")).isTrue();
    assertThat(withMinSqVersion("3.2").isCompatibleWith("3.2-RC2")).isTrue();
    assertThat(withMinSqVersion("3.2").isCompatibleWith("3.1-RC2")).isFalse();

    assertThat(withMinSqVersion("1.1").isCompatibleWith("1.0")).isFalse();
    assertThat(withMinSqVersion("2.0.1").isCompatibleWith("2.0.0")).isFalse();
    assertThat(withMinSqVersion("2.10").isCompatibleWith("2.1")).isFalse();
    assertThat(withMinSqVersion("10.10").isCompatibleWith("2.2")).isFalse();

    assertThat(withMinSqVersion("1.1-SNAPSHOT").isCompatibleWith("1.0")).isFalse();
    assertThat(withMinSqVersion("1.1-SNAPSHOT").isCompatibleWith("1.1")).isTrue();
    assertThat(withMinSqVersion("1.1-SNAPSHOT").isCompatibleWith("1.2")).isTrue();
    assertThat(withMinSqVersion("1.0.1-SNAPSHOT").isCompatibleWith("1.0")).isFalse();

    assertThat(withMinSqVersion("3.1-RC2").isCompatibleWith("3.2-SNAPSHOT")).isTrue();
    assertThat(withMinSqVersion("3.1-RC1").isCompatibleWith("3.2-RC2")).isTrue();
    assertThat(withMinSqVersion("3.1-RC1").isCompatibleWith("3.1-RC2")).isTrue();

    assertThat(withMinSqVersion(null).isCompatibleWith("0")).isTrue();
    assertThat(withMinSqVersion(null).isCompatibleWith("3.1")).isTrue();
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
    assertThat(pluginInfo.getFile()).isSameAs(jarFile);
    assertThat(pluginInfo.getMainClass()).isEqualTo("org.foo.FooPlugin");

    // optional fields
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
    manifest.setRequirePlugins(new String[]{"java:2.0", "pmd:1.3"});

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
    assertThat(pluginInfo.getRequiredPlugins()).extracting("key").containsExactly("java", "pmd");
  }

  @Test
  public void create_from_file() throws Exception {
    File checkstyleJar = FileUtils.toFile(getClass().getResource("/org/sonar/core/plugins/sonar-checkstyle-plugin-2.8.jar"));
    PluginInfo checkstyleInfo = PluginInfo.create(checkstyleJar);

    assertThat(checkstyleInfo.getName()).isEqualTo("Checkstyle");
    assertThat(checkstyleInfo.getMinimalSqVersion()).isEqualTo(Version.create("2.8"));
  }

  @Test
  public void test_toString() throws Exception {
    PluginInfo pluginInfo = new PluginInfo().setKey("java").setVersion(Version.create("1.1"));
    assertThat(pluginInfo.toString()).isEqualTo("[java / 1.1]");

    pluginInfo.setImplementationBuild("SHA1");
    assertThat(pluginInfo.toString()).isEqualTo("[java / 1.1 / SHA1]");
  }

  @Test
  public void isCore() throws Exception {
    PluginInfo pluginInfo = new PluginInfo();
    assertThat(pluginInfo.isCore()).isFalse();

    pluginInfo.setCore(true);
    assertThat(pluginInfo.isCore()).isTrue();
  }

  static PluginInfo withMinSqVersion(@Nullable String version) {
    PluginInfo pluginInfo = new PluginInfo("foo");
    if (version != null) {
      pluginInfo.setMinimalSqVersion(Version.create(version));
    }
    return pluginInfo;
  }
}
