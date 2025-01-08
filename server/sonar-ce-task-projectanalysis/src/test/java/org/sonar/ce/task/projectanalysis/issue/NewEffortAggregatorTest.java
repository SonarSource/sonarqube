/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.ce.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.ce.task.projectanalysis.period.PeriodHolderRule;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.component.BranchType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.impact.Severity.HIGH;
import static org.sonar.api.issue.impact.SoftwareQuality.MAINTAINABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.RELIABILITY;
import static org.sonar.api.issue.impact.SoftwareQuality.SECURITY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY;

class NewEffortAggregatorTest {
  private static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).setUuid("FILE").build();
  private static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  @RegisterExtension
  public PeriodHolderRule periodsHolder = new PeriodHolderRule();
  @RegisterExtension
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_TECHNICAL_DEBT)
    .add(NEW_RELIABILITY_REMEDIATION_EFFORT)
    .add(NEW_SECURITY_REMEDIATION_EFFORT)
    .add(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT)
    .add(NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT)
    .add(NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT);
  @RegisterExtension
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create();
  private final NewIssueClassifier newIssueClassifier = mock(NewIssueClassifier.class);
  private final NewEffortAggregator underTest = new NewEffortAggregator(metricRepository, measureRepository, newIssueClassifier);

  @Test
  void sum_new_maintainability_effort_of_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isNew(any(), any())).thenReturn(true);
    DefaultIssue unresolved1 = newMaintainabilityIssue(10L);
    DefaultIssue old1 = oldCodeSmellIssue(100L);
    DefaultIssue unresolved2 = newMaintainabilityIssue(30L);
    DefaultIssue old2 = oldCodeSmellIssue(300L);
    DefaultIssue unresolvedWithoutDebt = newMaintainabilityIssueWithoutEffort();
    DefaultIssue resolved = newMaintainabilityIssue(50L).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, old1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, old2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertValue(FILE, NEW_TECHNICAL_DEBT_KEY, 10 + 30);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 10 + 30);
  }

  @Test
  void sum_effort_when_multiple_impacts() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isNew(any(), any())).thenReturn(true);

    DefaultIssue unresolved1 = createIssue(CODE_SMELL, List.of(MAINTAINABILITY, RELIABILITY, SECURITY), 10, true);
    DefaultIssue unresolved2 = createIssue(CODE_SMELL, List.of(MAINTAINABILITY, RELIABILITY, SECURITY), 10, true);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.afterComponent(FILE);

    // total maintainability effort
    assertValue(FILE, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 20);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 20);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 20);
  }

  @Test
  void new_maintainability_effort_is_only_computed_using_maintainability_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isNew(any(), any())).thenReturn(true);
    DefaultIssue codeSmellIssue = newMaintainabilityIssue(10);
    DefaultIssue oldSmellIssue = oldCodeSmellIssue(100);
    // Issues of type BUG and VULNERABILITY should be ignored
    DefaultIssue bugIssue = newReliabilityIssue(15);
    DefaultIssue oldBugIssue = oldBugIssue(150);
    DefaultIssue vulnerabilityIssue = newSecurityIssue(12);
    DefaultIssue oldVulnerabilityIssue = oldVulnerabilityIssue(120);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, oldSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, oldBugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, oldVulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of CODE SMELL issue is used
    assertValue(FILE, NEW_TECHNICAL_DEBT_KEY, 10);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 10);
  }

  @Test
  void sum_new_reliability_effort_of_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isNew(any(), any())).thenReturn(true);
    DefaultIssue unresolved1 = newReliabilityIssue(10L);
    DefaultIssue old1 = oldBugIssue(100L);
    DefaultIssue unresolved2 = newReliabilityIssue(30L);

    DefaultIssue old2 = oldBugIssue(300L);
    DefaultIssue unresolvedWithoutDebt = newReliabilityIssueWithoutEffort();
    DefaultIssue resolved = newReliabilityIssue(50L).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, old1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, old2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertValue(FILE, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 10 + 30);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 10 + 30);
  }

  @Test
  void new_reliability_effort_is_only_computed_using_bug_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isNew(any(), any())).thenReturn(true);
    DefaultIssue bugIssue = newReliabilityIssue(15);
    DefaultIssue oldBugIssue = oldBugIssue(150);
    // Issues of type CODE SMELL and VULNERABILITY should be ignored
    DefaultIssue codeSmellIssue = newMaintainabilityIssue(10);
    DefaultIssue oldCodeSmellIssue = oldCodeSmellIssue(100);
    DefaultIssue vulnerabilityIssue = newSecurityIssue(12);
    DefaultIssue oldVulnerabilityIssue = oldVulnerabilityIssue(120);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, oldBugIssue);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, oldCodeSmellIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, oldVulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of BUG issue is used
    assertValue(FILE, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 15);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 15);
  }

  @Test
  void sum_new_vulnerability_effort_of_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    DefaultIssue unresolved1 = newSecurityIssue(10L);
    DefaultIssue old1 = oldVulnerabilityIssue(100L);
    DefaultIssue unresolved2 = newSecurityIssue(30L);
    DefaultIssue old2 = oldVulnerabilityIssue(300L);
    DefaultIssue unresolvedWithoutDebt = newSecurityIssueWithoutEffort();
    DefaultIssue resolved = newSecurityIssue(50L).setResolution(RESOLUTION_FIXED);
    DefaultIssue oldResolved = oldVulnerabilityIssue(500L).setResolution(RESOLUTION_FIXED);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, old1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, old2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.onIssue(FILE, oldResolved);
    underTest.afterComponent(FILE);

    assertValue(FILE, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 10 + 30);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 10 + 30);
  }

  @Test
  void new_security_effort_is_only_computed_using_vulnerability_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isNew(any(), any())).thenReturn(true);
    DefaultIssue vulnerabilityIssue = newSecurityIssue(12);
    DefaultIssue oldVulnerabilityIssue = oldVulnerabilityIssue(120);
    // Issues of type CODE SMELL and BUG should be ignored
    DefaultIssue codeSmellIssue = newMaintainabilityIssue(10);
    DefaultIssue oldCodeSmellIssue = oldCodeSmellIssue(100);
    DefaultIssue bugIssue = newReliabilityIssue(15);
    DefaultIssue oldBugIssue = oldBugIssue(150);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, oldCodeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, oldBugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, oldVulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of VULNERABILITY issue is used
    assertValue(FILE, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 12);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 12);
  }

  @Test
  void aggregate_new_characteristic_measures_of_children() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isNew(any(), any())).thenReturn(true);

    DefaultIssue codeSmellIssue = newMaintainabilityIssue(10);
    DefaultIssue oldCodeSmellIssue = oldCodeSmellIssue(100);
    DefaultIssue bugIssue = newReliabilityIssue(8);
    DefaultIssue oldBugIssue = oldBugIssue(80);
    DefaultIssue vulnerabilityIssue = newSecurityIssue(12);
    DefaultIssue oldVulnerabilityIssue = oldVulnerabilityIssue(120);

    DefaultIssue codeSmellProjectIssue = newMaintainabilityIssue(30);
    DefaultIssue oldCodeSmellProjectIssue = oldCodeSmellIssue(300);
    DefaultIssue bugProjectIssue = newReliabilityIssue(28);
    DefaultIssue oldBugProjectIssue = oldBugIssue(280);
    DefaultIssue vulnerabilityProjectIssue = newSecurityIssue(32);
    DefaultIssue oldVulnerabilityProjectIssue = oldVulnerabilityIssue(320);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, oldCodeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, oldBugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.onIssue(FILE, oldVulnerabilityIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, codeSmellProjectIssue);
    underTest.onIssue(PROJECT, oldCodeSmellProjectIssue);
    underTest.onIssue(PROJECT, bugProjectIssue);
    underTest.onIssue(PROJECT, oldBugProjectIssue);
    underTest.onIssue(PROJECT, vulnerabilityProjectIssue);
    underTest.onIssue(PROJECT, oldVulnerabilityProjectIssue);
    underTest.afterComponent(PROJECT);

    assertValue(PROJECT, NEW_TECHNICAL_DEBT_KEY, 10 + 30);
    assertValue(PROJECT, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 8 + 28);
    assertValue(PROJECT, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 12 + 32);

    assertValue(PROJECT, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 10 + 30);
    assertValue(PROJECT, NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 8 + 28);
    assertValue(PROJECT, NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 12 + 32);
  }

  @Test
  void no_measures_if_no_periods() {
    when(newIssueClassifier.isEnabled()).thenReturn(false);
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    periodsHolder.setPeriod(null);
    DefaultIssue unresolved = newMaintainabilityIssue(10);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved);
    underTest.afterComponent(FILE);

    assertThat(measureRepository.getRawMeasures(FILE)).isEmpty();
  }

  @Test
  void should_have_empty_measures_if_no_issues() {
    when(newIssueClassifier.isEnabled()).thenReturn(true);
    when(newIssueClassifier.isNew(any(), any())).thenReturn(true);

    underTest.beforeComponent(FILE);
    underTest.afterComponent(FILE);

    assertValue(FILE, NEW_TECHNICAL_DEBT_KEY, 0);
    assertValue(FILE, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 0);
    assertValue(FILE, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 0);

    assertValue(FILE, NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY, 0);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY, 0);
    assertValue(FILE, NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY, 0);
  }

  private void assertValue(Component component, String metricKey, int value) {
    Measure newMeasure = measureRepository.getRawMeasure(component, metricRepository.getByKey(metricKey)).get();
    assertThat(newMeasure.getLongValue()).isEqualTo(value);
  }

  private DefaultIssue newMaintainabilityIssue(long effort) {
    return createIssue(CODE_SMELL, MAINTAINABILITY, effort, true);
  }

  private DefaultIssue oldCodeSmellIssue(long effort) {
    return createIssue(CODE_SMELL, MAINTAINABILITY, effort, false);
  }

  private DefaultIssue newReliabilityIssue(long effort) {
    return createIssue(BUG, RELIABILITY, effort, true);
  }

  private DefaultIssue oldBugIssue(long effort) {
    return createIssue(BUG, RELIABILITY, effort, false);
  }

  private DefaultIssue newSecurityIssue(long effort) {
    return createIssue(VULNERABILITY, SECURITY, effort, true);
  }

  private DefaultIssue oldVulnerabilityIssue(long effort) {
    return createIssue(VULNERABILITY, SECURITY, effort, false);
  }

  private DefaultIssue newMaintainabilityIssueWithoutEffort() {
    DefaultIssue defaultIssue = new DefaultIssue()
      .setKey(UuidFactoryFast.getInstance().create())
      .replaceImpacts(Map.of(MAINTAINABILITY, Severity.HIGH))
      .setType(CODE_SMELL);
    when(newIssueClassifier.isNew(any(), eq(defaultIssue))).thenReturn(true);
    return defaultIssue;
  }

  private DefaultIssue createIssue(RuleType type, SoftwareQuality softwareQuality, long effort, boolean isNew) {
    return createIssue(type, List.of(softwareQuality), effort, isNew);
  }

  private DefaultIssue createIssue(RuleType type, List<SoftwareQuality> softwareQualities, long effort, boolean isNew) {
    DefaultIssue defaultIssue = new DefaultIssue()
      .setKey(UuidFactoryFast.getInstance().create())
      .setEffort(Duration.create(effort))
      .setType(type)
      .replaceImpacts(softwareQualities.stream().collect(Collectors.toMap(e -> e, e -> HIGH)));
    when(newIssueClassifier.isNew(any(), eq(defaultIssue))).thenReturn(isNew);
    return defaultIssue;
  }

  private static DefaultIssue newReliabilityIssueWithoutEffort() {
    return new DefaultIssue().setType(BUG).replaceImpacts(Map.of(RELIABILITY, Severity.HIGH));
  }

  private static DefaultIssue newSecurityIssueWithoutEffort() {
    return new DefaultIssue().setType(VULNERABILITY).replaceImpacts(Map.of(SECURITY, Severity.HIGH));
  }
}
