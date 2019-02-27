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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric.Level;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.qualitygate.EvaluatedQualityGate.newBuilder;

public class EvaluatedQualityGateTest {
  private static final String QUALITY_GATE_ID = "qg_id";
  private static final String QUALITY_GATE_NAME = "qg_name";
  private static final QualityGate NO_CONDITION_QUALITY_GATE = new QualityGate(QUALITY_GATE_ID, QUALITY_GATE_NAME, emptySet());
  private static final Condition CONDITION_1 = new Condition("metric_key_1", Condition.Operator.LESS_THAN, "2");
  private static final Condition CONDITION_2 = new Condition("a_metric", Condition.Operator.GREATER_THAN, "6");
  private static final Condition CONDITION_3 = new Condition(CoreMetrics.NEW_MAINTAINABILITY_RATING_KEY, Condition.Operator.GREATER_THAN, "6");

  private static final QualityGate ONE_CONDITION_QUALITY_GATE = new QualityGate(QUALITY_GATE_ID, QUALITY_GATE_NAME, singleton(CONDITION_1));
  private static final QualityGate ALL_CONDITIONS_QUALITY_GATE = new QualityGate(QUALITY_GATE_ID, QUALITY_GATE_NAME,
    new HashSet<>(Arrays.asList(CONDITION_1, CONDITION_2, CONDITION_3)));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final Random random = new Random();
  private final Level randomStatus = Level.values()[random.nextInt(Level.values().length)];
  private final EvaluatedCondition.EvaluationStatus randomEvaluationStatus = EvaluatedCondition.EvaluationStatus.values()[random
    .nextInt(EvaluatedCondition.EvaluationStatus.values().length)];
  private final String randomValue = random.nextBoolean() ? null : RandomStringUtils.randomAlphanumeric(3);

  private EvaluatedQualityGate.Builder builder = newBuilder();

  @Test
  public void build_fails_with_NPE_if_status_not_set() {
    builder.setQualityGate(NO_CONDITION_QUALITY_GATE);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    builder.build();
  }

  @Test
  public void addCondition_fails_with_NPE_if_condition_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("condition can't be null");

