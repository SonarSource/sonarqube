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
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_VARCHAR_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateProjectAlmSettingsTable extends DdlChange {

  private static final String TABLE_NAME = "project_alm_settings";

  private static final VarcharColumnDef PROJECT_UUID = newVarcharColumnDefBuilder()
    .setColumnName("project_uuid")
    .setIsNullable(false)
    .setLimit(UUID_VARCHAR_SIZE)
    .build();

  private static final VarcharColumnDef ALM_SETTING_UUID = newVarcharColumnDefBuilder()
    .setColumnName("alm_setting_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();

  public CreateProjectAlmSettingsTable(Database db) {
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
      .addColumn(ALM_SETTING_UUID)
      .addColumn(PROJECT_UUID)
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("alm_repo")
        .setIsNullable(true)
        .setLimit(256)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("alm_slug")
        .setIsNullable(true)
        .setLimit(256)
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
      .addColumn(PROJECT_UUID)
      .setName("uniq_project_alm_settings")
      .setUnique(true)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .addColumn(ALM_SETTING_UUID)
      .setName("project_alm_settings_alm")
      .build());
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(TABLE_NAME, connection);
    }
  }
}
