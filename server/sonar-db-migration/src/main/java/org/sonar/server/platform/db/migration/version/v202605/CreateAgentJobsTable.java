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
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code agent_jobs} table backing the Agent Orchestrator's normalized job store. This
 * table holds the columns common to every agent flow (both Hunter and Remediation); the
 * remediation-only columns live in the sibling {@code remediation_agent_jobs} table, which
 * logically references {@code agent_jobs.id}. Together they replace the legacy denormalized
 * {@code agentic_job} table.
 *
 * <p>{@code project_id} is the project's stable identifier (not its mutable key), so a job stays
 * linkable to its project even after the project's key changes. The orchestrator resolves
 * key&harr;id via the Projects capability's {@code ProjectsService} rather than persisting the key.
 */
public class CreateAgentJobsTable extends CreateTableChange {

  static final String TABLE_NAME = "agent_jobs";

  static final String COLUMN_ID = "id";
  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_BRANCH = "branch";
  static final String COLUMN_REPOSITORY_URL = "repository_url";
  static final String COLUMN_REVISION = "revision";
  static final String COLUMN_AGENT_TYPE = "agent_type";
  static final String COLUMN_WORKFLOW_TYPE = "workflow_type";
  static final String COLUMN_ANALYSIS_TYPE = "analysis_type";
  static final String COLUMN_STATUS = "status";
  static final String COLUMN_SUBSTATUS = "substatus";
  static final String COLUMN_ERROR_KEY = "error_key";
  static final String COLUMN_FINDINGS_COUNT = "findings_count";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";
  static final String COLUMN_STARTED_AT = "started_at";
  static final String COLUMN_FINISHED_AT = "finished_at";

  static final int ID_SIZE = 40;
  static final int PROJECT_ID_SIZE = 40;
  static final int BRANCH_SIZE = 255;
  static final int REPOSITORY_URL_SIZE = 2000;
  static final int REVISION_SIZE = 255;
  static final int AGENT_TYPE_SIZE = 20;
  static final int WORKFLOW_TYPE_SIZE = 40;
  static final int ANALYSIS_TYPE_SIZE = 20;
  static final int STATUS_SIZE = 20;
  static final int SUBSTATUS_SIZE = 100;
  static final int ERROR_KEY_SIZE = 255;

  static final String INDEX_STATUS = "idx_agent_jobs_status";
  static final String INDEX_PROJECT_STATUS = "idx_agent_jobs_project_status";

  protected CreateAgentJobsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_ID).setIsNullable(false).setLimit(PROJECT_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH).setIsNullable(true).setLimit(BRANCH_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_REPOSITORY_URL).setIsNullable(false).setLimit(REPOSITORY_URL_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_REVISION).setIsNullable(true).setLimit(REVISION_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_AGENT_TYPE).setIsNullable(false).setLimit(AGENT_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_WORKFLOW_TYPE).setIsNullable(true).setLimit(WORKFLOW_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ANALYSIS_TYPE).setIsNullable(false).setLimit(ANALYSIS_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_STATUS).setIsNullable(false).setLimit(STATUS_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SUBSTATUS).setIsNullable(true).setLimit(SUBSTATUS_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ERROR_KEY).setIsNullable(true).setLimit(ERROR_KEY_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_FINDINGS_COUNT).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_STARTED_AT).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_FINISHED_AT).setIsNullable(true).build())
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_STATUS)
      .setUnique(false)
      .addColumn(COLUMN_STATUS, false)
      .build());

    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_PROJECT_STATUS)
      .setUnique(false)
      .addColumn(COLUMN_PROJECT_ID, false)
      .addColumn(COLUMN_STATUS, false)
      .addColumn(COLUMN_CREATED_AT, false, true)
      .build());
  }
}
