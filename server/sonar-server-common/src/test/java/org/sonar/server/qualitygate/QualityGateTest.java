/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class QualityGateTest {
  private static final String QUALIGATE_ID = "qg_id";
  private static final String QUALIGATE_NAME = "qg_name";
  private static final Condition CONDITION_1 = new Condition("m1", Condition.Operator.GREATER_THAN, "1");
  private static final Condition CONDITION_2 = new Condition("m2", Condition.Operator.LESS_THAN, "2");

  private QualityGate underTest = new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1, CONDITION_2));

  @Test
  public void constructor_fails_with_NPE_if_id_is_null() {
    assertThatThrownBy(() -> new QualityGate(null, "name", emptySet()))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("id can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_name_is_null() {
    assertThatThrownBy(() -> new QualityGate("id", null, emptySet()))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("name can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_conditions_is_null() {
    assertThatThrownBy(() -> new QualityGate("id", "name", null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("conditions can't be null");
  }

  @Test
  public void constructor_fails_with_NPE_if_conditions_contains_null() {
    Random random = new Random();

    Set<Condition> conditions = Stream.of(
      IntStream.range(0, random.nextInt(5))
        .mapToObj(i -> new Condition("m_before_" + i, Condition.Operator.GREATER_THAN, "10")),
      Stream.of((Condition) null),
      IntStream.range(0, random.nextInt(5))
        .mapToObj(i -> new Condition("m_after_" + i, Condition.Operator.GREATER_THAN, "10")))
      .flatMap(s -> s)
      .collect(Collectors.toSet());

    assertThatThrownBy(() -> new QualityGate("id", "name", conditions))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("condition can't be null");
  }

  @Test
  public void verify_getters() {
    assertThat(underTest.getId()).isEqualTo(QUALIGATE_ID);
    assertThat(underTest.getName()).isEqualTo(QUALIGATE_NAME);
    assertThat(underTest.getConditions()).containsOnly(CONDITION_1, CONDITION_2);
  }

  @Test
  public void toString_is_override() {
    QualityGate underTest = new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2));

    assertThat(underTest).hasToString("QualityGate{id=qg_id, name='qg_name', conditions=[" +
      "Condition{metricKey='m2', operator=LESS_THAN, errorThreshold='2'}" +
      "]}");
  }

  @Test
  public void equals_is_based_on_all_fields() {
    assertThat(underTest)
      .isEqualTo(underTest)
      .isEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2, CONDITION_1)))
      .isNotNull()
      .isNotEqualTo(new Object())
      .isNotEqualTo(new QualityGate("other_id", QUALIGATE_NAME, ImmutableSet.of(CONDITION_2, CONDITION_1)))
      .isNotEqualTo(new QualityGate(QUALIGATE_ID, "other_name", ImmutableSet.of(CONDITION_2, CONDITION_1)))
      .isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, emptySet()))
      .isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1)))
      .isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2)))
      .isNotEqualTo(
        new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1, CONDITION_2, new Condition("new", Condition.Operator.GREATER_THAN, "a"))));
  }

  @Test
  public void hashcode_is_based_on_all_fields() {
    assertThat(underTest)
      .hasSameHashCodeAs(underTest)
      .hasSameHashCodeAs(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2, CONDITION_1)));
    assertThat(underTest.hashCode()).isNotEqualTo(new Object().hashCode())
      .isNotEqualTo(new QualityGate("other_id", QUALIGATE_NAME, ImmutableSet.of(CONDITION_2, CONDITION_1)).hashCode())
      .isNotEqualTo(new QualityGate(QUALIGATE_ID, "other_name", ImmutableSet.of(CONDITION_2, CONDITION_1)).hashCode())
      .isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, emptySet()).hashCode())
      .isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1)).hashCode())
      .isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2)).hashCode())
      .isNotEqualTo(
        new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1, CONDITION_2, new Condition("new", Condition.Operator.GREATER_THAN, "a"))).hashCode());
  }
}
