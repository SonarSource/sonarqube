/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.api.posttask;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collections;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;
import org.sonar.ce.task.projectanalysis.qualitygate.ConditionStatus;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ConditionToConditionTest {
  private static final String METRIC_KEY = "metricKey";
  private static final String ERROR_THRESHOLD = "error threshold";
  private static final Map<Condition, ConditionStatus> NO_STATUS_PER_CONDITIONS = Collections.emptyMap();
  private static final String SOME_VALUE = "some value";
  private static final ConditionStatus SOME_CONDITION_STATUS = ConditionStatus.create(ConditionStatus.EvaluationStatus.OK, SOME_VALUE);
  private static final Condition SOME_CONDITION = new Condition(newMetric(METRIC_KEY), Condition.Operator.LESS_THAN.getDbValue(), ERROR_THRESHOLD);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void apply_throws_NPE_if_Condition_argument_is_null() {
    ConditionToCondition underTest = new ConditionToCondition(NO_STATUS_PER_CONDITIONS);

    expectedException.expect(NullPointerException.class);

    underTest.apply(null);
  }

  @Test
  public void apply_throws_ISE_if_there_is_no_ConditionStatus_for_Condition_argument() {
    ConditionToCondition underTest = new ConditionToCondition(NO_STATUS_PER_CONDITIONS);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Missing ConditionStatus for condition on metric key " + METRIC_KEY);

    underTest.apply(SOME_CONDITION);
  }

  @Test
  @UseDataProvider("allEvaluationStatuses")
  public void apply_converts_all_values_of_status(ConditionStatus.EvaluationStatus status) {
    ConditionToCondition underTest = new ConditionToCondition(of(
      SOME_CONDITION,
      status == ConditionStatus.EvaluationStatus.NO_VALUE ? ConditionStatus.NO_VALUE_STATUS : ConditionStatus.create(status, SOME_VALUE)));

    assertThat(underTest.apply(SOME_CONDITION).getStatus().name()).isEqualTo(status.name());
  }

  @Test
  public void apply_converts_key_from_metric() {
    ConditionToCondition underTest = new ConditionToCondition(of(SOME_CONDITION, SOME_CONDITION_STATUS));

    assertThat(underTest.apply(SOME_CONDITION).getMetricKey()).isEqualTo(METRIC_KEY);
  }

  @Test
  public void apply_copies_thresholds() {
    ConditionToCondition underTest = new ConditionToCondition(of(SOME_CONDITION, SOME_CONDITION_STATUS));

    assertThat(underTest.apply(SOME_CONDITION).getErrorThreshold()).isEqualTo(ERROR_THRESHOLD);
  }

  @Test
  @UseDataProvider("allOperatorValues")
  public void apply_converts_all_values_of_operator(Condition.Operator operator) {
    Condition condition = new Condition(newMetric(METRIC_KEY), operator.getDbValue(), ERROR_THRESHOLD);
    ConditionToCondition underTest = new ConditionToCondition(of(condition, SOME_CONDITION_STATUS));

    assertThat(underTest.apply(condition).getOperator().name()).isEqualTo(operator.name());
  }

  @Test
  public void apply_copies_value() {
    Condition otherCondition = new Condition(newMetric(METRIC_KEY), Condition.Operator.LESS_THAN.getDbValue(), ERROR_THRESHOLD);
    ConditionToCondition underTest = new ConditionToCondition(of(
      SOME_CONDITION, SOME_CONDITION_STATUS,
      otherCondition, ConditionStatus.NO_VALUE_STATUS));

    assertThat(underTest.apply(SOME_CONDITION).getValue()).isEqualTo(SOME_VALUE);

    QualityGate.Condition res = underTest.apply(otherCondition);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("There is no value when status is NO_VALUE");

    res.getValue();
  }

  @DataProvider
  public static Object[][] allEvaluationStatuses() {
    Object[][] res = new Object[ConditionStatus.EvaluationStatus.values().length][1];
    int i = 0;
    for (ConditionStatus.EvaluationStatus status : ConditionStatus.EvaluationStatus.values()) {
      res[i][0] = status;
      i++;
    }
    return res;
  }

  @DataProvider
  public static Object[][] allOperatorValues() {
    Object[][] res = new Object[Condition.Operator.values().length][1];
    int i = 0;
    for (Condition.Operator operator : Condition.Operator.values()) {
      res[i][0] = operator;
      i++;
    }
    return res;
  }

  private static Metric newMetric(String metricKey) {
    Metric metric = mock(Metric.class);
    when(metric.getKey()).thenReturn(metricKey);
    return metric;
  }
}
