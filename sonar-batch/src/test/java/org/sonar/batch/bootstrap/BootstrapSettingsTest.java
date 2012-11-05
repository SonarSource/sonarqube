/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrap;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;

import static org.fest.assertions.Assertions.assertThat;

public class BootstrapSettingsTest {

  @Test
  public void shouldLoadBuildModel() {
    // this is the project as defined in the build tool
    ProjectDefinition project = ProjectDefinition.create();
    project.setProperty("foo", "bar");

    ProjectReactor reactor = new ProjectReactor(project);
    BootstrapSettings settings = new BootstrapSettings(new PropertyDefinitions(), reactor, new BaseConfiguration());

    assertThat(settings.getString("foo")).isEqualTo("bar");
  }

  @Test
  public void environmentShouldOverrideBuildModel() {
    ProjectDefinition project = ProjectDefinition.create();
    project.setProperty("BootstrapSettingsTest.testEnv", "build");
    System.setProperty("BootstrapSettingsTest.testEnv", "env");

    ProjectReactor reactor = new ProjectReactor(project);
    BootstrapSettings settings = new BootstrapSettings(new PropertyDefinitions(), reactor, new BaseConfiguration());

    assertThat(settings.getString("BootstrapSettingsTest.testEnv")).isEqualTo("env");
  }

  @Test
  public void shouldForwardToCommonsConfiguration() {
    ProjectDefinition project = ProjectDefinition.create();
    project.setProperty("hello", "world");
    project.setProperty("foo", "bar");
    ProjectReactor reactor = new ProjectReactor(project);
    BaseConfiguration deprecatedConfiguration = new BaseConfiguration();
    BootstrapSettings settings = new BootstrapSettings(new PropertyDefinitions(), reactor, deprecatedConfiguration);

    assertThat(deprecatedConfiguration.getString("hello")).isEqualTo("world");
    assertThat(deprecatedConfiguration.getString("foo")).isEqualTo("bar");

    settings.removeProperty("foo");
    assertThat(deprecatedConfiguration.getString("foo")).isNull();
    assertThat(deprecatedConfiguration.getString("hello")).isEqualTo("world");

    settings.clear();
    assertThat(deprecatedConfiguration.getString("foo")).isNull();
    assertThat(deprecatedConfiguration.getString("hello")).isNull();
  }
}
