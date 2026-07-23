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
import static org.sonar.server.platform.db.migration.def.SmallIntColumnDef.newSmallIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateMeasureHistoryTable extends CreateTableChange {

  static final String TABLE_NAME = "measure_history";
  public static final String METRIC_ID = "metric_id";
  public static final String ENTITY_ID = "entity_id";
  public static final String ENTITY_TYPE = "entity_type";
  public static final String RECORDED_AT_EPOCH = "recorded_at_epoch";
  public static final String TEXT_VALUE = "text_value";
  public static final String MSR_HIST_EPOCH_UQ_IDX_2 = "msr_hist_epoch_uq_idx2";
  public static final String MSR_HIST_ENTITY_METRIC_IDX = "msr_hist_entity_metric_idx";

  protected CreateMeasureHistoryTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addColumn(newSmallIntColumnDefBuilder().setColumnName(METRIC_ID).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(ENTITY_ID).setIsNullable(false).setLimit(40).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(ENTITY_TYPE).setIsNullable(false).setLimit(40).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(RECORDED_AT_EPOCH).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(TEXT_VALUE).setIsNullable(true).setLimit(4000).build())
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(MSR_HIST_EPOCH_UQ_IDX_2)
      .setUnique(true)
      .addColumn(ENTITY_ID, false)
      .addColumn(ENTITY_TYPE, false)
      .addColumn(METRIC_ID, false)
      .addColumn(RECORDED_AT_EPOCH, false, true)
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(MSR_HIST_ENTITY_METRIC_IDX)
      .addColumn(ENTITY_ID)
      .addColumn(ENTITY_TYPE)
      .addColumn(METRIC_ID)
      .build());
  }
}
