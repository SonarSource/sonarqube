/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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

public class NewDebtAggregatorTest {

  private static final Period PERIOD = new Period(1, TIMEMACHINE_MODE_PREVIOUS_ANALYSIS, null, 1_500_000_000L, 1000L);

  Component file = ReportComponent.builder(Component.Type.FILE, 1).setUuid("FILE").build();

  NewDebtCalculator calculator = mock(NewDebtCalculator.class);

  @org.junit.Rule
  public PeriodsHolderRule periodsHolder = new PeriodsHolderRule();

  DbClient dbClient = mock(DbClient.class, Mockito.RETURNS_DEEP_STUBS);

  @org.junit.Rule
  public MetricRepositoryRule metricRepository = new MetricRepositoryRule().add(CoreMetrics.NEW_TECHNICAL_DEBT);

  @org.junit.Rule
  public MeasureRepositoryRule measureRepository = MeasureRepositoryRule.create();

  NewDebtAggregator underTest = new NewDebtAggregator(calculator, periodsHolder, dbClient, metricRepository, measureRepository);

  @Test
  public void sum_new_debt_of_issues() {
    periodsHolder.setPeriods(PERIOD);
    DefaultIssue unresolved1 = new DefaultIssue().setDebt(Duration.create(10));
    DefaultIssue unresolved2 = new DefaultIssue().setDebt(Duration.create(30));
    DefaultIssue unresolvedWithoutDebt = new DefaultIssue().setDebt(null);
    DefaultIssue resolved = new DefaultIssue().setDebt(Duration.create(50)).setResolution(RESOLUTION_FIXED);
    when(calculator.calculate(same(unresolved1), anyList(), same(PERIOD))).thenReturn(4L);
    when(calculator.calculate(same(unresolved2), anyList(), same(PERIOD))).thenReturn(3L);
    verifyNoMoreInteractions(calculator);

    underTest.beforeComponent(file);
    underTest.onIssue(file, unresolved1);
    underTest.onIssue(file, unresolved2);
    underTest.onIssue(file, unresolvedWithoutDebt);
    underTest.onIssue(file, resolved);
    underTest.afterComponent(file);

    Measure newDebtMeasure = newDebtMeasure(file).get();
    assertThat(newDebtMeasure.getVariations().getVariation(PERIOD.getIndex())).isEqualTo(3 + 4);
    assertThat(newDebtMeasure.getVariations().hasVariation(PERIOD.getIndex() + 1)).isFalse();
  }

  private Optional<Measure> newDebtMeasure(Component component) {
    return measureRepository.getRawMeasure(component, metricRepository.getByKey(CoreMetrics.NEW_TECHNICAL_DEBT_KEY));
  }

  @Test
  public void aggregate_new_debt_of_children() {

  }

  @Test
  public void no_measures_if_no_periods() {
    periodsHolder.setPeriods();
    DefaultIssue unresolved = new DefaultIssue().setDebt(Duration.create(10));
    verifyZeroInteractions(calculator);

    underTest.beforeComponent(file);
    underTest.onIssue(file, unresolved);
    underTest.afterComponent(file);

    assertThat(newDebtMeasure(file).isPresent()).isFalse();
  }
}
