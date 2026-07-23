/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder.ColumnFlag;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.SmallIntColumnDef.newSmallIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateMeasureKeyMappingTable extends CreateTableChange {

  static final String TABLE_NAME = "measure_key_mapping";

  protected CreateMeasureKeyMappingTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newSmallIntColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), ColumnFlag.AUTO_INCREMENT)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("metric_name").setIsNullable(false).setLimit(64).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("metric_type").setIsNullable(true).setLimit(64).build())
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(tableName)
      .setName("msr_key_mapping_name_uq_idx")
      .setUnique(true)
      .addColumn("metric_name", false)
      .build());
  }
}
