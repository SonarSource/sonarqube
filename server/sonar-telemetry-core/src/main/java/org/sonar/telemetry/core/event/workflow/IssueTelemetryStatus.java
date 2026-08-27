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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;

/**
 * Merges the raw {@code status}/{@code resolution} columns into the {@code issue_status} value
 * reported by per-issue telemetry events. Unlike {@link IssueStatus#of(String, String)}, this
 * keeps {@code REOPENED} distinct from {@code OPEN} and reports a rule-deactivation closure
 * ({@code STATUS_CLOSED} + {@code RESOLUTION_REMOVED}) as {@code REMOVED} rather than {@code FIXED}
 * — both matter for telemetry even though {@code IssueStatus.of()} intentionally collapses them.
 */
public final class IssueTelemetryStatus {

  private IssueTelemetryStatus() {
  }

  @CheckForNull
  public static String of(@Nullable String status, @Nullable String resolution) {
    if (Issue.STATUS_REOPENED.equals(status)) {
      return "REOPENED";
    }
    if (Issue.STATUS_CLOSED.equals(status) && Issue.RESOLUTION_REMOVED.equals(resolution)) {
      return "REMOVED";
    }
    IssueStatus issueStatus = IssueStatus.of(status, resolution);
    return issueStatus != null ? issueStatus.name() : null;
  }
}
