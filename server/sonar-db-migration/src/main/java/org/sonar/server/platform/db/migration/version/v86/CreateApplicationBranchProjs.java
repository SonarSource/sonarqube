/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v86;

import java.sql.SQLException;
import org.sonar.core.util.Uuids;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateApplicationBranchProjs extends DdlChange {
  public CreateApplicationBranchProjs(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new CreateTableBuilder(getDialect(), "app_branch_project_branch")
      .addColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setIsNullable(false).setLimit(Uuids.MAX_LENGTH).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("application_uuid").setIsNullable(false).setLimit(Uuids.MAX_LENGTH).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("application_branch_uuid").setIsNullable(false).setLimit(Uuids.MAX_LENGTH).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("project_uuid").setIsNullable(false).setLimit(Uuids.MAX_LENGTH).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("project_branch_uuid").setIsNullable(false).setLimit(Uuids.MAX_LENGTH).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
  }

}
