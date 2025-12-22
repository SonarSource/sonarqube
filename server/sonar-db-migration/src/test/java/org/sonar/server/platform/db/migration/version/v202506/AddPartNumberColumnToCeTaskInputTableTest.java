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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.sql.DbPrimaryKeyConstraintFinder;
import org.sonar.server.platform.db.migration.sql.DropPrimaryKeySqlGenerator;

import static java.sql.Types.INTEGER;
import static org.sonar.server.platform.db.migration.version.v202506.AddPartNumberColumnToCeTaskInputTable.COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v202506.AddPartNumberColumnToCeTaskInputTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202506.AddPartNumberColumnToCeTaskInputTable.TASK_UUID;

class AddPartNumberColumnToCeTaskInputTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(AddPartNumberColumnToCeTaskInputTable.class);

  private final DropPrimaryKeySqlGenerator sqlGenerator = new DropPrimaryKeySqlGenerator(db.database(), new DbPrimaryKeyConstraintFinder(db.database()));

  private final AddPartNumberColumnToCeTaskInputTable underTest = new AddPartNumberColumnToCeTaskInputTable(db.database(), sqlGenerator);

  @Test
  void execute_whenColumnDoesNotExist_shouldCreateColumn() throws SQLException {
    db.assertColumnDoesNotExist(TABLE_NAME, COLUMN_NAME);
    db.assertPrimaryKey(TABLE_NAME, "PK_CE_TASK_INPUT", TASK_UUID);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, INTEGER, null, false);
    db.assertPrimaryKey(TABLE_NAME, "PK_CE_TASK_INPUT", TASK_UUID, COLUMN_NAME);
  }

  @Test
  void execute_whenColumnAlreadyExists_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_NAME, INTEGER, null, false);
    db.assertPrimaryKey(TABLE_NAME, "PK_CE_TASK_INPUT", TASK_UUID, COLUMN_NAME);
  }
}

