/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v104.CreateRuleTagsTable.IS_SYSTEM_TAG_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v104.CreateRuleTagsTable.RULE_TAGS_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v104.CreateRuleTagsTable.RULE_UUID_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v104.CreateRuleTagsTable.VALUE_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v104.CreateRuleTagsTable.VALUE_COLUMN_SIZE;

public class CreateRuleTagsTableIT {
  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateRuleTagsTable.class);

  private final DdlChange createScmAccountsTable = new CreateRuleTagsTable(db.database());

  @Test
  public void execute_whenRun_shouldCreateRuleTagsTable() throws SQLException {
    db.assertTableDoesNotExist(RULE_TAGS_TABLE_NAME);

    createScmAccountsTable.execute();

    db.assertTableExists(RULE_TAGS_TABLE_NAME);
    db.assertColumnDefinition(RULE_TAGS_TABLE_NAME, VALUE_COLUMN_NAME, Types.VARCHAR, VALUE_COLUMN_SIZE, false);
    db.assertColumnDefinition(RULE_TAGS_TABLE_NAME, IS_SYSTEM_TAG_COLUMN_NAME, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(RULE_TAGS_TABLE_NAME, RULE_UUID_COLUMN_NAME, Types.VARCHAR, UUID_SIZE, false);
    db.assertPrimaryKey(RULE_TAGS_TABLE_NAME, "pk_rule_tags", VALUE_COLUMN_NAME, RULE_UUID_COLUMN_NAME);
  }

  @Test
  public void execute_whenRunMoreThanOnce_shouldBeReentrant() throws SQLException {
    db.assertTableDoesNotExist(RULE_TAGS_TABLE_NAME);

    createScmAccountsTable.execute();
    // re-entrant
    createScmAccountsTable.execute();

    db.assertTableExists(RULE_TAGS_TABLE_NAME);
  }
}
