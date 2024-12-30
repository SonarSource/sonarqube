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

package org.sonar.telemetry.core.schema;

import org.junit.jupiter.api.Test;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataType;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisMetricTest {

  @Test
  void getters() {
    AnalysisMetric metric = new AnalysisMetric("memory", "100", "projectUuid", "analysisType");

    assertThat(metric.getKey()).isEqualTo("memory");
    assertThat(metric.getValue()).isEqualTo("100");
    assertThat(metric.getProjectUuid()).isEqualTo("projectUuid");
    assertThat(metric.getAnalysisType()).isEqualTo("analysisType");
    assertThat(metric.getGranularity()).isEqualTo(Granularity.ADHOC);
    assertThat(metric.getType()).isEqualTo(TelemetryDataType.STRING);
  }

  @Test
  void setters() {
    AnalysisMetric metric = new AnalysisMetric("memory", "100", "projectUuid", "analysisType");
    metric.setProjectUuid("newProjectUuid");
    metric.setAnalysisType("newAnalysisType");

    assertThat(metric.getProjectUuid()).isEqualTo("newProjectUuid");
    assertThat(metric.getAnalysisType()).isEqualTo("newAnalysisType");
  }
}
