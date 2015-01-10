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
package org.sonar.api.batch.maven;

import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.test.MavenTestUtils;

import static org.assertj.core.api.Assertions.assertThat;


public class MavenPluginTest {

  private MavenPlugin fakePlugin;

  @Before
  public void initPlugin() {
    fakePlugin = new MavenPlugin("foo", "bar", "1.0");
  }

  @Test
  public void getConfigurationXmlNode() {
    assertThat(fakePlugin.getConfigurationXmlNode()).isNotNull();
    assertThat(fakePlugin.getConfigurationXmlNode().getName()).isEqualTo("configuration");
  }

  @Test
  public void removeParameters() {
    fakePlugin
      .setParameter("foo", "bar")
      .setParameter("hello", "world")
      .removeParameters();

    assertThat(fakePlugin.getParameter("foo")).isNull();
    assertThat(fakePlugin.getParameter("hello")).isNull();
    assertThat(fakePlugin.hasConfiguration()).isFalse();
  }

  @Test
  public void shouldWriteAndReadSimpleConfiguration() {
    fakePlugin.setParameter("abc", "test");
    assertThat(fakePlugin.getParameter("abc")).isEqualTo("test");
  }

  @Test
  public void shouldWriteAndReadComplexConfiguration() {
    fakePlugin.setParameter("abc/def/ghi", "test");
    assertThat(fakePlugin.getParameter("abc/def/ghi")).isEqualTo("test");
  }

  @Test
  public void shouldReturnNullWhenChildNotFound() {
    assertThat(fakePlugin.getParameter("abc/def/ghi")).isNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void getChildValueShouldThrowExceptionWhenKeyIsNull() {
    fakePlugin.getParameter(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setChildValueShouldThrowExceptionWhenKeyIsNull() {
    fakePlugin.setParameter(null, null);
  }

  @Test
  public void shouldRemoveParameter() {
    fakePlugin.setParameter("abc", "1");
    assertThat(fakePlugin.getParameter("abc")).isNotNull();

    fakePlugin.removeParameter("abc");
    assertThat(fakePlugin.getParameter("abc")).isNull();
  }

  @Test
  public void shouldRemoveNestedParameter() {
    fakePlugin.setParameter("abc/def", "1");
    assertThat(fakePlugin.getParameter("abc/def")).isNotNull();

    fakePlugin.removeParameter("abc/def");

    assertThat(fakePlugin.getParameter("abc/def")).isNull();
  }

  @Test
  public void shouldRemoveNestedParameterButLeaveTheParent() {
    fakePlugin.setParameter("abc/x", "1");
    fakePlugin.setParameter("abc/y", "2");

    fakePlugin.removeParameter("abc/x");

    assertThat(fakePlugin.getParameter("abc/y")).isNotNull();
  }

  @Test
  public void shouldRemoveUnfoundChildWithoutError() {
    fakePlugin.removeParameter("abc/def");
  }


  @Test
  public void shouldSetParameter() {
    fakePlugin.addParameter("exclude", "abc");
    assertThat(fakePlugin.getParameter("exclude")).isEqualTo("abc");
    assertThat(fakePlugin.getParameters("exclude")).containsOnly("abc");
  }

  @Test
  public void shouldOverrideNestedParameter() {
    fakePlugin.setParameter("excludes/exclude", "abc");
    fakePlugin.setParameter("excludes/exclude", "overridden");
    assertThat(fakePlugin.getParameter("excludes/exclude")).isEqualTo("overridden");
    assertThat(fakePlugin.getParameters("excludes/exclude")).containsOnly("overridden");
  }

  @Test
  public void shouldOverriddeParameter() {
    fakePlugin.setParameter("exclude", "abc");
    fakePlugin.setParameter("exclude", "overridden");
    assertThat(fakePlugin.getParameter("exclude")).isEqualTo("overridden");
    assertThat(fakePlugin.getParameters("exclude")).containsOnly("overridden");
  }

  @Test
  public void shouldAddNestedParameter() {
    fakePlugin.addParameter("excludes/exclude", "abc");
    assertThat(fakePlugin.getParameter("excludes/exclude")).isEqualTo("abc");
    assertThat(fakePlugin.getParameters("excludes/exclude")).containsOnly("abc");
  }

  @Test
  public void shouldAddManyValuesToTheSameParameter() {
    fakePlugin.addParameter("excludes/exclude", "abc");
    fakePlugin.addParameter("excludes/exclude", "def");
    assertThat(fakePlugin.getParameters("excludes/exclude")).containsOnly("abc", "def");
  }

  @Test
  public void defaultParameterIndexIsZero() {
    fakePlugin.addParameter("items/item/entry", "value1");
    fakePlugin.addParameter("items/item/entry", "value2");

    assertThat(fakePlugin.getParameters("items/item/entry")).containsOnly("value1", "value2");
    assertThat(fakePlugin.getParameters("items/item[0]/entry")).containsOnly("value1", "value2");
  }


  @Test
  public void addIndexedParameters() {
    fakePlugin.addParameter("items/item[0]/entry", "value1");
    fakePlugin.addParameter("items/item[1]/entry", "value2");

    assertThat(fakePlugin.getParameter("items/item[0]/entry")).isEqualTo("value1");
    assertThat(fakePlugin.getParameters("items/item[0]/entry")).containsOnly("value1");

    assertThat(fakePlugin.getParameter("items/item[1]/entry")).isEqualTo("value2");
    assertThat(fakePlugin.getParameters("items/item[1]/entry")).containsOnly("value2");

    //ensure that indexes aren't serialized to real configuration
    assertThat(fakePlugin.getPlugin().getConfiguration().toString()).doesNotContain("item[0]");
    assertThat(fakePlugin.getPlugin().getConfiguration().toString()).doesNotContain("item[1]");
  }

  @Test
  public void removeIndexedParameter() {
    fakePlugin.addParameter("items/item[0]/entry", "value1");
    fakePlugin.addParameter("items/item[1]/entry", "value2");

    fakePlugin.removeParameter("items/item[1]");
    fakePlugin.removeParameter("items/notExists");

    assertThat(fakePlugin.getParameter("items/item[0]/entry")).isNotNull();
    assertThat(fakePlugin.getParameter("items/item[1]/entry")).isNull();
    assertThat(fakePlugin.getParameter("items/notExists")).isNull();
  }

  @Test
  public void registerNewPlugin() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "registerNewPlugin.xml");
    MavenPlugin mavenPlugin = MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", true);

    assertThat(mavenPlugin).isNotNull();
    Plugin plugin = MavenUtils.getPlugin(pom.getBuildPlugins(), "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getVersion()).isEqualTo("1.0");
  }

  @Test
  public void overridePluginManagementSection() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "overridePluginManagementSection.xml");
    MavenPlugin mavenPlugin = MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", true);
    assertThat(mavenPlugin).isNotNull();

    Plugin plugin = MavenUtils.getPlugin(pom.getBuildPlugins(), "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getVersion()).isEqualTo("1.0");

    Plugin pluginManagement = MavenUtils.getPlugin(pom.getPluginManagement().getPlugins(), "mygroup", "my.artifact");
    assertThat(pluginManagement).isNull();
  }

  @Test
  public void doNotOverrideVersionFromPluginManagementSection() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "overridePluginManagementSection.xml");
    MavenPlugin mavenPlugin = MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", false);
    assertThat(mavenPlugin).isNotNull();

