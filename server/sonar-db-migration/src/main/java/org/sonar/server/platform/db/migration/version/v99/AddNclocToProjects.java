/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v99;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

public class AddNclocToProjects extends DdlChange {

  public static final String PROJECT_TABLE_NAME = "projects";
  public static final String NCLOC_COLUMN_NAME = "ncloc";

  public AddNclocToProjects(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    if (checkIfColumnExists()) {
      return;
    }
    BigIntegerColumnDef columnDef = BigIntegerColumnDef.newBigIntegerColumnDefBuilder().setColumnName(NCLOC_COLUMN_NAME).setIsNullable(true).build();
    String request = new AddColumnsBuilder(getDialect(), PROJECT_TABLE_NAME).addColumn(columnDef).build();
    context.execute(request);
  }

  public boolean checkIfColumnExists() throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      if (DatabaseUtils.tableColumnExists(connection, PROJECT_TABLE_NAME, NCLOC_COLUMN_NAME)) {
        return true;
      }
    }
    return false;
  }
}
