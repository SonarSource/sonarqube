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
package org.sonar.server.platform.db.migration.version.v202506;

import org.junit.jupiter.api.Test;

import static org.sonar.server.platform.db.migration.version.DbVersionTestUtils.verifyMigrationNotEmpty;
import static org.sonar.server.platform.db.migration.version.DbVersionTestUtils.verifyMinimumMigrationNumber;

class DbVersion202506Test {

  private final DbVersion202506 underTest = new DbVersion202506();

  @Test
  void migrationNumber_starts_at_2025_06_000() {
    verifyMinimumMigrationNumber(underTest, 2025_06_000);
  }

  @Test
  void verify_migration_is_not_empty() {
    verifyMigrationNotEmpty(underTest);
  }

}
