/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.metrics.livemeasures;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateTableAsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CopyLiveMeasuresTable extends DdlChange {
  public CopyLiveMeasuresTable(Database db) {
    super(db);
  }

  @Override public void execute(Context context) throws SQLException {
    CreateTableAsBuilder builder = new CreateTableAsBuilder(getDialect(), "live_measures_copy", "live_measures")
      // this will cause the following changes:
      // * Add METRIC_UUID with values in METRIC_ID casted to varchar
      .addColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setLimit(VarcharColumnDef.UUID_SIZE).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("project_uuid").setLimit(50).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("component_uuid").setLimit(50).setIsNullable(false).build())
      .addColumnWithCast(newVarcharColumnDefBuilder().setColumnName("metric_uuid").setLimit(40).setIsNullable(false).build(), "metric_id")
      .addColumn(newDecimalColumnDefBuilder().setColumnName("value").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("text_value").setLimit(4000).build())
      .addColumn(newDecimalColumnDefBuilder().setColumnName("variation").build())
      .addColumn(newBlobColumnDefBuilder().setColumnName("measure_data").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("update_marker").setLimit(VarcharColumnDef.UUID_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build());
    context.execute(builder.build());

    /*
     *         "UUID VARCHAR(40) NOT NULL",
     *         "PROJECT_UUID VARCHAR(50) NOT NULL",
     *         "COMPONENT_UUID VARCHAR(50) NOT NULL",
     *         "METRIC_UUID VARCHAR(40) NOT NULL",
     *         "VALUE DOUBLE",
     *         "TEXT_VALUE VARCHAR(4000)",
     *         "VARIATION DOUBLE",
     *         "MEASURE_DATA BLOB",
     *         "UPDATE_MARKER VARCHAR(40)",
     *         "CREATED_AT BIGINT NOT NULL",
     *         "UPDATED_AT BIGINT NOT NULL"
     */
  }
}
