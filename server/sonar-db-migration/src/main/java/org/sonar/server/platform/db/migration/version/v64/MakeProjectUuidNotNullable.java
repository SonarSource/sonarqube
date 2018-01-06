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
package org.sonar.server.platform.db.migration.version.v64;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.DropIndexBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class MakeProjectUuidNotNullable extends DdlChange {

  private static final String TABLE_PROJECTS = "projects";
  private static final String INDEX_PROJECTS_PROJECT_UUID = "projects_project_uuid";

  public MakeProjectUuidNotNullable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder()
      .setColumnName("project_uuid")
      .setLimit(50)
      .setIsNullable(false)
      .build();

    context.execute(new DropIndexBuilder(getDialect())
      .setTable(TABLE_PROJECTS)
      .setName(INDEX_PROJECTS_PROJECT_UUID)
      .build());

    context.execute(new AlterColumnsBuilder(getDialect(), TABLE_PROJECTS)
      .updateColumn(projectUuidCol)
      .build());

    context.execute(new CreateIndexBuilder(getDialect())
      .setTable(TABLE_PROJECTS)
      .setName(INDEX_PROJECTS_PROJECT_UUID)
      .setUnique(false).addColumn(projectUuidCol)
      .build());
  }

}
