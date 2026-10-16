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

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IssueCountByRuleDtoTest {

  private static final Map<String, String> RULE_DEFAULT_IMPACTS = Map.of("MAINTAINABILITY", "LOW", "RELIABILITY", "MEDIUM");

  @Test
  void toDimension_whenIssueHasImpacts_shouldIgnoreRuleDefaultImpacts() {
    var count = new IssueCountByRuleDto(1, "MAJOR", "CONFIRMED", null, "rule-uuid", "FIL", null, "HIGH", null, 3);

    assertThat(count.toDimension("java:S1234", RULE_DEFAULT_IMPACTS))
      .isEqualTo(new IssueCountDimensionDto(1, "MAJOR", "CONFIRMED", "CONFIRMED", null, "MAIN", "java:S1234", null, "HIGH", null, 3));
  }

  @Test
  void toDimension_whenIssueHasNoImpacts_shouldUseRuleDefaultImpacts() {
    var count = new IssueCountByRuleDto(2, "MINOR", "RESOLVED", "FIXED", "rule-uuid", "UTS", null, null, null, 1);

    assertThat(count.toDimension("java:S1234", RULE_DEFAULT_IMPACTS))
      .isEqualTo(new IssueCountDimensionDto(2, "MINOR", "FIXED", "RESOLVED", null, "TEST", "java:S1234", "LOW", null, "MEDIUM", 1));
  }

  @Test
  void toDimension_whenStatusHasNoIssueStatus_shouldReturnNullIssueStatus() {
    var count = new IssueCountByRuleDto(1, "MAJOR", "RESOLVED", null, "rule-uuid", "FIL", "HIGH", null, null, 1);

    assertThat(count.toDimension("java:S1234", Map.of()).issueStatus()).isNull();
  }
}
