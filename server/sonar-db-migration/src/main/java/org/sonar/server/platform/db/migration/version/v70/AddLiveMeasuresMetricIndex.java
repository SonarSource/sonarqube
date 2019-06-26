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
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_VARCHAR_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Add this index only once table is populated to improve efficiency of populating the table.
 */
public class AddLiveMeasuresMetricIndex extends DdlChange {

  private static final String TABLE_NAME = "live_measures";

  public AddLiveMeasuresMetricIndex(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateIndexBuilder(getDialect())
      .addColumn(newVarcharColumnDefBuilder()
        .setColumnName("component_uuid")
        .setIsNullable(false)
        .setLimit(UUID_VARCHAR_SIZE)
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
