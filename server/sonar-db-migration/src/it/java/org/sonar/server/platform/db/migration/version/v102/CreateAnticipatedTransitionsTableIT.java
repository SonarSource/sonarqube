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
package org.sonar.server.platform.db.migration.version.v102;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.USER_UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v102.CreateAnticipatedTransitionsTable.ANTICIPATED_TRANSITIONS_TABLE_NAME;

public class CreateAnticipatedTransitionsTableIT {
  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateAnticipatedTransitionsTable.class);

  private final DdlChange createAnticipatedTransitionsTable = new CreateAnticipatedTransitionsTable(db.database());

  @Test
  public void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(ANTICIPATED_TRANSITIONS_TABLE_NAME);

    createAnticipatedTransitionsTable.execute();

    db.assertTableExists(ANTICIPATED_TRANSITIONS_TABLE_NAME);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "project_uuid", Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "user_uuid", Types.VARCHAR, USER_UUID_SIZE, false);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "transition", Types.VARCHAR, 20, false);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "transition_comment", Types.VARCHAR, MAX_SIZE, true);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "line", Types.INTEGER, null, true);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "message", Types.VARCHAR, MAX_SIZE, true);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "line_hash", Types.VARCHAR, 255, true);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "rule_key", Types.VARCHAR, 200, false);
    db.assertColumnDefinition(ANTICIPATED_TRANSITIONS_TABLE_NAME, "file_path", Types.VARCHAR, 1500, false);
    db.assertPrimaryKey(ANTICIPATED_TRANSITIONS_TABLE_NAME, "pk_anticipated_transitions", "uuid");
  }

  @Test
  public void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(ANTICIPATED_TRANSITIONS_TABLE_NAME);

    createAnticipatedTransitionsTable.execute();
    // re-entrant
    createAnticipatedTransitionsTable.execute();

    db.assertTableExists(ANTICIPATED_TRANSITIONS_TABLE_NAME);
  }
}
