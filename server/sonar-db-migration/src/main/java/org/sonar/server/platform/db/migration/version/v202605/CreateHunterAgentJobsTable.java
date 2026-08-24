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

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code hunter_agent_jobs} table holding the hunter-only attributes of an agent job,
 * the sibling of {@code remediation_agent_jobs}. Each row logically references a row in
 * {@code agent_jobs} via {@code job_id}; SonarQube does not create physical foreign keys, so
 * referential integrity is enforced in the application layer. The {@code job_id} column is unique
 * &mdash; there is at most one hunter extension per agent job.
 *
 * <p>{@code analyzed_commit_sha} is the commit the agent actually analysed: the SHA the orchestrator
 * resolved from the working tree when it cloned the repository. Distinct from {@code
 * agent_jobs.revision}, which carries the revision <em>requested</em> in the job request and is
 * frequently null &mdash; a scheduled run asks for a branch, not a commit. An incremental hunter scan
 * diffs against the analysed SHA of the previous run on the same branch, so the requested value
 * cannot stand in for it.
 *
 * <p>The column is nullable: a repository with no commits clones fine but has no HEAD to resolve, and
 * a job that fails before its clone completes never resolves one either. Both are jobs that simply
 * cannot serve as an incremental baseline, which is different from a job that was never recorded.
 */
public class CreateHunterAgentJobsTable extends CreateTableChange {

  static final String TABLE_NAME = "hunter_agent_jobs";

  static final String COLUMN_ID = "id";
  static final String COLUMN_JOB_ID = "job_id";
  static final String COLUMN_ANALYZED_COMMIT_SHA = "analyzed_commit_sha";

  static final int ID_SIZE = 40;
  static final int JOB_ID_SIZE = 40;
  // A git SHA-1 object id. Repositories using git's SHA-256 object format would need 64.
  static final int ANALYZED_COMMIT_SHA_SIZE = 40;

  static final String INDEX_JOB_ID = "uq_hunter_agent_jobs_job_id";

  protected CreateHunterAgentJobsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_JOB_ID).setIsNullable(false).setLimit(JOB_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ANALYZED_COMMIT_SHA).setIsNullable(true)
        .setLimit(ANALYZED_COMMIT_SHA_SIZE).build())
      .build());

    // At most one hunter extension per agent job.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_JOB_ID)
      .setUnique(true)
      .addColumn(COLUMN_JOB_ID, false)
      .build());
  }
}
