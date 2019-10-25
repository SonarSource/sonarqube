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
package org.sonar.server.platform.db.migration.version.v81;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateAlmSettingsTable extends DdlChange {

  private static final String TABLE_NAME = "alm_settings";

  private static final VarcharColumnDef KEY = newVarcharColumnDefBuilder()
    .setColumnName("kee")
    .setIsNullable(false)
    .setLimit(200)
    .build();

  public CreateAlmSettingsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (tableExists()) {
      return;
    }
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(newVarcharColumnDefBuilder()
        .setColumnName("uuid")
        .setIsNullable(false)
        .setLimit(UUID_SIZE)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("alm_id")
        .setIsNullable(false)
        .setLimit(40)
        .build())
      .addColumn(KEY)
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("url")
        .setIsNullable(true)
        .setLimit(2000)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("app_id")
        .setIsNullable(true)
        .setLimit(80)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("private_key")
        .setIsNullable(true)
        .setLimit(2000)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("pat")
        .setIsNullable(true)
        .setLimit(2000)
        .build())
      .addColumn(newBigIntegerColumnDefBuilder()
        .setColumnName("updated_at")
        .setIsNullable(false)
        .build())
      .addColumn(newBigIntegerColumnDefBuilder()
        .setColumnName("created_at")
        .setIsNullable(false)
        .build())
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .addColumn(KEY)
      .setName("uniq_alm_settings")
      .setUnique(true)
      .build());
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(TABLE_NAME, connection);
    }
  }
}
