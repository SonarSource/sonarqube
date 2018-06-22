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

import com.google.common.collect.ImmutableSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

public class QualityGateTest {
  private static final String QUALIGATE_ID = "qg_id";
  private static final String QUALIGATE_NAME = "qg_name";
  private static final Condition CONDITION_1 = new Condition("m1", Condition.Operator.EQUALS, "1", "2", false);
  private static final Condition CONDITION_2 = new Condition("m2", Condition.Operator.LESS_THAN, "2", "4", true);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private QualityGate underTest = new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1, CONDITION_2));

  @Test
  public void constructor_fails_with_NPE_if_id_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("id can't be null");

    new QualityGate(null, "name", emptySet());
  }

  @Test
  public void constructor_fails_with_NPE_if_name_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("name can't be null");

    new QualityGate("id", null, emptySet());
  }

  @Test
  public void constructor_fails_with_NPE_if_conditions_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("conditions can't be null");

    new QualityGate("id", "name", null);
  }

  @Test
  public void constructor_fails_with_NPE_if_conditions_contains_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("condition can't be null");
    Random random = new Random();
    Set<Condition> conditions = Stream.of(
      IntStream.range(0, random.nextInt(5))
        .mapToObj(i -> new Condition("m_before_" + i, Condition.Operator.EQUALS, null, null, false)),
      Stream.of((Condition) null),
      IntStream.range(0, random.nextInt(5))
        .mapToObj(i -> new Condition("m_after_" + i, Condition.Operator.EQUALS, null, null, false)))
      .flatMap(s -> s)
      .collect(Collectors.toSet());

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("condition can't be null");

    new QualityGate("id", "name", conditions);
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

    assertThat(underTest.toString()).isEqualTo("QualityGate{id=qg_id, name='qg_name', conditions=[" +
      "Condition{metricKey='m2', operator=LESS_THAN, warningThreshold='4', errorThreshold='2', onLeakPeriod=true}" +
      "]}");
  }

  @Test
  public void equals_is_based_on_all_fields() {
    assertThat(underTest).isEqualTo(underTest);
    assertThat(underTest).isEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2, CONDITION_1)));
    assertThat(underTest).isNotEqualTo(null);
    assertThat(underTest).isNotEqualTo(new Object());
    assertThat(underTest).isNotEqualTo(new QualityGate("other_id", QUALIGATE_NAME, ImmutableSet.of(CONDITION_2, CONDITION_1)));
    assertThat(underTest).isNotEqualTo(new QualityGate(QUALIGATE_ID, "other_name", ImmutableSet.of(CONDITION_2, CONDITION_1)));
    assertThat(underTest).isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, emptySet()));
    assertThat(underTest).isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1)));
    assertThat(underTest).isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2)));
    assertThat(underTest).isNotEqualTo(
      new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1, CONDITION_2, new Condition("new", Condition.Operator.GREATER_THAN, "a", "b", false))));
  }

  @Test
  public void hashcode_is_based_on_all_fields() {
    assertThat(underTest.hashCode()).isEqualTo(underTest.hashCode());
    assertThat(underTest.hashCode()).isEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2, CONDITION_1)).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(null);
    assertThat(underTest.hashCode()).isNotEqualTo(new Object().hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new QualityGate("other_id", QUALIGATE_NAME, ImmutableSet.of(CONDITION_2, CONDITION_1)).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new QualityGate(QUALIGATE_ID, "other_name", ImmutableSet.of(CONDITION_2, CONDITION_1)).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, emptySet()).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1)).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_2)).hashCode());
    assertThat(underTest.hashCode()).isNotEqualTo(
      new QualityGate(QUALIGATE_ID, QUALIGATE_NAME, ImmutableSet.of(CONDITION_1, CONDITION_2, new Condition("new", Condition.Operator.GREATER_THAN, "a", "b", false))).hashCode());
  }
}
