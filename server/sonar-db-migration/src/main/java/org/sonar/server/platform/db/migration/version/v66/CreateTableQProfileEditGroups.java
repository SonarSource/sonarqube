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

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.IntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;

public class CreateTableQProfileEditGroups extends DdlChange {

  private static final String TABLE_NAME = "qprofile_edit_groups";

  public CreateTableQProfileEditGroups(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    IntegerColumnDef groupColumn = IntegerColumnDef.newIntegerColumnDefBuilder()
      .setColumnName("group_id")
      .setIsNullable(false)
      .build();
    VarcharColumnDef qProfileUuidColumn = VarcharColumnDef.newVarcharColumnDefBuilder()
      .setColumnName("qprofile_uuid")
      .setIsNullable(false)
      .setLimit(255)
      .build();
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("uuid")
        .setIsNullable(false)
        .setLimit(40)
        .build())
      .addColumn(groupColumn)
      .addColumn(qProfileUuidColumn)
      .addColumn(newBigIntegerColumnDefBuilder()
        .setColumnName("created_at")
        .setIsNullable(false)
        .build())
      .build()
    );

    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName(TABLE_NAME + "_qprofile")
        .addColumn(qProfileUuidColumn)
        .setUnique(false)
        .build());
    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(TABLE_NAME)
        .setName(TABLE_NAME + "_unique")
        .addColumn(groupColumn)
        .addColumn(qProfileUuidColumn)
        .setUnique(true)
        .build());
  }
}
