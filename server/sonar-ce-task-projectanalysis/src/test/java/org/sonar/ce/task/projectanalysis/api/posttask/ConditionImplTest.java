/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.ce.posttask.QualityGate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(DataProviderRunner.class)
public class ConditionImplTest {
  private static final String METRIC_KEY = "metricKey";
  private static final String ERROR_THRESHOLD = "error threshold";
  private static final String VALUE = "value";


  private ConditionImpl.Builder builder = ConditionImpl.newBuilder()
    .setStatus(QualityGate.EvaluationStatus.OK)
    .setMetricKey(METRIC_KEY)
    .setOperator(QualityGate.Operator.GREATER_THAN)
    .setErrorThreshold(ERROR_THRESHOLD)
    .setValue(VALUE);

  @Test
  public void build_throws_NPE_if_status_is_null() {
    builder.setStatus(null);

    assertThatThrownBy(() -> builder.build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("status can not be null");
  }

  @Test
  public void build_throws_NPE_if_metricKey_is_null() {
    builder.setMetricKey(null);

    assertThatThrownBy(() -> builder.build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("metricKey can not be null");
  }

  @Test
  public void build_throws_NPE_if_operator_is_null() {
    builder.setOperator(null);

    assertThatThrownBy(() -> builder.build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("operator can not be null");
  }

  @Test
  public void build_throws_NPE_if_error_threshold_is_null() {
    builder.setErrorThreshold(null);

    assertThatThrownBy(() -> builder.build())
      .isInstanceOf(NullPointerException.class)
      .hasMessage("errorThreshold can not be null");
  }

  @Test
  public void getValue_throws_ISE_when_condition_type_is_NO_VALUE() {
    builder.setStatus(QualityGate.EvaluationStatus.NO_VALUE).setValue(null);
    ConditionImpl condition = builder.build();

    assertThatThrownBy(condition::getValue)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("There is no value when status is NO_VALUE");
  }

  @DataProvider
  public static Object[][] allStatusesButNO_VALUE() {
    Object[][] res = new Object[QualityGate.EvaluationStatus.values().length - 1][1];
    int i = 0;
    for (QualityGate.EvaluationStatus status : QualityGate.EvaluationStatus.values()) {
      if (status != QualityGate.EvaluationStatus.NO_VALUE) {
        res[i][0] = status;
        i++;
      }
    }
    return res;
  }

  @Test
  @UseDataProvider("allStatusesButNO_VALUE")
  public void build_throws_IAE_if_value_is_null_but_status_is_not_NO_VALUE(QualityGate.EvaluationStatus status) {
    builder.setStatus(status)
      .setValue(null);

    assertThatThrownBy(() -> builder.build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("value can not be null when status is not NO_VALUE");
  }

  @Test
  public void build_throws_IAE_if_value_is_not_null_but_status_is_NO_VALUE() {
    builder.setStatus(QualityGate.EvaluationStatus.NO_VALUE);

    assertThatThrownBy(() -> builder.build())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("value must be null when status is NO_VALUE");
  }

  @Test
  public void toString_ConditionImpl_of_type_different_from_NO_VALUE() {
    assertThat(builder.build())
      .hasToString(
        "ConditionImpl{status=OK, metricKey='metricKey', operator=GREATER_THAN, errorThreshold='error threshold', value='value'}");
  }

  @Test
  public void toString_ConditionImpl_of_type_NO_VALUE() {
    builder.setStatus(QualityGate.EvaluationStatus.NO_VALUE)
      .setValue(null);

    assertThat(builder.build())
      .hasToString(
        "ConditionImpl{status=NO_VALUE, metricKey='metricKey', operator=GREATER_THAN, errorThreshold='error threshold', value='null'}");
  }

  @Test
  public void verify_getters() {
    ConditionImpl underTest = builder.build();

    assertThat(underTest.getStatus()).isEqualTo(QualityGate.EvaluationStatus.OK);
    assertThat(underTest.getMetricKey()).isEqualTo(METRIC_KEY);
    assertThat(underTest.getOperator()).isEqualTo(QualityGate.Operator.GREATER_THAN);
    assertThat(underTest.getErrorThreshold()).isEqualTo(ERROR_THRESHOLD);
    assertThat(underTest.getValue()).isEqualTo(VALUE);
  }
}
