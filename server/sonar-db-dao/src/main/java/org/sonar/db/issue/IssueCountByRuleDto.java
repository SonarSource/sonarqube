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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.issue.IssueStatus;
import org.sonar.db.component.ComponentQualifiers;
import org.sonarsource.history.model.IssueCountByRule;

/**
 * Issue count grouped by the issue's own columns, before its rule key and rule default impacts are resolved.
 * MyBatis binds this canonical constructor by column order.
 */
@SuppressWarnings("java:S107")
public record IssueCountByRuleDto(
  int issueType,
  String severity,
  String status,
  @Nullable String resolution,
  String ruleUuid,
  String qualifier,
  @Nullable String maintainability,
  @Nullable String security,
  @Nullable String reliability,
  int issueCount) {

  IssueCountByRule toIssueCountByRule() {
    return new IssueCountByRule(
      null,
      ComponentQualifiers.UNIT_TEST_FILE.equals(qualifier) ? "TEST" : "MAIN",
      severity,
      Optional.ofNullable(IssueStatus.of(status, resolution)).map(IssueStatus::name).orElse(null),
      status,
      issueType,
      ruleUuid,
      maintainability,
      security,
      reliability,
      issueCount);
  }
}
