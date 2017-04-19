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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.ce.settings.ProjectSettingsFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.computation.task.projectanalysis.component.Component.Type.PROJECT;


public class SettingsRepositoryTest {

  private static final Component ROOT = ReportComponent.builder(PROJECT, 1).setKey("ROOT").build();

  @Rule
  public final DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbClient dbClient = dbTester.getDbClient();

  DbSession session;

  MapSettings globalSettings;

  SettingsRepository underTest;

  @Before
  public void createDao() {
    globalSettings = new MapSettings();
    session = dbClient.openSession(false);
    underTest = new SettingsRepositoryImpl(new ProjectSettingsFactory(globalSettings, dbClient));
  }

  @After
  public void tearDown() {
    session.close();
  }

  @Test
  public void get_project_settings_from_global_settings() {
    globalSettings.setProperty("key", "value");

    Settings settings = underTest.getSettings(ROOT);

    assertThat(settings.getString("key")).isEqualTo("value");
  }

  @Test
  public void get_project_settings_from_db() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(dbTester.organizations().insert()).setKey(ROOT.getKey());
    dbClient.componentDao().insert(session, project);
    dbClient.propertiesDao().saveProperty(session, new PropertyDto().setResourceId(project.getId()).setKey("key").setValue("value"));
    session.commit();

    Settings settings = underTest.getSettings(ROOT);

    assertThat(settings.getString("key")).isEqualTo("value");
  }

  @Test
  public void call_twice_get_project_settings() {
    globalSettings.setProperty("key", "value");

    Settings settings = underTest.getSettings(ROOT);
    assertThat(settings.getString("key")).isEqualTo("value");

    settings = underTest.getSettings(ROOT);
    assertThat(settings.getString("key")).isEqualTo("value");
  }
}
