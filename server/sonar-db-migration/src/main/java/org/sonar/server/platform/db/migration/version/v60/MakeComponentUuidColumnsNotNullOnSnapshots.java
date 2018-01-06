/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_VARCHAR_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class MakeComponentUuidColumnsNotNullOnSnapshots extends DdlChange {

  private static final String TABLE_SNAPSHOTS = "snapshots";

  public MakeComponentUuidColumnsNotNullOnSnapshots(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef componentUuid = newUuidColumn("component_uuid");
    VarcharColumnDef rootComponentUuid = newUuidColumn("root_component_uuid");
    context.execute(new AlterColumnsBuilder(getDatabase().getDialect(), TABLE_SNAPSHOTS)
      .updateColumn(componentUuid)
      .updateColumn(rootComponentUuid)
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_SNAPSHOTS)
      .setName("snapshot_component")
      .addColumn(componentUuid)
      .build());
    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_SNAPSHOTS)
      .setName("snapshot_root_component")
      .addColumn(rootComponentUuid)
      .build());
  }

  private static VarcharColumnDef newUuidColumn(String columnName) {
    return newVarcharColumnDefBuilder()
      .setColumnName(columnName)
      .setLimit(UUID_VARCHAR_SIZE)
      .setIsNullable(false)
      .setIgnoreOracleUnit(true)
      .build();
  }

}
