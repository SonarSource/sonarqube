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
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code agentic_job} table backing the Agent Orchestrator's job store (both the Hunter
 * and Remediation agent flows persist rows here; remediation-only columns are left {@code NULL} for
 * Hunter jobs). Column shape mirrors the standalone orchestrator's dev/test migration
 * (sonarqube-unification: {@code V1__create_agentic_job.sql} + {@code V3__add_remediation_columns.sql}),
 * which points to this framework as the canonical owner.
 */
public class CreateAgenticJobTable extends CreateTableChange {

  static final String TABLE_NAME = "agentic_job";

  static final String COLUMN_JOB_ID = "job_id";
  static final String COLUMN_PROJECT_KEY = "project_key";
  static final String COLUMN_REPOSITORY_URL = "repository_url";
  static final String COLUMN_BRANCH = "branch";
  static final String COLUMN_REVISION = "revision";
  static final String COLUMN_ANALYSIS_TYPE = "analysis_type";
  static final String COLUMN_STATUS = "status";
  static final String COLUMN_SUB_STATUS = "sub_status";
  static final String COLUMN_FINDINGS_COUNT = "findings_count";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";
  static final String COLUMN_AGENT_TYPE = "agent_type";
  static final String COLUMN_ISSUE_KEYS = "issue_keys";
  static final String COLUMN_FLOW = "flow";
  static final String COLUMN_STARTED_AT = "started_at";
  static final String COLUMN_PULL_REQUEST_URL = "pull_request_url";
  static final String COLUMN_PULL_REQUEST_KEY = "pull_request_key";
  static final String COLUMN_AGENT_BRANCH = "agent_branch";
  static final String COLUMN_HEAD_SHA = "head_sha";
  static final String COLUMN_ISSUE_OUTCOMES = "issue_outcomes";
  static final String COLUMN_FINISHED_AT = "finished_at";

  static final int JOB_ID_SIZE = 40;
  static final int PROJECT_KEY_SIZE = 400;
  static final int REPOSITORY_URL_SIZE = 2000;
  static final int BRANCH_SIZE = 255;
  static final int REVISION_SIZE = 255;
  static final int ANALYSIS_TYPE_SIZE = 20;
  static final int STATUS_SIZE = 20;
  static final int SUB_STATUS_SIZE = 100;
  static final int AGENT_TYPE_SIZE = 20;
  static final int FLOW_SIZE = 20;
  static final int PULL_REQUEST_URL_SIZE = 2000;
  static final int PULL_REQUEST_KEY_SIZE = 255;
  static final int AGENT_BRANCH_SIZE = 255;
  static final int HEAD_SHA_SIZE = 40;

  static final String INDEX_STATUS = "agentic_job_status";
  static final String INDEX_PROJECT_STATUS = "idx_agentic_job_project_status";

  protected CreateAgenticJobTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_JOB_ID).setIsNullable(false).setLimit(JOB_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_KEY).setIsNullable(false).setLimit(PROJECT_KEY_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_REPOSITORY_URL).setIsNullable(false).setLimit(REPOSITORY_URL_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH).setIsNullable(true).setLimit(BRANCH_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_REVISION).setIsNullable(true).setLimit(REVISION_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ANALYSIS_TYPE).setIsNullable(false).setLimit(ANALYSIS_TYPE_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_STATUS).setIsNullable(false).setLimit(STATUS_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SUB_STATUS).setIsNullable(true).setLimit(SUB_STATUS_SIZE).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName(COLUMN_FINDINGS_COUNT).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_AGENT_TYPE).setIsNullable(true).setLimit(AGENT_TYPE_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_ISSUE_KEYS).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_FLOW).setIsNullable(true).setLimit(FLOW_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_STARTED_AT).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PULL_REQUEST_URL).setIsNullable(true).setLimit(PULL_REQUEST_URL_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PULL_REQUEST_KEY).setIsNullable(true).setLimit(PULL_REQUEST_KEY_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_AGENT_BRANCH).setIsNullable(true).setLimit(AGENT_BRANCH_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_HEAD_SHA).setIsNullable(true).setLimit(HEAD_SHA_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_ISSUE_OUTCOMES).setIsNullable(true).build())
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
      .addColumn(COLUMN_PROJECT_KEY, false)
      .addColumn(COLUMN_STATUS, false)
      .addColumn(COLUMN_CREATED_AT, false, true)
      .build());
  }
}
