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
package org.sonar.server.platform.db.migration.version.v103;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThatCode;

class AddSqVersionColumnInQprofileChangesTableIT {

  private static final String TABLE_NAME = "qprofile_changes";
  private static final String COLUMN_NAME = "sq_version";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddSqVersionColumnInQprofileChangesTable.class);
  private final AddSqVersionColumnInQprofileChangesTable underTest = new AddSqVersionColumnInQprofileChangesTable(db.database());

  @Test
  void execute_whenColumnDoesNotExist_shouldCreateColumn() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
    underTest.execute();
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, Types.VARCHAR, 40, true);
  }

  @Test
  void execute_whenColumnsAlreadyExists_shouldNotFail() throws SQLException {
    underTest.execute();
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }
}
