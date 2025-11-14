/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

public class AnalysisMetric extends Metric {

  @JsonProperty("project_uuid")
  private String projectUuid;

  @JsonProperty("analysis_type")
  private String analysisType;

  public AnalysisMetric(String key, String value, String projectUuid, String analysisType) {
    this.key = key;
    this.value = value;
    this.type = TelemetryDataType.STRING;
    this.granularity = Granularity.ADHOC;
    this.projectUuid = projectUuid;
    this.analysisType = analysisType;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public void setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
  }

  public String getAnalysisType() {
    return analysisType;
  }

  public void setAnalysisType(String analysisType) {
    this.analysisType = analysisType;
  }
}
