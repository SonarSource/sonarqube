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
package org.sonar.ce.task.projectanalysis.qualitygate;

import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricImpl;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.fail;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.ERROR;
import static org.sonar.ce.task.projectanalysis.measure.Measure.Level.OK;
import static org.sonar.ce.task.projectanalysis.measure.Measure.newMeasureBuilder;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.BOOL;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.DATA;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.DISTRIB;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.FLOAT;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.INT;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.PERCENT;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.RATING;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.STRING;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.WORK_DUR;
import static org.sonar.ce.task.projectanalysis.metric.Metric.MetricType.values;
import static org.sonar.ce.task.projectanalysis.qualitygate.Condition.Operator.GREATER_THAN;
import static org.sonar.ce.task.projectanalysis.qualitygate.Condition.Operator.LESS_THAN;
import static org.sonar.ce.task.projectanalysis.qualitygate.EvaluationResultAssert.assertThat;

@RunWith(DataProviderRunner.class)
public class ConditionEvaluatorTest {

  private ConditionEvaluator underTest = new ConditionEvaluator();

  @Test
  public void test_input_numbers() {
    try {
      Metric metric = createMetric(FLOAT);
      Measure measure = newMeasureBuilder().create(10.2d, 1, null);
      underTest.evaluate(createCondition(metric, LESS_THAN, "20"), measure);
    } catch (NumberFormatException ex) {
      fail();
    }

    try {
      Metric metric = createMetric(INT);
      Measure measure = newMeasureBuilder().create(5, null);
      underTest.evaluate(createCondition(metric, LESS_THAN, "20.1"), measure);
    } catch (NumberFormatException ex) {
      fail();
    }

    try {
      Metric metric = createMetric(PERCENT);
      Measure measure = newMeasureBuilder().create(10.2d, 1, null);
      underTest.evaluate(createCondition(metric, LESS_THAN, "20.1"), measure);
    } catch (NumberFormatException ex) {
      fail();
    }
  }

  @Test
  public void testGreater() {
    Metric metric = createMetric(FLOAT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createCondition(metric, GREATER_THAN, "10.1"), measure)).hasLevel(ERROR).hasValue(10.2d);
    assertThat(underTest.evaluate(createCondition(metric, GREATER_THAN, "10.2"), measure)).hasLevel(OK).hasValue(10.2d);
    assertThat(underTest.evaluate(createCondition(metric, GREATER_THAN, "10.3"), measure)).hasLevel(OK).hasValue(10.2d);
  }

  @Test
  public void testSmaller() {
    Metric metric = createMetric(FLOAT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createCondition(metric, LESS_THAN, "10.1"), measure)).hasLevel(OK).hasValue(10.2d);
    assertThat(underTest.evaluate(createCondition(metric, LESS_THAN, "10.2"), measure)).hasLevel(OK).hasValue(10.2d);
    assertThat(underTest.evaluate(createCondition(metric, LESS_THAN, "10.3"), measure)).hasLevel(ERROR).hasValue(10.2d);
  }

  @Test
  public void getLevel_throws_IEA_if_error_threshold_is_not_parsable_long() {
    Metric metric = createMetric(WORK_DUR);
    Measure measure = newMeasureBuilder().create(60L, null);

    assertThatThrownBy(() -> underTest.evaluate(createCondition(metric, LESS_THAN, "polop"), measure))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Quality Gate: Unable to parse value 'polop' to compare against name");
  }

  @Test
  public void testErrorLevel() {
    Metric metric = createMetric(FLOAT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createCondition(metric, LESS_THAN, "10.3"), measure)).hasLevel(ERROR);
    assertThat(underTest.evaluate(createCondition(metric, LESS_THAN, "10.1"), measure)).hasLevel(OK);

    assertThat(underTest.evaluate(new Condition(metric, LESS_THAN.getDbValue(), "10.3"), measure)).hasLevel(Measure.Level.ERROR);
  }

  @Test
  public void condition_is_always_ok_when_measure_is_noValue() {
    for (MetricType metricType : from(asList(values())).filter(not(in(ImmutableSet.of(BOOL, DATA, DISTRIB, STRING))))) {
      Metric metric = createMetric(metricType);
      Measure measure = newMeasureBuilder().createNoValue();

      assertThat(underTest.evaluate(createCondition(metric, LESS_THAN, "10.2"), measure)).hasLevel(OK);
    }
  }

  @Test
  @UseDataProvider("unsupportedMetricTypes")
  public void fail_when_metric_is_not_supported(MetricType metricType) {
    Metric metric = createMetric(metricType);
    Measure measure = newMeasureBuilder().create("3.14159265358");

    assertThatThrownBy(() -> underTest.evaluate(createCondition(metric, LESS_THAN, "1.60217657"), measure))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Conditions on MetricType %s are not supported", metricType));
  }

  @DataProvider
  public static Object[][] unsupportedMetricTypes() {
    return new Object[][] {
      {BOOL},
      {STRING},
      {DATA},
      {DISTRIB}
    };
  }

  @Test
  public void test_condition_on_rating() {
    Metric metric = createMetric(RATING);
    Measure measure = newMeasureBuilder().create(4, "D");

    assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "4"), measure)).hasLevel(OK).hasValue(4);
    assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "2"), measure)).hasLevel(ERROR).hasValue(4);
  }

  private static Condition createCondition(Metric metric, Condition.Operator operator, String errorThreshold) {
    return new Condition(metric, operator.getDbValue(), errorThreshold);
  }

  private static MetricImpl createMetric(MetricType metricType) {
    return new MetricImpl("1", "key", "name", metricType);
  }
}
