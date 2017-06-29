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
package org.sonar.server.setting;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.ce.settings.ProjectConfigurationFactory;
import org.sonar.db.DbClient;
import org.sonar.db.property.PropertyDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectSettingsFactoryTest {

  static final String PROJECT_KEY = "PROJECT_KEY";

  MapSettings settings = new MapSettings();
  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);

  ProjectConfigurationFactory underTest = new ProjectConfigurationFactory(settings, dbClient);

  @Test
  public void return_global_settings() {
    settings.setProperty("key", "value");
    Configuration config = underTest.newProjectConfiguration(PROJECT_KEY);

    assertThat(config.get("key")).hasValue("value");
  }

  @Test
  public void return_project_settings() {
    when(dbClient.propertiesDao().selectProjectProperties(PROJECT_KEY)).thenReturn(newArrayList(
      new PropertyDto().setKey("1").setValue("val1"),
      new PropertyDto().setKey("2").setValue("val2"),
      new PropertyDto().setKey("3").setValue("val3")));

    Configuration config = underTest.newProjectConfiguration(PROJECT_KEY);

    assertThat(config.get("1")).hasValue("val1");
    assertThat(config.get("2")).hasValue("val2");
    assertThat(config.get("3")).hasValue("val3");
  }

  @Test
  public void project_settings_override_global_settings() {
    settings.setProperty("key", "value");
    when(dbClient.propertiesDao().selectProjectProperties(PROJECT_KEY)).thenReturn(newArrayList(
      new PropertyDto().setKey("key").setValue("value2")));

    Configuration projectConfig = underTest.newProjectConfiguration(PROJECT_KEY);
    assertThat(projectConfig.get("key")).hasValue("value2");
  }
}
