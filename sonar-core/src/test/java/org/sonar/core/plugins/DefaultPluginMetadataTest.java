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
package org.sonar.core.plugins;

import org.junit.Test;
import org.sonar.api.platform.PluginMetadata;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Ordering.natural;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultPluginMetadataTest {

  @Test
  public void testGettersAndSetters() {
    DefaultPluginMetadata metadata = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar"));
    metadata.setKey("checkstyle")
      .setLicense("LGPL")
      .setDescription("description")
      .setHomepage("http://home")
      .setIssueTrackerUrl("http://jira.codehuas.org")
      .setMainClass("org.Main")
      .setOrganization("SonarSource")
      .setOrganizationUrl("http://sonarsource.org")
      .setVersion("1.1")
      .setSonarVersion("3.0")
      .setUseChildFirstClassLoader(true)
      .setCore(false)
      .setImplementationBuild("abcdef");

    assertThat(metadata.getKey()).isEqualTo("checkstyle");
    assertThat(metadata.getParent()).isNull();
    assertThat(metadata.getLicense()).isEqualTo("LGPL");
    assertThat(metadata.getDescription()).isEqualTo("description");
    assertThat(metadata.getHomepage()).isEqualTo("http://home");
    assertThat(metadata.getIssueTrackerUrl()).isEqualTo("http://jira.codehuas.org");
    assertThat(metadata.getMainClass()).isEqualTo("org.Main");
    assertThat(metadata.getOrganization()).isEqualTo("SonarSource");
    assertThat(metadata.getOrganizationUrl()).isEqualTo("http://sonarsource.org");
    assertThat(metadata.getVersion()).isEqualTo("1.1");
    assertThat(metadata.getSonarVersion()).isEqualTo("3.0");
    assertThat(metadata.isUseChildFirstClassLoader()).isTrue();
    assertThat(metadata.isCore()).isFalse();
    assertThat(metadata.getBasePlugin()).isNull();
    assertThat(metadata.getFile()).isNotNull();
    assertThat(metadata.getDeployedFiles()).isEmpty();
    assertThat(metadata.getImplementationBuild()).isEqualTo("abcdef");
  }

  @Test
  public void testDeployedFiles() {
    DefaultPluginMetadata metadata = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar"))
      .addDeployedFile(new File("foo.jar"))
      .addDeployedFile(new File("bar.jar"));

    assertThat(metadata.getDeployedFiles()).hasSize(2);
  }

  @Test
  public void testInternalPathToDependencies() {
    DefaultPluginMetadata metadata = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar"))
      .setPathsToInternalDeps(newArrayList("META-INF/lib/commons-lang.jar", "META-INF/lib/commons-io.jar"));

    assertThat(metadata.getPathsToInternalDeps()).containsOnly("META-INF/lib/commons-lang.jar", "META-INF/lib/commons-io.jar");
  }

  @Test
  public void shouldEquals() {
    DefaultPluginMetadata checkstyle = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar")).setKey("checkstyle");
    PluginMetadata pmd = DefaultPluginMetadata.create(new File("sonar-pmd-plugin.jar")).setKey("pmd");

    assertThat(checkstyle).isEqualTo(checkstyle);
    assertThat(checkstyle).isEqualTo(DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar")).setKey("checkstyle"));
    assertThat(checkstyle).isNotEqualTo(pmd);
  }

  @Test
  public void shouldCompare() {
    DefaultPluginMetadata checkstyle = DefaultPluginMetadata.create(new File("sonar-checkstyle-plugin.jar"))
      .setKey("checkstyle")
      .setName("Checkstyle");
    DefaultPluginMetadata pmd = DefaultPluginMetadata.create(new File("sonar-pmd-plugin.jar"))
      .setKey("pmd")
      .setName("PMD");
    List<DefaultPluginMetadata> plugins = Arrays.asList(pmd, checkstyle);

    assertThat(natural().sortedCopy(plugins)).extracting("key").containsExactly("checkstyle", "pmd");
  }

  @Test
  public void should_check_compatibility_with_sonar_version() {
    assertThat(pluginWithVersion("1.1").isCompatibleWith("1.1")).isTrue();
    assertThat(pluginWithVersion("1.1").isCompatibleWith("1.1.0")).isTrue();
    assertThat(pluginWithVersion("1.0").isCompatibleWith("1.0.0")).isTrue();

    assertThat(pluginWithVersion("1.0").isCompatibleWith("1.1")).isTrue();
    assertThat(pluginWithVersion("1.1.1").isCompatibleWith("1.1.2")).isTrue();
    assertThat(pluginWithVersion("2.0").isCompatibleWith("2.1.0")).isTrue();
    assertThat(pluginWithVersion("3.2").isCompatibleWith("3.2-RC1")).isTrue();
    assertThat(pluginWithVersion("3.2").isCompatibleWith("3.2-RC2")).isTrue();
    assertThat(pluginWithVersion("3.2").isCompatibleWith("3.1-RC2")).isFalse();

    assertThat(pluginWithVersion("1.1").isCompatibleWith("1.0")).isFalse();
    assertThat(pluginWithVersion("2.0.1").isCompatibleWith("2.0.0")).isFalse();
    assertThat(pluginWithVersion("2.10").isCompatibleWith("2.1")).isFalse();
    assertThat(pluginWithVersion("10.10").isCompatibleWith("2.2")).isFalse();

    assertThat(pluginWithVersion("1.1-SNAPSHOT").isCompatibleWith("1.0")).isFalse();
    assertThat(pluginWithVersion("1.1-SNAPSHOT").isCompatibleWith("1.1")).isTrue();
    assertThat(pluginWithVersion("1.1-SNAPSHOT").isCompatibleWith("1.2")).isTrue();
    assertThat(pluginWithVersion("1.0.1-SNAPSHOT").isCompatibleWith("1.0")).isFalse();

    assertThat(pluginWithVersion("3.1-RC2").isCompatibleWith("3.2-SNAPSHOT")).isTrue();
    assertThat(pluginWithVersion("3.1-RC1").isCompatibleWith("3.2-RC2")).isTrue();
    assertThat(pluginWithVersion("3.1-RC1").isCompatibleWith("3.1-RC2")).isTrue();

    assertThat(pluginWithVersion(null).isCompatibleWith("0")).isTrue();
    assertThat(pluginWithVersion(null).isCompatibleWith("3.1")).isTrue();
  }

  static DefaultPluginMetadata pluginWithVersion(String version) {
    return DefaultPluginMetadata.create("foo").setSonarVersion(version);
  }
}
