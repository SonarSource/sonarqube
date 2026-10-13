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
 * Widens {@code remediation_agent_jobs.result_commit} from {@code VARCHAR(40)} (SHA-1 only) to
 * {@code VARCHAR(64)}, for consistency with the same widening of the Hunter agent's commit-SHA
 * columns. Not actively written to today &mdash; kept the same shape regardless, so it is not a
 * second gap to close later if a future caller starts populating it with a real SHA.
 */
public class WidenRemediationAgentJobsResultCommitColumn extends DdlChange {

  static final String TABLE_NAME = "remediation_agent_jobs";
  static final String COLUMN_RESULT_COMMIT = "result_commit";

  static final int RESULT_COMMIT_SIZE = 64;

  public WidenRemediationAgentJobsResultCommitColumn(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) {
    context.execute(new AlterColumnsBuilder(getDialect(), TABLE_NAME)
      .updateColumn(newVarcharColumnDefBuilder().setColumnName(COLUMN_RESULT_COMMIT).setIsNullable(true)
        .setLimit(RESULT_COMMIT_SIZE).build())
      .build());
  }
}
