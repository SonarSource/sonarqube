/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY;

class EffortAggregatorTest {

  static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).build();
  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  @RegisterExtension
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(TECHNICAL_DEBT)
    .add(RELIABILITY_REMEDIATION_EFFORT)
    .add(SECURITY_REMEDIATION_EFFORT)
    .add(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT)
    .add(SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT)
    .add(SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT);

  @RegisterExtension
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create(PROJECT, metricRepository);

  EffortAggregator underTest = new EffortAggregator(metricRepository, measureRepository);

  @Test
  void sum_maintainability_effort_of_unresolved_issues() {
    DefaultIssue unresolved1 = newMaintainabilityIssue(10);
    DefaultIssue unresolved2 = newMaintainabilityIssue(30);
    DefaultIssue unresolvedWithoutEffort = newMaintainabilityIssueWithoutEffort();
    DefaultIssue resolved = newMaintainabilityIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutEffort);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    // total maintainability effort
    assertMeasure(FILE, TECHNICAL_DEBT_KEY, 10L + 30L);
    assertMeasure(FILE, SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
  }

  @Test
  void sum_effort_when_multiple_impacts() {
    DefaultIssue unresolved1 = newIssue(10, Map.of(MAINTAINABILITY, HIGH, SECURITY, HIGH, RELIABILITY, HIGH));
    DefaultIssue unresolved2 = newIssue(30, Map.of(MAINTAINABILITY, HIGH, SECURITY, HIGH, RELIABILITY, HIGH));

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.afterComponent(FILE);

    // total maintainability effort
    assertMeasure(FILE, SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
    assertMeasure(FILE, SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
    assertMeasure(FILE, SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
  }

  @Test
  void maintainability_effort_is_only_computed_using_maintainability_issues() {
    DefaultIssue codeSmellIssue = newMaintainabilityIssue(10);
    // Issues of type BUG and VULNERABILITY should be ignored
    DefaultIssue bugIssue = newReliabilityIssue(15);
    DefaultIssue vulnerabilityIssue = newSecurityIssue(12);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of CODE SMELL issue is used
    assertMeasure(FILE, TECHNICAL_DEBT_KEY, 10L);
    assertMeasure(FILE, SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 10L);

  }

  @Test
  void sum_reliability_effort_of_unresolved_issues() {
    DefaultIssue unresolved1 = newReliabilityIssue(10);
    DefaultIssue unresolved2 = newReliabilityIssue(30);
    DefaultIssue unresolvedWithoutEffort = newReliabilityIssueWithoutEffort();
    DefaultIssue resolved = newReliabilityIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutEffort);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertMeasure(FILE, RELIABILITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
    assertMeasure(FILE, SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
  }

  @Test
  void reliability_effort_is_only_computed_using_reliability_issues() {
    DefaultIssue bugIssue = newReliabilityIssue(10);
    // Issues of type CODE SMELL and VULNERABILITY should be ignored
    DefaultIssue codeSmellIssue = newMaintainabilityIssue(15);
    DefaultIssue vulnerabilityIssue = newSecurityIssue(12);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of BUG issue is used
    assertMeasure(FILE, RELIABILITY_REMEDIATION_EFFORT_KEY, 10L);
    assertMeasure(FILE, SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 10L);
  }

  @Test
  void sum_security_effort_of_unresolved_issues() {
    DefaultIssue unresolved1 = newSecurityIssue(10);
    DefaultIssue unresolved2 = newSecurityIssue(30);
    DefaultIssue unresolvedWithoutEffort = newSecurityIssueWithoutEffort();
    DefaultIssue resolved = newSecurityIssue(50).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutEffort);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertMeasure(FILE, SECURITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
    assertMeasure(FILE, SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
  }

  @Test
  void security_effort_is_only_computed_using_maintainability_issues() {
    DefaultIssue vulnerabilityIssue = newSecurityIssue(10);
    // Issues of type BUG and CODE SMELL should be ignored
    DefaultIssue bugIssue = newReliabilityIssue(15);
    DefaultIssue codeSmellIssue = newMaintainabilityIssue(12);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.afterComponent(FILE);

    // Only effort of VULNERABILITY issue is used
    assertMeasure(FILE, SECURITY_REMEDIATION_EFFORT_KEY, 10L);
    assertMeasure(FILE, SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 10L);
  }

  @Test
  void aggregate_maintainability_measures_of_children() {
    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, newMaintainabilityIssue(10));
    underTest.onIssue(FILE, newReliabilityIssue(8));
    underTest.onIssue(FILE, newSecurityIssue(12));
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, newMaintainabilityIssue(30));
    underTest.onIssue(PROJECT, newReliabilityIssue(38));
    underTest.onIssue(PROJECT, newSecurityIssue(42));
    underTest.afterComponent(PROJECT);

    assertMeasure(PROJECT, TECHNICAL_DEBT_KEY, 10L + 30L);
    assertMeasure(PROJECT, RELIABILITY_REMEDIATION_EFFORT_KEY, 8L + 38L);
    assertMeasure(PROJECT, SECURITY_REMEDIATION_EFFORT_KEY, 12L + 42L);

    assertMeasure(PROJECT, SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 10L + 30L);
    assertMeasure(PROJECT, SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 8L + 38L);
    assertMeasure(PROJECT, SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 12L + 42L);
  }

  @Test
  void sum_characteristic_measures_of_issues_without_effort() {
    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, newMaintainabilityIssueWithoutEffort());
    underTest.onIssue(FILE, newReliabilityIssueWithoutEffort());
    underTest.onIssue(FILE, newSecurityIssueWithoutEffort());
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, newMaintainabilityIssueWithoutEffort());
    underTest.afterComponent(PROJECT);

    assertMeasure(PROJECT, TECHNICAL_DEBT_KEY, 0L);
    assertMeasure(PROJECT, RELIABILITY_REMEDIATION_EFFORT_KEY, 0L);
    assertMeasure(PROJECT, SECURITY_REMEDIATION_EFFORT_KEY, 0L);

    assertMeasure(PROJECT, SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 0L);
    assertMeasure(PROJECT, SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 0L);
    assertMeasure(PROJECT, SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 0L);
  }

  private void assertMeasure(Component component, String metricKey, long expectedValue) {
    assertThat(measureRepository.getAddedRawMeasure(component, metricKey).get().getLongValue()).isEqualTo(expectedValue);
  }

  private static DefaultIssue newIssue(long effort, Map<SoftwareQuality, Severity> impacts) {
    return newMaintainabilityIssueWithoutEffort().setEffort(Duration.create(effort)).replaceImpacts(impacts);
  }

  private static DefaultIssue newMaintainabilityIssue(long effort) {
    return newMaintainabilityIssueWithoutEffort().setEffort(Duration.create(effort)).setType(CODE_SMELL).replaceImpacts(Map.of(MAINTAINABILITY, HIGH));
  }

  private static DefaultIssue newReliabilityIssue(long effort) {
    return newMaintainabilityIssueWithoutEffort().setEffort(Duration.create(effort)).setType(BUG).replaceImpacts(Map.of(RELIABILITY, HIGH));
  }

  private static DefaultIssue newSecurityIssue(long effort) {
    return newMaintainabilityIssueWithoutEffort().setEffort(Duration.create(effort)).setType(VULNERABILITY).replaceImpacts(Map.of(SECURITY, HIGH));
  }

  private static DefaultIssue newMaintainabilityIssueWithoutEffort() {
    return new DefaultIssue().setType(CODE_SMELL).replaceImpacts(Map.of(MAINTAINABILITY, HIGH));
  }

  private static DefaultIssue newReliabilityIssueWithoutEffort() {
    return new DefaultIssue().setType(BUG).replaceImpacts(Map.of(RELIABILITY, HIGH));
  }

  private static DefaultIssue newSecurityIssueWithoutEffort() {
    return new DefaultIssue().setType(VULNERABILITY).replaceImpacts(Map.of(SECURITY, HIGH));
  }

}
