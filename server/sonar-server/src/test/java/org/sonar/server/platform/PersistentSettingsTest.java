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
package org.sonar.server.platform;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PersistentSettingsTest {

  private PropertiesDao dao;
  private ServerSettings settings;

  @Before
  public void init() throws URISyntaxException {
    dao = mock(PropertiesDao.class);

    settings = new ServerSettings(
      new PropertyDefinitions(),
      new Properties());
  }

  @Test
  public void load_database_properties_at_startup() {
    when(dao.selectGlobalProperties()).thenReturn(Arrays.asList(
      new PropertyDto().setKey("in_db").setValue("bar")
      ));

    PersistentSettings persistentSettings = new PersistentSettings(dao, settings);
    persistentSettings.start();

    assertThat(settings.getString("in_db")).isEqualTo("bar");
  }

  @Test
  public void saveProperty() {
    PersistentSettings persistentSettings = new PersistentSettings(dao, settings);
    persistentSettings.saveProperty("foo", "bar");

    // kept in memory cache and persisted in db
    assertThat(settings.getString("foo")).isEqualTo("bar");
    verify(dao).setProperty(argThat(new ArgumentMatcher<PropertyDto>() {
      @Override
      public boolean matches(Object o) {
        PropertyDto dto = (PropertyDto) o;
        return dto.getKey().equals("foo");
      }
    }));
  }

  @Test
  public void deleteProperty() {
    settings.setProperty("foo", "bar");
    assertThat(settings.hasKey("foo")).isTrue();

    PersistentSettings persistentSettings = new PersistentSettings(dao, settings);
    persistentSettings.deleteProperty("foo");

    assertThat(settings.hasKey("foo")).isFalse();
    verify(dao).deleteGlobalProperty("foo");
  }

  @Test
  public void deleteProperties() {
    settings.setProperty("foo", "bar");
    assertThat(settings.hasKey("foo")).isTrue();

    PersistentSettings persistentSettings = new PersistentSettings(dao, settings);
    persistentSettings.deleteProperties();

    assertThat(settings.getProperties()).isEmpty();
    verify(dao).deleteGlobalProperties();
  }

  @Test
  public void shortcuts_on_settings() {
    settings.setProperty("foo", "bar");
    assertThat(settings.hasKey("foo")).isTrue();

    PersistentSettings persistentSettings = new PersistentSettings(dao, settings);

    assertThat(persistentSettings.getProperties()).isEqualTo(settings.getProperties());
    assertThat(persistentSettings.getString("foo")).isEqualTo("bar");
    assertThat(persistentSettings.getSettings()).isEqualTo(settings);
  }

  @Test
  public void saveProperties() {
    PersistentSettings persistentSettings = new PersistentSettings(dao, settings);
    ImmutableMap<String, String> props = ImmutableMap.of("foo", "bar");
    persistentSettings.saveProperties(props);

    assertThat(settings.getString("foo")).isEqualTo("bar");
    verify(dao).saveGlobalProperties(props);
  }

}
