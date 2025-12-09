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
package org.sonar.server.qualitygate;

import java.util.Arrays;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConditionTest {
  private static final String METRIC_KEY = "metric_key";
  private static final Condition.Operator OPERATOR = Condition.Operator.GREATER_THAN;
  private static final String ERROR_THRESHOLD = "2";

  private Condition underTest = new Condition(METRIC_KEY, OPERATOR, ERROR_THRESHOLD);

  @Test
  public void constructor_throws_NPE_if_metricKey_is_null() {
    assertThatThrownBy(() -> new Condition(null, OPERATOR, ERROR_THRESHOLD))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("metricKey can't be null");
  }

  @Test
  public void constructor_throws_NPE_if_operator_operator_is_null() {
    assertThatThrownBy(() -> new Condition(METRIC_KEY, null, ERROR_THRESHOLD))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("operator can't be null");
  }

  @Test
  public void constructor_throws_NPE_if_errorThreshold_is_null() {
    assertThatThrownBy(() -> new Condition(METRIC_KEY, OPERATOR, null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("errorThreshold can't be null");
  }

  @Test
  public void verify_getters() {
    assertThat(underTest.getMetricKey()).isEqualTo(METRIC_KEY);
    assertThat(underTest.getOperator()).isEqualTo(OPERATOR);
    assertThat(underTest.getErrorThreshold()).contains(ERROR_THRESHOLD);
  }

  @Test
  public void toString_is_override() {
    assertThat(underTest.toString())
      .isEqualTo("Condition{metricKey='metric_key', operator=GREATER_THAN, errorThreshold='2'}");
  }

  @Test
  public void equals_is_based_on_all_fields() {
    assertThat(underTest)
      .isEqualTo(underTest)
      .isNotNull()
      .isNotEqualTo(new Object())
      .isEqualTo(new Condition(METRIC_KEY, OPERATOR, ERROR_THRESHOLD))
      .isNotEqualTo(new Condition("other_metric_key", OPERATOR, ERROR_THRESHOLD));
    Arrays.stream(Condition.Operator.values())
      .filter(s -> !OPERATOR.equals(s))
      .forEach(otherOperator -> assertThat(underTest)
        .isNotEqualTo(new Condition(METRIC_KEY, otherOperator, ERROR_THRESHOLD)));
    assertThat(underTest).isNotEqualTo(new Condition(METRIC_KEY, OPERATOR, "other_error_threshold"));
  }

  @Test
  public void hashcode_is_based_on_all_fields() {
    assertThat(underTest).hasSameHashCodeAs(underTest);
    assertThat(underTest.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(underTest).hasSameHashCodeAs(new Condition(METRIC_KEY, OPERATOR, ERROR_THRESHOLD));
    assertThat(underTest.hashCode()).isNotEqualTo(new Condition("other_metric_key", OPERATOR, ERROR_THRESHOLD).hashCode());
    Arrays.stream(Condition.Operator.values())
      .filter(s -> !OPERATOR.equals(s))
      .forEach(otherOperator -> assertThat(underTest.hashCode())
        .isNotEqualTo(new Condition(METRIC_KEY, otherOperator, ERROR_THRESHOLD).hashCode()));
    assertThat(underTest.hashCode()).isNotEqualTo(new Condition(METRIC_KEY, OPERATOR, "other_error_threshold").hashCode());
  }
}
