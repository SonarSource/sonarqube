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
package org.sonar.server.platform.db.migration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DatabaseMigrationLoggerContextTest {

  private final DatabaseMigrationLoggerContext underTest = new DatabaseMigrationLoggerContext();

  @Test
  void addMigrationData() {
    underTest.addMigrationData("001", 45L, true, 100L, "2025.1");
    underTest.addMigrationData("002", 46L, false, 101L, "2025.1");
    underTest.addMigrationData("003", 47L, true, 102L, "2025.1");

    assertThat(underTest.getMigrationData())
      .hasSize(3)
      .extracting(DatabaseMigrationLoggerContext.MigrationData::step, DatabaseMigrationLoggerContext.MigrationData::durationInMs,
        DatabaseMigrationLoggerContext.MigrationData::success, DatabaseMigrationLoggerContext.MigrationData::startedAt, DatabaseMigrationLoggerContext.MigrationData::targetVersion)
      .containsExactly(
        tuple("001", 45L, true, 100L, "2025.1"),
        tuple("002", 46L, false, 101L, "2025.1"),
        tuple("003", 47L, true, 102L, "2025.1"));
  }

}
