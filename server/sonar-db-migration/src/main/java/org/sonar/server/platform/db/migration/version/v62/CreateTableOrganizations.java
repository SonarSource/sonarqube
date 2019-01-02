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
package org.sonar.server.platform.db.migration.version.v62;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableOrganizations extends DdlChange {

  private static final String TABLE_NAME = "organizations";

  public CreateTableOrganizations(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef keeColumn = newVarcharColumnDefBuilder().setColumnName("kee").setLimit(32).setIsNullable(false).setIgnoreOracleUnit(true).build();
    context.execute(
      new CreateTableBuilder(getDialect(), TABLE_NAME)
        .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setLimit(UUID_SIZE).setIsNullable(false).setIgnoreOracleUnit(true).build())
        .addColumn(keeColumn)
        .addColumn(newVarcharColumnDefBuilder().setColumnName("name").setLimit(64).setIsNullable(false).setIgnoreOracleUnit(true).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("description").setLimit(256).setIsNullable(true).setIgnoreOracleUnit(true).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("url").setLimit(256).setIsNullable(true).setIgnoreOracleUnit(true).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("avatar_url").setLimit(256).setIsNullable(true).setIgnoreOracleUnit(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName("organization_key")
      .addColumn(keeColumn)
      .build());
  }
}
