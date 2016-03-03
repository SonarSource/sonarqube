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
package org.sonar.server.computation.issue;

import com.google.common.base.Optional;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.db.DbClient;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepositoryRule;
import org.sonar.server.computation.metric.MetricRepositoryRule;
import org.sonar.server.computation.period.Period;
import org.sonar.server.computation.period.PeriodsHolderRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.core.config.CorePropertyDefinitions.TIMEMACHINE_MODE_PREVIOUS_ANALYSIS;

public class NewEffortAggregatorTest {

  private static final Period PERIOD = new Period(1, TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, null, 1_500_000_000L, 1000L);

  static Component FILE = ReportComponent.builder(Component.Type.FILE, 1).setUuid("FILE").build();
  static final Component PROJECT = ReportComponent.builder(Component.Type.PROJECT, 2).addChildren(FILE).build();

  NewEffortCalculator calculator = mock(NewEffortCalculator.class);

  @org.junit.Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);

  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(CoreMetrics.NEW_TECHNICAL_DEBT);

  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create();

  NewEffortAggregator underTest = new NewEffortAggregator(calculator, periodsHolder, dbClient, metricRepository, measureRepository);

  @Test
  public void sum_new_maintainability_effort_of_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue unresolved1 = newCodeSmellIssue(10L);
    DefaultIssue unresolved2 = newCodeSmellIssue(30L);
    DefaultIssue unresolvedWithoutDebt = newCodeSmellIssueWithoutEffort();
    DefaultIssue resolved =newCodeSmellIssue(50L).setResolution(RESOLUTION_FIXED);

    when(calculator.calculate(same(unresolved1), anyList(), same(PERIOD))).thenReturn(4L);
    when(calculator.calculate(same(unresolved2), anyList(), same(PERIOD))).thenReturn(3L);
    verifyNoMoreInteractions(calculator);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved1);
    underTest.onIssue(FILE, unresolved2);
    underTest.onIssue(FILE, unresolvedWithoutDebt);
    underTest.onIssue(FILE, resolved);
    underTest.afterComponent(FILE);

    Measure newMeasure = newMaintainabilityEffortMeasure(FILE).get();
    assertThat(newMeasure.getVariations().getVariation(PERIOD.getIndex())).isEqualTo(3 + 4);
    assertThat(newMeasure.getVariations().hasVariation(PERIOD.getIndex() + 1)).isFalse();
  }

  @Test
  public void new_maintainability_effort_is_only_computed_using_code_smell_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue codeSmellIssue = newCodeSmellIssue(10);
    // Issue of type BUG should be ignored
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

    // Only effort of code smell issue is used
    Measure newMeasure = newMaintainabilityEffortMeasure(FILE).get();
    assertThat(newMeasure.getVariations().getVariation(PERIOD.getIndex())).isEqualTo(4);
  }

  @Test
  public void aggregate_new_debt_of_children() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue fileIssue = newCodeSmellIssue(10);
    DefaultIssue projectIssue = newCodeSmellIssue(30);
    when(calculator.calculate(same(fileIssue), anyList(), same(PERIOD))).thenReturn(4L);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, fileIssue);
    underTest.afterComponent(FILE);
    underTest.beforeComponent(PROJECT);
    underTest.onIssue(PROJECT, projectIssue);
    underTest.afterComponent(PROJECT);

    Measure newMeasure = newMaintainabilityEffortMeasure(PROJECT).get();
    assertThat(newMeasure.getVariations().getVariation(PERIOD.getIndex())).isEqualTo(4);
    assertThat(newMeasure.getVariations().hasVariation(PERIOD.getIndex() + 1)).isFalse();
  }

  @Test
  public void no_measures_if_no_periods() {
    periodsHolder.setPeriods();
    DefaultIssue unresolved = new DefaultIssue().setDebt(Duration.create(10));
    verifyZeroInteractions(calculator);

    underTest.beforeComponent(FILE);
    underTest.onIssue(FILE, unresolved);
    underTest.afterComponent(FILE);

    assertThat(newMaintainabilityEffortMeasure(FILE).isPresent()).isFalse();
  }

  private Optional<Measure> newMaintainabilityEffortMeasure(Component component) {
    return measureRepository.getRawMeasure(component, metricRepository.getByKey(CoreMetrics.NEW_TECHNICAL_DEBT_KEY));
  }

  private static DefaultIssue newCodeSmellIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setDebt(Duration.create(effort)).setType(RuleType.CODE_SMELL);
  }

  private static DefaultIssue newCodeSmellIssueWithoutEffort() {
    return new DefaultIssue().setType(RuleType.CODE_SMELL);
  }

  private static DefaultIssue newBugIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setDebt(Duration.create(effort)).setType(RuleType.BUG);
  }

  private static DefaultIssue newVulnerabilityIssue(long effort) {
    return newCodeSmellIssueWithoutEffort().setDebt(Duration.create(effort)).setType(RuleType.VULNERABILITY);
  }
}
