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

import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationStateImplTest {
  private DatabaseMigrationStateImpl underTest = new DatabaseMigrationStateImpl();

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
  void getStartedAt_whenComponentIsCreated_shouldReturnNull() {
    assertThat(underTest.getStartedAt()).isNull();
  }

  @Test
  void getStartedAt_shouldReturnArgumentOfSetStartedAt() {
    Date expected = new Date();
    underTest.setStartedAt(expected);

    assertThat(underTest.getStartedAt()).isSameAs(expected);
  }

  @Test
  void getError_whenComponentIsCreated_shouldReturnNull() {
    assertThat(underTest.getError()).isNull();
  }

  @Test
  void getError_shouldReturnArgumentOfSetError() {
    RuntimeException expected = new RuntimeException();
    underTest.setError(expected);

    assertThat(underTest.getError()).isSameAs(expected);
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
  void incrementCompletedMigrations_shouldUpdateExpectedFinishDate() {
    Date startDate = new Date();

    underTest.incrementCompletedMigrations();

    // At the moment the expected finish date gets update with the timestamp of the last migration completed
    assertThat(underTest.getExpectedFinishDate()).isAfterOrEqualTo(startDate);
  }
  
}
