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
 * Records which earlier job an incremental Hunter job based its scan on: that job's id, and the
 * commit its clone was taken at.
 *
 * <p>An incremental Hunter scan is only meaningful relative to a baseline. The baseline belongs to
 * the Hunter-specific part of the job, alongside the analysed commit. Keeping it there avoids adding
 * Hunter-only state to the generic {@code agent_jobs} table.
 *
 * <p>{@code previous_job_commit_sha} duplicates a value the pointed-at job also holds, deliberately.
 * The diff the agent is handed is computed between that commit and this job's own, so the sha this
 * job actually used has to survive independently of the row it was copied from; resolving it through
 * the pointer would make an old job's meaning change when its baseline is purged.
 *
 * <p>Both nullable, and in practice both or neither. A FULL scan has no previous job, a job created
 * before these columns existed has none recorded, and an incremental job that found no usable baseline
 * is refused rather than written with half a pointer. VARCHAR(40) matches {@code agent_jobs.id} for the
 * pointer and the width of a hex object id for the sha; no foreign key, since the baseline job may be
 * purged while this row remains, which is the case the copied sha exists for.
 *
 * <p>Nothing in this repository reads or writes these columns. The Hunter agent orchestrator in
 * {@code sonarqube-unification} owns the invariant that it writes both values or neither, rather than
 * a CHECK constraint — this schema carries none, for the same portability reason as its absent native
 * ENUMs.
 */
public class AddPreviousJobToHunterAgentJobsTable extends DdlChange {

  static final String TABLE_NAME = "hunter_agent_jobs";

  static final String COLUMN_PREVIOUS_JOB_ID = "previous_job_id";
  static final String COLUMN_PREVIOUS_JOB_COMMIT_SHA = "previous_job_commit_sha";

  static final int COLUMN_SIZE = 40;

  public AddPreviousJobToHunterAgentJobsTable(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    try (Connection connection = getDatabase().getDataSource().getConnection()) {
      if (!tableColumnExists(connection, TABLE_NAME, COLUMN_PREVIOUS_JOB_ID)) {
        context.execute(new AddColumnsBuilder(getDialect(), TABLE_NAME)
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PREVIOUS_JOB_ID).setIsNullable(true)
            .setLimit(COLUMN_SIZE).build())
          .addColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_PREVIOUS_JOB_COMMIT_SHA).setIsNullable(true)
            .setLimit(COLUMN_SIZE).build())
          .build());
      }
    }
  }
}
