/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.telemetry.core;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents the dimension of the data provided by a {@link TelemetryDataProvider}.
 * {@link Dimension#PROJECT}, {@link Dimension#LANGUAGE} and {@link Dimension#USER} should not provide aggregated data.
 * For aggregated data (i.e. average number of lines of code per project), use #INSTALLATION.
 */
public enum Dimension {
  INSTALLATION("installation"),
  USER("user"),
  PROJECT("project"),
  LANGUAGE("language"),
  ANALYSIS("analysis");

  private final String value;

  Dimension(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  public static Dimension fromValue(String value) {
    for (Dimension dimension : Dimension.values()) {
      if (dimension.value.equalsIgnoreCase(value)) {
        return dimension;
      }
    }
    throw new IllegalArgumentException("Unknown dimension value: " + value);
  }
}
