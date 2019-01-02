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

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateAlmAppInstallsTable extends DdlChange {

  private static final String TABLE_NAME = "alm_app_installs";

  private static final VarcharColumnDef UUID = newVarcharColumnDefBuilder()
    .setColumnName("uuid")
    .setLimit(UUID_SIZE)
    .setIsNullable(false)
    .build();
  private static final VarcharColumnDef ALM_ID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("alm_id")
    .setIsNullable(false)
    .setLimit(40)
    .build();
  private static final VarcharColumnDef OWNER_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("owner_id")
    .setIsNullable(false)
    .setLimit(MAX_SIZE)
    .build();
  private static final VarcharColumnDef INSTALL_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("install_id")
    .setIsNullable(false)
    .setLimit(MAX_SIZE)
    .build();
  private static final BigIntegerColumnDef CREATED_AT_COLUMN = newBigIntegerColumnDefBuilder()
    .setColumnName("created_at")
    .setIsNullable(false)
    .build();
  private static final BigIntegerColumnDef UPDATED_AT_COLUMN = newBigIntegerColumnDefBuilder()
    .setColumnName("updated_at")
    .setIsNullable(false)
    .build();

  public CreateAlmAppInstallsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {

    if (!tableExists()) {
      context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
        .addPkColumn(UUID)
        .addColumn(ALM_ID_COLUMN)
        .addColumn(OWNER_COLUMN)
        .addColumn(INSTALL_COLUMN)
        .addColumn(CREATED_AT_COLUMN)
        .addColumn(UPDATED_AT_COLUMN)
        .build());

      context.execute(new CreateIndexBuilder(getDialect())
        .addColumn(ALM_ID_COLUMN)
        .addColumn(OWNER_COLUMN)
        .setUnique(true)
        .setTable(TABLE_NAME)
        .setName("alm_app_installs_owner")
        .build());
      context.execute(new CreateIndexBuilder(getDialect())
        .addColumn(ALM_ID_COLUMN)
        .addColumn(INSTALL_COLUMN)
        .setUnique(true)
        .setTable(TABLE_NAME)
        .setName("alm_app_installs_install")
        .build());
    }
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(TABLE_NAME, connection);
    }
  }
}
