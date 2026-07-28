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
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateIssueTtrHistoryTable extends CreateTableChange {

  static final String TABLE_NAME = "issue_ttr_history";
  static final String COLUMN_ENTITY_ID = "entity_id";
  static final String COLUMN_ENTITY_TYPE = "entity_type";
  static final String COLUMN_DIMENSION_ID = "dimension_id";
  static final String COLUMN_RECORDED_AT_EPOCH = "recorded_at_epoch";
  static final String COLUMN_TOTAL_MINUTES_TO_RESOLUTION = "total_minutes_to_resolution";
  static final String COLUMN_ISSUES_RESOLVED = "issues_resolved";
  static final String COLUMN_TOTAL_MINUTES_TO_RESOLUTION_RECENT = "total_minutes_to_resolution_recent";
  static final String COLUMN_ISSUES_RESOLVED_RECENT = "issues_resolved_recent";
  static final String INDEX_UNIQUE = "issue_ttr_history_uq_idx";
  static final String INDEX_ENTITY_TYPE_RECORDED_AT = "iss_ttr_history_ent_type_epoch";

  static final int ENTITY_ID_SIZE = 40;
  static final int ENTITY_TYPE_SIZE = 40;

  protected CreateIssueTtrHistoryTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ENTITY_ID).setIsNullable(false).setLimit(ENTITY_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ENTITY_TYPE).setIsNullable(false).setLimit(ENTITY_TYPE_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_DIMENSION_ID).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_RECORDED_AT_EPOCH).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_TOTAL_MINUTES_TO_RESOLUTION).setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_ISSUES_RESOLVED).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_TOTAL_MINUTES_TO_RESOLUTION_RECENT).setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_ISSUES_RESOLVED_RECENT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_UNIQUE)
      .setUnique(true)
      .addColumn(COLUMN_ENTITY_ID, false)
      .addColumn(COLUMN_ENTITY_TYPE, false)
      .addColumn(COLUMN_DIMENSION_ID, false)
      .addColumn(COLUMN_RECORDED_AT_EPOCH, false, true)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_ENTITY_TYPE_RECORDED_AT)
      .addColumn(COLUMN_ENTITY_ID)
      .addColumn(COLUMN_ENTITY_TYPE)
      .addColumn(COLUMN_RECORDED_AT_EPOCH)
      .build());
  }
}
