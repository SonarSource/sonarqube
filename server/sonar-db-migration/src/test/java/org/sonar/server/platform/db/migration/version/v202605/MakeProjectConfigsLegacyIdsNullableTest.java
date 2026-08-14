/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_ORGANIZATION_LEGACY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_PROJECT_LEGACY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.ORGANIZATION_LEGACY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.PROJECT_LEGACY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.TABLE_NAME;

class MakeProjectConfigsLegacyIdsNullableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MakeProjectConfigsLegacyIdsNullable.class);

  private final MakeProjectConfigsLegacyIdsNullable underTest = new MakeProjectConfigsLegacyIdsNullable(db.database());

  @Test
  void execute_shouldMakeLegacyIdColumnsNullable() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_LEGACY_ID, Types.VARCHAR, PROJECT_LEGACY_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_LEGACY_ID, Types.VARCHAR, ORGANIZATION_LEGACY_ID_SIZE, false);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_LEGACY_ID, Types.VARCHAR, PROJECT_LEGACY_ID_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_LEGACY_ID, Types.VARCHAR, ORGANIZATION_LEGACY_ID_SIZE, true);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_LEGACY_ID, Types.VARCHAR, PROJECT_LEGACY_ID_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_LEGACY_ID, Types.VARCHAR, ORGANIZATION_LEGACY_ID_SIZE, true);
  }

  @Test
  void execute_shouldAllowOmittedLegacyIdsToStoreNull() throws SQLException {
    underTest.execute();

    db.executeInsert(TABLE_NAME,
      "id", "cfg-uuid",
      "organization_id", "org-uuid",
      "project_id", "project-uuid",
      "is_enabled", false,
      "cron", "0 0 0 * * ?");

    Map<String, Object> row = db.selectFirst(
      "SELECT project_legacy_id, organization_legacy_id FROM " + TABLE_NAME);
    assertThat(row.get("PROJECT_LEGACY_ID")).isNull();
    assertThat(row.get("ORGANIZATION_LEGACY_ID")).isNull();
  }

  @Test
  void execute_shouldDoNothingWhenTableDoesNotExist() throws SQLException {
    db.executeDdl("DROP TABLE " + TABLE_NAME);

    underTest.execute();

    db.assertTableDoesNotExist(TABLE_NAME);
  }
}
