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
package org.sonar.server.platform.db.migration.history;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class NoTableMigrationHistoryImplIT {
  @RegisterExtension
  public final MigrationDbTester dbTester = MigrationDbTester.createEmpty();

  private final MigrationHistoryMeddler migrationHistoryMeddler = mock(MigrationHistoryMeddler.class);
  private final MigrationHistoryImpl underTest = new MigrationHistoryImpl(dbTester.database(), migrationHistoryMeddler);

  @Test
  void start_fails_with_ISE_if_table_history_does_not_exist() {
    assertThatThrownBy(() -> {
      underTest.start();
      verifyNoInteractions(migrationHistoryMeddler);
    })
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Migration history table is missing");
  }
}
