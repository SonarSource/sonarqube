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
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code agent_schedules} table backing the AI remediation agent's scheduled workflow.
 *
 * <p>A single global row ({@code project_id = '*'}) is the system switch and carries the
 * {@code project_selection_mode}; any other {@code project_id} is a per-project row. Column shape
 * mirrors the standalone orchestrator's dev/test migration (sonarqube-unification), which points to
 * this framework as the canonical owner.
 */
public class CreateAgentSchedulesTable extends CreateTableChange {

  static final String TABLE_NAME = "agent_schedules";

  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_ENABLED = "enabled";
  static final String COLUMN_NEXT_RUN_AT = "next_run_at";
  static final String COLUMN_FREQUENCY = "frequency";
  static final String COLUMN_DAY_OF_WEEK = "day_of_week";
  static final String COLUMN_RUN_HOUR = "run_hour";
  static final String COLUMN_TIMEZONE = "timezone";
  static final String COLUMN_PROJECT_SELECTION_MODE = "project_selection_mode";
  static final String COLUMN_MAX_ISSUES_PER_RUN = "max_issues_per_run";
  static final String COLUMN_MAX_OPEN_PRS = "max_open_prs";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int PROJECT_ID_SIZE = 400;
  static final int FREQUENCY_SIZE = 10;
  static final int DAY_OF_WEEK_SIZE = 10;
  static final int TIMEZONE_SIZE = 50;
  static final int PROJECT_SELECTION_MODE_SIZE = 10;

  static final String INDEX_DUE = "idx_agent_schedules_due";

  protected CreateAgentSchedulesTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_ID).setIsNullable(false).setLimit(PROJECT_ID_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_ENABLED).setIsNullable(false).setDefaultValue(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_NEXT_RUN_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_FREQUENCY).setIsNullable(false).setLimit(FREQUENCY_SIZE).setDefaultValue("DAILY").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_DAY_OF_WEEK).setIsNullable(true).setLimit(DAY_OF_WEEK_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_RUN_HOUR).setIsNullable(false).setDefaultValue(2).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_TIMEZONE).setIsNullable(false).setLimit(TIMEZONE_SIZE).setDefaultValue("UTC").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_SELECTION_MODE).setIsNullable(true).setLimit(PROJECT_SELECTION_MODE_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_MAX_ISSUES_PER_RUN).setIsNullable(false).setDefaultValue(5).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_MAX_OPEN_PRS).setIsNullable(false).setDefaultValue(3).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    // Plain single-column btree so the scheduler's "due" query (next_run_at <= now) is portable
    // across PostgreSQL/Oracle/MSSQL; `enabled` is filtered in application code, not the index.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_DUE)
      .setUnique(false)
      .addColumn(COLUMN_NEXT_RUN_AT, false)
      .build());
  }
}
