/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.properties;

import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProjectSettingsFactoryTest {

  static final String PROJECT_KEY = "PROJECT_KEY";

  Settings settings = new Settings();
  PropertiesDao dao = mock(PropertiesDao.class);

  ProjectSettingsFactory underTest = new ProjectSettingsFactory(settings, dao);

  @Test
  public void return_global_settings() {
    settings.setProperty("key", "value");
    Settings projectSettings = underTest.newProjectSettings(PROJECT_KEY);

    assertThat(projectSettings.getProperties()).hasSize(1);
    assertThat(projectSettings.getString("key")).isEqualTo("value");
  }

  @Test
  public void return_project_settings() {
    when(dao.selectProjectProperties(PROJECT_KEY)).thenReturn(newArrayList(
      new PropertyDto().setKey("1").setValue("val1"),
      new PropertyDto().setKey("2").setValue("val2"),
      new PropertyDto().setKey("3").setValue("val3"))
      );

    Settings projectSettings = underTest.newProjectSettings(PROJECT_KEY);

    assertThat(projectSettings.getString("1")).isEqualTo("val1");
    assertThat(projectSettings.getString("2")).isEqualTo("val2");
    assertThat(projectSettings.getString("3")).isEqualTo("val3");
  }

  @Test
  public void project_settings_override_global_settings() {
    settings.setProperty("key", "value");
    when(dao.selectProjectProperties(PROJECT_KEY)).thenReturn(newArrayList(
        new PropertyDto().setKey("key").setValue("value2"))
    );

    Settings projectSettings = underTest.newProjectSettings(PROJECT_KEY);
    assertThat(projectSettings.getString("key")).isEqualTo("value2");
  }
}
