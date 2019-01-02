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
package org.sonar.server.platform.db.migration.version.v66;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

import java.sql.SQLException;

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateTableCeTaskCharacteristics extends DdlChange {
  private static final String TABLE_NAME = "ce_task_characteristics";

  public CreateTableCeTaskCharacteristics(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef uuidColumn = newVarcharColumnDefBuilder()
      .setColumnName("uuid")
      .setLimit(UUID_SIZE)
      .setIsNullable(false)
      .setIgnoreOracleUnit(true)
      .build();
    VarcharColumnDef ceTaskUuidColumn = newVarcharColumnDefBuilder()
      .setColumnName("task_uuid")
      .setLimit(UUID_SIZE)
      .setIsNullable(false)
      .setIgnoreOracleUnit(true)
      .build();
    VarcharColumnDef keyColumn = newVarcharColumnDefBuilder()
      .setColumnName("kee")
      .setLimit(512)
      .setIsNullable(false)
      .setIgnoreOracleUnit(true)
      .build();
    VarcharColumnDef valueColumn = newVarcharColumnDefBuilder()
      .setColumnName("text_value")
      .setLimit(512)
      .setIsNullable(true)
      .setIgnoreOracleUnit(true)
      .build();

    context.execute(
      new CreateTableBuilder(getDialect(), TABLE_NAME)
        .addPkColumn(uuidColumn)
        .addColumn(ceTaskUuidColumn)
        .addColumn(keyColumn)
        .addColumn(valueColumn)
        .build());

    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName("ce_characteristics_" + ceTaskUuidColumn.getName())
        .addColumn(ceTaskUuidColumn)
        .setUnique(false)
        .build());
  }

}
