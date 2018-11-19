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
package org.sonar.server.webhook;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final Random random = new Random();
  private boolean onLeak = random.nextBoolean();
  private QualityGate.EvaluationStatus randomEvaluationStatus = QualityGate.EvaluationStatus.values()[random.nextInt(QualityGate.EvaluationStatus.values().length)];
  private QualityGate.Condition condition = new QualityGate.Condition(
    randomEvaluationStatus, "k", QualityGate.Operator.GREATER_THAN, "l", "m", onLeak, "val");
  private QualityGate.Status randomStatus = QualityGate.Status.values()[random.nextInt(QualityGate.Status.values().length)];
  private QualityGate underTest = new QualityGate("i", "j", randomStatus, singleton(condition));

  @Test
  public void constructor_throws_NPE_if_id_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("id can't be null");

    new QualityGate(null, "j", QualityGate.Status.WARN, singleton(condition));
  }

  @Test
  public void constructor_throws_NPE_if_name_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    new QualityGate("i", null, QualityGate.Status.WARN, singleton(condition));
  }

  @Test
  public void constructor_throws_NPE_if_status_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    new QualityGate("i", "j", null, singleton(condition));
  }

  @Test
  public void constructor_throws_NPE_if_conditions_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("conditions can't be null");

    new QualityGate("i", "j", QualityGate.Status.WARN, null);
  }

  @Test
  public void condition_constructor_throws_NPE_if_evaluation_status_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    new QualityGate.Condition(null, "k", QualityGate.Operator.GREATER_THAN, "l", "m", onLeak, "val");
  }

  @Test
  public void condition_constructor_throws_NPE_if_metricKey_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("metricKey can't be null");

    new QualityGate.Condition(randomEvaluationStatus, null, QualityGate.Operator.GREATER_THAN, "l", "m", onLeak, "val");
  }

  @Test
  public void condition_constructor_throws_NPE_if_operator_status_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("operator can't be null");

    new QualityGate.Condition(randomEvaluationStatus, "k", null, "l", "m", onLeak, "val");
  }

  @Test
  public void verify_getters() {
    assertThat(underTest.getId()).isEqualTo("i");
    assertThat(underTest.getName()).isEqualTo("j");
    assertThat(underTest.getStatus()).isEqualTo(randomStatus);
    assertThat(underTest.getConditions()).isEqualTo(singleton(condition));
  }

  @Test
  public void verify_condition_getters() {
    assertThat(condition.getStatus()).isEqualTo(randomEvaluationStatus);
    assertThat(condition.getMetricKey()).isEqualTo("k");
    assertThat(condition.getErrorThreshold()).contains("l");
    assertThat(condition.getWarningThreshold()).contains("m");
    assertThat(condition.isOnLeakPeriod()).isEqualTo(onLeak);
    assertThat(condition.getValue()).contains("val");

    QualityGate.Condition conditionWithNulls = new QualityGate.Condition(
        randomEvaluationStatus, "k", QualityGate.Operator.GREATER_THAN, null, null, onLeak, null);

    assertThat(conditionWithNulls.getErrorThreshold()).isEmpty();
    assertThat(conditionWithNulls.getWarningThreshold()).isEmpty();
    assertThat(conditionWithNulls.getValue()).isEmpty();
  }
}
