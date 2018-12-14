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
package org.sonar.server.qualitygate;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import java.util.OptionalDouble;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.measures.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.server.qualitygate.ConditionEvaluatorTest.FakeMeasure.newFakeMeasureOnLeak;

@RunWith(DataProviderRunner.class)
public class ConditionEvaluatorTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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

    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.LESS_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    testOnLeak(newFakeMeasureOnLeak(10), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");
  }

  @Test
  public void evaluate_throws_IAE_if_fail_to_parse_threshold() {

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Quality Gate: unable to parse threshold '9bar' to compare against foo");

    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "9bar", EvaluatedCondition.EvaluationStatus.ERROR, "10da");
  }

  @Test
  public void no_value_present() {
    test(new FakeMeasure((Integer) null), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, null);
    test(null, Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, null);
  }


  @Test
  @UseDataProvider("unsupportedMetricTypes")
  public void fail_when_condition_is_on_unsupported_metric(Metric.ValueType metricType) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(String.format("Condition is not allowed for type %s", metricType));

    test(new FakeMeasure(metricType), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
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

  private void test(@Nullable QualityGateEvaluator.Measure measure, Condition.Operator operator, String errorThreshold, EvaluatedCondition.EvaluationStatus expectedStatus, @Nullable String expectedValue) {
    Condition condition = new Condition("foo", operator, errorThreshold);

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
    Condition condition = new Condition("new_foo", operator, errorThreshold);

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

    FakeMeasure(Metric.ValueType valueType) {
      this.valueType = valueType;
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
    public OptionalDouble getNewMetricValue() {
      return leakValue == null ? OptionalDouble.empty() : OptionalDouble.of(leakValue);
    }
  }

}
