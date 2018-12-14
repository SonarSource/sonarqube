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
package org.sonar.ce.task.projectanalysis.measure.qualitygatedetails;

import org.junit.Test;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.qualitygate.Condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EvaluatedConditionTest {

  private static final Metric SOME_METRIC = mock(Metric.class);
  static {
    when(SOME_METRIC.getKey()).thenReturn("dummy key");
  }
  private static final Condition SOME_CONDITION = new Condition(SOME_METRIC, Condition.Operator.LESS_THAN.getDbValue(), "1");
  private static final Measure.Level SOME_LEVEL = Measure.Level.OK;
  private static final String SOME_VALUE = "some value";

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_Condition_arg_is_null() {
    new EvaluatedCondition(null, SOME_LEVEL, SOME_VALUE);
  }

  @Test(expected = NullPointerException.class)
  public void constructor_throws_NPE_if_Level_arg_is_null() {
    new EvaluatedCondition(SOME_CONDITION, null, SOME_VALUE);
  }

  @Test
  public void getCondition_returns_object_passed_in_constructor() {
    assertThat(new EvaluatedCondition(SOME_CONDITION, SOME_LEVEL, SOME_VALUE).getCondition()).isSameAs(SOME_CONDITION);
  }

  @Test
  public void getLevel_returns_object_passed_in_constructor() {
    assertThat(new EvaluatedCondition(SOME_CONDITION, SOME_LEVEL, SOME_VALUE).getLevel()).isSameAs(SOME_LEVEL);
  }

  @Test
  public void getValue_returns_empty_string_if_null_was_passed_in_constructor() {
    assertThat(new EvaluatedCondition(SOME_CONDITION, SOME_LEVEL, null).getActualValue()).isEmpty();
  }

  @Test
  public void getValue_returns_toString_of_Object_passed_in_constructor() {
    assertThat(new EvaluatedCondition(SOME_CONDITION, SOME_LEVEL, new A()).getActualValue()).isEqualTo("A string");
  }

  private static class A {
    @Override
    public String toString() {
      return "A string";
    }
  }
}
