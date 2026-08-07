/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.Connection;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AddColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.db.DatabaseUtils.tableColumnExists;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;

/**
 * Adds {@code selected_project_ids} to {@code agent_schedules}, storing SELECTED-mode project
 * selection as a JSON array of project keys directly on the global row, decoupled from
 * per-project override rows.
 */
public class AddSelectedProjectIdsToAgentSchedulesTable extends DdlChange {

  static final String TABLE_NAME = "agent_schedules";
  static final String COLUMN_SELECTED_PROJECT_IDS = "selected_project_ids";

  public AddSelectedProjectIdsToAgentSchedulesTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_SELECTED_PROJECT_IDS)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_SELECTED_PROJECT_IDS).setIsNullable(true).build())
          .build());
      }
    }
  }
}