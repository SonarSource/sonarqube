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
package org.sonar.server.computation.qualitygate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureVariations;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricImpl;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.sonar.server.computation.measure.Measure.Level.ERROR;
import static org.sonar.server.computation.measure.Measure.Level.OK;
import static org.sonar.server.computation.measure.Measure.newMeasureBuilder;
import static org.sonar.server.computation.metric.Metric.MetricType;
import static org.sonar.server.computation.qualitygate.Condition.Operator.EQUALS;
import static org.sonar.server.computation.qualitygate.Condition.Operator.GREATER_THAN;
import static org.sonar.server.computation.qualitygate.Condition.Operator.LESS_THAN;
import static org.sonar.server.computation.qualitygate.Condition.Operator.NOT_EQUALS;
import static org.sonar.server.computation.qualitygate.EvaluationResultAssert.assertThat;

public class ConditionEvaluatorTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ConditionEvaluator underTest = new ConditionEvaluator();

  @Test
  public void testInputNumbers() {
    try {
      Metric metric = createMetric(MetricType.FLOAT);
      Measure measure = newMeasureBuilder().create(10.2d, 1, null);
      underTest.evaluate(createErrorCondition(metric, LESS_THAN, "20"), measure);
    } catch (NumberFormatException ex) {
      fail();
    }

    try {
      Metric metric = createMetric(MetricType.INT);
      Measure measure = newMeasureBuilder().create(5, null);
      underTest.evaluate(createErrorCondition(metric, LESS_THAN, "20.1"), measure);
    } catch (NumberFormatException ex) {
      fail();
    }

    try {
      Metric metric = createMetric(MetricType.PERCENT);
      Measure measure = newMeasureBuilder().create(10.2d, 1, null);
      underTest.evaluate(createErrorCondition(metric, LESS_THAN, "20.1"), measure);
    } catch (NumberFormatException ex) {
      fail();
    }
  }

  private static Condition createErrorCondition(Metric metric, Condition.Operator operator, String errorThreshold) {
    return new Condition(metric, operator.getDbValue(), errorThreshold, null, null);
  }

  private static MetricImpl createMetric(MetricType metricType) {
    return new MetricImpl(1, "key", "name", metricType);
  }

  @Test
  public void testEquals_for_double() {
    Metric metric = createMetric(MetricType.FLOAT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"), measure)).hasLevel(ERROR).hasValue(10.2d);
    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.1"), measure)).hasLevel(OK).hasValue(10.2d);
  }

  @Test
  public void testEquals_for_String() {
    Metric metric = createMetric(MetricType.STRING);
    Measure measure = newMeasureBuilder().create("TEST");

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "TEST"), measure)).hasLevel(ERROR).hasValue("TEST");
    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "TEST2"), measure)).hasLevel(OK).hasValue("TEST");
  }

  @Test
  public void testNotEquals_for_double() {
    Metric metric = createMetric(MetricType.FLOAT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "10.2"), measure)).hasLevel(OK).hasValue(10.2d);
    assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "10.1"), measure)).hasLevel(ERROR).hasValue(10.2d);
  }

  @Test
  public void testNotEquals() {
    Metric metric = createMetric(MetricType.STRING);
    Measure measure = newMeasureBuilder().create("TEST");

    assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "TEST"), measure)).hasLevel(OK).hasValue("TEST");
    assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "TEST2"), measure)).hasLevel(ERROR).hasValue("TEST");
  }

  @Test
  public void testGreater() {
    Metric metric = createMetric(MetricType.FLOAT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, GREATER_THAN, "10.1"), measure)).hasLevel(ERROR).hasValue(10.2d);
    assertThat(underTest.evaluate(createErrorCondition(metric, GREATER_THAN, "10.2"), measure)).hasLevel(OK).hasValue(10.2d);
    assertThat(underTest.evaluate(createErrorCondition(metric, GREATER_THAN, "10.3"), measure)).hasLevel(OK).hasValue(10.2d);
  }

  @Test
  public void testSmaller() {
    Metric metric = createMetric(MetricType.FLOAT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, LESS_THAN, "10.1"), measure)).hasLevel(OK).hasValue(10.2d);
    assertThat(underTest.evaluate(createErrorCondition(metric, LESS_THAN, "10.2"), measure)).hasLevel(OK).hasValue(10.2d);
    assertThat(underTest.evaluate(createErrorCondition(metric, LESS_THAN, "10.3"), measure)).hasLevel(ERROR).hasValue(10.2d);
  }

  @Test
  public void testEquals_Percent() {
    Metric metric = createMetric(MetricType.PERCENT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"), measure)).hasLevel(ERROR).hasValue(10.2d);
  }

  @Test
  public void testEquals_Float() {
    Metric metric = createMetric(MetricType.PERCENT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"), measure)).hasLevel(ERROR).hasValue(10.2d);
  }

  @Test
  public void testEquals_Int() {
    Metric metric = createMetric(MetricType.INT);
    Measure measure = newMeasureBuilder().create(10, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10"), measure)).hasLevel(ERROR).hasValue(10);
    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"), measure)).hasLevel(ERROR).hasValue(10);
  }

  @Test
  public void testEquals_Level() {
    Metric metric = createMetric(MetricType.LEVEL);
    Measure measure = newMeasureBuilder().create(ERROR);

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, ERROR.name()), measure)).hasLevel(ERROR).hasValue(ERROR.name());

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, OK.name()), measure)).hasLevel(OK).hasValue(ERROR.name());
  }

  @Test
  public void testNotEquals_Level() {
    Metric metric = createMetric(MetricType.LEVEL);
    Measure measure = newMeasureBuilder().create(ERROR);

    assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, OK.name()), measure)).hasLevel(ERROR).hasValue(ERROR.name());
  }

  @Test
  public void testEquals_BOOL() {
    Metric metric = createMetric(MetricType.BOOL);
    Measure measure = newMeasureBuilder().create(false, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "1"), measure)).hasLevel(OK).hasValue(false);
    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "0"), measure)).hasLevel(ERROR).hasValue(false);
  }

  @Test
  public void testNotEquals_BOOL() {
    Metric metric = createMetric(MetricType.BOOL);
    Measure measure = newMeasureBuilder().create(false, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "1"), measure)).hasLevel(ERROR).hasValue(false);
    assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "0"), measure)).hasLevel(OK).hasValue(false);
  }

  @Test
  public void getLevel_throws_IEA_if_error_threshold_is_not_parsable_boolean() {
    Metric metric = createMetric(MetricType.BOOL);
    Measure measure = newMeasureBuilder().create(false, null);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Quality Gate: Unable to parse value 'polop' to compare against name");

    underTest.evaluate(createErrorCondition(metric, EQUALS, "polop"), measure);
  }

  @Test
  public void testEquals_work_duration() {
    Metric metric = createMetric(MetricType.WORK_DUR);
    Measure measure = newMeasureBuilder().create(60l, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "60"), measure)).hasLevel(ERROR);
  }

  @Test
  public void getLevel_throws_IEA_if_error_threshold_is_not_parsable_long() {
    Metric metric = createMetric(MetricType.WORK_DUR);
    Measure measure = newMeasureBuilder().create(60l, null);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Quality Gate: Unable to parse value 'polop' to compare against name");

    underTest.evaluate(createErrorCondition(metric, EQUALS, "polop"), measure);
  }

  @Test
  public void testErrorAndWarningLevel() {
    Metric metric = createMetric(MetricType.FLOAT);
    Measure measure = newMeasureBuilder().create(10.2d, 1, null);

    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"), measure)).hasLevel(ERROR);
    assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.1"), measure)).hasLevel(OK);

    assertThat(underTest.evaluate(new Condition(metric, EQUALS.getDbValue(), "10.3", "10.2", null), measure)).hasLevel(Measure.Level.WARN);
    assertThat(underTest.evaluate(new Condition(metric, LESS_THAN.getDbValue(), "10.3", "10.2", null), measure)).hasLevel(Measure.Level.ERROR);
  }

  @Test
  public void condition_is_always_ok_when_measure_is_noValue() {
    for (MetricType metricType : from(asList(MetricType.values())).filter(not(in(ImmutableSet.of(MetricType.DATA, MetricType.LEVEL))))) {
      Metric metric = createMetric(metricType);
      Measure measure = newMeasureBuilder().createNoValue();

      assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"), measure)).hasLevel(OK);
    }
  }

  @Test
  public void testUnsupportedType() {
    Metric metric = createMetric(MetricType.DATA);
    Measure measure = newMeasureBuilder().create("3.14159265358");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Conditions on MetricType DATA are not supported");

    underTest.evaluate(createErrorCondition(metric, EQUALS, "1.60217657"), measure);
  }

  @Test
  public void test_condition_on_period() {
    for (MetricType metricType : ImmutableList.of(MetricType.FLOAT, MetricType.INT, MetricType.WORK_DUR)) {
      Metric metric = createMetric(metricType);
      Measure measure = newMeasureBuilder().setVariations(new MeasureVariations(null, 3d, 4d, null, null)).createNoValue();

      assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "3", null, 2), measure)).hasLevel(OK);
      assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "2", null, 2), measure)).hasLevel(ERROR);
      assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "4", null, 3), measure)).hasLevel(OK);
      assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "3", null, 3), measure)).hasLevel(ERROR);
    }
  }

  @Test
  public void condition_on_period_without_value_is_OK() {
    Metric metric = createMetric(MetricType.FLOAT);
    Measure measure = newMeasureBuilder().setVariations(new MeasureVariations(null, 3d, 4d, null, null)).createNoValue();

    assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "3", null, 1), measure)).hasLevel(OK);
    assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "3", null, 4), measure)).hasLevel(OK);
    assertThat(underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "3", null, 5), measure)).hasLevel(OK);
  }

  @Test
  public void evaluate_throws_IAE_if_condition_is_on_invalid_period() {
    Metric metric = createMetric(MetricType.FLOAT);
    Measure measure = newMeasureBuilder().setVariations(new MeasureVariations(null, 3d, 4d, null, null)).createNoValue();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Following index period is not allowed : 50");

    underTest.evaluate(new Condition(metric, GREATER_THAN.getDbValue(), "3", null, 50), measure);
  }
}
