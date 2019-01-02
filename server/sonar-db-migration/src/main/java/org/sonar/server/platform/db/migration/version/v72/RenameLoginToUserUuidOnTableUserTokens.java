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
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.sql.RenameColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class RenameLoginToUserUuidOnTableUserTokens extends DdlChange {

  private static final String USER_TOKENS_TABLE = "user_tokens";

  public RenameLoginToUserUuidOnTableUserTokens(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(USER_TOKENS_TABLE)
      .setName("user_tokens_login_name")
      .build());

    VarcharColumnDef userUuidColumn = newVarcharColumnDefBuilder()
      .setColumnName("user_uuid")
      .setLimit(255)
      .setIsNullable(false)
      .build();
    context.execute(new RenameColumnsBuilder(getDialect(), USER_TOKENS_TABLE)
      .renameColumn("login",
        userUuidColumn)
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(USER_TOKENS_TABLE)
      .setName("user_tokens_user_uuid_name")
      .setUnique(true)
      .addColumn(userUuidColumn)
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("name")
        .setLimit(100)
        .setIsNullable(false)
        .build())
      .build());
  }
}
