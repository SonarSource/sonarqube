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
package org.sonar.server.platform.db.migration.version.v70;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableLiveMeasures extends DdlChange {

  private static final String TABLE_NAME = "live_measures";

  public CreateTableLiveMeasures(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(newVarcharColumnDefBuilder()
        .setColumnName("uuid")
        .setIsNullable(false)
        .setLimit(VarcharColumnDef.UUID_SIZE)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("project_uuid")
        .setIsNullable(false)
        .setLimit(VarcharColumnDef.UUID_VARCHAR_SIZE)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("component_uuid")
        .setIsNullable(false)
        .setLimit(VarcharColumnDef.UUID_VARCHAR_SIZE)
        .build())
      .addColumn(newIntegerColumnDefBuilder()
        .setColumnName("metric_id")
        .setIsNullable(false)
        .build())
      .addColumn(newDecimalColumnDefBuilder()
        .setColumnName("value")
        .setPrecision(38)
        .setScale(20)
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("text_value")
        .setIsNullable(true)
        .setLimit(4_000)
        .build())
      .addColumn(newDecimalColumnDefBuilder()
        .setColumnName("variation")
        .setPrecision(38)
        .setScale(20)
        .build())
      .addColumn(newBlobColumnDefBuilder()
        .setColumnName("measure_data")
        .build())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("update_marker")
        .setIsNullable(true)
        .setLimit(UUID_SIZE)
        .build())
      .addColumn(newBigIntegerColumnDefBuilder()
        .setColumnName("created_at")
        .setIsNullable(false)
        .build())
      .addColumn(newBigIntegerColumnDefBuilder()
        .setColumnName("updated_at")
        .setIsNullable(false)
        .build())
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("project_uuid")
        .setIsNullable(false)
        .setLimit(VarcharColumnDef.UUID_VARCHAR_SIZE)
        .build())
      .setUnique(false)
      .setTable(TABLE_NAME)
      .setName("live_measures_project")
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("component_uuid")
        .setIsNullable(false)
        .setLimit(VarcharColumnDef.UUID_VARCHAR_SIZE)
        .build())
      .addColumn(newIntegerColumnDefBuilder()
        .setColumnName("metric_id")
        .setIsNullable(false)
        .build())
      .setUnique(true)
      .setTable(TABLE_NAME)
      .setName("live_measures_component")
      .build());
  }
}
