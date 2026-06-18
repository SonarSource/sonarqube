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
package org.sonar.server.issue.index;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.core.rule.RuleType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.impact.Severity.BLOCKER;
import static org.sonar.api.issue.impact.Severity.INFO;
import static org.sonar.api.issue.impact.Severity.LOW;

class IssueDocTest {

  // Standard mode: FIELD_ISSUE_STANDARD_SORT_RANK = (type_rank * 5) + severity_rank
  // type_rank: CODE_SMELL=0, BUG=1, VULNERABILITY=2, SECURITY_HOTSPOT=3
  // severity_rank: INFO=0, MINOR=1, MAJOR=2, CRITICAL=3, BLOCKER=4

  @Test
  void standard_sort_rank_is_type_primary_severity_secondary() {
    // Vulnerability:Blocker = 2*5+4 = 14 (highest)
    assertStandardRank(RuleType.VULNERABILITY, org.sonar.api.rule.Severity.BLOCKER, (byte) 14);
    // Vulnerability:Info  = 2*5+0 = 10 (always > Bug:Blocker=9)
    assertStandardRank(RuleType.VULNERABILITY, org.sonar.api.rule.Severity.INFO, (byte) 10);
    // Bug:Blocker         = 1*5+4 = 9
    assertStandardRank(RuleType.BUG, org.sonar.api.rule.Severity.BLOCKER, (byte) 9);
    // Bug:Info            = 1*5+0 = 5  (always > CodeSmell:Blocker=4)
    assertStandardRank(RuleType.BUG, org.sonar.api.rule.Severity.INFO, (byte) 5);
    // CodeSmell:Blocker   = 0*5+4 = 4
    assertStandardRank(RuleType.CODE_SMELL, org.sonar.api.rule.Severity.BLOCKER, (byte) 4);
    // CodeSmell:Info      = 0*5+0 = 0
    assertStandardRank(RuleType.CODE_SMELL, org.sonar.api.rule.Severity.INFO, (byte) 0);
  }

  @Test
  void standard_sort_rank_is_set_when_type_called_after_severity() {
    IssueDoc doc = newDoc();
    doc.setSeverity(org.sonar.api.rule.Severity.MAJOR);
    doc.setType(RuleType.BUG);
    assertThat((byte) doc.getField(IssueIndexDefinition.FIELD_ISSUE_STANDARD_SORT_RANK)).isEqualTo((byte) 7); // 1*5+2
  }

  @Test
  void standard_sort_rank_is_set_when_severity_called_after_type() {
    IssueDoc doc = newDoc();
    doc.setType(RuleType.BUG);
    doc.setSeverity(org.sonar.api.rule.Severity.MAJOR);
    assertThat((byte) doc.getField(IssueIndexDefinition.FIELD_ISSUE_STANDARD_SORT_RANK)).isEqualTo((byte) 7); // 1*5+2
  }

  // MQR mode: FIELD_ISSUE_MQR_SORT_RANK = max over all impacts of (quality_rank * 5) + severity_rank
  // quality_rank: MAINTAINABILITY=0, RELIABILITY=1, SECURITY=2
  // severity_rank: INFO=0, LOW=1, MEDIUM=2, HIGH=3, BLOCKER=4

  @Test
  void mqr_sort_rank_is_quality_primary_severity_secondary() {
    // Security:Blocker = 2*5+4 = 14 (highest)
    assertMqrRank(Map.of(SoftwareQuality.SECURITY, BLOCKER), (byte) 14);
    // Security:Info    = 2*5+0 = 10 (always > Reliability:Blocker=9)
    assertMqrRank(Map.of(SoftwareQuality.SECURITY, INFO), (byte) 10);
    // Reliability:Blocker = 1*5+4 = 9
    assertMqrRank(Map.of(SoftwareQuality.RELIABILITY, BLOCKER), (byte) 9);
    // Reliability:Info    = 1*5+0 = 5 (always > Maintainability:Blocker=4)
    assertMqrRank(Map.of(SoftwareQuality.RELIABILITY, INFO), (byte) 5);
    // Maintainability:Blocker = 0*5+4 = 4
    assertMqrRank(Map.of(SoftwareQuality.MAINTAINABILITY, BLOCKER), (byte) 4);
    // Maintainability:Info    = 0*5+0 = 0
    assertMqrRank(Map.of(SoftwareQuality.MAINTAINABILITY, INFO), (byte) 0);
  }

  @Test
  void mqr_sort_rank_picks_best_impact_across_multiple() {
    // {Security:Low, Reliability:Blocker} → Security wins (quality is primary)
    // Security:Low = 2*5+1 = 11, Reliability:Blocker = 1*5+4 = 9 → max = 11
    assertMqrRank(Map.of(SoftwareQuality.SECURITY, LOW, SoftwareQuality.RELIABILITY, BLOCKER), (byte) 11);
  }

  @Test
  void mqr_sort_rank_with_empty_impacts_does_not_set_field() {
    IssueDoc doc = newDoc();
    doc.setImpacts(Map.of());
    assertThat(doc.<Object>getNullableField(IssueIndexDefinition.FIELD_ISSUE_MQR_SORT_RANK)).isNull();
  }

  private static void assertStandardRank(RuleType type, String severity, byte expected) {
    IssueDoc doc = newDoc();
    doc.setSeverity(severity);
    doc.setType(type);
    assertThat((byte) doc.getField(IssueIndexDefinition.FIELD_ISSUE_STANDARD_SORT_RANK)).isEqualTo(expected);
  }

  private static void assertMqrRank(Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts, byte expected) {
    IssueDoc doc = newDoc();
    doc.setImpacts(impacts);
    assertThat((byte) doc.getField(IssueIndexDefinition.FIELD_ISSUE_MQR_SORT_RANK)).isEqualTo(expected);
  }

  private static IssueDoc newDoc() {
    return new IssueDoc(new HashMap<>());
  }
}
