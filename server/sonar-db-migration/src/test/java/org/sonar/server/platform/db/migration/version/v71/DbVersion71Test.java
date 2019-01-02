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
package org.sonar.server.platform.db.migration.version.v71;

import org.junit.Test;

import static org.sonar.server.platform.db.migration.version.DbVersionTestUtils.verifyMigrationCount;
import static org.sonar.server.platform.db.migration.version.DbVersionTestUtils.verifyMinimumMigrationNumber;

public class DbVersion71Test {

  private DbVersion71 underTest = new DbVersion71();

  @Test
  public void migrationNumber_starts_at_2000() {
    verifyMinimumMigrationNumber(underTest, 2000);
  }

  @Test
  public void verify_migration_count() {
    verifyMigrationCount(underTest, 24);
  }

}
