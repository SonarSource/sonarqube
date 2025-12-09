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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202501.CreateUserAIToolUsagesTable.COLUMN_ACTIVATED_AT;
import static org.sonar.server.platform.db.migration.version.v202501.CreateUserAIToolUsagesTable.COLUMN_LAST_ACTIVITY_AT;
import static org.sonar.server.platform.db.migration.version.v202501.CreateUserAIToolUsagesTable.COLUMN_USER_UUID;
import static org.sonar.server.platform.db.migration.version.v202501.CreateUserAIToolUsagesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202501.CreateUserAIToolUsagesTable.USER_AI_TOOLS_USAGES_TABLE_NAME;

class CreateUserAIToolUsagesTableIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateUserAIToolUsagesTable.class);

  private final DdlChange underTest = new CreateUserAIToolUsagesTable(db.database());

  @Test
  void execute_shouldCreateTable() throws SQLException {
    db.assertTableDoesNotExist(USER_AI_TOOLS_USAGES_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(USER_AI_TOOLS_USAGES_TABLE_NAME);
    db.assertPrimaryKey(USER_AI_TOOLS_USAGES_TABLE_NAME, "pk_user_ai_tool_usages", COLUMN_UUID);
    db.assertColumnDefinition(USER_AI_TOOLS_USAGES_TABLE_NAME, COLUMN_USER_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(USER_AI_TOOLS_USAGES_TABLE_NAME, COLUMN_ACTIVATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(USER_AI_TOOLS_USAGES_TABLE_NAME, COLUMN_LAST_ACTIVITY_AT, Types.BIGINT, null, true);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    db.assertTableDoesNotExist(USER_AI_TOOLS_USAGES_TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(USER_AI_TOOLS_USAGES_TABLE_NAME);
  }
}
