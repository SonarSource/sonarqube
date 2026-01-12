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
package org.sonar.server.v2.api.dop.jfrog.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.sonar.server.v2.api.dop.jfrog.response.GateCondition;
import org.sonar.server.v2.api.dop.jfrog.response.GateStatus;
import org.sonar.server.v2.api.dop.jfrog.response.QualityGateEvidence;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubePredicate;

import static org.assertj.core.api.Assertions.assertThat;

class JFrogEvidenceMarkdownServiceTest {

  private final JFrogEvidenceMarkdownService underTest = new JFrogEvidenceMarkdownService();

  @Test
  void generateMarkdown_withPassedGate_shouldFormatCorrectly() {
    GateCondition condition = GateCondition.builder()
      .metricKey("new_coverage")
      .status(GateStatus.OK)
      .comparator(GateCondition.Comparator.LT)
      .actualValue("85")
      .errorThreshold("80")
      .build();

    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.OK)
      .ignoredConditions(false)
      .conditions(List.of(condition))
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result)
      .contains("# SonarQube Quality Gate Evidence")
      .contains("## Quality Gate Status: Passed")
      .contains("### Passed Conditions")
      .contains("[PASSED] **Coverage on New Code**: 85 is less than 80");
  }

  @Test
  void generateMarkdown_withFailedGate_shouldFormatCorrectly() {
    GateCondition condition = GateCondition.builder()
      .metricKey("new_coverage")
      .status(GateStatus.ERROR)
      .comparator(GateCondition.Comparator.LT)
      .actualValue("70")
      .errorThreshold("80")
      .build();

    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.ERROR)
      .ignoredConditions(false)
      .conditions(List.of(condition))
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result)
      .contains("## Quality Gate Status: Failed")
      .contains("### Failed Conditions")
      .contains("[FAILED] **Coverage on New Code**: 70 is less than 80");
  }

  @Test
  void generateMarkdown_withWarningStatus_shouldFormatCorrectly() {
    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.WARN)
      .ignoredConditions(false)
      .conditions(List.of())
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result).contains("## Quality Gate Status: Warning");
  }

  @Test
  void generateMarkdown_withNoGate_shouldFormatCorrectly() {
    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.NONE)
      .ignoredConditions(false)
      .conditions(List.of())
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result)
      .contains("## Quality Gate Status: No Quality Gate")
      .contains("*No quality gate conditions were evaluated.*");
  }

  @Test
  void generateMarkdown_withIgnoredConditions_shouldShowMessage() {
    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.OK)
      .ignoredConditions(true)
      .conditions(List.of())
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result).contains("*Some conditions were ignored because they were not relevant to the analysis.*");
  }

  @Test
  void generateMarkdown_withDifferentComparators_shouldFormatCorrectly() {
    GateCondition gtCondition = GateCondition.builder()
      .metricKey("new_violations")
      .status(GateStatus.OK)
      .comparator(GateCondition.Comparator.GT)
      .actualValue("0")
      .errorThreshold("10")
      .build();

    GateCondition eqCondition = GateCondition.builder()
      .metricKey("new_blocker_violations")
      .status(GateStatus.OK)
      .comparator(GateCondition.Comparator.EQ)
      .actualValue("0")
      .errorThreshold("0")
      .build();

    GateCondition neCondition = GateCondition.builder()
      .metricKey("new_critical_violations")
      .status(GateStatus.OK)
      .comparator(GateCondition.Comparator.NE)
      .actualValue("0")
      .errorThreshold("1")
      .build();

    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.OK)
      .ignoredConditions(false)
      .conditions(List.of(gtCondition, eqCondition, neCondition))
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result)
      .contains("is greater than")
      .contains("equals")
      .contains("is not equal to");
  }

  @Test
  void generateMarkdown_withUnknownMetric_shouldUseMetricKey() {
    GateCondition condition = GateCondition.builder()
      .metricKey("custom_metric")
      .status(GateStatus.OK)
      .comparator(GateCondition.Comparator.LT)
      .actualValue("100")
      .errorThreshold("50")
      .build();

    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.OK)
      .ignoredConditions(false)
      .conditions(List.of(condition))
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result).contains("[PASSED] **custom_metric**");
  }

  @Test
  void generateMarkdown_withConditionMissingDetails_shouldShowOnlyMetricName() {
    GateCondition condition = GateCondition.builder()
      .metricKey("new_coverage")
      .status(GateStatus.OK)
      .build();

    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.OK)
      .ignoredConditions(false)
      .conditions(List.of(condition))
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result)
      .contains("[PASSED] **Coverage on New Code**")
      .doesNotContain("is less than");
  }

  @Test
  void generateMarkdown_withMixedConditions_shouldGroupCorrectly() {
    GateCondition passedCondition = GateCondition.builder()
      .metricKey("new_coverage")
      .status(GateStatus.OK)
      .comparator(GateCondition.Comparator.LT)
      .actualValue("85")
      .errorThreshold("80")
      .build();

    GateCondition failedCondition = GateCondition.builder()
      .metricKey("new_duplicated_lines_density")
      .status(GateStatus.ERROR)
      .comparator(GateCondition.Comparator.GT)
      .actualValue("5")
      .errorThreshold("3")
      .build();

    QualityGateEvidence gate = QualityGateEvidence.builder()
      .status(GateStatus.ERROR)
      .ignoredConditions(false)
      .conditions(List.of(passedCondition, failedCondition))
      .build();

    SonarQubePredicate predicate = new SonarQubePredicate(List.of(gate));

    String result = underTest.generateMarkdown(predicate);

    assertThat(result)
      .contains("### Failed Conditions")
      .contains("[FAILED] **Duplicated Lines on New Code**")
      .contains("### Passed Conditions")
      .contains("[PASSED] **Coverage on New Code**");
  }

}
