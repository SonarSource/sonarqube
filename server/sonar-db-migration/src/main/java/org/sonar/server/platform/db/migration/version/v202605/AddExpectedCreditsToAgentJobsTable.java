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
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;

/** Adds the admission-time credit cost. Nullable for existing and non-Hunter jobs. */
public class AddExpectedCreditsToAgentJobsTable extends DdlChange {

  static final String TABLE_NAME = "agent_jobs";
  static final String COLUMN_EXPECTED_CREDITS = "expected_credits";

  public AddExpectedCreditsToAgentJobsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_EXPECTED_CREDITS)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newIntegerColumnDefBuilder()
            .setColumnName(COLUMN_EXPECTED_CREDITS)
            .setIsNullable(true)
            .build())
          .build());
      }
    }
  }
}
