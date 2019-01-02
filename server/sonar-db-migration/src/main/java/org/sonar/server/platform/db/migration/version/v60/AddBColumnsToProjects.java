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
package org.sonar.server.platform.db.migration.version.v60;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class AddBColumnsToProjects extends DdlChange {

  private static final String TABLE_PROJECTS = "projects";

  public AddBColumnsToProjects(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(new AddColumnsBuilder(getDatabase().getDialect(), TABLE_PROJECTS)
      .addColumn(newBooleanColumnDefBuilder().setColumnName("b_changed").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_copy_component_uuid").setLimit(50).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_description").setLimit(2000).setIgnoreOracleUnit(true).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("b_enabled").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_language").setLimit(20).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_long_name").setLimit(500).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_module_uuid").setLimit(50).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_module_uuid_path").setLimit(1500).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_name").setLimit(500).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_path").setLimit(2000).setIgnoreOracleUnit(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("b_qualifier").setLimit(10).setIgnoreOracleUnit(true).build())
      .build());
  }

}
