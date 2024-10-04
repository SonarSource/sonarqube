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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static java.sql.Types.BOOLEAN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.sonar.server.platform.db.migration.version.v107.AddAiCodeAssuranceColumnInProjectsTable.AI_CODE_ASSURANCE;
import static org.sonar.server.platform.db.migration.version.v107.AddAiCodeAssuranceColumnInProjectsTable.DEFAULT_COLUMN_VALUE;
import static org.sonar.server.platform.db.migration.version.v107.AddAiCodeAssuranceColumnInProjectsTable.PROJECTS_TABLE_NAME;

class AddAiCodeAssuranceColumnInProjectsTableIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddAiCodeAssuranceColumnInProjectsTable.class);

  private final AddAiCodeAssuranceColumnInProjectsTable underTest = new AddAiCodeAssuranceColumnInProjectsTable(db.database());

  @Test
  void execute_whenColumnDoesNotExist_shouldCreateColumn() throws SQLException {
    db.assertColumnDoesNotExist(PROJECTS_TABLE_NAME, AI_CODE_ASSURANCE);
    underTest.execute();
    assertColumnExists();
  }

  @Test
  void execute_whenColumnsAlreadyExists_shouldNotFail() throws SQLException {
    underTest.execute();
    assertColumnExists();
    assertThatCode(underTest::execute).doesNotThrowAnyException();
  }

  @Test
  void execute_whenDataAlreadyExists_shouldCreateColumnWithDefaultValue() throws SQLException {
    db.executeInsert(PROJECTS_TABLE_NAME,
      "UUID", "uuid",
      "KEE", "uuid",
      "QUALIFIER", "TRK",
      "PRIVATE", true,
      "UPDATED_AT", 1,
      "CREATION_METHOD", "UI");

    underTest.execute();
    assertAiCodeAssuranceColumnSetToDefault();
    assertColumnExists();
  }

  private void assertAiCodeAssuranceColumnSetToDefault() {
    Map<String, Object> selectResult = db.selectFirst("select ai_code_assurance from projects where uuid = 'uuid'");
    assertThat(selectResult).containsEntry(AI_CODE_ASSURANCE, DEFAULT_COLUMN_VALUE);
  }

  private void assertColumnExists() {
    db.assertColumnDefinition(PROJECTS_TABLE_NAME, AI_CODE_ASSURANCE, BOOLEAN, null, false);
  }

}
