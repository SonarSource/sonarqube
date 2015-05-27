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

package org.sonar.server.computation.component;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;
import org.sonar.server.component.ComponentTesting;
import org.sonar.server.component.db.ComponentDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.properties.ProjectSettingsFactory;
import org.sonar.test.DbTests;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class ProjectSettingsRepositoryTest {

  private static final String PROJECT_KEY = "PROJECT_KEY";

  @ClassRule
  public static final DbTester dbTester = new DbTester();

  DbClient dbClient;

  DbSession session;

  Settings globalSettings;

  ProjectSettingsRepository sut;

  @Before
  public void createDao() {
    dbTester.truncateTables();
    globalSettings = new Settings();
    PropertiesDao propertiesDao = new PropertiesDao(dbTester.myBatis());
    dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), propertiesDao, new ComponentDao());
    session = dbClient.openSession(false);
    sut = new ProjectSettingsRepository(new ProjectSettingsFactory(globalSettings, propertiesDao));
  }

  @After
  public void tearDown() throws Exception {
    session.close();
  }

  @Test
  public void get_project_settings_from_global_settings() throws Exception {
    globalSettings.setProperty("key", "value");

    Settings settings = sut.getProjectSettings(PROJECT_KEY);

    assertThat(settings.getString("key")).isEqualTo("value");
  }

  @Test
  public void get_project_settings_from_db() throws Exception {
    ComponentDto project = ComponentTesting.newProjectDto().setKey(PROJECT_KEY);
    dbClient.componentDao().insert(session, project);
    dbClient.propertiesDao().setProperty(new PropertyDto().setResourceId(project.getId()).setKey("key").setValue("value"), session);
    session.commit();

    Settings settings = sut.getProjectSettings(PROJECT_KEY);

    assertThat(settings.getString("key")).isEqualTo("value");
  }

  @Test
  public void call_twice_get_project_settings() throws Exception {
    globalSettings.setProperty("key", "value");

    Settings settings = sut.getProjectSettings(PROJECT_KEY);
    assertThat(settings.getString("key")).isEqualTo("value");

    settings = sut.getProjectSettings(PROJECT_KEY);
    assertThat(settings.getString("key")).isEqualTo("value");
  }
}
