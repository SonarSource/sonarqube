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

import org.junit.jupiter.api.Test;
import org.sonar.telemetry.core.Granularity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.telemetry.core.TelemetryDataType.STRING;

class FixSuggestionMetricTest {

  @Test
  void getters() {
    FixSuggestionMetric metric = new FixSuggestionMetric("ai_codefix.suggestion_rule_key", "rule:key", STRING, "projectUuid", "fixSuggestionUuid");

    assertThat(metric.getKey()).isEqualTo("ai_codefix.suggestion_rule_key");
    assertThat(metric.getValue()).isEqualTo("rule:key");
    assertThat(metric.getProjectUuid()).isEqualTo("projectUuid");
    assertThat(metric.getGranularity()).isEqualTo(Granularity.ADHOC);
    assertThat(metric.getType()).isEqualTo(STRING);
    assertThat(metric.getFixSuggestionUuid()).isEqualTo("fixSuggestionUuid");
  }

  @Test
  void setters() {
    FixSuggestionMetric metric = new FixSuggestionMetric("ai_codefix.suggestion_rule_key", "rule:key", STRING, "projectUuid", "fixSuggestionUuid");
    metric.setProjectUuid("newProjectUuid");
    metric.setFixSuggestionUuid("newFixSuggestionUuid");

    assertThat(metric.getProjectUuid()).isEqualTo("newProjectUuid");
    assertThat(metric.getFixSuggestionUuid()).isEqualTo("newFixSuggestionUuid");
  }
}
