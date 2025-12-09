/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.junit.jupiter.api.Test;
import org.sonar.server.platform.db.migration.DatabaseMigrationStateImpl;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationStatusListenerImplTest {

  @Test
  void onMigrationStepCompleted_incrementsTheStepsInTheState() {
    final DatabaseMigrationStateImpl state = new DatabaseMigrationStateImpl();
    final MigrationStatusListenerImpl underTest = new MigrationStatusListenerImpl(state);

    assertThat(state.getCompletedMigrations()).isZero();

    underTest.onMigrationStepCompleted();

    assertThat(state.getCompletedMigrations()).isEqualTo(1);
  }

  @Test
  void onMigrationStart_setsTheCorrectNumberOfTotalSteps() {
    final DatabaseMigrationStateImpl state = new DatabaseMigrationStateImpl();
    final MigrationStatusListenerImpl underTest = new MigrationStatusListenerImpl(state);
    final int totalMigrations = 10;

    assertThat(state.getTotalMigrations()).isZero();

    underTest.onMigrationsStart(totalMigrations);

    assertThat(state.getTotalMigrations()).isEqualTo(totalMigrations);
  }

}
