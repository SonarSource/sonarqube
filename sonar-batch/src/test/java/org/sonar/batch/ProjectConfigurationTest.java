/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.batch;

import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.api.resources.Project;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ProjectConfigurationTest extends AbstractDbUnitTestCase {

  @Test
  public void loadSystemProperties() {
    System.setProperty("foo", "bar");
    setupData("global-properties");

    ProjectConfiguration config = new ProjectConfiguration(getSession(), newProject());
    assertThat(config.getString("foo"), is("bar"));
    assertNull(config.getString("unknown"));
  }

  @Test
  public void loadDatabaseProperties() {
    setupData("global-properties");
    ProjectConfiguration config = new ProjectConfiguration(getSession(), newProject());
    assertThat(config.getString("key1"), is("value1"));
    assertNull(config.getString("key3"));
  }

  @Test
  public void loadProjectDatabaseProperties() {
    setupData("project-properties");
    ProjectConfiguration config = new ProjectConfiguration(getSession(), newProject());
    assertThat(config.getString("key1"), is("overriden_value1"));
    assertThat(config.getString("key2"), is("value2"));
    assertThat(config.getString("key3"), is("value3"));
  }

  @Test
  public void loadModuleDatabaseProperties() {
    setupData("modules-properties");
    ProjectConfiguration moduleConfig = new ProjectConfiguration(getSession(), newModule());

    assertThat(moduleConfig.getString("key1"), is("project_value_1"));
    assertThat(moduleConfig.getString("key2"), is("value_2"));
    assertThat(moduleConfig.getString("key3"), is("module_value_3"));
    assertThat(moduleConfig.getString("key4"), is("module_value_4"));
  }

  @Test
  public void mavenSettingsLoadedBeforeGlobalSettings() {
    setupData("global-properties");
    Project project = newProject();
    project.getPom().getProperties().put("key1", "maven1");
    ProjectConfiguration config = new ProjectConfiguration(getSession(), project);
    assertThat(config.getString("key1"), is("maven1"));
  }

  @Test
  public void projectSettingsLoadedBeforeMavenSettings() {
    setupData("project-properties");
    Project project = newProject();
    project.getPom().getProperties().put("key1", "maven1");
    ProjectConfiguration config = new ProjectConfiguration(getSession(), project);
    assertThat(config.getString("key1"), is("overriden_value1"));
  }

  @Test
  public void addPropertyAtRuntime() {
    setupData("global-properties");
    ProjectConfiguration config = new ProjectConfiguration(getSession(), newProject());

    config.getInMemoryConfiguration().setProperty("new-key", "new-value");
    assertThat(config.getString("new-key"), is("new-value"));
  }

  @Test
  public void overridePropertyAtRuntime() {
    setupData("global-properties");
    ProjectConfiguration config = new ProjectConfiguration(getSession(), newProject());

    assertThat(config.getString("key1"), is("value1"));
    config.setProperty("key1", "new1");
    assertThat(config.getString("key1"), is("new1"));
  }

  private Project newProject() {
    return new Project("mygroup:myproject").setPom(new MavenProject());
  }

  private Project newModule() {
    Project module = new Project("mygroup:mymodule").setPom(new MavenProject());
    module.setParent(newProject());
    return module;
  }
}
