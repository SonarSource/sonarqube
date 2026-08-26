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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.sonar.telemetry.core.event.AnalyticsEventType;

/**
 * Issue backlog of a branch, grouped by rule, sent once on the branch's first analysis. Each rule's
 * {@code total_count} covers every issue found for that rule regardless of status; the per-status
 * counts cover only OPEN/CONFIRMED/FALSE_POSITIVE/ACCEPTED/IN_SANDBOX.
 */
public record IssueBacklogTelemetryEvent(
  @JsonProperty("project_uuid") String projectUuid,
  @JsonProperty("branch_uuid") String branchUuid,
  @JsonProperty("branch_type") String branchType,
  @JsonProperty("analysis_date") long analysisDate,
  @JsonProperty("rules") List<RuleBacklog> rules) {

  public static final AnalyticsEventType TYPE = new AnalyticsEventType("Analytics.Workflow.FirstBranchAnalysisIssuesRaised", "1.0", "Workflow", "IssueTelemetry");

  /**
   * {@code totalCount} is every issue found for the rule, regardless of status (closed and fixed
   * issues included) — it is not the sum of the five status counts below, which only cover
   * {@code OPEN}/{@code CONFIRMED}/{@code FALSE_POSITIVE}/{@code ACCEPTED}/{@code IN_SANDBOX}.
   */
  public record RuleBacklog(
    @JsonProperty("plugin_rule_key") String pluginRuleKey,
    @JsonProperty("total_count") int totalCount,
    @JsonProperty("open_count") int openCount,
    @JsonProperty("confirmed_count") int confirmedCount,
    @JsonProperty("false_positive_count") int falsePositiveCount,
    @JsonProperty("accepted_count") int acceptedCount,
    @JsonProperty("in_sandbox_count") int inSandboxCount) {
  }
}
