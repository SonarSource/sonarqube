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
import org.sonar.server.platform.db.migration.step.DataChange;

/**
 * Seeds the single global row ({@code project_id = '*'}) of {@code agent_schedules}.
 *
 * <p>The global row is a permanent singleton: the scheduled-agent-config API only ever updates it,
 * never creates it, so it must exist up front. It is seeded disabled, in {@code ALL} mode, with the
 * default schedule ({@code DAILY} at 02:00 UTC); {@code next_run_at} is informational until the row
 * is enabled. Timestamps are seeded to {@code 0} — they are re-stamped by the DAO on the first write.
 *
 * <p>Guarded by an existence check rather than a database-specific {@code ON CONFLICT}, so the step
 * is reentrant and portable across PostgreSQL/Oracle/MSSQL. Mirrors the standalone orchestrator's
 * dev/test seed (sonarqube-unification), which points to this framework as the canonical owner.
 */
public class SeedGlobalAgentSchedule extends DataChange {

  static final String GLOBAL_PROJECT_ID = "*";

  static final boolean DEFAULT_ENABLED = false;
  static final long DEFAULT_NEXT_RUN_AT = 0L;
  static final String DEFAULT_FREQUENCY = "DAILY";
  static final int DEFAULT_RUN_HOUR = 2;
  static final String DEFAULT_TIMEZONE = "UTC";
  static final String DEFAULT_PROJECT_SELECTION_MODE = "ALL";
  static final int DEFAULT_MAX_ISSUES_PER_RUN = 5;
  static final int DEFAULT_MAX_OPEN_PRS = 3;
  static final long DEFAULT_TIMESTAMP = 0L;

  public SeedGlobalAgentSchedule(Database db) {
    super(db);
  }

  @Override
  protected void execute(Context context) throws SQLException {
    Long existing = context.prepareSelect("select count(*) from agent_schedules where project_id = ?")
      .setString(1, GLOBAL_PROJECT_ID)
      .get(row -> row.getLong(1));
    if (existing != null && existing > 0) {
      return;
    }

    context.prepareUpsert("insert into agent_schedules ("
      + "project_id, enabled, next_run_at, frequency, day_of_week, run_hour, timezone, "
      + "project_selection_mode, max_issues_per_run, max_open_prs, created_at, updated_at) "
      + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
      .setString(1, GLOBAL_PROJECT_ID)
      .setBoolean(2, DEFAULT_ENABLED)
      .setLong(3, DEFAULT_NEXT_RUN_AT)
      .setString(4, DEFAULT_FREQUENCY)
      .setString(5, null)
      .setInt(6, DEFAULT_RUN_HOUR)
      .setString(7, DEFAULT_TIMEZONE)
      .setString(8, DEFAULT_PROJECT_SELECTION_MODE)
      .setInt(9, DEFAULT_MAX_ISSUES_PER_RUN)
      .setInt(10, DEFAULT_MAX_OPEN_PRS)
      .setLong(11, DEFAULT_TIMESTAMP)
      .setLong(12, DEFAULT_TIMESTAMP)
      .execute()
      .commit();
  }
}
