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
import javax.annotation.Nullable;
import org.sonar.telemetry.core.event.AnalyticsEventType;

public record IssueBacklogTelemetryEvent(
  @JsonProperty("project_uuid") String projectUuid,
  @JsonProperty("branch_uuid") String branchUuid,
  @JsonProperty("branch_type") String branchType,
  @Nullable @JsonProperty("merge_branch_uuid") String mergeBranchUuid,
  @JsonProperty("analysis_date") long analysisDate,
  @JsonProperty("rules") List<RuleCounts> rules) {

  public static final AnalyticsEventType TYPE = new AnalyticsEventType("Analytics.Workflow.IssueBacklogAnalysisFinished", "1.0", "Workflow", "IssueTelemetry");

  public record RuleCounts(
    @JsonProperty("plugin_rule_key") String pluginRuleKey,
    @JsonProperty("open_count") int openCount,
    @JsonProperty("confirmed_count") int confirmedCount,
    @JsonProperty("false_positive_count") int falsePositiveCount,
    @JsonProperty("accepted_count") int acceptedCount,
    @JsonProperty("in_sandbox_count") int inSandboxCount) {
  }
}
