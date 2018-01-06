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
package org.sonar.server.platform.db.migration.version.v66;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class CreateTableProjectBranches extends DdlChange {

  private static final String TABLE_NAME = "project_branches";

  public CreateTableProjectBranches(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), TABLE_NAME)
      .addPkColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("uuid")
        .setIsNullable(false)
        .setLimit(50)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("project_uuid")
        .setIsNullable(false)
        .setLimit(50)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("kee")
        .setIsNullable(false)
        .setLimit(255)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("branch_type")
        .setIsNullable(true)
        .setLimit(5)
        .build())
      .addColumn(VarcharColumnDef.newVarcharColumnDefBuilder()
        .setColumnName("merge_branch_uuid")
        .setIsNullable(true)
        .setLimit(50)
        .build())
      .addColumn(BigIntegerColumnDef.newBigIntegerColumnDefBuilder()
        .setColumnName("created_at")
        .setIsNullable(false)
        .build())
      .addColumn(BigIntegerColumnDef.newBigIntegerColumnDefBuilder()
        .setColumnName("updated_at")
        .setIsNullable(false)
        .build())
      .build()
    );
  }
}
