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
import javax.annotation.CheckForNull;
import org.sonar.telemetry.core.event.AnalyticsEventType;

public record IssueUpdatedBatchEvent(
  @JsonProperty("project_uuid") String projectUuid,
  @JsonProperty("branch_id") String branchId,
  @JsonProperty("branch_type") String branchType,
  @JsonProperty("issues") List<IssueUpdate> issues) {

  public static final AnalyticsEventType TYPE = new AnalyticsEventType("Analytics.Workflow.IssueUpdated", "1.0", "Workflow", "IssueTelemetry");

  public static final int MAX_ISSUES_PER_EVENT = 500;

  public record IssueUpdate(
    @JsonProperty("issue_key") String issueKey,
    @JsonProperty("plugin_rule_key") String pluginRuleKey,
    @JsonProperty("issue_raised_at") Long issueRaisedAt,
    @CheckForNull @JsonProperty("issue_status") String issueStatus,
    @CheckForNull @JsonProperty("issue_resolved_at") Long issueResolvedAt) {
  }
}
