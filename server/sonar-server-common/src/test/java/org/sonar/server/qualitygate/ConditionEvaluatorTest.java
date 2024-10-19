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
package org.sonar.server.qualitygate;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.measures.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.STRING;

@RunWith(DataProviderRunner.class)
public class ConditionEvaluatorTest {

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

    test(new FakeMeasure(10), Condition.Operator.GREATER_THAN, "9", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(10), Condition.Operator.GREATER_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.GREATER_THAN, "11", EvaluatedCondition.EvaluationStatus.OK, "10");
  }

  @Test
  public void LESS_THAN_int() {
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");

    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");
  }

  @Test
  public void GREATER_THAN_long() {
    test(new FakeMeasure(10L), Condition.Operator.GREATER_THAN, "9", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(10L), Condition.Operator.GREATER_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10L), Condition.Operator.GREATER_THAN, "11", EvaluatedCondition.EvaluationStatus.OK, "10");

    test(new FakeMeasure(10L), Condition.Operator.GREATER_THAN, "9", EvaluatedCondition.EvaluationStatus.ERROR, "10");
    test(new FakeMeasure(10L), Condition.Operator.GREATER_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10L), Condition.Operator.GREATER_THAN, "11", EvaluatedCondition.EvaluationStatus.OK, "10");
  }

  @Test
  public void LESS_THAN_long() {
    test(new FakeMeasure(10L), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10L), Condition.Operator.LESS_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10L), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");

    test(new FakeMeasure(10L), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10L), Condition.Operator.LESS_THAN, "10", EvaluatedCondition.EvaluationStatus.OK, "10");
    test(new FakeMeasure(10L), Condition.Operator.LESS_THAN, "11", EvaluatedCondition.EvaluationStatus.ERROR, "10");
  }

  @Test
  public void evaluate_throws_IAE_if_fail_to_parse_threshold() {
    assertThatThrownBy(() -> test(new FakeMeasure(10), Condition.Operator.LESS_THAN, "9bar", EvaluatedCondition.EvaluationStatus.ERROR, "10da"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Quality Gate: unable to parse threshold '9bar' to compare against foo");
  }

  @Test
  public void no_value_present() {
    test(new FakeMeasure((Integer) null), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, null);
    test(null, Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, null);
  }

  @Test
  @UseDataProvider("unsupportedMetricTypes")
  public void fail_when_condition_is_on_unsupported_metric(Metric.ValueType metricType) {
    assertThatThrownBy(() -> test(new FakeMeasure(metricType), Condition.Operator.LESS_THAN, "9", EvaluatedCondition.EvaluationStatus.OK, "10"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Condition is not allowed for type %s", metricType));
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

  private void test(@Nullable QualityGateEvaluator.Measure measure, Condition.Operator operator, String errorThreshold, EvaluatedCondition.EvaluationStatus expectedStatus,
    @Nullable String expectedValue) {
    Condition condition = new Condition("foo", operator, errorThreshold);

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
}
