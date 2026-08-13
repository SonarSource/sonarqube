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
 * Adds {@code dop_user_id} to {@code remediation_agent_jobs}, storing the DevOps-platform external user
 * id ({@code users.external_id}) of the user who triggered the remediation. It is captured at trigger
 * time so it survives to the asynchronous PR-open event, where it is emitted on the
 * {@code AiAgentPrCreated} usage event. Nullable: only populated for manual-assignment jobs triggered
 * by a user provisioned via a DevOps platform; null for scheduled/PR-triggered and local users.
 */
public class AddDopUserIdToRemediationAgentJobsTable extends DdlChange {

  static final String TABLE_NAME = "remediation_agent_jobs";
  static final String COLUMN_DOP_USER_ID = "dop_user_id";
  static final int DOP_USER_ID_SIZE = 255;

  public AddDopUserIdToRemediationAgentJobsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_DOP_USER_ID)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_DOP_USER_ID).setIsNullable(true).setLimit(DOP_USER_ID_SIZE).build())
          .build());
      }
    }
  }
}
