/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.property.PropertyTesting.newComponentPropertyDto;

public class ProjectConfigurationFactoryTest {
  @Rule
  public DbTester db = DbTester.create();

  private final MapSettings settings = new MapSettings();
  private final ProjectConfigurationFactory underTest = new ProjectConfigurationFactory(settings, db.getDbClient());

  @Test
  public void return_global_settings() {
    settings.setProperty("key", "value");
    Configuration config = underTest.newProjectConfiguration("unknown");

    assertThat(config.get("key")).hasValue("value");
  }

  @Test
  public void return_project_settings() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("1").setValue("val1"),
      newComponentPropertyDto(project).setKey("2").setValue("val2"),
      newComponentPropertyDto(project).setKey("3").setValue("val3"));

    Configuration config = underTest.newProjectConfiguration(project.getUuid());

    assertThat(config.get("1")).hasValue("val1");
    assertThat(config.get("2")).hasValue("val2");
    assertThat(config.get("3")).hasValue("val3");
  }

  @Test
  public void project_settings_override_global_settings() {
    settings.setProperty("key", "value");
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    db.properties().insertProperties(null, project.getKey(), project.getName(), project.getQualifier(),
      newComponentPropertyDto(project).setKey("key").setValue("value2"));

    Configuration projectConfig = underTest.newProjectConfiguration(project.getUuid());

    assertThat(projectConfig.get("key")).hasValue("value2");
  }
}
