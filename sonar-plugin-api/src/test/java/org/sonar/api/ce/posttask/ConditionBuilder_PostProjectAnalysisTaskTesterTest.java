/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.ce.posttask;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConditionBuilder_PostProjectAnalysisTaskTesterTest {
  private static final String SOME_METRIC_KEY = "some metric key";
  private static final QualityGate.Operator SOME_OPERATOR = QualityGate.Operator.GREATER_THAN;
  private static final String SOME_ERROR_THRESHOLD = "some error threshold";
  private static final QualityGate.EvaluationStatus SOME_STATUS_BUT_NO_VALUE = QualityGate.EvaluationStatus.OK;
  private static final String SOME_VALUE = "some value";

  private PostProjectAnalysisTaskTester.ConditionBuilder underTest = PostProjectAnalysisTaskTester.newConditionBuilder();

  @Test
  public void setMetricKey_throws_NPE_if_operator_is_null() {
    assertThatThrownBy(() -> underTest.setMetricKey(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("metricKey cannot be null");
  }

  @Test
  public void setOperator_throws_NPE_if_operator_is_null() {
    assertThatThrownBy(() -> underTest.setOperator(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("operator cannot be null");
  }

  @Test
  public void buildNoValue_throws_NPE_if_metricKey_is_null() {
    underTest.setOperator(SOME_OPERATOR).setErrorThreshold(SOME_ERROR_THRESHOLD);

    assertThatThrownBy(() -> underTest.buildNoValue())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("metricKey cannot be null");
  }

  @Test
  public void buildNoValue_throws_NPE_if_operator_is_null() {
    underTest.setMetricKey(SOME_METRIC_KEY).setErrorThreshold(SOME_ERROR_THRESHOLD);

    assertThatThrownBy(() -> underTest.buildNoValue())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("operator cannot be null");
  }

  @Test
  public void buildNoValue_throws_NPE_if_errorThreshold_is_null() {
    underTest.setMetricKey(SOME_METRIC_KEY).setOperator(SOME_OPERATOR);

    assertThatThrownBy(() -> underTest.buildNoValue())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("errorThreshold cannot be null");
  }

  @Test
  public void buildNoValue_returns_Condition_which_getStatus_method_returns_NO_VALUE() {
    initValidBuilder();

    assertThat(underTest.buildNoValue().getStatus()).isEqualTo(QualityGate.EvaluationStatus.NO_VALUE);
  }

  @Test
  public void buildNoValue_returns_Condition_which_getValue_method_throws_ISE() {
    initValidBuilder();
    QualityGate.Condition condition = underTest.buildNoValue();

    assertThatThrownBy(() -> condition.getValue())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("There is no value when status is NO_VALUE");
  }

  @Test
  public void buildNoValue_returns_new_instance_at_each_call() {
    initValidBuilder();

    assertThat(underTest.buildNoValue()).isNotSameAs(underTest.buildNoValue());
  }

  @Test
  public void buildNoValue_has_no_value_in_toString_and_status_is_NO_VALUE() {
    initValidBuilder();

    assertThat(underTest.buildNoValue().toString())
      .isEqualTo(
        "Condition{status=NO_VALUE, metricKey='some metric key', operator=GREATER_THAN, " +
          "errorThreshold='some error threshold'}");
  }

  @Test
  public void verify_getters_of_object_returned_by_buildNoValue() {
    initValidBuilder().setOnLeakPeriod(true);

    QualityGate.Condition condition = underTest.buildNoValue();

    assertThat(condition.getMetricKey()).isEqualTo(SOME_METRIC_KEY);
    assertThat(condition.getOperator()).isEqualTo(SOME_OPERATOR);
    assertThat(condition.getErrorThreshold()).isEqualTo(SOME_ERROR_THRESHOLD);
  }

  @Test
  public void build_throws_NPE_if_status_is_null() {
    initValidBuilder();

    assertThatThrownBy(() -> underTest.build(null, SOME_VALUE))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("status cannot be null");
  }

  @Test
  public void build_throws_IAE_if_status_is_NO_VALUE() {
    initValidBuilder();

    assertThatThrownBy(() -> underTest.build(QualityGate.EvaluationStatus.NO_VALUE, SOME_VALUE))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("status cannot be NO_VALUE, use method buildNoValue() instead");
  }

  @Test
  public void build_throws_NPE_if_value_is_null() {
    initValidBuilder();

    assertThatThrownBy(() -> underTest.build(SOME_STATUS_BUT_NO_VALUE, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("value cannot be null, use method buildNoValue() instead");
  }

  @Test
  public void build_throws_NPE_if_metricKey_is_null() {
    underTest.setOperator(SOME_OPERATOR).setErrorThreshold(SOME_ERROR_THRESHOLD);

    assertThatThrownBy(() -> underTest.build(SOME_STATUS_BUT_NO_VALUE, SOME_VALUE))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("metricKey cannot be null");
  }

  @Test
  public void build_throws_NPE_if_operator_is_null() {
    underTest.setMetricKey(SOME_METRIC_KEY).setErrorThreshold(SOME_ERROR_THRESHOLD);

    assertThatThrownBy(() -> underTest.build(SOME_STATUS_BUT_NO_VALUE, SOME_VALUE))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("operator cannot be null");
  }

  @Test
  public void build_throws_NPE_if_errorThreshold_is_null() {
    underTest.setMetricKey(SOME_METRIC_KEY).setOperator(SOME_OPERATOR);

    assertThatThrownBy(() -> underTest.build(SOME_STATUS_BUT_NO_VALUE, SOME_VALUE))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("errorThreshold cannot be null");
  }

  @Test
  public void build_returns_new_instance_at_each_call() {
    initValidBuilder();

    assertThat(underTest.build(SOME_STATUS_BUT_NO_VALUE, SOME_VALUE)).isNotSameAs(underTest.build(SOME_STATUS_BUT_NO_VALUE, SOME_VALUE));
  }

  @Test
  public void build_has_value_in_toString_and_specified_status() {
    initValidBuilder();

    assertThat(underTest.build(SOME_STATUS_BUT_NO_VALUE, SOME_VALUE).toString())
      .isEqualTo(
        "Condition{status=OK, metricKey='some metric key', operator=GREATER_THAN, "
          + "errorThreshold='some error threshold', value='some value'}");
  }

  @Test
  public void build_returns_Condition_which_getStatus_method_returns_NO_VALUE() {
    initValidBuilder();

    assertThat(underTest.buildNoValue().getStatus()).isEqualTo(QualityGate.EvaluationStatus.NO_VALUE);
  }

  @Test
  public void build_returns_Condition_which_getValue_method_throws_ISE() {
    initValidBuilder();
    QualityGate.Condition condition = underTest.buildNoValue();

    assertThatThrownBy(() -> condition.getValue())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("There is no value when status is NO_VALUE");
  }

  @Test
  public void verify_getters_of_object_returned_by_build() {
    initValidBuilder().setOnLeakPeriod(true);

    QualityGate.Condition condition = underTest.build(SOME_STATUS_BUT_NO_VALUE, SOME_VALUE);

    assertThat(condition.getStatus()).isEqualTo(SOME_STATUS_BUT_NO_VALUE);
    assertThat(condition.getMetricKey()).isEqualTo(SOME_METRIC_KEY);
    assertThat(condition.getOperator()).isEqualTo(SOME_OPERATOR);
    assertThat(condition.getErrorThreshold()).isEqualTo(SOME_ERROR_THRESHOLD);
    assertThat(condition.getValue()).isEqualTo(SOME_VALUE);
  }

  private PostProjectAnalysisTaskTester.ConditionBuilder initValidBuilder() {
    underTest.setMetricKey(SOME_METRIC_KEY).setOperator(SOME_OPERATOR).setErrorThreshold(SOME_ERROR_THRESHOLD);
    return underTest;
  }
}
