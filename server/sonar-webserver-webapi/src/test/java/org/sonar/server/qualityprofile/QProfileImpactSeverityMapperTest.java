/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.qualityprofile;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.Severity;
import org.sonar.core.rule.RuleType;

import static org.assertj.core.api.Assertions.assertThat;

class QProfileImpactSeverityMapperTest {

  public static final Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> IMPACTS = Map.of(
    SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH,
    SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW,
    SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.INFO);

  @Test
  void mapImpactSeverities_whenSecurityHotspot_shouldReturnEmptyMap() {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> result = QProfileImpactSeverityMapper.mapImpactSeverities(Severity.MAJOR,
      Map.of(),
      RuleType.SECURITY_HOTSPOT);

    assertThat(result).isEmpty();
  }

  @Test
  void mapImpactSeverities_whenSeverityIsNull_shouldReturnRuleImpacts() {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts = Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH);
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> result = QProfileImpactSeverityMapper.mapImpactSeverities(null,
      impacts,
      RuleType.SECURITY_HOTSPOT);

    assertThat(result).isEqualTo(impacts);
  }

  @Test
  void mapImpactSeverities_whenOneImpact_shouldReturnOverriddenImpact() {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> result = QProfileImpactSeverityMapper.mapImpactSeverities(Severity.INFO,
      Map.of(SoftwareQuality.MAINTAINABILITY,
        org.sonar.api.issue.impact.Severity.HIGH),
      RuleType.CODE_SMELL);

    assertThat(result).hasSize(1).containsEntry(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.INFO);
  }

  @Test
  void mapImpactSeverities_whenOneDifferentImpact_shouldNotOverrideImpact() {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> result = QProfileImpactSeverityMapper.mapImpactSeverities(Severity.INFO,
      Map.of(SoftwareQuality.RELIABILITY,
        org.sonar.api.issue.impact.Severity.HIGH),
      RuleType.CODE_SMELL);

    assertThat(result).hasSize(1).containsEntry(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.HIGH);
  }

  @Test
  void mapImpactSeverities_whenMultipleImpact_shouldReturnOverriddenImpactMatchingCodeSmell() {

    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> result = QProfileImpactSeverityMapper.mapImpactSeverities(Severity.BLOCKER, IMPACTS, RuleType.CODE_SMELL);

    assertThat(result).hasSize(3)
      .containsEntry(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.BLOCKER)
      .containsEntry(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW)
      .containsEntry(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.INFO);

    result = QProfileImpactSeverityMapper.mapImpactSeverities(Severity.BLOCKER, IMPACTS, RuleType.BUG);

    assertThat(result).hasSize(3)
      .containsEntry(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH)
      .containsEntry(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.BLOCKER)
      .containsEntry(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.INFO);

    result = QProfileImpactSeverityMapper.mapImpactSeverities(Severity.BLOCKER, IMPACTS, RuleType.VULNERABILITY);

    assertThat(result).hasSize(3)
      .containsEntry(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH)
      .containsEntry(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW)
      .containsEntry(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.BLOCKER);
  }

  @Test
  void mapImpactSeverities_whenMultipleImpactNotMatchingRuleType_shouldReturnRuleImpacts() {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts = Map.of(
      SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW,
      SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.INFO);

    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> result = QProfileImpactSeverityMapper.mapImpactSeverities(Severity.BLOCKER, impacts, RuleType.CODE_SMELL);
    assertThat(result).hasSize(2)
      .containsEntry(SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW)
      .containsEntry(SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.INFO);
  }

  @Test
  void mapSeverity_whenOneImpactNotMatchingRuleType_ShouldReturnSameSeverity() {
    String severity = QProfileImpactSeverityMapper.mapSeverity(
      Map.of(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.HIGH),
      RuleType.BUG, Severity.BLOCKER);

    assertThat(severity).isEqualTo(Severity.BLOCKER);
  }

  @Test
  void mapSeverity_whenMultipleImpacts_ShouldReturnMappedImpactSeverity() {
    String severity = QProfileImpactSeverityMapper.mapSeverity(
      IMPACTS,
      RuleType.BUG, Severity.BLOCKER);

    assertThat(severity).isEqualTo(Severity.MINOR);

    severity = QProfileImpactSeverityMapper.mapSeverity(
      IMPACTS,
      RuleType.VULNERABILITY, Severity.BLOCKER);

    assertThat(severity).isEqualTo(Severity.INFO);

    severity = QProfileImpactSeverityMapper.mapSeverity(
      IMPACTS,
      RuleType.CODE_SMELL, Severity.BLOCKER);

    assertThat(severity).isEqualTo(Severity.CRITICAL);
  }

  @Test
  void mapImpactSeverities_whenMultipleImpactNotMatchingRuleType_shouldReturnRuleSeverity() {
    Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> impacts = Map.of(
      SoftwareQuality.RELIABILITY, org.sonar.api.issue.impact.Severity.LOW,
      SoftwareQuality.SECURITY, org.sonar.api.issue.impact.Severity.INFO);

    String severity = QProfileImpactSeverityMapper.mapSeverity(
      impacts,
      RuleType.CODE_SMELL, Severity.BLOCKER);

    assertThat(severity).isEqualTo(Severity.BLOCKER);
  }

}
