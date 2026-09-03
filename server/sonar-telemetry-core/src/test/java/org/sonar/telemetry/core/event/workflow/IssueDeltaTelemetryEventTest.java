/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.telemetry.core.event.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.telemetry.core.event.workflow.IssueDeltaTelemetryEvent.RuleDeltaCounts;

import static org.assertj.core.api.Assertions.assertThat;

class IssueDeltaTelemetryEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serialize_shouldUseSnakeCaseKeysAndNoTypeMetadataLeak() {
    IssueDeltaTelemetryEvent event = new IssueDeltaTelemetryEvent(
      "project-uuid",
      "branch-uuid",
      "BRANCH",
      "merge-branch-uuid",
      1_700_000_000_000L,
      List.of(new RuleDeltaCounts("java:S1234", 10, 2, 5, 1, 3, 5)));

    JsonNode json = objectMapper.valueToTree(event);

    assertThat(json.get("project_uuid").asText()).isEqualTo("project-uuid");
    assertThat(json.get("branch_uuid").asText()).isEqualTo("branch-uuid");
    assertThat(json.get("branch_type").asText()).isEqualTo("BRANCH");
    assertThat(json.get("merge_branch_uuid").asText()).isEqualTo("merge-branch-uuid");
    assertThat(json.get("analysis_date").asLong()).isEqualTo(1_700_000_000_000L);
    assertThat(json.get("rules")).hasSize(1);

    JsonNode rule = json.get("rules").get(0);
    assertThat(rule.get("plugin_rule_key").asText()).isEqualTo("java:S1234");
    assertThat(rule.get("new_open_count").asInt()).isEqualTo(10);
    assertThat(rule.get("new_in_sandbox_count").asInt()).isEqualTo(2);
    assertThat(rule.get("new_fixed_count").asInt()).isEqualTo(5);
    assertThat(rule.get("new_fixed_in_1_days").asInt()).isEqualTo(1);
    assertThat(rule.get("new_fixed_in_7_days").asInt()).isEqualTo(3);
    assertThat(rule.get("new_fixed_in_30_days").asInt()).isEqualTo(5);

    assertThat(fieldNames(json)).containsExactlyInAnyOrder(
      "project_uuid", "branch_uuid", "branch_type", "merge_branch_uuid", "analysis_date", "rules");
    assertThat(fieldNames(rule)).containsExactlyInAnyOrder(
      "plugin_rule_key", "new_open_count", "new_in_sandbox_count", "new_fixed_count",
      "new_fixed_in_1_days", "new_fixed_in_7_days", "new_fixed_in_30_days");
  }

  @Test
  void serialize_whenMergeBranchUuidIsNull_shouldSerializeAsNull() {
    IssueDeltaTelemetryEvent event = new IssueDeltaTelemetryEvent(
      "project-uuid", "branch-uuid", "BRANCH", null, 1_700_000_000_000L, List.of());

    JsonNode json = objectMapper.valueToTree(event);

    assertThat(json.get("merge_branch_uuid").isNull()).isTrue();
  }

  @Test
  void type_shouldExposeExpectedMetadata() {
    assertThat(IssueDeltaTelemetryEvent.TYPE.eventType()).isEqualTo("Analytics.Workflow.IssueDeltaAnalysisFinished");
    assertThat(IssueDeltaTelemetryEvent.TYPE.eventVersion()).isEqualTo("1.0");
    assertThat(IssueDeltaTelemetryEvent.TYPE.sourceDomain()).isEqualTo("Workflow");
  }

  private static List<String> fieldNames(JsonNode node) {
    List<String> names = new ArrayList<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }
}
