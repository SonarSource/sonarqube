/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.config;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;

public class TelemetryProperties {

  public static final String PROP_ENABLE = "sonar.telemetry.enable";
  public static final String PROP_FREQUENCY = "sonar.telemetry.frequencyInSeconds";
  public static final String PROP_URL = "sonar.telemetry.url";

  private TelemetryProperties() {
    // only static stuff
  }

  public static List<PropertyDefinition> all() {
    return ImmutableList.of(
      PropertyDefinition.builder(PROP_ENABLE)
        .defaultValue(Boolean.toString(true))
        .type(PropertyType.BOOLEAN)
        .name("Share SonarQube statistics")
        .description("By sharing anonymous SonarQube statistics, you help us understand how SonarQube is used so we can improve the plugin to work even better for you. " +
          "We don't collect source code or IP addresses. And we don't share the data with anyone else.")
        .hidden()
        .build(),
      PropertyDefinition.builder(PROP_FREQUENCY)
        // 6 hours in seconds
        .defaultValue("21600")
        .type(PropertyType.INTEGER)
        .name("Frequency of telemetry checks, in seconds")
        .hidden()
        .build(),
      PropertyDefinition.builder(PROP_URL)
        .defaultValue("https://telemetry.sonarsource.com/sonarqube")
        .type(PropertyType.STRING)
        .name("URL where telemetry data is sent")
        .hidden()
        .build()
    );

  }
}