    Plugin plugin = MavenUtils.getPlugin(pom.getBuildPlugins(), "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getVersion()).isEqualTo("0.9");

    Plugin pluginManagement = MavenUtils.getPlugin(pom.getPluginManagement().getPlugins(), "mygroup", "my.artifact");
    assertThat(pluginManagement).isNull();
  }

  @Test
  public void keepPluginManagementDependencies() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "keepPluginManagementDependencies.xml");
    MavenPlugin mavenPlugin = MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", false);
    assertThat(mavenPlugin).isNotNull();

    Plugin plugin = MavenUtils.getPlugin(pom.getBuildPlugins(), "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getVersion()).isEqualTo("0.9");
    assertThat(plugin.getDependencies().size()).isEqualTo(1);
  }

  @Test
  public void keepPluginDependencies() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "keepPluginDependencies.xml");
    MavenPlugin mavenPlugin = MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", false);
    assertThat(mavenPlugin).isNotNull();

    Plugin plugin = MavenUtils.getPlugin(pom.getBuildPlugins(), "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getVersion()).isEqualTo("0.9");
    assertThat(plugin.getDependencies().size()).isEqualTo(1);
  }

  @Test
  public void mergeSettings() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "mergeSettings.xml");
    MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", false);

    MavenPlugin plugin = MavenPlugin.getPlugin(pom, "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getPlugin().getVersion()).isEqualTo("0.9");
    assertThat(plugin.getParameter("foo")).isEqualTo("bar");
  }

  @Test
  public void overrideVersionFromPluginManagement() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "overrideVersionFromPluginManagement.xml");
    MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", true);

    MavenPlugin plugin = MavenPlugin.getPlugin(pom, "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getPlugin().getVersion()).isEqualTo("1.0");
    assertThat(plugin.getParameter("foo")).isEqualTo("bar");
  }

  @Test
  public void overrideVersion() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "overrideVersion.xml");
    MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", true);

    MavenPlugin plugin = MavenPlugin.getPlugin(pom, "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getPlugin().getVersion()).isEqualTo("1.0");
    assertThat(plugin.getParameter("foo")).isEqualTo("bar");
  }

  @Test
  public void getConfigurationFromReport() {
    MavenProject pom = MavenTestUtils.loadPom(getClass(), "getConfigurationFromReport.xml");
    MavenPlugin.registerPlugin(pom, "mygroup", "my.artifact", "1.0", true);

    assertThat(pom.getBuildPlugins().size()).isEqualTo(1);
    assertThat(pom.getReportPlugins().size()).isEqualTo(0);

    MavenPlugin plugin = MavenPlugin.getPlugin(pom, "mygroup", "my.artifact");
    assertThat(plugin).isNotNull();
    assertThat(plugin.getPlugin().getVersion()).isEqualTo("1.0");
    assertThat(plugin.getParameter("foo")).isEqualTo("bar");
  }
}
