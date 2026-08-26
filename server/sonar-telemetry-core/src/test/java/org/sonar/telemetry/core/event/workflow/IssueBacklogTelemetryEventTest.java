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
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent.RuleBacklog;

import static org.assertj.core.api.Assertions.assertThat;

class IssueBacklogTelemetryEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serialize_shouldUseSnakeCaseKeysAndNoTypeMetadataLeak() {
    IssueBacklogTelemetryEvent event = new IssueBacklogTelemetryEvent(
      "project-uuid",
      "branch-uuid",
      "BRANCH",
      1_700_000_000_000L,
      List.of(new RuleBacklog("java:S1234", 19, 10, 5, 2, 1, 1)));

    JsonNode json = objectMapper.valueToTree(event);

    assertThat(json.get("project_uuid").asText()).isEqualTo("project-uuid");
    assertThat(json.get("branch_uuid").asText()).isEqualTo("branch-uuid");
    assertThat(json.get("branch_type").asText()).isEqualTo("BRANCH");
    assertThat(json.get("analysis_date").asLong()).isEqualTo(1_700_000_000_000L);
    assertThat(json.get("rules")).hasSize(1);

    JsonNode rule = json.get("rules").get(0);
    assertThat(rule.get("plugin_rule_key").asText()).isEqualTo("java:S1234");
    assertThat(rule.get("total_count").asInt()).isEqualTo(19);
    assertThat(rule.get("open_count").asInt()).isEqualTo(10);
    assertThat(rule.get("confirmed_count").asInt()).isEqualTo(5);
    assertThat(rule.get("false_positive_count").asInt()).isEqualTo(2);
    assertThat(rule.get("accepted_count").asInt()).isEqualTo(1);
    assertThat(rule.get("in_sandbox_count").asInt()).isEqualTo(1);

    assertThat(fieldNames(json)).containsExactlyInAnyOrder(
      "project_uuid", "branch_uuid", "branch_type", "analysis_date", "rules");
    assertThat(fieldNames(rule)).containsExactlyInAnyOrder(
      "plugin_rule_key", "total_count", "open_count", "confirmed_count", "false_positive_count", "accepted_count", "in_sandbox_count");
  }

  @Test
  void type_shouldExposeExpectedMetadata() {
    assertThat(IssueBacklogTelemetryEvent.TYPE.eventType()).isEqualTo("Analytics.Workflow.FirstBranchAnalysisIssuesRaised");
    assertThat(IssueBacklogTelemetryEvent.TYPE.eventVersion()).isEqualTo("1.0");
    assertThat(IssueBacklogTelemetryEvent.TYPE.sourceDomain()).isEqualTo("Workflow");
  }

  private static List<String> fieldNames(JsonNode node) {
    List<String> names = new ArrayList<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }
}
