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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

@SupportsBlueGreen
public class CreateProjectQualityGatesTable extends DdlChange {

  private static final String TABLE_NAME = "project_qgates";

  private static final VarcharColumnDef PROJECT_UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("project_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();
  private static final VarcharColumnDef QUALITY_GATE_UUID_COLUMN = newVarcharColumnDefBuilder()
    .setColumnName("quality_gate_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();

  public CreateProjectQualityGatesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (tableExists()) {
      return;
    }
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(PROJECT_UUID_COLUMN)
      .addColumn(QUALITY_GATE_UUID_COLUMN)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(TABLE_NAME)
      .setName("uniq_project_qgates")
      .setUnique(true)
      .addColumn(PROJECT_UUID_COLUMN)
      .addColumn(QUALITY_GATE_UUID_COLUMN)
      .build());
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(TABLE_NAME, connection);
    }
  }
}
