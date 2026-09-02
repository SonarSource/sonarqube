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
import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;

/**
 * Adds {@code analyzed_at} to {@code hunter_agent_jobs}.
 */
public class AddAnalyzedAtToHunterAgentJobsTable extends DdlChange {

  static final String TABLE_NAME = "hunter_agent_jobs";
  static final String COLUMN_ANALYZED_AT = "analyzed_at";

  public AddAnalyzedAtToHunterAgentJobsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_ANALYZED_AT)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          // Permanently nullable: earlier jobs cannot be backfilled because their recorded source-preparation time was never stored.
          .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_ANALYZED_AT).setIsNullable(true).build())
          .build());
      }
    }

    // No index: this value is read only through a single-job primary-key/job_id lookup.
  }
}
