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
package org.sonar.ce.task.projectanalysis.analysis;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.task.projectanalysis.component.DefaultBranchImpl;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;

public class ProjectConfigurationFactoryTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @Rule
  public DbTester db = DbTester.create();

  private MapSettings settings = new MapSettings();

  private ProjectConfigurationFactory underTest = new ProjectConfigurationFactory(settings, db.getDbClient());

  @Test
  public void return_global_settings() {
    settings.setProperty("key", "value");
    Configuration config = underTest.newProjectConfiguration(PROJECT_KEY, new DefaultBranchImpl());

    assertThat(config.get("key")).hasValue("value");
  }

  @Test
  public void return_project_settings() {
    ComponentDto project = db.components().insertPrivateProject();
    db.properties().insertProperties(
      newComponentPropertyDto(project).setKey("1").setValue("val1"),
      newComponentPropertyDto(project).setKey("2").setValue("val2"),
      newComponentPropertyDto(project).setKey("3").setValue("val3"));

    Configuration config = underTest.newProjectConfiguration(project.getDbKey(), new DefaultBranchImpl());

    assertThat(config.get("1")).hasValue("val1");
    assertThat(config.get("2")).hasValue("val2");
    assertThat(config.get("3")).hasValue("val3");
  }

  @Test
  public void project_settings_override_global_settings() {
    settings.setProperty("key", "value");
    ComponentDto project = db.components().insertPrivateProject();
    db.properties().insertProperties(newComponentPropertyDto(project).setKey("key").setValue("value2"));

    Configuration projectConfig = underTest.newProjectConfiguration(project.getDbKey(), new DefaultBranchImpl());

    assertThat(projectConfig.get("key")).hasValue("value2");
  }

  @Test
  public void branch_settings() {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    db.properties().insertProperties(newComponentPropertyDto(branch).setKey("sonar.leak.period").setValue("1"));

    Configuration config = underTest.newProjectConfiguration(project.getKey(), createBranch(branch.getBranch(), false));

    assertThat(config.get("sonar.leak.period")).hasValue("1");
  }

  @Test
  public void branch_settings_contains_global_settings() {
    settings.setProperty("global", "global_value");
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    db.properties().insertProperties(newComponentPropertyDto(branch).setKey("sonar.leak.period").setValue("1"));

    Configuration config = underTest.newProjectConfiguration(project.getKey(), createBranch(branch.getBranch(), false));

    assertThat(config.get("global")).hasValue("global_value");
    assertThat(config.get("sonar.leak.period")).hasValue("1");
  }

  @Test
  public void branch_settings_contains_project_settings() {
    ComponentDto project = db.components().insertMainBranch();
    db.properties().insertProperties(newComponentPropertyDto(project).setKey("key").setValue("value"));
    ComponentDto branch = db.components().insertProjectBranch(project);
    db.properties().insertProperties(newComponentPropertyDto(branch).setKey("sonar.leak.period").setValue("1"));

    Configuration config = underTest.newProjectConfiguration(project.getKey(), createBranch(branch.getBranch(), false));

    assertThat(config.get("key")).hasValue("value");
    assertThat(config.get("sonar.leak.period")).hasValue("1");
  }

  @Test
  public void branch_settings_override_project_settings() {
    ComponentDto project = db.components().insertMainBranch();
    db.properties().insertProperties(newComponentPropertyDto(project).setKey("sonar.leak.period").setValue("1"));
    ComponentDto branch = db.components().insertProjectBranch(project);
    db.properties().insertProperties(newComponentPropertyDto(branch).setKey("sonar.leak.period").setValue("2"));

    Configuration config = underTest.newProjectConfiguration(project.getKey(), createBranch(branch.getBranch(), false));

    assertThat(config.get("sonar.leak.period")).hasValue("2");
  }

  @Test
  public void main_branch() {
    ComponentDto project = db.components().insertMainBranch();
    db.properties().insertProperties(newComponentPropertyDto(project).setKey("sonar.leak.period").setValue("1"));
    Branch branch = createBranch("master", true);
    when(branch.isMain()).thenReturn(true);

    Configuration config = underTest.newProjectConfiguration(project.getKey(), createBranch(branch.getName(), true));

    assertThat(config.get("sonar.leak.period")).hasValue("1");
  }

  private static Branch createBranch(String name, boolean isMain) {
    Branch branch = mock(Branch.class);
    when(branch.getName()).thenReturn(name);
    when(branch.isMain()).thenReturn(isMain);
    return branch;
  }
}
