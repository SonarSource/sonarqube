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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.qualitygate.Condition.Operator.GREATER_THAN;
import static org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus.ERROR;
import static org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus.OK;

public class EvaluatedConditionTest {
  private static final Condition CONDITION_1 = new Condition("metricKey", GREATER_THAN, "2");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private EvaluatedCondition underTest = new EvaluatedCondition(CONDITION_1, ERROR, "value");

  @Test
  public void constructor_throws_NPE_if_condition_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("condition can't be null");

    new EvaluatedCondition(null, ERROR, "value");
  }

  @Test
  public void constructor_throws_NPE_if_EvaluationStatus_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    new EvaluatedCondition(CONDITION_1, null, "value");
  }

  @Test
  public void constructor_accepts_null_value() {
    EvaluatedCondition underTest = new EvaluatedCondition(CONDITION_1, ERROR, null);

    assertThat(underTest.getValue()).isEmpty();
  }

  @Test
  public void verify_getters() {
    EvaluatedCondition underTest = new EvaluatedCondition(CONDITION_1, ERROR, "value");

    assertThat(underTest.getCondition()).isEqualTo(CONDITION_1);
    assertThat(underTest.getStatus()).isEqualTo(ERROR);
    assertThat(underTest.getValue()).contains("value");
  }

  @Test
  public void override_toString() {
    assertThat(underTest.toString()).isEqualTo("EvaluatedCondition{condition=" +
      "Condition{metricKey='metricKey', operator=GREATER_THAN, errorThreshold='2'}, " +
      "status=ERROR, value='value'}");
  }

  @Test
  public void toString_does_not_quote_null_value() {
    EvaluatedCondition underTest = new EvaluatedCondition(CONDITION_1, ERROR, null);

    assertThat(underTest.toString()).isEqualTo("EvaluatedCondition{condition=" +
      "Condition{metricKey='metricKey', operator=GREATER_THAN, errorThreshold='2'}, " +
      "status=ERROR, value=null}");
  }

  @Test
  public void equals_is_based_on_all_fields() {
    assertThat(underTest).isEqualTo(underTest);
    assertThat(underTest).isEqualTo(new EvaluatedCondition(CONDITION_1, ERROR, "value"));
    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new Object());
    assertThat(underTest).isNotEqualTo(new EvaluatedCondition(new Condition("other_metric", GREATER_THAN, "a"), ERROR, "value"));
    assertThat(underTest).isNotEqualTo(new EvaluatedCondition(CONDITION_1, OK, "value"));
    assertThat(underTest).isNotEqualTo(new EvaluatedCondition(CONDITION_1, ERROR, null));
    assertThat(underTest).isNotEqualTo(new EvaluatedCondition(CONDITION_1, ERROR, "other_value"));
  }

  @Test
  public void hashcode_is_based_on_all_fields() {
    assertThat(underTest.hashCode()).isEqualTo(underTest.hashCode());
    assertThat(underTest.hashCode()).isEqualTo(new EvaluatedCondition(CONDITION_1, ERROR, "value").hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(null);
    assertThat(underTest.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new EvaluatedCondition(new Condition("other_metric", GREATER_THAN, "a"), ERROR, "value").hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new EvaluatedCondition(CONDITION_1, OK, "value").hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new EvaluatedCondition(CONDITION_1, ERROR, null).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new EvaluatedCondition(CONDITION_1, ERROR, "other_value").hashCode());
  }
}
