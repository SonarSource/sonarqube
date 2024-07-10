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
package org.sonar.telemetry.metrics.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonar.telemetry.Granularity;
import org.sonar.telemetry.TelemetryDataType;

public class LanguageMetric extends Metric {

  @JsonProperty("language")
  private String language;

  public LanguageMetric(String key, Object value, String language, TelemetryDataType type, Granularity granularity) {
    this.key = key;
    this.value = value;
    this.language = language;
    this.type = type;
    this.granularity = granularity;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }
}
