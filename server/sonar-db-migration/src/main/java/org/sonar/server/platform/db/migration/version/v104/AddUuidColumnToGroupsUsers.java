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

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

public class AddUuidColumnToGroupsUsers extends DdlChange {

  public static final String GROUPS_USERS_TABLE_NAME = "groups_users";
  public static final String GROUPS_USERS_UUID_COLUMN_NAME = "uuid";

  public AddUuidColumnToGroupsUsers(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!DatabaseUtils.tableColumnExists(connection, GROUPS_USERS_TABLE_NAME, GROUPS_USERS_UUID_COLUMN_NAME)) {
        ColumnDef columnDef = VarcharColumnDef.newVarcharColumnDefBuilder()
          .setColumnName(GROUPS_USERS_UUID_COLUMN_NAME)
          .setLimit(UUID_SIZE)
          .setIsNullable(true)
          .build();
        context.execute(new AddColumnsBuilder(getDialect(), GROUPS_USERS_TABLE_NAME).addColumn(columnDef).build());
      }
    }
  }
}
