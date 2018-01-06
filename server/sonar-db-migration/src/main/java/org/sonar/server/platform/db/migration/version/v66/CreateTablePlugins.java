/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v66;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTablePlugins extends DdlChange {

  static final int PLUGIN_KEY_MAX_SIZE = 200;
  private static final String TABLE_NAME = "plugins";

  public CreateTablePlugins(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef keyColumn = newVarcharColumnDefBuilder()
      .setColumnName("kee")
      .setLimit(PLUGIN_KEY_MAX_SIZE)
      .setIsNullable(false)
      .build();
    context.execute(
      new CreateTableBuilder(getDialect(), TABLE_NAME)
        .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setLimit(UUID_SIZE).setIsNullable(false).setIgnoreOracleUnit(true).build())
        .addColumn(keyColumn)
        .addColumn(newVarcharColumnDefBuilder()
          .setColumnName("base_plugin_key")
          .setLimit(PLUGIN_KEY_MAX_SIZE)
          .setIsNullable(true)
          .build())
        .addColumn(newVarcharColumnDefBuilder()
          .setColumnName("file_hash")
          .setLimit(200)
          .setIsNullable(false)
          .build())
        .addColumn(newBigIntegerColumnDefBuilder()
          .setColumnName("created_at")
          .setIsNullable(false)
          .build())
        .addColumn(newBigIntegerColumnDefBuilder()
          .setColumnName("updated_at")
          .setIsNullable(false)
          .build())
        .build());

    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName("plugins_key")
        .addColumn(keyColumn)
        .setUnique(true)
        .build());
  }
}
