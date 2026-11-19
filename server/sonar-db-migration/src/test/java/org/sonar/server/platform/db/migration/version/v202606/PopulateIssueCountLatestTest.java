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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateIssueCountLatestTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateIssueCountLatest.class);

  private final PopulateIssueCountLatest underTest = new PopulateIssueCountLatest(db.database());

  @Test
  void execute_withNoHistory_shouldLeaveTheTableEmpty() throws SQLException {
    underTest.execute();

    assertThat(latest()).isEmpty();
  }

  @Test
  void execute_shouldCopyTheNewestRowOfEachDimension() throws SQLException {
    insertHistory("branch", "PROJECT_BRANCH", 1, 100L, 5);
    insertHistory("branch", "PROJECT_BRANCH", 1, 300L, 7);
    insertHistory("branch", "PROJECT_BRANCH", 1, 200L, 99);
    insertHistory("branch", "PROJECT_BRANCH", 2, 50L, 1);

    underTest.execute();

    assertThat(latest()).containsOnly(
      Map.entry("branch|PROJECT_BRANCH|1", "300:7"),
      Map.entry("branch|PROJECT_BRANCH|2", "50:1"));
  }

  @Test
  void execute_shouldKeepEntitiesAndEntityTypesApart() throws SQLException {
    insertHistory("branch", "PROJECT_BRANCH", 1, 100L, 1);
    insertHistory("branch", "PORTFOLIO", 1, 200L, 2);
    insertHistory("other", "PROJECT_BRANCH", 1, 300L, 3);

    underTest.execute();

    assertThat(latest()).containsOnly(
      Map.entry("branch|PROJECT_BRANCH|1", "100:1"),
      Map.entry("branch|PORTFOLIO|1", "200:2"),
      Map.entry("other|PROJECT_BRANCH|1", "300:3"));
  }

  @Test
  void execute_shouldCopyAZeroCount() throws SQLException {
    insertHistory("branch", "PROJECT_BRANCH", 1, 100L, 4);
    insertHistory("branch", "PROJECT_BRANCH", 1, 200L, 0);

    underTest.execute();

    assertThat(latest()).containsOnly(Map.entry("branch|PROJECT_BRANCH|1", "200:0"));
  }

  @Test
  void execute_whenRunAgain_shouldChangeNothing() throws SQLException {
    insertHistory("branch", "PROJECT_BRANCH", 1, 100L, 5);
    insertHistory("branch", "PROJECT_BRANCH", 2, 100L, 6);
    underTest.execute();
    Map<String, String> afterFirstRun = latest();

    underTest.execute();

    assertThat(latest()).isEqualTo(afterFirstRun).hasSize(2);
  }

  @Test
  void execute_shouldLeaveAnExistingLatestRowAlone() throws SQLException {
    insertHistory("branch", "PROJECT_BRANCH", 1, 100L, 5);
    insertHistory("branch", "PROJECT_BRANCH", 2, 100L, 6);
    db.executeInsert("issue_count_latest",
      "entity_id", "branch", "entity_type", "PROJECT_BRANCH", "dimension_id", 1, "recorded_at_epoch", 500L, "issue_count", 9);

    underTest.execute();

    assertThat(latest()).containsOnly(
      Map.entry("branch|PROJECT_BRANCH|1", "500:9"),
      Map.entry("branch|PROJECT_BRANCH|2", "100:6"));
  }

  private void insertHistory(String entityId, String entityType, int dimensionId, long recordedAtEpoch, int issueCount) {
    db.executeInsert("issue_count_history",
      "entity_id", entityId, "entity_type", entityType, "dimension_id", dimensionId, "recorded_at_epoch", recordedAtEpoch, "issue_count", issueCount);
  }

  /** The latest rows keyed by "entity|type|dimension", valued "epoch:count". */
  private Map<String, String> latest() {
    Map<String, String> rows = new TreeMap<>();
    List<Map<String, Object>> selected = db.select(
      "SELECT entity_id, entity_type, dimension_id, recorded_at_epoch, issue_count FROM issue_count_latest");
    selected.forEach(row -> rows.put(
      row.get("ENTITY_ID") + "|" + row.get("ENTITY_TYPE") + "|" + ((Number) row.get("DIMENSION_ID")).intValue(),
      ((Number) row.get("RECORDED_AT_EPOCH")).longValue() + ":" + ((Number) row.get("ISSUE_COUNT")).intValue()));
    return rows;
  }
}
