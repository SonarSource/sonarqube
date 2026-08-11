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
import static org.sonar.api.issue.impact.Severity.HIGH;
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

  // MQR mode: FIELD_ISSUE_IMPACT_RANK is computed in two tiers.
  // Security/Reliability tier: severity_index*1000 + (quality_index+1)*10
  //   quality_index: SECURITY=0, RELIABILITY=1 (breaks ties at the same severity, Security first)
  // Maintainability tier: 10_000 + severity_index*1000 + 10 (always below every Security/Reliability combination)
  // severity_index: BLOCKER=0, HIGH=1, MEDIUM=2, LOW=3, INFO=4
  // Lower value is more important, so the minimum across impacts is kept.

  @Test
  void impact_rank_is_severity_primary_quality_secondary() {
    // Security:Blocker = 0*1000+(0+1)*10 = 10 (most important)
    assertImpactRank(Map.of(SoftwareQuality.SECURITY, BLOCKER), 10);
    // Reliability:Blocker = 0*1000+(1+1)*10 = 20 (still ranked above Security:Info, since severity dominates)
    assertImpactRank(Map.of(SoftwareQuality.RELIABILITY, BLOCKER), 20);
    // Security:Info = 4*1000+(0+1)*10 = 4010
    assertImpactRank(Map.of(SoftwareQuality.SECURITY, INFO), 4010);
    // Reliability:Info = 4*1000+(1+1)*10 = 4020 (always < Maintainability:Blocker=10010)
    assertImpactRank(Map.of(SoftwareQuality.RELIABILITY, INFO), 4020);
    // Maintainability:Blocker = 10_000+0*1000+10 = 10010, regardless of its own severity being Blocker
    assertImpactRank(Map.of(SoftwareQuality.MAINTAINABILITY, BLOCKER), 10010);
    // Maintainability:Info = 10_000+4*1000+10 = 14010 (least important)
    assertImpactRank(Map.of(SoftwareQuality.MAINTAINABILITY, INFO), 14010);
  }

  @Test
  void impact_rank_breaks_ties_at_same_severity_with_security_before_reliability() {
    // {Security:High, Reliability:High} → same severity, Security wins the tie-break
    // Security:High = 1*1000+10 = 1010, Reliability:High = 1*1000+20 = 1020 → min = 1010
    assertImpactRank(Map.of(SoftwareQuality.SECURITY, HIGH, SoftwareQuality.RELIABILITY, HIGH), 1010);
  }

  @Test
  void impact_rank_picks_best_impact_across_multiple() {
    // {Security:Low, Reliability:Blocker} → Reliability:Blocker wins (severity is primary, not quality)
    // Security:Low = 3*1000+10 = 3010, Reliability:Blocker = 0*1000+20 = 20 → min = 20
    assertImpactRank(Map.of(SoftwareQuality.SECURITY, LOW, SoftwareQuality.RELIABILITY, BLOCKER), 20);
  }

  @Test
  void impact_rank_with_empty_impacts_does_not_set_field() {
    IssueDoc doc = newDoc();
    doc.setImpacts(Map.of());
    assertThat(doc.<Object>getNullableField(IssueIndexDefinition.FIELD_ISSUE_IMPACT_RANK)).isNull();
  }

  private static void assertStandardRank(RuleType type, String severity, byte expected) {
    IssueDoc doc = newDoc();
    doc.setSeverity(severity);
    doc.setType(type);
    assertThat((byte) doc.getField(IssueIndexDefinition.FIELD_ISSUE_STANDARD_SORT_RANK)).isEqualTo(expected);
  }

  private static void assertImpactRank(Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts, int expected) {
    IssueDoc doc = newDoc();
    doc.setImpacts(impacts);
    assertThat((int) doc.getField(IssueIndexDefinition.FIELD_ISSUE_IMPACT_RANK)).isEqualTo(expected);
  }

  private static IssueDoc newDoc() {
    return new IssueDoc(new HashMap<>());
  }
}
