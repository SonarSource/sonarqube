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
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class SeedGlobalAgentScheduleTest {

  private static final String SELECT_GLOBAL =
    "select enabled, next_run_at, frequency, day_of_week, run_hour, timezone, project_selection_mode, "
      + "max_issues_per_run, max_open_prs, created_at, updated_at from agent_schedules where project_id = '*'";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(SeedGlobalAgentSchedule.class);

  private final SeedGlobalAgentSchedule underTest = new SeedGlobalAgentSchedule(db.database());

  @Test
  void execute_whenNoGlobalRow_shouldSeedDisabledAllDefaultRow() throws SQLException {
    assertThat(db.select(SELECT_GLOBAL)).isEmpty();

    underTest.execute();

    List<Map<String, Object>> rows = db.select(SELECT_GLOBAL);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row)
      .containsEntry("ENABLED", false)
      .containsEntry("FREQUENCY", "DAILY")
      .containsEntry("TIMEZONE", "UTC")
      .containsEntry("PROJECT_SELECTION_MODE", "ALL");
    assertThat(row.get("DAY_OF_WEEK")).isNull();
    assertThat(((Number) row.get("RUN_HOUR")).intValue()).isEqualTo(2);
    assertThat(((Number) row.get("NEXT_RUN_AT")).longValue()).isZero();
    assertThat(((Number) row.get("MAX_ISSUES_PER_RUN")).intValue()).isEqualTo(5);
    assertThat(((Number) row.get("MAX_OPEN_PRS")).intValue()).isEqualTo(3);
    assertThat(((Number) row.get("CREATED_AT")).longValue()).isZero();
    assertThat(((Number) row.get("UPDATED_AT")).longValue()).isZero();
  }

  @Test
  void execute_whenGlobalRowAlreadyExists_shouldNotOverwriteIt() throws SQLException {
    // A pre-existing (e.g. already-configured) global row must be left untouched.
    insertGlobalRow();

    underTest.execute();

    List<Map<String, Object>> rows = db.select(SELECT_GLOBAL);
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.getFirst();
    assertThat(row)
      .containsEntry("ENABLED", true)
      .containsEntry("FREQUENCY", "WEEKLY")
      .containsEntry("DAY_OF_WEEK", "MONDAY")
      .containsEntry("PROJECT_SELECTION_MODE", "SELECTED");
    assertThat(((Number) row.get("RUN_HOUR")).intValue()).isEqualTo(9);
  }

  @Test
  void execute_isReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    assertThat(db.select(SELECT_GLOBAL)).hasSize(1);
  }

  private void insertGlobalRow() {
    db.executeInsert("agent_schedules",
      "project_id", "*",
      "enabled", true,
      "next_run_at", 123L,
      "frequency", "WEEKLY",
      "day_of_week", "MONDAY",
      "run_hour", 9,
      "timezone", "Europe/Paris",
      "project_selection_mode", "SELECTED",
      "max_issues_per_run", 10,
      "max_open_prs", 7,
      "created_at", 111L,
      "updated_at", 222L);
  }
}
