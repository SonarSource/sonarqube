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
package org.sonar.server.platform.monitoring;

import java.util.SortedMap;
import org.junit.Test;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;

import static org.apache.commons.lang.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class SettingsMonitorTest {

  private static final String PASSWORD_PROPERTY = "sonar.password";

  PropertyDefinitions defs = new PropertyDefinitions(PropertyDefinition.builder(PASSWORD_PROPERTY).type(PropertyType.PASSWORD).build());
  Settings settings = new MapSettings(defs);
  SettingsMonitor underTest = new SettingsMonitor(settings);

  @Test
  public void return_properties_and_sort_by_key() {
    settings.setProperty("foo", "foo value");
    settings.setProperty("bar", "bar value");

    SortedMap<String, Object> attributes = underTest.attributes();
    assertThat(attributes).containsExactly(entry("bar", "bar value"), entry("foo", "foo value"));
  }

  @Test
  public void truncate_long_property_values() {
    settings.setProperty("foo", repeat("abcde", 1_000));

    String value = (String) underTest.attributes().get("foo");
    assertThat(value).hasSize(SettingsMonitor.MAX_VALUE_LENGTH).startsWith("abcde");
  }

  @Test
  public void exclude_password_properties() {
    settings.setProperty(PASSWORD_PROPERTY, "abcde");

    assertThat(underTest.attributes()).isEmpty();
  }

  @Test
  public void test_monitor_name() {
    assertThat(underTest.name()).isEqualTo("Settings");

  }
}
