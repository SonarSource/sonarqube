/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.qualitygate;

import java.util.Optional;
import java.util.OptionalDouble;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.qualitygate.ConditionEvaluatorTest.FakeMeasure.newFakeMeasureOnLeak;

public class ConditionEvaluatorTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void EQUALS_double() {
    test(new FakeMeasure(10.1d), Condition.Operator.EQUALS, "10.2", EvaluatedCondition.EvaluationStatus.OK, "10.1");
    test(new FakeMeasure(10.2d), Condition.Operator.EQUALS, "10.2", EvaluatedCondition.EvaluationStatus.ERROR, "10.2");
    test(new FakeMeasure(10.3d), Condition.Operator.EQUALS, "10.2", EvaluatedCondition.EvaluationStatus.OK, "10.3");
  }

  @Test
  public void NOT_EQUALS_double() {
    test(new FakeMeasure(10.1d), Condition.Operator.NOT_EQUALS, "10.2", EvaluatedCondition.EvaluationStatus.ERROR, "10.1");
    test(new FakeMeasure(10.2d), Condition.Operator.NOT_EQUALS, "10.2", EvaluatedCondition.EvaluationStatus.OK, "10.2");
    test(new FakeMeasure(10.3d), Condition.Operator.NOT_EQUALS, "10.2", EvaluatedCondition.EvaluationStatus.ERROR, "10.3");
  }

  @Test
  public void GREATER_THAN_double() {
    test(new FakeMeasure(10.1d), Condition.Operator.GREATER_THAN, "10.2", EvaluatedCondition.EvaluationStatus.OK, "10.1");
    test(new FakeMeasure(10.2d), Condition.Operator.GREATER_THAN, "10.2", EvaluatedCondition.EvaluationStatus.OK, "10.2");
    test(new FakeMeasure(10.3d), Condition.Operator.GREATER_THAN, "10.2", EvaluatedCondition.EvaluationStatus.ERROR, "10.3");
  }

  @Test
  public void LESS_THAN_double() {
    test(new FakeMeasure(10.1d), Condition.Operator.LESS_THAN, "10.2", EvaluatedCondition.EvaluationStatus.ERROR, "10.1");
    test(new FakeMeasure(10.2d), Condition.Operator.LESS_THAN, "10.2", EvaluatedCondition.EvaluationStatus.OK, "10.2");
    test(new FakeMeasure(10.3d), Condition.Operator.LESS_THAN, "10.2", EvaluatedCondition.EvaluationStatus.OK, "10.3");
  }

  @Test
  public void EQUALS_int() {
    test(new FakeMeasure(10), Condition.Operator.EQUALS, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.EQUALS, "10", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(10), Condition.Operator.EQUALS, "11", EvaluatedCondition.EvaluationStatus.OK, "10");

    // badly stored thresholds are truncated
    test(new FakeMeasure(10), Condition.Operator.EQUALS, "10.4", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(10), Condition.Operator.EQUALS, "10.9", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(11), Condition.Operator.EQUALS, "10.9", EvaluatedCondition.EvaluationStatus.OK, "11");
  }

  @Test
  public void NOT_EQUALS_int() {
    test(new FakeMeasure(10), Condition.Operator.NOT_EQUALS, "9", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(10), Condition.Operator.NOT_EQUALS, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.NOT_EQUALS, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");

    // badly stored thresholds are truncated
    test(new FakeMeasure(10), Condition.Operator.NOT_EQUALS, "10.4", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.NOT_EQUALS, "10.9", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.NOT_EQUALS, "9.9", EvaluatedCondition.EvaluationStatus.ERROR, "10");
  }

  @Test
  public void GREATER_THAN_int() {
    test(new FakeMeasure(10), Condition.Operator.GREATER_THAN, "9", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(10), Condition.Operator.GREATER_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.GREATER_THAN, "11", EvaluatedCondition.EvaluationStatus.OK, "10");

    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.GREATER_THAN, "9", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.GREATER_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.GREATER_THAN, "11", EvaluatedCondition.EvaluationStatus.OK, "10");
  }

  @Test
  public void LESS_THAN_int() {
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");

    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.LESS_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");
  }

  @Test
  public void no_value_present() {
    test(new FakeMeasure((Integer) null), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, null);
    test(null, Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, null);
  }

  @Test
  public void empty_warning_condition() {
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "9", null, EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "9", "", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(3), Condition.Operator.LESS_THAN, "9", "", EvaluatedCondition.EvaluationStatus.ERROR, "3");
  }

  private void test(@Nullable QualityGateEvaluator.Measure measure, Condition.Operator operator, String errorThreshold, EvaluatedCondition.EvaluationStatus expectedStatus,
    @Nullable String expectedValue) {
    test(measure, operator, errorThreshold, null, expectedStatus, expectedValue);
  }

  private void test(@Nullable QualityGateEvaluator.Measure measure, Condition.Operator operator, String errorThreshold, @Nullable String warningThreshold,
    EvaluatedCondition.EvaluationStatus expectedStatus, @Nullable String expectedValue) {
    Condition condition = new Condition("foo", operator, errorThreshold, warningThreshold, false);

    EvaluatedCondition result = ConditionEvaluator.evaluate(condition, new FakeMeasures(measure));

    assertThat(result.getStatus()).isEqualTo(expectedStatus);
    if (expectedValue == null) {
      assertThat(result.getValue()).isNotPresent();
    } else {
      assertThat(result.getValue()).hasValue(expectedValue);
    }
  }

  private void testOnLeak(QualityGateEvaluator.Measure measure, Condition.Operator operator, String errorThreshold, EvaluatedCondition.EvaluationStatus expectedStatus,
    @Nullable String expectedValue) {
    Condition condition = new Condition("foo", operator, errorThreshold, null, true);

    EvaluatedCondition result = ConditionEvaluator.evaluate(condition, new FakeMeasures(measure));

    assertThat(result.getStatus()).isEqualTo(expectedStatus);
    if (expectedValue == null) {
      assertThat(result.getValue()).isNotPresent();
    } else {
      assertThat(result.getValue()).hasValue(expectedValue);
    }
  }

  private static class FakeMeasures implements QualityGateEvaluator.Measures {
    private final QualityGateEvaluator.Measure measure;

    FakeMeasures(@Nullable QualityGateEvaluator.Measure measure) {
      this.measure = measure;
    }

    @Override
    public Optional<QualityGateEvaluator.Measure> get(String metricKey) {
      return Optional.ofNullable(measure);
    }
  }

  static class FakeMeasure implements QualityGateEvaluator.Measure {
    private Double leakValue;
    private Double value;
    private Metric.ValueType valueType;

    private FakeMeasure() {

    }

    FakeMeasure(@Nullable Double value) {
      this.value = value;
      this.valueType = Metric.ValueType.FLOAT;
    }

    FakeMeasure(@Nullable Integer value) {
      this.value = value == null ? null : value.doubleValue();
      this.valueType = Metric.ValueType.INT;
    }

    static FakeMeasure newFakeMeasureOnLeak(@Nullable Integer value) {
      FakeMeasure that = new FakeMeasure();
      that.leakValue = value == null ? null : value.doubleValue();
      that.valueType = Metric.ValueType.INT;
      return that;
    }

    @Override
    public Metric.ValueType getType() {
      return valueType;
    }

    @Override
    public OptionalDouble getValue() {
      return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    @Override
    public Optional<String> getStringValue() {
      return Optional.empty();
    }

    @Override
    public OptionalDouble getLeakValue() {
      return leakValue == null ? OptionalDouble.empty() : OptionalDouble.of(leakValue);
    }
  }

  // @Test
  // public void testEquals_for_String() {
  // Metric metric = createMetric(STRING);
  // Measure measure = newMeasureBuilder().create("TEST");
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "TEST"),
  // measure)).hasLevel(ERROR).hasValue("TEST");
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "TEST2"),
  // measure)).hasLevel(OK).hasValue("TEST");
  // }
  //
  // @Test
  //
  // public void testNotEquals() {
  // Metric metric = createMetric(STRING);
  // Measure measure = newMeasureBuilder().create("TEST");
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "TEST"),
  // measure)).hasLevel(OK).hasValue("TEST");
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "TEST2"),
  // measure)).hasLevel(ERROR).hasValue("TEST");
  // }
  //
  // @Test
  // public void testEquals_Percent() {
  // Metric metric = createMetric(PERCENT);
  // Measure measure = newMeasureBuilder().create(10.2d, 1, null);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"),
  // measure)).hasLevel(ERROR).hasValue(10.2d);
  // }
  //
  // @Test
  // public void testEquals_Float() {
  // Metric metric = createMetric(PERCENT);
  // Measure measure = newMeasureBuilder().create(10.2d, 1, null);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"),
  // measure)).hasLevel(ERROR).hasValue(10.2d);
  // }
  //
  // @Test
  // public void testEquals_Int() {
  // Metric metric = createMetric(INT);
  // Measure measure = newMeasureBuilder().create(10, null);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10"), measure)).hasLevel(ERROR).hasValue(10);
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"),
  // measure)).hasLevel(ERROR).hasValue(10);
  // }
  //
  // @Test
  // public void testEquals_Level() {
  // Metric metric = createMetric(LEVEL);
  // Measure measure = newMeasureBuilder().create(ERROR);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, ERROR.name()),
  // measure)).hasLevel(ERROR).hasValue(ERROR.name());
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, OK.name()),
  // measure)).hasLevel(OK).hasValue(ERROR.name());
  // }
  //
  // @Test
  // public void testNotEquals_Level() {
  // Metric metric = createMetric(LEVEL);
  // Measure measure = newMeasureBuilder().create(ERROR);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, OK.name()),
  // measure)).hasLevel(ERROR).hasValue(ERROR.name());
  // }
  //
  // @Test
  // public void testEquals_BOOL() {
  // Metric metric = createMetric(BOOL);
  // Measure measure = newMeasureBuilder().create(false, null);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "1"), measure)).hasLevel(OK).hasValue(false);
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "0"),
  // measure)).hasLevel(ERROR).hasValue(false);
  // }
  //
  // @Test
  // public void testNotEquals_BOOL() {
  // Metric metric = createMetric(BOOL);
  // Measure measure = newMeasureBuilder().create(false, null);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "1"),
  // measure)).hasLevel(ERROR).hasValue(false);
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, NOT_EQUALS, "0"),
  // measure)).hasLevel(OK).hasValue(false);
  // }
  //
  // @Test
  // public void getLevel_throws_IEA_if_error_threshold_is_not_parsable_boolean() {
  // Metric metric = createMetric(BOOL);
  // Measure measure = newMeasureBuilder().create(false, null);
  //
  // expectedException.expect(IllegalArgumentException.class);
  // expectedException.expectMessage("Quality Gate: Unable to parse value 'polop' to compare against name");
  //
  // underTest.evaluate(createErrorCondition(metric, EQUALS, "polop"), measure);
  // }
  //
  // @Test
  // public void testEquals_work_duration() {
  // Metric metric = createMetric(WORK_DUR);
  // Measure measure = newMeasureBuilder().create(60l, null);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "60"), measure)).hasLevel(ERROR);
  // }
  //
  // @Test
  // public void getLevel_throws_IEA_if_error_threshold_is_not_parsable_long() {
  // Metric metric = createMetric(WORK_DUR);
  // Measure measure = newMeasureBuilder().create(60l, null);
  //
  // expectedException.expect(IllegalArgumentException.class);
  // expectedException.expectMessage("Quality Gate: Unable to parse value 'polop' to compare against name");
  //
  // underTest.evaluate(createErrorCondition(metric, EQUALS, "polop"), measure);
  // }
  //
  // @Test
  // public void testErrorAndWarningLevel() {
  // Metric metric = createMetric(FLOAT);
  // Measure measure = newMeasureBuilder().create(10.2d, 1, null);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"), measure)).hasLevel(ERROR);
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.1"), measure)).hasLevel(OK);
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // EQUALS.getDbValue(), "10.3", "10.2", false), measure)).hasLevel(Measure.Level.WARN);
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // LESS_THAN.getDbValue(), "10.3", "10.2", false), measure)).hasLevel(Measure.Level.ERROR);
  // }
  //
  // @Test
  // public void condition_is_always_ok_when_measure_is_noValue() {
  // for (Metric.MetricType metricType : from(asList(values())).filter(not(in(ImmutableSet.of(DATA, LEVEL))))) {
  // Metric metric = createMetric(metricType);
  // Measure measure = newMeasureBuilder().createNoValue();
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(createErrorCondition(metric, EQUALS, "10.2"), measure)).hasLevel(OK);
  // }
  // }
  //
  // @Test
  // public void testUnsupportedType() {
  // Metric metric = createMetric(DATA);
  // Measure measure = newMeasureBuilder().create("3.14159265358");
  //
  // expectedException.expect(IllegalArgumentException.class);
  // expectedException.expectMessage("Conditions on MetricType DATA are not supported");
  //
  // underTest.evaluate(createErrorCondition(metric, EQUALS, "1.60217657"), measure);
  // }
  //
  // @Test
  // public void condition_on_period() {
  // for (Metric.MetricType metricType : ImmutableList.of(FLOAT, INT, WORK_DUR)) {
  // Metric metric = createMetric(metricType);
  // Measure measure = newMeasureBuilder().setVariation(3d).createNoValue();
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // GREATER_THAN.getDbValue(), "3", null, true), measure)).hasLevel(OK);
  // }
  // }
  //
  // @Test
  // public void condition_on_period_without_value_is_OK() {
  // Metric metric = createMetric(FLOAT);
  // Measure measure = newMeasureBuilder().createNoValue();
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // GREATER_THAN.getDbValue(), "3", null, true), measure)).hasLevel(OK).hasValue(null);
  // }
  //
  // @Test
  // public void condition_on_rating() throws Exception {
  // Metric metric = createMetric(RATING);
  // Measure measure = newMeasureBuilder().create(4, "D");
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // GREATER_THAN.getDbValue(), "4", null, false), measure)).hasLevel(OK).hasValue(4);
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // GREATER_THAN.getDbValue(), "2", null, false), measure)).hasLevel(ERROR).hasValue(4);
  // }
  //
  // @Test
  // public void condition_on_rating_on_leak_period() throws Exception {
  // Metric metric = createMetric(RATING);
  // Measure measure = newMeasureBuilder().setVariation(4d).createNoValue();
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // GREATER_THAN.getDbValue(), "5", null, true), measure)).hasLevel(OK).hasValue(4);
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // GREATER_THAN.getDbValue(), "2", null, true), measure)).hasLevel(ERROR).hasValue(4);
  // }
  //
  // @Test
  // public void condition_on_rating_on_leak_period_when_variation_is_zero() throws Exception {
  // Metric metric = createMetric(RATING);
  // Measure measure = newMeasureBuilder().setVariation(0d).createNoValue();
  //
  // EvaluationResultAssert.assertThat(underTest.evaluate(new org.sonar.server.computation.task.projectanalysis.qualitygate.Condition(metric,
  // GREATER_THAN.getDbValue(), "4", null, true), measure)).hasLevel(OK).hasValue(0);
  // }
  //
  // private static org.sonar.server.computation.task.projectanalysis.qualitygate.Condition createErrorCondition(Metric metric,
  // org.sonar.server.computation.task.projectanalysis.qualitygate.Condition.Operator operator, String errorThreshold) {
  // return new Condition(metric, operator.getDbValue(), errorThreshold, null, false);
  // }
  //
  // private static MetricImpl createMetric(Metric.MetricType metricType) {
  // return new MetricImpl(1, "key", "name", metricType);
  // }
}
