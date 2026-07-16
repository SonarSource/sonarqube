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

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.CreateTableChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code agent_sched_proc_issues} table: the remediation scheduler's permanent dedup
 * ledger. Once an issue has been dispatched to the remediation agent for a project it is recorded
 * here and never dispatched again.
 *
 * <p>The table name is abbreviated to fit the migration framework's 25-character limit (the
 * standalone orchestrator's dev/test migration spells it {@code agent_schedule_processed_issues}).
 */
public class CreateAgentScheduleProcessedIssuesTable extends CreateTableChange {

  static final String TABLE_NAME = "agent_sched_proc_issues";

  static final String COLUMN_UUID = "uuid";
  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_ISSUE_KEY = "issue_key";
  static final String COLUMN_PROCESSED_AT = "processed_at";

  static final int UUID_SIZE = 40;
  static final int PROJECT_ID_SIZE = 400;
  static final int ISSUE_KEY_SIZE = 50;

  static final String INDEX_PROJECT_ISSUE = "uq_agent_sched_proc_issues";

  protected CreateAgentScheduleProcessedIssuesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_UUID).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_ID).setIsNullable(false).setLimit(PROJECT_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ISSUE_KEY).setIsNullable(false).setLimit(ISSUE_KEY_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_PROCESSED_AT).setIsNullable(false).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_PROJECT_ISSUE)
      .setUnique(true)
      .addColumn(COLUMN_PROJECT_ID, false)
      .addColumn(COLUMN_ISSUE_KEY, false)
      .build());
  }
}
