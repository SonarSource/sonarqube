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

import org.junit.jupiter.api.Test;
import org.sonarsource.history.model.IssueCountByRule;

import static org.assertj.core.api.Assertions.assertThat;

class IssueCountByRuleDtoTest {

  @Test
  void toIssueCountByRule_whenFileIssue_shouldMapMainScopeIssueStatusAndOwnImpacts() {
    var count = new IssueCountByRuleDto(1, "MAJOR", "CONFIRMED", null, "rule-uuid", "FIL", "LOW", "HIGH", "MEDIUM", 3);

    assertThat(count.toIssueCountByRule())
      .isEqualTo(new IssueCountByRule(null, "MAIN", "MAJOR", "CONFIRMED", "CONFIRMED", 1, "rule-uuid", "LOW", "HIGH", "MEDIUM", 3));
  }

  @Test
  void toIssueCountByRule_whenUnitTestFileIssue_shouldMapTestScopeAndResolutionIssueStatus() {
    var count = new IssueCountByRuleDto(2, "MINOR", "RESOLVED", "FIXED", "rule-uuid", "UTS", null, null, null, 1);

    assertThat(count.toIssueCountByRule())
      .isEqualTo(new IssueCountByRule(null, "TEST", "MINOR", "FIXED", "RESOLVED", 2, "rule-uuid", null, null, null, 1));
  }

  @Test
  void toIssueCountByRule_whenStatusHasNoIssueStatus_shouldReturnNullIssueStatus() {
    var count = new IssueCountByRuleDto(1, "MAJOR", "RESOLVED", null, "rule-uuid", "FIL", "HIGH", null, null, 1);

    assertThat(count.toIssueCountByRule().issueStatus()).isNull();
  }
}
