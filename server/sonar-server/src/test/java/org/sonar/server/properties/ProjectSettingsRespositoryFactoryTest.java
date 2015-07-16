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

package org.sonar.server.properties;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.PropertyDto;

import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ProjectSettingsRespositoryFactoryTest {

  private ProjectSettingsFactory underTest;

  @Before
  public void before() {
    Settings settings = mock(Settings.class);
    PropertiesDao dao = mock(PropertiesDao.class);

    this.underTest = new ProjectSettingsFactory(settings, dao);
  }

  @Test
  public void newProjectSettings_returns_a_ProjectSettings() {
    Settings projectSettings = underTest.newProjectSettings("PROJECT_KEY");

    assertThat(projectSettings).isInstanceOf(ProjectSettings.class);
  }

  @Test
  public void transform_empty_list_into_empty_map() {
    Map<String, String> propertyMap = underTest.getPropertyMap(Lists.<PropertyDto>newArrayList());

    assertThat(propertyMap).isEmpty();
  }

  @Test
  public void transform_list_of_properties_in_map_key_value() {
    PropertyDto property1 = new PropertyDto().setKey("1").setValue("val1");
    PropertyDto property2 = new PropertyDto().setKey("2").setValue("val2");
    PropertyDto property3 = new PropertyDto().setKey("3").setValue("val3");

    Map<String, String> propertyMap = underTest.getPropertyMap(newArrayList(property1, property2, property3));

    assertThat(propertyMap.get("1")).isEqualTo("val1");
    assertThat(propertyMap.get("2")).isEqualTo("val2");
    assertThat(propertyMap.get("3")).isEqualTo("val3");
  }
}
