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

package org.sonar.server.telemetry;

import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;

import static org.assertj.core.api.Assertions.assertThat;

public class TelemetryPropertiesTest {

  Configuration underTest = new MapSettings(new PropertyDefinitions(TelemetryProperties.class)).asConfig();

  @Test
  public void default_frequency_is_6_hours_in_seconds() {
    assertThat(underTest.getInt("sonar.telemetry.frequency")).contains(6 * 60 * 60);
  }

  @Test
  public void default_url_point_to_telemetry_server() {
    assertThat(underTest.get("sonar.telemetry.url")).contains("https://telemetry.sonarsource.com/sonarqube");
  }
}
