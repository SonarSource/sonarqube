/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.component;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.System2;
import org.sonar.ce.settings.ProjectConfigurationFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.task.projectanalysis.analysis.Project;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigurationRepositoryTest {

  private static Project PROJECT = new Project("UUID", "KEY", "NAME");

  @Rule
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private MapSettings globalSettings = new MapSettings();
  private AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule().setProject(PROJECT);
  private ConfigurationRepository underTest = new ConfigurationRepositoryImpl(analysisMetadataHolder, new ProjectConfigurationFactory(globalSettings, dbClient));

  @Test
  public void get_project_settings_from_global_settings() {
    globalSettings.setProperty("key", "value");

    Configuration config = underTest.getConfiguration();

    assertThat(config.get("key")).hasValue("value");
  }

  @Test
  public void get_project_settings_from_db() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey(PROJECT.getKey()));
    insertProjectProperty(project, "key", "value");

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
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey(PROJECT.getKey()));
    insertProjectProperty(project, "key", "value2");

    Configuration config = underTest.getConfiguration();
    assertThat(config.get("key")).hasValue("value2");
  }

  @Test
  public void project_settings_are_cached_to_avoid_db_access() {
    ComponentDto project = db.components().insertPrivateProject(p -> p.setDbKey(PROJECT.getKey()));
    insertProjectProperty(project, "key", "value");

    Configuration config = underTest.getConfiguration();
    assertThat(config.get("key")).hasValue("value");

    db.executeUpdateSql("delete from properties");
    db.commit();

    assertThat(config.get("key")).hasValue("value");
  }

  private void insertProjectProperty(ComponentDto project, String propertyKey, String propertyValue) {
    db.properties().insertProperties(new PropertyDto().setKey(propertyKey).setValue(propertyValue).setResourceId(project.getId()));
  }
}
