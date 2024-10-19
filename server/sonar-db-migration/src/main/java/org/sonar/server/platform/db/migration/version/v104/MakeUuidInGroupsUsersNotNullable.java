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
package org.sonar.server.platform.db.migration.version.v104;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v104.AddUuidColumnToGroupsUsers.GROUPS_USERS_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v104.AddUuidColumnToGroupsUsers.GROUPS_USERS_UUID_COLUMN_NAME;

public class MakeUuidInGroupsUsersNotNullable extends DdlChange {

  private static final VarcharColumnDef UUID_COLUMN_DEF = VarcharColumnDef.newVarcharColumnDefBuilder(GROUPS_USERS_UUID_COLUMN_NAME)
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();

  public MakeUuidInGroupsUsersNotNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AlterColumnsBuilder(getDialect(), GROUPS_USERS_TABLE_NAME).updateColumn(UUID_COLUMN_DEF).build());

  }
}
