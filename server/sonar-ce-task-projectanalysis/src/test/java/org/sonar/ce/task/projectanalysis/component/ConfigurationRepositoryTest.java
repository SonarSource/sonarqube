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
package org.sonar.ce.task.projectanalysis.component;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.ProjectConfigurationFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.project.Project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;

public class ConfigurationRepositoryTest {
  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  private final DbClient dbClient = db.getDbClient();
  private final MapSettings globalSettings = new MapSettings();
  private final Project project = Project.from(newPrivateProjectDto());
  private final Component root = mock(Component.class);
  private ConfigurationRepository underTest;

  @Before
  public void setUp() {
    analysisMetadataHolder.setProject(project);
    when(root.getUuid()).thenReturn(project.getUuid());
    underTest = new ConfigurationRepositoryImpl(analysisMetadataHolder, new ProjectConfigurationFactory(globalSettings, dbClient));
  }

  @Test
  public void get_project_settings_from_global_settings() {
    globalSettings.setProperty("key", "value");

    Configuration config = underTest.getConfiguration();

    assertThat(config.get("key")).hasValue("value");
  }

  @Test
  public void get_project_settings_from_db() {
    insertComponentProperty(project.getUuid(), "key", "value");

    Configuration config = underTest.getConfiguration();

    assertThat(config.get("key")).hasValue("value");
  }

  @Test
  public void call_twice_get_project_settings() {
    globalSettings.setProperty("key", "value");

    Configuration config = underTest.getConfiguration();
    assertThat(config.get("key")).hasValue("value");

    config = underTest.getConfiguration();
    assertThat(config.get("key")).hasValue("value");
  }

  @Test
  public void project_settings_override_global_settings() {
    globalSettings.setProperty("key", "value1");
    insertComponentProperty(project.getUuid(), "key", "value2");

    Configuration config = underTest.getConfiguration();
    assertThat(config.get("key")).hasValue("value2");
  }

  @Test
  public void project_settings_are_cached_to_avoid_db_access() {
    insertComponentProperty(project.getUuid(), "key", "value");

    Configuration config = underTest.getConfiguration();
    assertThat(config.get("key")).hasValue("value");

    db.executeUpdateSql("delete from properties");
    db.commit();

    assertThat(config.get("key")).hasValue("value");
  }

  @Test
  public void branch_settings() {
    globalSettings.setProperty("global", "global value");
    insertComponentProperty(project.getUuid(), "project", "project value");
    insertComponentProperty(root.getUuid(), "branch", "branch value");

    Configuration config = underTest.getConfiguration();

    assertThat(config.get("global")).hasValue("global value");
    assertThat(config.get("project")).hasValue("project value");
    assertThat(config.get("branch")).hasValue("branch value");
  }

  private void insertComponentProperty(String componentUuid, String propertyKey, String propertyValue) {
    db.properties().insertProperties(null, null, null, null,
      new PropertyDto().setKey(propertyKey).setValue(propertyValue).setEntityUuid(componentUuid));
  }
}
