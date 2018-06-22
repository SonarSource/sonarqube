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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.qualitygate.Condition.Operator.EQUALS;
import static org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus.OK;
import static org.sonar.server.qualitygate.EvaluatedCondition.EvaluationStatus.WARN;

public class EvaluatedConditionTest {
  private static final Condition CONDITION_1 = new Condition("metricKey", EQUALS, "2", "4", false);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private EvaluatedCondition underTest = new EvaluatedCondition(CONDITION_1, WARN, "value");

  @Test
  public void constructor_throws_NPE_if_condition_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("condition can't be null");

    new EvaluatedCondition(null, WARN, "value");
  }

  @Test
  public void constructor_throws_NPE_if_EvaluationStatus_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    new EvaluatedCondition(CONDITION_1, null, "value");
  }

  @Test
  public void constructor_accepts_null_value() {
    EvaluatedCondition underTest = new EvaluatedCondition(CONDITION_1, WARN, null);

    assertThat(underTest.getValue()).isEmpty();
  }

  @Test
  public void verify_getters() {
    EvaluatedCondition underTest = new EvaluatedCondition(CONDITION_1, WARN, "value");

    assertThat(underTest.getCondition()).isEqualTo(CONDITION_1);
    assertThat(underTest.getStatus()).isEqualTo(WARN);
    assertThat(underTest.getValue()).contains("value");
  }

  @Test
  public void override_toString() {
    assertThat(underTest.toString()).isEqualTo("EvaluatedCondition{condition=" +
      "Condition{metricKey='metricKey', operator=EQUALS, warningThreshold='4', errorThreshold='2', onLeakPeriod=false}, " +
      "status=WARN, value='value'}");
  }

  @Test
  public void toString_does_not_quote_null_value() {
    EvaluatedCondition underTest = new EvaluatedCondition(CONDITION_1, WARN, null);

    assertThat(underTest.toString()).isEqualTo("EvaluatedCondition{condition=" +
      "Condition{metricKey='metricKey', operator=EQUALS, warningThreshold='4', errorThreshold='2', onLeakPeriod=false}, " +
      "status=WARN, value=null}");
  }

  @Test
  public void equals_is_based_on_all_fields() {
    assertThat(underTest).isEqualTo(underTest);
    assertThat(underTest).isEqualTo(new EvaluatedCondition(CONDITION_1, WARN, "value"));
    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new Object());
    assertThat(underTest).isNotEqualTo(new EvaluatedCondition(new Condition("other_metric", EQUALS, "a", "b", true), WARN, "value"));
    assertThat(underTest).isNotEqualTo(new EvaluatedCondition(CONDITION_1, OK, "value"));
    assertThat(underTest).isNotEqualTo(new EvaluatedCondition(CONDITION_1, WARN, null));
    assertThat(underTest).isNotEqualTo(new EvaluatedCondition(CONDITION_1, WARN, "other_value"));
  }

  @Test
  public void hashcode_is_based_on_all_fields() {
    assertThat(underTest.hashCode()).isEqualTo(underTest.hashCode());
    assertThat(underTest.hashCode()).isEqualTo(new EvaluatedCondition(CONDITION_1, WARN, "value").hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(null);
    assertThat(underTest.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new EvaluatedCondition(new Condition("other_metric", EQUALS, "a", "b", true), WARN, "value").hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new EvaluatedCondition(CONDITION_1, OK, "value").hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new EvaluatedCondition(CONDITION_1, WARN, null).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new EvaluatedCondition(CONDITION_1, WARN, "other_value").hashCode());
  }
}
