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
 * Creates the {@code hunter_agent_runs} table: one row per completed hunter scan, holding the
 * per-scan state a later incremental scan needs. It lives next to {@code findings} on the hunter
 * capability rather than next to {@code agent_jobs}, because the component that decides a scan's run
 * mode is the capability, not the agent orchestrator that executes it.
 *
 * <p>Deliberately its own table rather than columns on {@code findings}: a scan that finds nothing
 * writes no {@code findings} row, and a scan that found nothing still analysed a commit and is a
 * valid baseline. Deriving the run from the findings would lose exactly those scans.
 *
 * <p>{@code analyzed_commit_sha} also exists on {@code hunter_agent_jobs}, and the two are not
 * redundant. That column is the orchestrator's own write-side record, set at clone time and read back
 * by its post-job step on a later poll; a row exists there for every <em>dispatched</em> job,
 * including ones that failed after cloning, which have a SHA and must never serve as a baseline. This
 * table holds only scans whose findings were persisted &mdash; exactly the set that may be a baseline
 * &mdash; and its value is the one carried on the completion payload, so the two cannot disagree.
 * Serving the baseline lookup from {@code hunter_agent_jobs} instead is not possible: it carries
 * neither the resolved org/project/branch identity nor a scan timestamp, so the query would have to
 * join {@code agent_jobs} (shared with the remediation agent) for the branch key and the start time,
 * resolve that key to a branch id at query time, and filter on the orchestrator's job lifecycle.
 *
 * <p>Portable type choices, consistent with the other unified capability tables ({@code findings},
 * {@code agent_jobs}, {@code hunter_agent_jobs}): uuids as {@code VARCHAR(40)}, timestamps as epoch
 * millis in {@code BIGINT}, no native ENUM types and no {@code CHECK} constraints. {@code job_id} is
 * a logical reference to {@code agent_jobs.id}; as elsewhere in SonarQube no physical foreign key is
 * created and referential integrity is enforced in the application layer.
 */
public class CreateHunterAgentRunsTable extends CreateTableChange {

  static final String TABLE_NAME = "hunter_agent_runs";

  static final String COLUMN_ID = "id";
  static final String COLUMN_JOB_ID = "job_id";
  static final String COLUMN_ORG_ID = "org_id";
  static final String COLUMN_PROJECT_ID = "project_id";
  static final String COLUMN_BRANCH_ID = "branch_id";
  static final String COLUMN_ANALYZED_COMMIT_SHA = "analyzed_commit_sha";
  static final String COLUMN_ANALYZED_AT = "analyzed_at";
  static final String COLUMN_CREATED_AT = "created_at";

  static final int ID_SIZE = 40;
  static final int JOB_ID_SIZE = 40;
  static final int ORG_ID_SIZE = 40;
  static final int PROJECT_ID_SIZE = 40;
  static final int BRANCH_ID_SIZE = 40;
  // A git SHA-1 object id. Repositories using git's SHA-256 object format would need 64.
  static final int ANALYZED_COMMIT_SHA_SIZE = 40;

  static final String INDEX_JOB_ID = "uq_hunter_agent_runs_job_id";
  static final String INDEX_BRANCH_ANALYZED = "idx_hunter_runs_br_analyzed";

  protected CreateHunterAgentRunsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_JOB_ID).setIsNullable(false).setLimit(JOB_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ORG_ID).setIsNullable(false).setLimit(ORG_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_ID).setIsNullable(false).setLimit(PROJECT_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_BRANCH_ID).setIsNullable(false).setLimit(BRANCH_ID_SIZE).build())
      // The commit the analysed archive was taken at. Nullable: a scan whose clone reported no commit
      // has no baseline to offer, and a fabricated placeholder would be indistinguishable from a real
      // one to the next scan.
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ANALYZED_COMMIT_SHA).setIsNullable(true)
        .setLimit(ANALYZED_COMMIT_SHA_SIZE).build())
      // When the scan ran, as reported by the orchestrator. Ordering key of the baseline lookup, and
      // deliberately not created_at: the result fire-back is at-least-once, so a result re-posted
      // after a transient publish failure would otherwise outrank a scan that started later and the
      // next incremental scan would diff against the older commit.
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_ANALYZED_AT).setIsNullable(false).build())
      // Row bookkeeping only. Never an ordering key.
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .build());

    // One run per job. The fire-back is at-least-once by design, so a redelivered result must update
    // the existing row rather than add a second one; the capability's upsert conflicts on this index,
    // which makes it load-bearing rather than an optimisation.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_JOB_ID)
      .setUnique(true)
      .addColumn(COLUMN_JOB_ID, false)
      .build());

    // Supports the only read this table has: the most recent scan of a branch, which is how an
    // incremental scan locates its baseline. Keyed by branch, not by the previous job.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_BRANCH_ANALYZED)
      .setUnique(false)
      .addColumn(COLUMN_ORG_ID, false)
      .addColumn(COLUMN_PROJECT_ID, false)
      .addColumn(COLUMN_BRANCH_ID, false)
      .addColumn(COLUMN_ANALYZED_AT, false, true)
      .build());
  }
}
