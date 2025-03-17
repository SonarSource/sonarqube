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
package org.sonar.server.platform.db.migration.version.v202503;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import java.sql.SQLException;

import static java.sql.Types.VARCHAR;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class AddDeclaredLicenseExpressionToScaReleasesTableTest {
  static final String TABLE_NAME = "sca_releases";
  static final String COLUMN_NAME = "declared_license_expression";
  private static final int COLUMN_SIZE = 400;

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddDeclaredLicenseExpressionToScaReleasesTable.class);

  private final AddDeclaredLicenseExpressionToScaReleasesTable underTest = new AddDeclaredLicenseExpressionToScaReleasesTable(db.database());

  @Test
  void execute_whenColumnDoesNotExist_shouldCreateColumn() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
    underTest.execute();
    assertColumnExists();
  }

  @Test
  void execute_whenColumnsAlreadyExists_shouldNotFail() throws SQLException {
    underTest.execute();
    assertColumnExists();
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  private void assertColumnExists() {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, COLUMN_SIZE, false);
  }
}