    builder.addEvaluatedCondition(null, EvaluatedCondition.EvaluationStatus.ERROR, "a_value");
  }

  @Test
  public void addCondition_fails_with_NPE_if_status_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("status can't be null");

    builder.addEvaluatedCondition(new Condition("metric_key", Condition.Operator.LESS_THAN, "2"), null, "a_value");
  }

  @Test
  public void addCondition_accepts_null_value() {
    builder.addEvaluatedCondition(CONDITION_1, EvaluatedCondition.EvaluationStatus.NO_VALUE, null);

    assertThat(builder.getEvaluatedConditions())
      .containsOnly(new EvaluatedCondition(CONDITION_1, EvaluatedCondition.EvaluationStatus.NO_VALUE, null));
  }

  @Test
  public void getEvaluatedConditions_returns_empty_with_no_condition_added_to_builder() {
    assertThat(builder.getEvaluatedConditions()).isEmpty();
  }

  @Test
  public void build_fails_with_IAE_if_condition_added_and_no_on_QualityGate() {
    builder.setQualityGate(NO_CONDITION_QUALITY_GATE)
      .setStatus(randomStatus)
      .addEvaluatedCondition(CONDITION_1, randomEvaluationStatus, randomValue);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Evaluation provided for unknown conditions: [" + CONDITION_1 + "]");

    builder.build();
  }

  @Test
  public void build_fails_with_IAE_if_condition_is_missing_for_one_defined_in_QualityGate() {
    builder.setQualityGate(ONE_CONDITION_QUALITY_GATE)
      .setStatus(randomStatus);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Evaluation missing for the following conditions: [" + CONDITION_1 + "]");

    builder.build();
  }

  @Test
  public void getEvaluatedConditions_is_sorted() {
    EvaluatedQualityGate underTest = builder
      .setQualityGate(ALL_CONDITIONS_QUALITY_GATE)
      .setStatus(randomStatus)
      .addEvaluatedCondition(CONDITION_1, randomEvaluationStatus, randomValue)
      .addEvaluatedCondition(CONDITION_2, randomEvaluationStatus, randomValue)
      .addEvaluatedCondition(CONDITION_3, randomEvaluationStatus, randomValue)
      .build();

    assertThat(underTest.getQualityGate()).isEqualTo(ALL_CONDITIONS_QUALITY_GATE);
    assertThat(underTest.getStatus()).isEqualTo(randomStatus);
    assertThat(underTest.getEvaluatedConditions()).extracting(c -> c.getCondition().getMetricKey())
      .contains(CONDITION_3.getMetricKey(), CONDITION_2.getMetricKey(), CONDITION_1.getMetricKey());
  }

  @Test
  public void verify_getters() {
    EvaluatedQualityGate underTest = builder
      .setQualityGate(ONE_CONDITION_QUALITY_GATE)
      .setStatus(randomStatus)
      .addEvaluatedCondition(CONDITION_1, randomEvaluationStatus, randomValue)
      .build();

    assertThat(underTest.getQualityGate()).isEqualTo(ONE_CONDITION_QUALITY_GATE);
    assertThat(underTest.getStatus()).isEqualTo(randomStatus);
    assertThat(underTest.getEvaluatedConditions())
      .containsOnly(new EvaluatedCondition(CONDITION_1, randomEvaluationStatus, randomValue));
  }

  @Test
  public void verify_getters_when_no_condition() {
    EvaluatedQualityGate underTest = builder
      .setQualityGate(NO_CONDITION_QUALITY_GATE)
      .setStatus(randomStatus)
      .build();

    assertThat(underTest.getQualityGate()).isEqualTo(NO_CONDITION_QUALITY_GATE);
    assertThat(underTest.getStatus()).isEqualTo(randomStatus);
    assertThat(underTest.getEvaluatedConditions()).isEmpty();
  }

  @Test
  public void verify_getters_when_multiple_conditions() {
    QualityGate qualityGate = new QualityGate(QUALITY_GATE_ID, QUALITY_GATE_NAME, ImmutableSet.of(CONDITION_1, CONDITION_2));
    EvaluatedQualityGate underTest = builder
      .setQualityGate(qualityGate)
      .setStatus(randomStatus)
      .addEvaluatedCondition(CONDITION_1, randomEvaluationStatus, randomValue)
      .addEvaluatedCondition(CONDITION_2, EvaluatedCondition.EvaluationStatus.ERROR, "bad")
      .build();

    assertThat(underTest.getQualityGate()).isEqualTo(qualityGate);
    assertThat(underTest.getStatus()).isEqualTo(randomStatus);
    assertThat(underTest.getEvaluatedConditions()).containsOnly(
      new EvaluatedCondition(CONDITION_1, randomEvaluationStatus, randomValue),
      new EvaluatedCondition(CONDITION_2, EvaluatedCondition.EvaluationStatus.ERROR, "bad"));
  }

  @Test
  public void equals_is_based_on_all_fields() {
    EvaluatedQualityGate.Builder builder = this.builder
      .setQualityGate(ONE_CONDITION_QUALITY_GATE)
      .setStatus(Level.ERROR)
      .addEvaluatedCondition(CONDITION_1, EvaluatedCondition.EvaluationStatus.ERROR, "foo");

    EvaluatedQualityGate underTest = builder.build();
    assertThat(underTest).isEqualTo(builder.build());
    assertThat(underTest).isNotSameAs(builder.build());
    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new Object());
    assertThat(underTest).isNotEqualTo(builder.setQualityGate(new QualityGate("other_id", QUALITY_GATE_NAME, singleton(CONDITION_1))).build());
    assertThat(underTest).isNotEqualTo(builder.setQualityGate(ONE_CONDITION_QUALITY_GATE).setStatus(Level.OK).build());
    assertThat(underTest).isNotEqualTo(newBuilder()
      .setQualityGate(ONE_CONDITION_QUALITY_GATE)
      .setStatus(Level.ERROR)
      .addEvaluatedCondition(CONDITION_1, EvaluatedCondition.EvaluationStatus.OK, "foo")
      .build());
  }

  @Test
  public void hashcode_is_based_on_all_fields() {
    EvaluatedQualityGate.Builder builder = this.builder
      .setQualityGate(ONE_CONDITION_QUALITY_GATE)
      .setStatus(Level.ERROR)
      .addEvaluatedCondition(CONDITION_1, EvaluatedCondition.EvaluationStatus.ERROR, "foo");

    EvaluatedQualityGate underTest = builder.build();
    assertThat(underTest.hashCode()).isEqualTo(builder.build().hashCode());
    assertThat(underTest.hashCode()).isNotSameAs(builder.build().hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(null);
    assertThat(underTest.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(builder.setQualityGate(new QualityGate("other_id", QUALITY_GATE_NAME, singleton(CONDITION_1))).build().hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(builder.setQualityGate(ONE_CONDITION_QUALITY_GATE).setStatus(Level.OK).build().hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(newBuilder()
      .setQualityGate(ONE_CONDITION_QUALITY_GATE)
      .setStatus(Level.ERROR)
      .addEvaluatedCondition(CONDITION_1, EvaluatedCondition.EvaluationStatus.OK, "foo")
      .build().hashCode());
  }
}
