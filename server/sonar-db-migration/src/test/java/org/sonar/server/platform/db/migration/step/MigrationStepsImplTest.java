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
package org.sonar.server.platform.db.migration.step;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MigrationStepsImplTest {

  private MigrationStepsImpl underTest = new MigrationStepsImpl(Arrays.asList(
    new RegisteredMigrationStep(1, "mmmmmm", MigrationStep.class),
    new RegisteredMigrationStep(2, "sds", MigrationStep.class),
    new RegisteredMigrationStep(8, "ss", MigrationStep.class)));
  private MigrationStepsImpl unorderedSteps = new MigrationStepsImpl(Arrays.asList(
      new RegisteredMigrationStep(2, "sds", MigrationStep.class),
      new RegisteredMigrationStep(8, "ss", MigrationStep.class),
      new RegisteredMigrationStep(1, "mmmmmm", MigrationStep.class)));

  @Test
  public void constructor_fails_with_NPE_if_argument_is_null() {
    assertThatThrownBy(() ->  new MigrationStepsImpl(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("steps can't be null");
  }

  @Test
  public void constructor_fails_with_IAE_if_argument_is_empty() {
    assertThatThrownBy(() -> new MigrationStepsImpl(Collections.emptyList()))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("steps can't be empty");
  }

  @Test
  public void constructor_fails_with_NPE_if_argument_contains_a_null() {
    assertThatThrownBy(() -> {
      new MigrationStepsImpl(Arrays.asList(
        new RegisteredMigrationStep(12, "sdsd", MigrationStep.class),
        null,
        new RegisteredMigrationStep(88, "q", MigrationStep.class)));
    })
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void getMaxMigrationNumber_returns_migration_of_last_step_in_constructor_list_argument() {
    assertThat(underTest.getMaxMigrationNumber()).isEqualTo(8L);
    assertThat(unorderedSteps.getMaxMigrationNumber()).isOne();
  }

  @Test
  public void readAll_iterates_over_all_steps_in_constructor_list_argument() {
    verifyContainsNumbers(underTest.readAll(), 1L, 2L, 8L);
  }

  @Test
  public void readFrom_throws_IAE_if_number_is_less_than_0() {
    assertThatThrownBy(() -> underTest.readFrom(-1))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Migration number must be >= 0");
  }

  @Test
  public void readFrom_returns_stream_of_sublist_from_the_first_migration_with_number_greater_or_equal_to_argument() {
    verifyContainsNumbers(underTest.readFrom(1), 1L, 2L, 8L);
    verifyContainsNumbers(underTest.readFrom(2), 2L, 8L);
    verifyContainsNumbers(underTest.readFrom(3), 8L);
    verifyContainsNumbers(underTest.readFrom(4), 8L);
    verifyContainsNumbers(underTest.readFrom(5), 8L);
    verifyContainsNumbers(underTest.readFrom(6), 8L);
    verifyContainsNumbers(underTest.readFrom(7), 8L);
    verifyContainsNumbers(underTest.readFrom(8), 8L);
  }

  @Test
  public void readFrom_returns_an_empty_stream_if_argument_is_greater_than_biggest_migration_number() {
    verifyContainsNumbers(underTest.readFrom(9));
    verifyContainsNumbers(unorderedSteps.readFrom(9));
  }

  private static void verifyContainsNumbers(List<RegisteredMigrationStep> steps, Long... expectedMigrationNumbers) {
    assertThat(steps).hasSize(expectedMigrationNumbers.length);
    Iterator<RegisteredMigrationStep> iterator = steps.iterator();
    Arrays.stream(expectedMigrationNumbers).forEach(expected -> assertThat(iterator.next().getMigrationNumber()).isEqualTo(expected));
  }
}
