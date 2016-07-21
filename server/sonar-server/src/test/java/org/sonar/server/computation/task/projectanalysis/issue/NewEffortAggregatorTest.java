/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.task.projectanalysis.issue;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.measure.Measure;
import org.sonar.server.computation.task.projectanalysis.measure.MeasureRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.metric.MetricRepositoryRule;
import org.sonar.server.computation.task.projectanalysis.period.Period;
import org.sonar.server.computation.task.projectanalysis.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.api.rules.RuleType.BUG;
import static org.sonar.api.rules.RuleType.CODE_SMELL;
import static org.sonar.api.rules.RuleType.VULNERABILITY;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS;

public class NewEffortAggregatorTest {

  private static final Period PERIOD = new Period(1, TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, null, 1_500_000_000L, "U1");

  static final Component FILE = ReportComponent.builder(Component.Type.FILE, 1).setUuid("FILE").build();
  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  NewEffortCalculator calculator = mock(NewEffortCalculator.class);

  @org.junit.Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule()
    .add(NEW_TECHNICAL_DEBT)
    .add(NEW_RELIABILITY_REMEDIATION_EFFORT)
    .add(NEW_SECURITY_REMEDIATION_EFFORT);

  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create();

  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);

  NewEffortAggregator underTest = new NewEffortAggregator(calculator, periodsHolder, dbClient, metricRepository, measureRepository);

  @Test
  public void sum_new_maintainability_effort_of_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue unresolved1 = newCodeSmellIssue(10L);
    DefaultIssue unresolved2 = newCodeSmellIssue(30L);
    DefaultIssue unresolvedWithoutDebt = newCodeSmellIssueWithoutEffort();
    DefaultIssue resolved = newCodeSmellIssue(50L).setResolution(RESOLUTION_FIXED);

    when(calculator.calculate(same(unresolved1), anyList(), same(PERIOD))).thenReturn(4L);
    when(calculator.calculate(same(unresolved2), anyList(), same(PERIOD))).thenReturn(3L);
    verifyNoMoreInteractions(calculator);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertVariation(FILE, NEW_TECHNICAL_DEBT_KEY, 3 + 4);
  }

  @Test
  public void new_maintainability_effort_is_only_computed_using_code_smell_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    // Issues of type BUG and VULNERABILITY should be ignored
    DefaultIssue bugIssue = newBugIssue(15);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);

    when(calculator.calculate(same(codeSmellIssue), anyList(), same(PERIOD))).thenReturn(4L);
    when(calculator.calculate(same(bugIssue), anyList(), same(PERIOD))).thenReturn(3L);
    when(calculator.calculate(same(vulnerabilityIssue), anyList(), same(PERIOD))).thenReturn(5L);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of CODE SMELL issue is used
    assertVariation(FILE, NEW_TECHNICAL_DEBT_KEY, 4);
  }

  @Test
  public void sum_new_reliability_effort_of_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue unresolved1 = newBugIssue(10L);
    DefaultIssue unresolved2 = newBugIssue(30L);
    DefaultIssue unresolvedWithoutDebt = newBugIssueWithoutEffort();
    DefaultIssue resolved = newBugIssue(50L).setResolution(RESOLUTION_FIXED);

    when(calculator.calculate(same(unresolved1), anyList(), same(PERIOD))).thenReturn(4L);
    when(calculator.calculate(same(unresolved2), anyList(), same(PERIOD))).thenReturn(3L);
    verifyNoMoreInteractions(calculator);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertVariation(FILE, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 3 + 4);
  }

  @Test
  public void new_reliability_effort_is_only_computed_using_bug_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue bugIssue = newBugIssue(15);
    // Issues of type CODE SMELL and VULNERABILITY should be ignored
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);

    when(calculator.calculate(same(bugIssue), anyList(), same(PERIOD))).thenReturn(3L);
    when(calculator.calculate(same(codeSmellIssue), anyList(), same(PERIOD))).thenReturn(4L);
    when(calculator.calculate(same(vulnerabilityIssue), anyList(), same(PERIOD))).thenReturn(5L);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of BUG issue is used
    assertVariation(FILE, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 3);
  }

  @Test
  public void sum_new_securtiy_effort_of_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue unresolved1 = newVulnerabilityIssue(10L);
    DefaultIssue unresolved2 = newVulnerabilityIssue(30L);
    DefaultIssue unresolvedWithoutDebt = newVulnerabilityIssueWithoutEffort();
    DefaultIssue resolved = newVulnerabilityIssue(50L).setResolution(RESOLUTION_FIXED);

    when(calculator.calculate(same(unresolved1), anyList(), same(PERIOD))).thenReturn(4L);
    when(calculator.calculate(same(unresolved2), anyList(), same(PERIOD))).thenReturn(3L);
    verifyNoMoreInteractions(calculator);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    assertVariation(FILE, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 3 + 4);
  }

  @Test
  public void new_security_effort_is_only_computed_using_vulnerability_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);
    // Issues of type CODE SMELL and BUG should be ignored
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    DefaultIssue bugIssue = newBugIssue(15);

    when(calculator.calculate(same(vulnerabilityIssue), anyList(), same(PERIOD))).thenReturn(5L);
    when(calculator.calculate(same(codeSmellIssue), anyList(), same(PERIOD))).thenReturn(4L);
    when(calculator.calculate(same(bugIssue), anyList(), same(PERIOD))).thenReturn(3L);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);

    // Only effort of VULNERABILITY issue is used
    assertVariation(FILE, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 5);
  }

  @Test
  public void aggregate_new_characteristic_measures_of_children() {
    periodsHolder.setPeriods(PERIOD);

    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    when(calculator.calculate(same(codeSmellIssue), anyList(), same(PERIOD))).thenReturn(4L);
    DefaultIssue bugIssue = newBugIssue(8);
    when(calculator.calculate(same(bugIssue), anyList(), same(PERIOD))).thenReturn(3L);
    DefaultIssue vulnerabilityIssue = newVulnerabilityIssue(12);
    when(calculator.calculate(same(vulnerabilityIssue), anyList(), same(PERIOD))).thenReturn(6L);

    DefaultIssue codeSmellProjectIssue = newCodeSmellIssue(30);
    when(calculator.calculate(same(codeSmellProjectIssue), anyList(), same(PERIOD))).thenReturn(1L);
    DefaultIssue bugProjectIssue = newBugIssue(28);
    when(calculator.calculate(same(bugProjectIssue), anyList(), same(PERIOD))).thenReturn(2L);
    DefaultIssue vulnerabilityProjectIssue = newVulnerabilityIssue(32);
    when(calculator.calculate(same(vulnerabilityProjectIssue), anyList(), same(PERIOD))).thenReturn(4L);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, codeSmellIssue);
    underTest.onIssue(FILE, bugIssue);
    underTest.onIssue(FILE, vulnerabilityIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, codeSmellProjectIssue);
    underTest.onIssue(PROJECT, bugProjectIssue);
    underTest.onIssue(PROJECT, vulnerabilityProjectIssue);
    underTest.afterComponent(PROJECT);

    assertVariation(PROJECT, NEW_TECHNICAL_DEBT_KEY, 4 + 1);
    assertVariation(PROJECT, NEW_RELIABILITY_REMEDIATION_EFFORT_KEY, 3 + 2);
    assertVariation(PROJECT, NEW_SECURITY_REMEDIATION_EFFORT_KEY, 6 + 4);
  }

  @Test
  public void no_measures_if_no_periods() {
    periodsHolder.setPeriods();
    DefaultIssue unresolved = new DefaultIssue().setEffort(Duration.create(10));
    verifyZeroInteractions(calculator);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved);
    underTest.afterComponent(FILE);

    assertThat(measureRepository.getRawMeasures(FILE)).isEmpty();
  }

  private void assertVariation(Component component, String metricKey, int variation) {
    Measure newMeasure = measureRepository.getRawMeasure(component, metricRepository.getByKey(metricKey)).get();
    assertThat(newMeasure.getVariations().getVariation(PERIOD.getIndex())).isEqualTo(variation);
    assertThat(newMeasure.getVariations().hasVariation(PERIOD.getIndex() + 1)).isFalse();
  }

  private static DefaultIssue newCodeSmellIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setEffort(Duration.create(effort)).setType(RuleType.CODE_SMELL);
  }

  private static DefaultIssue newBugIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setEffort(Duration.create(effort)).setType(RuleType.BUG);
  }

  private static DefaultIssue newVulnerabilityIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setEffort(Duration.create(effort)).setType(RuleType.VULNERABILITY);
  }

  private static DefaultIssue newCodeSmellIssueWithoutEffort() {
    return new DefaultIssue().setType(CODE_SMELL);
  }

  private static DefaultIssue newBugIssueWithoutEffort() {
    return new DefaultIssue().setType(BUG);
  }

  private static DefaultIssue newVulnerabilityIssueWithoutEffort() {
    return new DefaultIssue().setType(VULNERABILITY);
  }
}
