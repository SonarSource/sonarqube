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
package org.sonar.server.platform.db.migration.version.v61;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.DbVersionTestUtils.verifyMigrationCount;
import static org.sonar.server.platform.db.migration.version.DbVersionTestUtils.verifyMinimumMigrationNumber;

public class DbVersion61Test {
  private DbVersion61 underTest = new DbVersion61();

  @Test
  public void verify_support_components() {
    assertThat(underTest.getSupportComponents())
      .containsExactly(ShrinkModuleUuidPathOfProjects.class, AddBUuidPathToProjects.class);
  }

  @Test
  public void migrationNumber_starts_at_1300() {
    verifyMinimumMigrationNumber(underTest, 1300);
  }

  @Test
  public void verify_migration_count() {
    verifyMigrationCount(underTest, 17);
  }

}
