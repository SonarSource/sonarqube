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
package org.sonar.server.computation.task.projectanalysis.qualitygate;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.computation.task.projectanalysis.metric.Metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConditionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final Metric SOME_METRIC = mock(Metric.class);
  private static final String SOME_OPERATOR = "EQ";

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_for_null_metric_argument() {
    new Condition(null, SOME_OPERATOR, null, null, false);
  }

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_for_null_operator_argument() {
    new Condition(SOME_METRIC, null, null, null, false);
  }

  @Test
  public void constructor_throws_IAE_if_operator_is_not_valid() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported operator value: 'troloto'");

    new Condition(SOME_METRIC, "troloto", null, null, false);
  }

  @Test
  public void verify_getters() {
    String error = "error threshold";
    String warning = "warning threshold";

    Condition condition = new Condition(SOME_METRIC, SOME_OPERATOR, error, warning, true);

    assertThat(condition.getMetric()).isSameAs(SOME_METRIC);
    assertThat(condition.getOperator()).isSameAs(Condition.Operator.EQUALS);
    assertThat(condition.hasPeriod()).isTrue();
    assertThat(condition.getErrorThreshold()).isEqualTo(error);
    assertThat(condition.getWarningThreshold()).isEqualTo(warning);
  }

  @Test
  public void all_fields_are_displayed_in_toString() {
    when(SOME_METRIC.toString()).thenReturn("metric1");

    assertThat(new Condition(SOME_METRIC, SOME_OPERATOR, "error_l", "warn", true).toString())
      .isEqualTo("Condition{metric=metric1, hasPeriod=true, operator=EQUALS, warningThreshold=warn, errorThreshold=error_l}");

  }

}
