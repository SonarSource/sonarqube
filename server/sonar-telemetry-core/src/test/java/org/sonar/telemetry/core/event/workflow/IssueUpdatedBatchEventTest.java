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
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent.IssueUpdate;

import static org.assertj.core.api.Assertions.assertThat;

class IssueUpdatedBatchEventTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void serialize_shouldUseSnakeCaseKeysAndNestedIssuesArray() {
    IssueUpdatedBatchEvent event = new IssueUpdatedBatchEvent(
      "project-uuid",
      "branch-uuid",
      "BRANCH",
      List.of(
        new IssueUpdate("issue1", "java:S1234", 1_700_000_000_000L, "OPEN", null),
        new IssueUpdate("issue2", "java:S1234", 1_700_000_000_000L, "FIXED", 1_700_000_100_000L)));

    JsonNode json = objectMapper.valueToTree(event);

    assertThat(json.get("project_uuid").asText()).isEqualTo("project-uuid");
    assertThat(json.get("branch_id").asText()).isEqualTo("branch-uuid");
    assertThat(json.get("branch_type").asText()).isEqualTo("BRANCH");
    assertThat(fieldNames(json)).containsExactlyInAnyOrder("project_uuid", "branch_id", "branch_type", "issues");

    assertThat(json.get("issues")).hasSize(2);
    JsonNode firstIssue = json.get("issues").get(0);
    assertThat(firstIssue.get("issue_key").asText()).isEqualTo("issue1");
    assertThat(firstIssue.get("plugin_rule_key").asText()).isEqualTo("java:S1234");
    assertThat(firstIssue.get("issue_raised_at").asLong()).isEqualTo(1_700_000_000_000L);
    assertThat(firstIssue.get("issue_status").asText()).isEqualTo("OPEN");
    assertThat(firstIssue.get("issue_resolved_at").isNull()).isTrue();

    JsonNode secondIssue = json.get("issues").get(1);
    assertThat(secondIssue.get("issue_status").asText()).isEqualTo("FIXED");
    assertThat(secondIssue.get("issue_resolved_at").asLong()).isEqualTo(1_700_000_100_000L);

    assertThat(fieldNames(firstIssue)).containsExactlyInAnyOrder(
      "issue_key", "plugin_rule_key", "issue_raised_at", "issue_status", "issue_resolved_at");
    assertThat(fieldNames(firstIssue)).doesNotContain("issue_resolution_status");
  }

  @Test
  void serialize_whenIssueStatusIsNull_omitsNeitherKeyButEmitsNullValue() {
    IssueUpdatedBatchEvent event = new IssueUpdatedBatchEvent(
      "project-uuid", "branch-uuid", "BRANCH",
      List.of(new IssueUpdate("issue1", "java:S1234", 1_700_000_000_000L, null, null)));

    JsonNode issue = objectMapper.valueToTree(event).get("issues").get(0);

    assertThat(issue.get("issue_status").isNull()).isTrue();
    assertThat(issue.get("issue_resolved_at").isNull()).isTrue();
  }

  @Test
  void type_shouldExposeExpectedMetadata() {
    assertThat(IssueUpdatedBatchEvent.TYPE.eventType()).isEqualTo("Analytics.Workflow.IssueUpdated");
    assertThat(IssueUpdatedBatchEvent.TYPE.eventVersion()).isEqualTo("1.0");
    assertThat(IssueUpdatedBatchEvent.TYPE.sourceDomain()).isEqualTo("Workflow");
    assertThat(IssueUpdatedBatchEvent.MAX_ISSUES_PER_EVENT).isEqualTo(500);
  }

  private static List<String> fieldNames(JsonNode node) {
    List<String> names = new ArrayList<>();
    node.fieldNames().forEachRemaining(names::add);
    return names;
  }
}
