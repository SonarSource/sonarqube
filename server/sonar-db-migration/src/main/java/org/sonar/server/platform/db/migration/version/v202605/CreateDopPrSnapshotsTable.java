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
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Creates the {@code dop_pr_snapshots} table backing the DOP-events polling job (MMF-5874). One row
 * per agent-originated pull request tracked across scheduled polls: the columns hold the last
 * observed state (open/closed/merged, check-run conclusions, requested reviewers) so each poll can
 * diff the freshly-fetched ALM state against this row and emit only the events for what changed.
 *
 * <p>Identity is {@code (project_uuid, pull_request_id)}: a SonarQube project has exactly one DevOps
 * binding → one repo, and the project uuid is globally unique and stable. The repo's owner/slug,
 * platform and access token are therefore NOT stored here — the polling job re-resolves them from
 * {@code project_uuid} each run, so tracking survives a repo <em>rename</em> or <em>transfer</em>
 * (still the same repo, re-resolved fresh). Known limitation, accepted for v1: it does NOT survive a
 * <em>rebind</em> to a different repository — rebinding updates {@code project_alm_settings} in place
 * for the same {@code project_uuid}, so the project's stale rows here would then be diffed against the
 * newly-bound repo. Rebinding to a different repo is rare enough that we don't clean those rows up today.
 *
 * <p>{@code ai_agent_job_id} is a logical reference to {@code agent_jobs.id} (no physical FK, matching
 * the repo convention used by {@code remediation_agent_jobs.job_id}); it is nullable because a
 * snapshot can outlive its originating job record. {@code check_runs_json} /
 * {@code requested_reviewers_json} are JSON-encoded arrays, mirroring
 * {@code remediation_agent_jobs.issue_keys}'s JSON-string-array convention. {@code terminal} freezes a
 * row once its PR reaches merged/closed state — the polling job stops fetching state for it.
 */
public class CreateDopPrSnapshotsTable extends CreateTableChange {

  static final String TABLE_NAME = "dop_pr_snapshots";

  static final String COLUMN_ID = "id";
  static final String COLUMN_PROJECT_UUID = "project_uuid";
  static final String COLUMN_PULL_REQUEST_ID = "pull_request_id";
  static final String COLUMN_AI_AGENT_JOB_ID = "ai_agent_job_id";
  static final String COLUMN_WORKFLOW_ORIGIN = "workflow_origin";
  static final String COLUMN_PR_STATE = "pr_state";
  static final String COLUMN_CHECK_RUNS_JSON = "check_runs_json";
  static final String COLUMN_REQUESTED_REVIEWERS_JSON = "requested_reviewers_json";
  static final String COLUMN_TERMINAL = "terminal";
  static final String COLUMN_CREATED_AT = "created_at";
  static final String COLUMN_UPDATED_AT = "updated_at";

  static final int ID_SIZE = 40;
  static final int PROJECT_UUID_SIZE = 40;
  static final int PULL_REQUEST_ID_SIZE = 255;
  static final int AI_AGENT_JOB_ID_SIZE = 40;
  static final int WORKFLOW_ORIGIN_SIZE = 30;
  static final int PR_STATE_SIZE = 20;

  static final String INDEX_PR = "uq_dop_pr_snapshots_pr";
  static final String INDEX_NON_TERMINAL = "idx_dop_pr_snap_term_upd";

  protected CreateDopPrSnapshotsTable(Database db) {
    super(db, TABLE_NAME);
  }

  @Override
  public void execute(Context context, String tableName) throws SQLException {
    var dialect = getDialect();

    context.execute(new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ID).setIsNullable(false).setLimit(ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PROJECT_UUID).setIsNullable(false).setLimit(PROJECT_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PULL_REQUEST_ID).setIsNullable(false).setLimit(PULL_REQUEST_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_AI_AGENT_JOB_ID).setIsNullable(true).setLimit(AI_AGENT_JOB_ID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_WORKFLOW_ORIGIN).setIsNullable(true).setLimit(WORKFLOW_ORIGIN_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PR_STATE).setIsNullable(false).setLimit(PR_STATE_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_CHECK_RUNS_JSON).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(COLUMN_REQUESTED_REVIEWERS_JSON).setIsNullable(true).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName(COLUMN_TERMINAL).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_CREATED_AT).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName(COLUMN_UPDATED_AT).setIsNullable(false).build())
      .build());

    // One tracked snapshot per (project, PR): a project maps to one repo, and re-running discovery for
    // an already-tracked PR must update the existing row, not create a duplicate.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_PR)
      .setUnique(true)
      .addColumn(COLUMN_PROJECT_UUID, false)
      .addColumn(COLUMN_PULL_REQUEST_ID, false)
      .build());

    // The polling job's only fan-out query is "all non-terminal PRs, least-recently-polled first"
    // ({@code WHERE terminal = false ORDER BY updated_at}). Leading with terminal serves that filter,
    // and updated_at then serves the sort — so the query never scans the terminal rows that
    // accumulate over the table's lifetime. NOTE: this relies on the poller bumping updated_at on
    // *every* poll (it re-saves each snapshot even on a no-op poll), so here updated_at is the
    // last-polled time. If a future change made writes conditional on state actually changing, a
    // never-changing PR would keep its original timestamp and sort ahead of everything forever.
    context.execute(new CreateIndexBuilder(dialect)
      .setTable(tableName)
      .setName(INDEX_NON_TERMINAL)
      .setUnique(false)
      .addColumn(COLUMN_TERMINAL, false)
      .addColumn(COLUMN_UPDATED_AT, false)
      .build());
  }
}
