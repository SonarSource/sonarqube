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

import org.sonar.api.config.Configuration;

class TelemetryFrequency {
  private final Configuration config;
  private Long frequency;

  TelemetryFrequency(Configuration config) {
    this.config = config;
  }

  long get() {
    if (frequency == null) {
      frequency = config.getLong(TelemetryProperties.PROP_FREQUENCY)
        .orElseThrow(() -> new IllegalStateException(String.format("Setting '%s' must be provided.", TelemetryProperties.PROP_FREQUENCY)));
    }

    return frequency;
  }
}
