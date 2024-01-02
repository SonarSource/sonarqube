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

import org.junit.Before;
import org.junit.Test;
import org.sonar.ce.task.projectanalysis.metric.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConditionTest {


  private static final Metric SOME_METRIC = mock(Metric.class);
  private static final String SOME_OPERATOR = "LT";

  @Before
  public void setUp() {
    when(SOME_METRIC.getKey()).thenReturn("dummy key");
  }

  @Test
  public void constructor_throws_NPE_for_null_metric_argument() {
    assertThatThrownBy(() -> new Condition(null, SOME_OPERATOR, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void constructor_throws_NPE_for_null_operator_argument() {
    assertThatThrownBy(() -> new Condition(SOME_METRIC, null, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void constructor_throws_IAE_if_operator_is_not_valid() {
    assertThatThrownBy(() ->  new Condition(SOME_METRIC, "troloto", null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Unsupported operator value: 'troloto'");
  }

  @Test
  public void verify_getters() {
    String error = "error threshold";

    Condition condition = new Condition(SOME_METRIC, SOME_OPERATOR, error);

    assertThat(condition.getMetric()).isSameAs(SOME_METRIC);
    assertThat(condition.getOperator()).isSameAs(Condition.Operator.LESS_THAN);
    assertThat(condition.getErrorThreshold()).isEqualTo(error);
  }

  @Test
  public void all_fields_are_displayed_in_toString() {
    when(SOME_METRIC.toString()).thenReturn("metric1");

    assertThat(new Condition(SOME_METRIC, SOME_OPERATOR, "error_l"))
      .hasToString("Condition{metric=metric1, operator=LESS_THAN, errorThreshold=error_l}");

  }

}
