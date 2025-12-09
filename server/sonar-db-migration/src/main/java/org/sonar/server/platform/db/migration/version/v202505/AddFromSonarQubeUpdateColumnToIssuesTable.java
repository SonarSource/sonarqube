/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202505;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;

public class AddFromSonarQubeUpdateColumnToIssuesTable extends DdlChange {
  static final String ISSUES_TABLE_NAME = "issues";
  static final String FROM_SONARQUBE_UPDATE_COLUMN_NAME = "from_sonarqube_update";

  public AddFromSonarQubeUpdateColumnToIssuesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (var connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, ISSUES_TABLE_NAME, FROM_SONARQUBE_UPDATE_COLUMN_NAME)) {
        var columnDef = BooleanColumnDef.newBooleanColumnDefBuilder()
          .setColumnName(FROM_SONARQUBE_UPDATE_COLUMN_NAME)
          .setIsNullable(false)
          .setDefaultValue(false)
          .build();
        context.execute(new AddColumnsBuilder(getDialect(), ISSUES_TABLE_NAME)
          .addColumn(columnDef)
          .build());
      }
    }
  }
}
