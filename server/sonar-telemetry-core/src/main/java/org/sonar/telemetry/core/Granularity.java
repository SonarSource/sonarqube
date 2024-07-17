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
 * Represent the granularity of the data provided by a {@link TelemetryDataProvider}. This both defines the time period between to pushes to
 * telemetry server for a given metric and the time period that the data represents.
 * Modifying this enum needs to be discussed beforehand with Data Platform team.
 */
public enum Granularity {
  DAILY("daily"),
  WEEKLY("weekly"),
  MONTHLY("monthly");

  private final String value;

  Granularity(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
