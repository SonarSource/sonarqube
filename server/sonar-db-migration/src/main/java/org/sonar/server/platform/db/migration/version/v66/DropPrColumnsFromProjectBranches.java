/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class DropPrColumnsFromProjectBranches extends DdlChange {
  static final String PROJECT_BRANCHES_TABLE_NAME = "project_branches";

  public DropPrColumnsFromProjectBranches(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new DropIndexBuilder(getDialect()).setTable(PROJECT_BRANCHES_TABLE_NAME).setName("project_branches_kee").build());
    context.execute(new DropColumnsBuilder(getDialect(), PROJECT_BRANCHES_TABLE_NAME, "kee_type", "pull_request_title").build());
    context.execute(
      new CreateIndexBuilder(getDialect())
        .setTable(PROJECT_BRANCHES_TABLE_NAME)
        .setName("project_branches_kee")
        .setUnique(true)
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
        .build());
  }

}
