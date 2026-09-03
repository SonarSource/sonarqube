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

public record IssueDeltaTelemetryEvent(
  @JsonProperty("project_uuid") String projectUuid,
  @JsonProperty("branch_uuid") String branchUuid,
  @JsonProperty("branch_type") String branchType,
  @Nullable @JsonProperty("merge_branch_uuid") String mergeBranchUuid,
  @JsonProperty("analysis_date") long analysisDate,
  @JsonProperty("rules") List<RuleDeltaCounts> rules) {

  public static final AnalyticsEventType TYPE = new AnalyticsEventType("Analytics.Workflow.IssueDeltaAnalysisFinished", "1.0", "Workflow", "IssueTelemetry");

  public record RuleDeltaCounts(
    @JsonProperty("plugin_rule_key") String pluginRuleKey,
    @JsonProperty("new_open_count") int newOpenCount,
    @JsonProperty("new_in_sandbox_count") int newInSandboxCount,
    @JsonProperty("new_fixed_count") int newFixedCount,
    @JsonProperty("new_fixed_in_1_days") int newFixedIn1Days,
    @JsonProperty("new_fixed_in_7_days") int newFixedIn7Days,
    @JsonProperty("new_fixed_in_30_days") int newFixedIn30Days) {
  }
}
