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
package org.sonar.server.platform.db.migration.version.v202503;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;

import static java.sql.Types.BIGINT;
import static org.assertj.core.api.Assertions.assertThatCode;

class UpdateScaLicenseProfilesPolicyUpdatedAtColumnNotNullableIT {
  static final String TABLE_NAME = "sca_license_profiles";
  static final String COLUMN_NAME = "policy_updated_at";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(UpdateScaLicenseProfilesPolicyUpdatedAtColumnNotNullable.class);
  private final UpdateScaLicenseProfilesPolicyUpdatedAtColumnNotNullable underTest = new UpdateScaLicenseProfilesPolicyUpdatedAtColumnNotNullable(db.database());

  @Test
  void execute_whenColumnExists_shouldMakeColumnNotNull() throws SQLException {
    // Verify column is nullable before update
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, BIGINT, null, true);

    underTest.execute();

    // Verify column is not nullable after update
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, BIGINT, null, false);
  }

  @Test
  void execute_whenColumnDoesNotExist_shouldNotFail() throws SQLException {
    // Ensure the column does not exist before executing the migration
    DropColumnsBuilder dropColumnsBuilder = new DropColumnsBuilder(db.database().getDialect(), TABLE_NAME, COLUMN_NAME);
    dropColumnsBuilder.build().forEach(db::executeDdl);

    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  @Test
  void execute_whenExecutedTwice_shouldBeIdempotent() throws SQLException {
    underTest.execute();
    assertThatCode(underTest::execute).doesNotThrowAnyException();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, BIGINT, null, false);
  }
}
