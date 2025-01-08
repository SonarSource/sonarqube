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
package org.sonar.server.platform.db.migration.version.v107;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTelemetryMetricsSentTable extends CreateTableChange {

  private static final String TABLE_NAME = "telemetry_metrics_sent";
  private static final int LENGTH_OF_METRIC_KEY_AND_DIMENSION = 40;

  protected CreateTelemetryMetricsSentTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("metric_key").setIsNullable(false)
        .setLimit(LENGTH_OF_METRIC_KEY_AND_DIMENSION).build())
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("dimension").setIsNullable(false)
        .setLimit(LENGTH_OF_METRIC_KEY_AND_DIMENSION).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("last_sent").setIsNullable(false).build())
      .build());
  }
}
