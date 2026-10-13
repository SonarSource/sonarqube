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
package org.sonar.server.platform.db.migration.version.v202606;

import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.sql.AlterColumnsBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

/**
 * Widens {@code hunter_agent_runs.analyzed_commit_sha} from {@code VARCHAR(40)} (SHA-1 only) to
 * {@code VARCHAR(64)}, matching the same widening of {@code hunter_agent_jobs.analyzed_commit_sha}
 * in {@link WidenHunterAgentJobsCommitShaColumns} &mdash; the two columns must not disagree on the
 * SHA format they can hold.
 */
public class WidenHunterAgentRunsAnalyzedCommitShaColumn extends DdlChange {

  static final String TABLE_NAME = "hunter_agent_runs";
  static final String COLUMN_ANALYZED_COMMIT_SHA = "analyzed_commit_sha";

  static final int ANALYZED_COMMIT_SHA_SIZE = 64;

  public WidenHunterAgentRunsAnalyzedCommitShaColumn(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) {
    context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
      .updateColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_ANALYZED_COMMIT_SHA).setIsNullable(true)
        .setLimit(ANALYZED_COMMIT_SHA_SIZE).build())
      .build());
  }
}
