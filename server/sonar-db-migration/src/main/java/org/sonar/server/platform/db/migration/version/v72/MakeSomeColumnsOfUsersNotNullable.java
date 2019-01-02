/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v72;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class MakeSomeColumnsOfUsersNotNullable extends DdlChange {

  public static final String USERS_TABLE = "users";
  public static final String USERS_LOGIN_INDEX = "users_login";

  public MakeSomeColumnsOfUsersNotNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(USERS_TABLE)
      .setName(USERS_LOGIN_INDEX)
      .build());

    context.execute(new AlterColumnsBuilder(getDialect(), USERS_TABLE)
      .updateColumn(notNullableColumn("uuid", 255))
      .updateColumn(notNullableColumn("login", 255))
      .updateColumn(notNullableColumn("external_id", 255))
      .updateColumn(notNullableColumn("external_login", 255))
      .updateColumn(notNullableColumn("external_identity_provider", 100))
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(USERS_TABLE)
      .setName(USERS_LOGIN_INDEX)
      .addColumn(notNullableColumn("login", 255))
      .setUnique(true)
      .build());
  }

  private static VarcharColumnDef notNullableColumn(String columnName, int limit) {
    return newVarcharColumnDefBuilder()
      .setColumnName(columnName)
      .setLimit(limit)
      .setIsNullable(false)
      .build();
  }

}
