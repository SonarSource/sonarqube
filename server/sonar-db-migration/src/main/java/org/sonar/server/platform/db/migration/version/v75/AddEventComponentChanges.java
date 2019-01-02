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
package org.sonar.server.platform.db.migration.version.v75;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.SupportsBlueGreen;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_VARCHAR_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

@SupportsBlueGreen
public class AddEventComponentChanges extends DdlChange {
  private static final String TABLE_NAME = "event_component_changes";
  private static final VarcharColumnDef COLUMN_UUID = newVarcharColumnDefBuilder()
    .setColumnName("uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();
  private static final VarcharColumnDef COLUMN_EVENT_UUID = newVarcharColumnDefBuilder()
    .setColumnName("event_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();
  private static final VarcharColumnDef COLUMN_EVENT_ANALYSIS_UUID = newVarcharColumnDefBuilder()
    .setColumnName("event_analysis_uuid")
    .setIsNullable(false)
    .setLimit(UUID_VARCHAR_SIZE)
    .build();
  private static final VarcharColumnDef COLUMN_EVENT_COMPONENT_UUID = newVarcharColumnDefBuilder()
    .setColumnName("event_component_uuid")
    .setIsNullable(false)
    .setLimit(UUID_VARCHAR_SIZE)
    .build();
  private static final VarcharColumnDef COLUMN_CHANGE_CATEGORY = newVarcharColumnDefBuilder()
    .setColumnName("change_category")
    .setIsNullable(false)
    .setLimit(12)
    .build();
  private static final VarcharColumnDef COLUMN_COMPONENT_UUID = newVarcharColumnDefBuilder()
    .setColumnName("component_uuid")
    .setIsNullable(false)
    .setLimit(UUID_VARCHAR_SIZE)
    .build();
  private static final VarcharColumnDef COLUMN_COMPONENT_KEY = newVarcharColumnDefBuilder()
    .setColumnName("component_key")
    .setIsNullable(false)
    .setLimit(400)
    .build();
  private static final VarcharColumnDef COLUMN_COMPONENT_NAME = newVarcharColumnDefBuilder()
    .setColumnName("component_name")
    .setIsNullable(false)
    .setLimit(2000)
    .build();
  private static final VarcharColumnDef COLUMN_COMPONENT_BRANCH_KEY = newVarcharColumnDefBuilder()
    .setColumnName("component_branch_key")
    .setIsNullable(true)
    .setLimit(255)
    .build();
  private static final BigIntegerColumnDef COLUMN_CREATED_AT = newBigIntegerColumnDefBuilder()
    .setColumnName("created_at")
    .setIsNullable(false)
    .build();

  public AddEventComponentChanges(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    CreateTableBuilder createTableBuilder = new CreateTableBuilder(getDialect(), TABLE_NAME);
    if (!createTableBuilder.tableExists(getDatabase())) {
      context.execute(createTableBuilder
        .addPkColumn(COLUMN_UUID)
        .addColumn(COLUMN_EVENT_UUID)
        .addColumn(COLUMN_EVENT_COMPONENT_UUID)
        .addColumn(COLUMN_EVENT_ANALYSIS_UUID)
        .addColumn(COLUMN_CHANGE_CATEGORY)
        .addColumn(COLUMN_COMPONENT_UUID)
        .addColumn(COLUMN_COMPONENT_KEY)
        .addColumn(COLUMN_COMPONENT_NAME)
        .addColumn(COLUMN_COMPONENT_BRANCH_KEY)
        .addColumn(COLUMN_CREATED_AT)
        .build());

      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName(TABLE_NAME + "_unique")
        .addColumn(COLUMN_EVENT_UUID)
        .addColumn(COLUMN_CHANGE_CATEGORY)
        .addColumn(COLUMN_COMPONENT_UUID)
        .setUnique(true)
        .build());
      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName("event_cpnt_changes_cpnt")
        .addColumn(COLUMN_EVENT_COMPONENT_UUID)
        .setUnique(false)
        .build());
      context.execute(new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName("event_cpnt_changes_analysis")
        .addColumn(COLUMN_EVENT_ANALYSIS_UUID)
        .setUnique(false)
        .build());
    }

  }
}
