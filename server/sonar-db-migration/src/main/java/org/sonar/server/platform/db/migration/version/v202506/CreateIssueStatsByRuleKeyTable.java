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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateIssueStatsByRuleKeyTable extends CreateTableChange {
  static final String TABLE_NAME = "issue_stats_by_rule_key";
  static final String COLUMN_AGGREGATION_TYPE = "aggregation_type";
  static final String COLUMN_AGGREGATION_ID = "aggregation_id";
  static final String COLUMN_RULE_KEY = "rule_key";
  static final String COLUMN_ISSUE_COUNT = "issue_count";
  static final String COLUMN_RATING = "rating";
  static final String COLUMN_HOTSPOT_COUNT = "hotspot_count";
  static final String COLUMN_HOTSPOT_RATING = "hotspot_rating";

  public CreateIssueStatsByRuleKeyTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_AGGREGATION_TYPE).setIsNullable(false).setLimit(20).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_AGGREGATION_ID).setIsNullable(false).setLimit(40).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RULE_KEY).setIsNullable(false).setLimit(100).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_ISSUE_COUNT).setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_RATING).setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_HOTSPOT_COUNT).setIsNullable(true).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_HOTSPOT_RATING).setIsNullable(true).build())
      .build());
  }
}
