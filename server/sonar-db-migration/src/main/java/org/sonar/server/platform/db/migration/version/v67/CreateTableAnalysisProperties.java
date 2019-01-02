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
package org.sonar.server.platform.db.migration.version.v67;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.def.ClobColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateTableAnalysisProperties extends DdlChange {
  private static final String TABLE_NAME = "analysis_properties";
  private static final String SNAPSHOT_UUID = "snapshot_uuid";
  private static final VarcharColumnDef SNAPSHOT_UUID_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName(SNAPSHOT_UUID)
    .setIsNullable(false)
    .setLimit(VarcharColumnDef.UUID_SIZE)
    .build();

  public CreateTableAnalysisProperties(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("uuid")
        .setIsNullable(false)
        .setLimit(VarcharColumnDef.UUID_SIZE)
        .build())
      .addColumn(SNAPSHOT_UUID_COLUMN)
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("kee")
        .setIsNullable(false)
        .setLimit(512)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("text_value")
        .setIsNullable(true)
        .setLimit(VarcharColumnDef.MAX_SIZE)
        .build())
      .addColumn(ClobColumnDef.newClobColumnDefBuilder()
        .setColumnName("clob_value")
        .setIsNullable(true)
        .build())
      .addColumn(BooleanColumnDef.newBooleanColumnDefBuilder()
        .setColumnName("is_empty")
        .setIsNullable(false)
        .build())
      .addColumn(BigIntegerColumnDef.newBigIntegerColumnDefBuilder()
        .setColumnName("created_at")
        .setIsNullable(false)
        .build())
      .build()
    );

    context.execute(new CreateIndexBuilder(getDialect())
      .addColumn(SNAPSHOT_UUID_COLUMN)
      .setUnique(false)
      .setTable(TABLE_NAME)
      .setName("ix_snapshot_uuid")
      .build()
    );
  }
}
