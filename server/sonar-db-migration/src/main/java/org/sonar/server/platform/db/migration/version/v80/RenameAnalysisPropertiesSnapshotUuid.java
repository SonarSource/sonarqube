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
package org.sonar.server.platform.db.migration.version.v80;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.sql.RenameColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

public class RenameAnalysisPropertiesSnapshotUuid extends DdlChange {
  private static final VarcharColumnDef ANALYSIS_UUID_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("analysis_uuid")
    .setIsNullable(false)
    .setLimit(UUID_SIZE)
    .build();

  public RenameAnalysisPropertiesSnapshotUuid(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    String tableName = "analysis_properties";

    context.execute(new DropIndexBuilder(getDialect())
      .setTable(tableName)
      .setName("ix_snapshot_uuid")
      .build());

    context.execute(new RenameColumnsBuilder(getDialect(), tableName)
      .renameColumn("snapshot_uuid", ANALYSIS_UUID_COLUMN)
      .build());

    context.execute(new CreateIndexBuilder()
      .setTable(tableName)
      .setName("analysis_properties_analysis")
      .setUnique(false)
      .addColumn(ANALYSIS_UUID_COLUMN)
      .build());
  }
}
