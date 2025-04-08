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

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static java.sql.Types.VARCHAR;
import static org.assertj.core.api.Assertions.assertThatCode;

class UpdateScaIssuesReleasesStatusColumnNotNullableTest {
  static final String TABLE_NAME = "sca_issues_releases";
  static final String COLUMN_NAME = "status";
  static final int COLUMN_SIZE = 40;

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(UpdateScaIssuesReleasesStatusColumnNotNullable.class);

  private final UpdateScaIssuesReleasesStatusColumnNotNullable underTest = new UpdateScaIssuesReleasesStatusColumnNotNullable(db.database());

  @Test
  void execute_whenColumnExists_shouldMakeColumnNotNull() throws SQLException {
    // Verify column is nullable before update
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, COLUMN_SIZE, true);

    underTest.execute();

    // Verify column is not nullable after update
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, COLUMN_SIZE, false);
  }

  @Test
  void execute_whenColumnDoesNotExist_shouldNotFail() throws SQLException {
    // Ensure the column does not exist before executing the migration
    db.executeDdl(String.format("ALTER TABLE %s DROP COLUMN IF EXISTS %s", TABLE_NAME, COLUMN_NAME));
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  @Test
  void execute_whenExecutedTwice_shouldBeIdempotent() throws SQLException {
    underTest.execute();
    assertThatCode(underTest::execute).doesNotThrowAnyException();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, VARCHAR, COLUMN_SIZE, false);
  }
}
