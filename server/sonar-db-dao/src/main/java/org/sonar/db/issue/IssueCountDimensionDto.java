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
package org.sonar.db.issue;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * One row of a per-dimension issue count, pre-aggregated in SQL for a branch's non-closed issues.
 * Each row already carries the issue count for its unique combination of dimension attributes,
 * so the caller never needs to load individual issues to compute it. MyBatis binds this canonical
 * constructor directly, matching each SQL column alias to a component name.
 *
 * @param ruleKey already composed as {@code repository:rule} (e.g. {@code java:S1234})
 */
@SuppressWarnings("java:S107")
public record IssueCountDimensionDto(
  int issueType,
  String severity,
  @Nullable String issueStatus,
  @Nullable String status,
  @Nullable String hotspotResolution,
  String codeScope,
  String ruleKey,
  @Nullable String effectiveMaintainability,
  @Nullable String effectiveSecurity,
  @Nullable String effectiveReliability,
  int issueCount) {

  public IssueCountDimensionDto withIssueCount(int issueCount) {
    return new IssueCountDimensionDto(issueType, severity, issueStatus, status, hotspotResolution, codeScope, ruleKey,
      effectiveMaintainability, effectiveSecurity, effectiveReliability, issueCount);
  }

  public IssueCountDimensionDto withCodeScope(String codeScope) {
    return new IssueCountDimensionDto(issueType, severity, issueStatus, status, hotspotResolution, codeScope, ruleKey,
      effectiveMaintainability, effectiveSecurity, effectiveReliability, issueCount);
  }

  public IssueCountDimensionDto withSeverity(String severity) {
    return new IssueCountDimensionDto(issueType, severity, issueStatus, status, hotspotResolution, codeScope, ruleKey,
      effectiveMaintainability, effectiveSecurity, effectiveReliability, issueCount);
  }

  /** Returns the effective (issue-override-or-rule-default) severity per software quality, omitting absent ones. */
  public Map<String, String> effectiveImpacts() {
    Map<String, String> result = new HashMap<>();
    if (effectiveMaintainability != null) {
      result.put("MAINTAINABILITY", effectiveMaintainability);
    }
    if (effectiveSecurity != null) {
      result.put("SECURITY", effectiveSecurity);
    }
    if (effectiveReliability != null) {
      result.put("RELIABILITY", effectiveReliability);
    }
    return result;
  }
}
