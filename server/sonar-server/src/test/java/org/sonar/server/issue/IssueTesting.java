/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue;


import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.db.IssueDto;

import static org.fest.assertions.Assertions.assertThat;

public class IssueTesting {

  public static void assertIsEquivalent(IssueDto dto, Issue issue) {

    assertThat(issue).isNotNull();
    assertThat(dto).isNotNull();

    assertThat(issue.actionPlanKey()).isEqualTo(dto.getActionPlanKey());
    assertThat(issue.assignee()).isEqualTo(dto.getAssignee());
    assertThat(issue.authorLogin()).isEqualTo(dto.getAuthorLogin());
    assertThat(issue.closeDate()).isEqualTo(dto.getIssueCloseDate());
    assertThat(issue.componentKey()).isEqualTo(dto.getComponentKey());
    assertThat(issue.effortToFix()).isEqualTo(dto.getEffortToFix());
    assertThat(issue.resolution()).isEqualTo(dto.getResolution());
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of(dto.getRuleRepo(), dto.getRule()));
    assertThat(issue.line()).isEqualTo(dto.getLine());
    assertThat(issue.message()).isEqualTo(dto.getMessage());
    assertThat(issue.reporter()).isEqualTo(dto.getReporter());
    assertThat(issue.key()).isEqualTo(dto.getKey());

    // assertThat(issue.updateDate()).isEqualTo(dto.getIssueUpdateDate());
    assertThat(issue.status()).isEqualTo(dto.getStatus());
    assertThat(issue.severity()).isEqualTo(dto.getSeverity());
  }
}
