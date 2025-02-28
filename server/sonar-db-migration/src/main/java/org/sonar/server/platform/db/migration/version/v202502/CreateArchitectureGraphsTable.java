/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v202502;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

import java.sql.SQLException;
import java.util.List;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.ClobColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

public class CreateArchitectureGraphsTable extends CreateTableChange {
  public static final String TABLE_NAME = "architecture_graphs";


  protected CreateArchitectureGraphsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    List<String> createQueries = new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("branch_uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("source").setIsNullable(false).setLimit(255).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("type").setIsNullable(false).setLimit(255).build())
      .addColumn(ClobColumnDef.newClobColumnDefBuilder().setColumnName("graph_data").setIsNullable(false).build())
      .build();

    context.execute(createQueries);
  }
}
