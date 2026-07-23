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

import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.SmallIntColumnDef.newSmallIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateIssueCountDimensionsTable extends CreateTableChange {

  static final String TABLE_NAME = "issue_count_dimensions";

  protected CreateIssueCountDimensionsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), ColumnFlag.AUTO_INCREMENT)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("hotspot_resolution").setIsNullable(true).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("issue_code_scope").setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("issue_severity").setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("issue_status").setIsNullable(true).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("status").setIsNullable(true).setLimit(40).build())
      .addColumn(newSmallIntColumnDefBuilder().setColumnName("issue_type").setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("rule_key").setIsNullable(false).setLimit(200).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("dimension_hash").setIsNullable(false).setLimit(40).build())
      .addColumn(newSmallIntColumnDefBuilder().setColumnName("maintainability_rating").setIsNullable(false).build())
      .addColumn(newSmallIntColumnDefBuilder().setColumnName("security_rating").setIsNullable(false).build())
      .addColumn(newSmallIntColumnDefBuilder().setColumnName("reliability_rating").setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(tableName)
      .setName("iss_cnt_dim_rule_key_idx")
      .addColumn("rule_key")
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(tableName)
      .setName("iss_cnt_dim_hash_uq_idx")
      .setUnique(true)
      .addColumn("dimension_hash", false)
      .build());
  }
}
