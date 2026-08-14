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
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Adds {@code issues_selection_strategy} to {@code remediation_agent_jobs}, storing the strategy
 * ({@code "legacy"} naive FIFO vs {@code "llm"}) used to pick issues for a scheduled remediation job.
 * Nullable: only populated for scheduled jobs where issues are selected by the orchestrator, not for
 * manual-assignment or PR-triggered jobs where the caller supplies explicit issue keys.
 */
public class AddIssuesSelectionStrategyToRemediationAgentJobsTable extends DdlChange {

  static final String TABLE_NAME = "remediation_agent_jobs";
  static final String COLUMN_ISSUES_SELECTION_STRATEGY = "issues_selection_strategy";
  static final int ISSUES_SELECTION_STRATEGY_SIZE = 40;

  public AddIssuesSelectionStrategyToRemediationAgentJobsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_ISSUES_SELECTION_STRATEGY)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ISSUES_SELECTION_STRATEGY).setIsNullable(true)
            .setLimit(ISSUES_SELECTION_STRATEGY_SIZE).build())
          .build());
      }
    }
  }
}
