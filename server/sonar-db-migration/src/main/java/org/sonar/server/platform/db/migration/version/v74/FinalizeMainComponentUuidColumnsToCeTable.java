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
package org.sonar.server.platform.db.migration.version.v74;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.sql.RenameColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public abstract class FinalizeMainComponentUuidColumnsToCeTable extends DdlChange {
  private static final VarcharColumnDef COLUMN_COMPONENT_UUID = newVarcharColumnDefBuilder()
    .setColumnName("component_uuid")
    .setLimit(VarcharColumnDef.UUID_SIZE)
    .setIsNullable(true)
    .build();
  private static final VarcharColumnDef COLUMN_MAIN_COMPONENT_UUID = newVarcharColumnDefBuilder()
    .setColumnName("main_component_uuid")
    .setLimit(VarcharColumnDef.UUID_SIZE)
    .setIsNullable(true)
    .build();
  private final String tableName;

  FinalizeMainComponentUuidColumnsToCeTable(Database db, String tableName) {
    super(db);
    this.tableName = tableName;
  }

  @Override
  public void execute(Context context) throws SQLException {
    // drop index on existing column COMPONENT_UUID
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(tableName + "_component_uuid")
      .build());
    // drop existing column
    context.execute(new DropColumnsBuilder(getDialect(), tableName, "component_uuid").build());

    // drop indexes on tmp columns
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(tableName + "_tmp_cpnt_uuid")
      .build());
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(tableName + "_tmp_main_cpnt_uuid")
      .build());

    // rename tmp columns
    context.execute(new RenameColumnsBuilder(getDialect(), tableName)
      .renameColumn("tmp_component_uuid", COLUMN_COMPONENT_UUID)
      .renameColumn("tmp_main_component_uuid", COLUMN_MAIN_COMPONENT_UUID)
      .build());

    // recreate indexes on renamed columns
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(tableName + "_component")
      .addColumn(COLUMN_COMPONENT_UUID)
      .setUnique(false)
      .build());
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(tableName)
      .setName(tableName + "_main_component")
      .addColumn(COLUMN_MAIN_COMPONENT_UUID)
      .setUnique(false)
      .build());
  }
}
