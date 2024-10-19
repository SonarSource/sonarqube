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
package org.sonar.server.platform.db.migration.version.v106;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.CoreDbTester;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.version.v104.CreateRuleTagsTable;

class ResizeValueColumnInRuleTagsTableIT {

  private static final String EXPECTED_TABLE_NAME = "rule_tags";
  private static final String EXPECTED_COLUMN_NAME = "value";

  /**
   * This is database that has run the new version of the {@link CreateRuleTagsTable} migration with the 400 limit of the value column.
   */
  @RegisterExtension
  public final MigrationDbTester dbWith400LimitOnValueColumn = MigrationDbTester.createForMigrationStep(ResizeValueColumnInRuleTagsTable.class);

  /**
   * This is the database that has run the old version of the {@link CreateRuleTagsTable} migration with the 40 limit of the value column.
   */
  @RegisterExtension
  public final CoreDbTester dbWith40LimitOnValueColumn = CoreDbTester.createForSchema(ResizeValueColumnInRuleTagsTableIT.class, "schema.sql");

  private final ResizeValueColumnInRuleTagsTable underTestNoAction = new ResizeValueColumnInRuleTagsTable(dbWith400LimitOnValueColumn.database());
  private final ResizeValueColumnInRuleTagsTable underTestThatFixesColumnSize = new ResizeValueColumnInRuleTagsTable(dbWith40LimitOnValueColumn.database());

  @Test
  void execute_whenColumnIsNotResized_shouldResizeTheColumn() throws SQLException {
    dbWith40LimitOnValueColumn.assertColumnDefinition(EXPECTED_TABLE_NAME, EXPECTED_COLUMN_NAME, Types.VARCHAR, 40, false);
    underTestThatFixesColumnSize.execute();
    dbWith40LimitOnValueColumn.assertColumnDefinition(EXPECTED_TABLE_NAME, EXPECTED_COLUMN_NAME, Types.VARCHAR, 400, false);
  }

  @Test
  void execute_whenColumnIsAlreadyResized_shouldDoNothing() throws SQLException {
    dbWith400LimitOnValueColumn.assertColumnDefinition(EXPECTED_TABLE_NAME, EXPECTED_COLUMN_NAME, Types.VARCHAR, 400, false);
    underTestNoAction.execute();
    dbWith400LimitOnValueColumn.assertColumnDefinition(EXPECTED_TABLE_NAME, EXPECTED_COLUMN_NAME, Types.VARCHAR, 400, false);
  }


}
