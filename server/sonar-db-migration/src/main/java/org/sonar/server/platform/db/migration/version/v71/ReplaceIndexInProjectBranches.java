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
package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class ReplaceIndexInProjectBranches extends DdlChange {

  static final String TABLE_NAME = "project_branches";
  private static final String OLD_INDEX_NAME = "project_branches_kee";
  static final String NEW_INDEX_NAME = "project_branches_kee_key_type";

  static final VarcharColumnDef PROJECT_UUID_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("project_uuid")
    .setIsNullable(false)
    .setLimit(50)
    .build();

  static final VarcharColumnDef KEE_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("kee")
    .setIsNullable(false)
    .setLimit(255)
    .build();

  static final VarcharColumnDef KEY_TYPE_COLUMN = VarcharColumnDef.newVarcharColumnDefBuilder()
    .setColumnName("key_type")
    .setIsNullable(false)
    .setLimit(12)
    .build();

  public ReplaceIndexInProjectBranches(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_NAME)
      .setName(OLD_INDEX_NAME)
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .addColumn(PROJECT_UUID_COLUMN)
      .addColumn(KEE_COLUMN)
      .addColumn(KEY_TYPE_COLUMN)
      .setUnique(true)
      .setTable(TABLE_NAME)
      .setName(NEW_INDEX_NAME)
      .build()
    );
  }
}
