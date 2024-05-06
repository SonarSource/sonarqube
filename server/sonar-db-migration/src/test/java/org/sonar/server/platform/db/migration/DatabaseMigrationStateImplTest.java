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
package org.sonar.server.platform.db.migration;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DatabaseMigrationStateImplTest {
  private final DatabaseMigrationStateImpl underTest = new DatabaseMigrationStateImpl();

  @Test
  void getStatus_whenComponentIsCreated_shouldReturnNONE() {
    assertThat(underTest.getStatus()).isEqualTo(DatabaseMigrationState.Status.NONE);
  }

  @Test
  void getStatus_shouldReturnArgumentOfSetStatus() {
    for (DatabaseMigrationState.Status status : DatabaseMigrationState.Status.values()) {
      underTest.setStatus(status);

      assertThat(underTest.getStatus()).isEqualTo(status);
    }
  }

  @Test
  void getStartedAt_whenComponentIsCreated_shouldNotBePresent() {
    assertThat(underTest.getStartedAt()).isEmpty();
  }

  @Test
  void getStartedAt_shouldReturnArgumentOfSetStartedAt() {
    Instant expected = Instant.now();
    underTest.setStartedAt(expected);

    assertThat(underTest.getStartedAt()).get().isSameAs(expected);
  }

  @Test
  void getError_whenComponentIsCreated_shouldNotBePresent() {
    assertThat(underTest.getError()).isEmpty();
  }

  @Test
  void getError_shouldReturnArgumentOfSetError() {
    RuntimeException expected = new RuntimeException();
    underTest.setError(expected);

    assertThat(underTest.getError()).get().isSameAs(expected);
  }
  
  @Test
  void incrementCompletedMigrations_shouldIncrementCompletedMigrations() {
    assertThat(underTest.getCompletedMigrations()).isZero();
    
    underTest.incrementCompletedMigrations();
    
    assertThat(underTest.getCompletedMigrations()).isEqualTo(1);
  }

  @Test
  void getTotalMigrations_shouldReturnArgumentOfSetTotalMigrations() {
    underTest.setTotalMigrations(10);

    assertThat(underTest.getTotalMigrations()).isEqualTo(10);
  }

  @Test
  void when_noStartedMigration_expectedFinishDateShouldBeAbsent() {
    Instant startDate = Instant.now();
    Instant later = startDate.plus(1, ChronoUnit.MINUTES);

    underTest.setTotalMigrations(2);

    assertThat(underTest.getExpectedFinishDate(later)).isEmpty();
  }

  @Test
  void when_noStepCompleted_expectedFinishDateShouldBeAbsent() {
    Instant startDate = Instant.now();
    Instant later = startDate.plus(1, ChronoUnit.MINUTES);

    underTest.setStartedAt(startDate);
    underTest.setTotalMigrations(2);

    assertThat(underTest.getExpectedFinishDate(later)).isEmpty();
  }

  @Test
  void when_StepCompleted_expectedFinishDateShouldBePresent() {
    Instant startDate = Instant.now();
    Instant later = startDate.plus(1, ChronoUnit.MINUTES);
    Instant expectedEnd = startDate.plus(2, ChronoUnit.MINUTES);

    underTest.setStartedAt(startDate);
    underTest.setTotalMigrations(2);
    underTest.incrementCompletedMigrations();

    assertThat(underTest.getExpectedFinishDate(later)).get(InstanceOfAssertFactories.INSTANT)
      .isCloseTo(expectedEnd, within(1, ChronoUnit.SECONDS));
  }
}
