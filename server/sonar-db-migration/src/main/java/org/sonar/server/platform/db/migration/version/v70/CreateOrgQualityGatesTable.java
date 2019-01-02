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

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateOrgQualityGatesTable extends DdlChange {

  private static final String ORG_QUALITY_GATES = "org_quality_gates";
  private static final VarcharColumnDef ORGANIZATION_UUID_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("organization_uuid")
    .setIsNullable(false)
    .setLimit(VarcharColumnDef.UUID_SIZE)
    .build();
  private static final VarcharColumnDef QUALITY_GATE_UUID_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("quality_gate_uuid")
    .setIsNullable(false)
    .setLimit(VarcharColumnDef.UUID_SIZE)
    .build();

  private Database db;

  public CreateOrgQualityGatesTable(Database db) {
    super(db);
    this.db = db;
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (!tableExists()) {
      context.execute(new CreateTableBuilder(getDialect(), ORG_QUALITY_GATES)
        .addPkColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
          .setColumnName("uuid")
          .setIsNullable(false)
          .setLimit(VarcharColumnDef.UUID_SIZE)
          .build())
        .addColumn(ORGANIZATION_UUID_COLUMN)
        .addColumn(QUALITY_GATE_UUID_COLUMN)
        .build()
      );

      context.execute(new CreateIndexBuilder(getDialect())
        .addColumn(ORGANIZATION_UUID_COLUMN)
        .addColumn(QUALITY_GATE_UUID_COLUMN)
        .setUnique(true)
        .setTable(ORG_QUALITY_GATES)
        .setName("uniq_org_quality_gates")
        .build()
      );
    }
  }

  private boolean tableExists() throws SQLException {
    try (Connection connection = db.getDataSource().getConnection()) {
      return DatabaseUtils.tableExists(ORG_QUALITY_GATES, connection);
    }
  }
}
