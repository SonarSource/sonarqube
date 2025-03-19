/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.telemetry.core.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.sonar.telemetry.core.Granularity.ADHOC;

public class FixSuggestionMetric extends InstallationMetric {

  @JsonProperty("fix_suggestion_uuid")
  private String fixSuggestionUuid;

  @JsonProperty("project_uuid")
  private String projectUuid;

  public FixSuggestionMetric(String key, Object value, TelemetryDataType type, String projectUuid, String fixSuggestionUuid) {
    super(key, value, type, ADHOC);
    this.projectUuid = projectUuid;
    this.fixSuggestionUuid = fixSuggestionUuid;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public void setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
  }

  public String getFixSuggestionUuid() {
    return fixSuggestionUuid;
  }

  public void setFixSuggestionUuid(String fixSuggestionUuid) {
    this.fixSuggestionUuid = fixSuggestionUuid;
  }
}
