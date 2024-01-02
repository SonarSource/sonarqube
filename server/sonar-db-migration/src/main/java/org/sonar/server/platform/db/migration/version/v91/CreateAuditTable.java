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
package org.sonar.server.platform.db.migration.version.v91;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateAuditTable extends DdlChange {

  public CreateAuditTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (auditTableExists()) {
      return;
    }

    BigIntegerColumnDef createdAtColumn = newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build();
    var tableName = "audits";
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnBuilder("user_uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnBuilder("user_login").setIsNullable(false).setLimit(255).build())
      .addColumn(newVarcharColumnBuilder("category").setIsNullable(false).setLimit(25).build())
      .addColumn(newVarcharColumnBuilder("operation").setIsNullable(false).setLimit(50).build())
      .addColumn(newVarcharColumnBuilder("new_value").setIsNullable(true).setLimit(4000).build())
      .addColumn(createdAtColumn)
      .build());

    addIndex(context, tableName, "audits_created_at", false, createdAtColumn);
  }

  private static void addIndex(Context context, String table, String index, boolean unique, ColumnDef firstColumn, ColumnDef... otherColumns) {
    CreateIndexBuilder builder = new CreateIndexBuilder()
      .setTable(table)
      .setName(index)
      .setUnique(unique);
    concat(of(firstColumn), stream(otherColumns)).forEach(builder::addColumn);
    context.execute(builder.build());
  }

  private static VarcharColumnDef.Builder newVarcharColumnBuilder(String column) {
    return newVarcharColumnDefBuilder().setColumnName(column);
  }

  private boolean auditTableExists() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      return DatabaseUtils.tableExists("audits", connection);
    }
  }
}
