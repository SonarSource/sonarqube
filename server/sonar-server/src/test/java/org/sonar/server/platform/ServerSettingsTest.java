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
import org.sonar.api.config.PropertyDefinitions;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ServerSettingsTest {

  Properties properties;

  ServerSettings settings;

  @Before
  public void before() throws Exception {
    properties = new Properties();
    properties.put("hello", "world");
    properties.put("in_file", "true");
    properties.put("ServerSettingsTestEnv", "in_file");
    settings = new ServerSettings(new PropertyDefinitions(), properties);
  }

  @Test
  public void load_properties_file() {
    assertThat(settings.getString("hello")).isEqualTo("world");
  }

  @Test
  public void activate_database_settings() {
    Map<String, String> databaseProperties = ImmutableMap.of("in_db", "true");
    settings.activateDatabaseSettings(databaseProperties);

    assertThat(settings.getString("in_db")).isEqualTo("true");
  }

  @Test
  public void file_settings_override_db_settings() {
    assertThat(settings.getString("in_file")).isEqualTo("true");

    Map<String, String> databaseProperties = ImmutableMap.of("in_file", "false");
    settings.activateDatabaseSettings(databaseProperties);

    assertThat(settings.getString("in_file")).isEqualTo("true");
  }

}
