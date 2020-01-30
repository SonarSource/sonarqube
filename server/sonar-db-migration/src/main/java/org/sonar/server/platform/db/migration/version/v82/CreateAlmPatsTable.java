/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v82;

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

public class CreateAlmPatsTable extends DdlChange {

  private static final String TABLE_NAME = "alm_pats";

  private static final VarcharColumnDef userUuidColumn = newVarcharColumnDefBuilder()
    .setColumnName("user_uuid")
    .setIsNullable(false)
    .setLimit(256)
    .build();

  private static final VarcharColumnDef alm_setting_uuid = newVarcharColumnDefBuilder()
    .setColumnName("alm_setting_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();

  public CreateAlmPatsTable(Database db) {
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
        .setColumnName("pat")
        .setIsNullable(false)
        .setLimit(2000)
        .build())
      .addColumn(userUuidColumn)
      .addColumn(alm_setting_uuid)
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
      .addColumn(userUuidColumn)
      .addColumn(alm_setting_uuid)
      .setName("uniq_alm_pats")
      .setUnique(true)
      .build());
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(TABLE_NAME, connection);
    }
  }
}
