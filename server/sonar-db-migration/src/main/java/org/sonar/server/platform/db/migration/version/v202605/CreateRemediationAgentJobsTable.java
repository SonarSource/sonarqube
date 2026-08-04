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

import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code remediation_agent_jobs} table holding the remediation-only attributes of an
 * agent job. Each row logically references a row in {@code agent_jobs} via {@code job_id}; SonarQube
 * does not create physical foreign keys, so referential integrity is enforced in the application
 * layer. The {@code job_id} column is unique &mdash; there is at most one remediation extension per
 * agent job.
 */
public class CreateRemediationAgentJobsTable extends CreateTableChange {

  static final String TABLE_NAME = "remediation_agent_jobs";

  static final String COLUMN_ID = "id";
  static final String COLUMN_JOB_ID = "job_id";
  static final String COLUMN_DEVOPS_PLATFORM = "devops_platform";
  static final String COLUMN_SOURCE_PR_KEY = "source_pr_key";
  static final String COLUMN_RESULT_PR_KEY = "result_pr_key";
  static final String COLUMN_RESULT_BRANCH = "result_branch";
  static final String COLUMN_RESULT_COMMIT = "result_commit";
  static final String COLUMN_ISSUE_KEYS = "issue_keys";

  static final int ID_SIZE = 40;
  static final int JOB_ID_SIZE = 40;
  static final int DEVOPS_PLATFORM_SIZE = 30;
  static final int SOURCE_PR_KEY_SIZE = 255;
  static final int RESULT_PR_KEY_SIZE = 255;
  static final int RESULT_BRANCH_SIZE = 255;
  static final int RESULT_COMMIT_SIZE = 40;

  static final String INDEX_JOB_ID = "uq_rem_agent_jobs_job_id";

  protected CreateRemediationAgentJobsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_JOB_ID).setIsNullable(false).setLimit(JOB_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_DEVOPS_PLATFORM).setIsNullable(true).setLimit(DEVOPS_PLATFORM_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_SOURCE_PR_KEY).setIsNullable(true).setLimit(SOURCE_PR_KEY_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RESULT_PR_KEY).setIsNullable(true).setLimit(RESULT_PR_KEY_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RESULT_BRANCH).setIsNullable(true).setLimit(RESULT_BRANCH_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RESULT_COMMIT).setIsNullable(true).setLimit(RESULT_COMMIT_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_ISSUE_KEYS).setIsNullable(true).build())
      .build());

    // At most one remediation extension per agent job.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_JOB_ID)
      .setUnique(true)
      .addColumn(COLUMN_JOB_ID, false)
      .build());
  }
}
