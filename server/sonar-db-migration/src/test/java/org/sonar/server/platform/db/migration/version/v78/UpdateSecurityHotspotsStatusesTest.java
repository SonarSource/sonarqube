/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

package org.sonar.server.platform.db.migration.version.v78;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.apache.commons.lang.math.RandomUtils;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.es.MigrationEsClient;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class UpdateSecurityHotspotsStatusesTest {

  private static final long PAST = 5_000_000_000L;
  private static final long NOW = 10_000_000_000L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(UpdateSecurityHotspotsStatusesTest.class, "schema.sql");

  private MapSettings settings = new MapSettings();
  private TestSystem2 system2 = new TestSystem2().setNow(NOW);
  private MigrationEsClient esClient = mock(MigrationEsClient.class);

  private DataChange underTest = new UpdateSecurityHotspotsStatuses(db.database(), settings.asConfig(), system2, esClient, UuidFactoryFast.getInstance());

  @Test
  public void migrate_open_and_reopen_hotspots() throws SQLException {
    int rule = insertRule(4);
    String issue1 = insertIssue("OPEN", null, 4, rule);
    String issue2 = insertIssue("REOPENED", null, 4, rule);
    // Other type of issues should not be updated
    String issue3 = insertIssue("OPEN", null, 1, rule);
    String issue4 = insertIssue("REOPENED", null, 2, rule);
    String issue5 = insertIssue("OPEN", null, 3, rule);

    underTest.execute();

    assertIssues(
      tuple(issue1, "TO_REVIEW", null, 4, NOW),
      tuple(issue2, "TO_REVIEW", null, 4, NOW),
      // Not updated
      tuple(issue3, "OPEN", null, 1, PAST),
      tuple(issue4, "REOPENED", null, 2, PAST),
      tuple(issue5, "OPEN", null, 3, PAST));
  }

  @Test
  public void migrate_resolved_as_fixed_and_wont_fix_hotspots() throws SQLException {
    int rule = insertRule(4);
    String issue1 = insertIssue("RESOLVED", "FIXED", 4, rule);
    String issue2 = insertIssue("RESOLVED", "WONTFIX", 4, rule);
    // Other type of issues should not be updated
    String issue3 = insertIssue("RESOLVED", "FIXED", 1, rule);
    String issue4 = insertIssue("RESOLVED", "WONTFIX", 2, rule);
    String issue5 = insertIssue("RESOLVED", "WONTFIX", 3, rule);

    underTest.execute();

    assertIssues(
      tuple(issue1, "IN_REVIEW", null, 4, NOW),
      tuple(issue2, "REVIEWED", "FIXED", 4, NOW),
      // Not updated
      tuple(issue3, "RESOLVED", "FIXED", 1, PAST),
      tuple(issue4, "RESOLVED", "WONTFIX", 2, PAST),
      tuple(issue5, "RESOLVED", "WONTFIX", 3, PAST));
  }

  @Test
  public void insert_issue_changes() throws SQLException {
    int rule = insertRule(4);
    String issue1 = insertIssue("REOPENED", null, 4, rule);
    // No changelog on OPEN issue as there was no previous state
    String issue2 = insertIssue("OPEN", null, 4, rule);
    String issue3 = insertIssue("RESOLVED", "FIXED", 4, rule);
    String issue4 = insertIssue("RESOLVED", "WONTFIX", 4, rule);

    underTest.execute();

    assertIssueChanges(
      tuple(issue1, "diff", "status=REOPENED|TO_REVIEW,resolution=", NOW, NOW, NOW),
      tuple(issue3, "diff", "status=RESOLVED|IN_REVIEW,resolution=FIXED|", NOW, NOW, NOW),
      tuple(issue4, "diff", "status=RESOLVED|REVIEWED,resolution=WONTFIX|FIXED", NOW, NOW, NOW));
  }

  @Test
  public void do_not_update_vulnerabilities_coming_from_hotspot() throws SQLException {
    int rule = insertRule(4);
    String issue1 = insertIssue("OPEN", null, 3, rule);

    underTest.execute();

    assertIssues(tuple(issue1, "OPEN", null, 3, PAST));
    assertNoIssueChanges();
  }

  @Test
  public void do_not_update_closed_hotspots() throws SQLException {
    int rule = insertRule(4);
    String issue1 = insertIssue("CLOSED", "FIXED", 4, rule);
    String issue2 = insertIssue("CLOSED", "REMOVED", 4, rule);

    underTest.execute();

    assertIssues(
      tuple(issue1, "CLOSED", "FIXED", 4, PAST),
      tuple(issue2, "CLOSED", "REMOVED", 4, PAST));
    assertNoIssueChanges();
  }

  @Test
  public void do_nothing_on_sonarcloud() throws SQLException {
    settings.setProperty("sonar.sonarcloud.enabled", "true");
    int rule = insertRule(4);
    String issue1 = insertIssue("OPEN", null, 4, rule);

    underTest.execute();

    assertIssues(tuple(issue1, "OPEN", null, 4, PAST));
    assertNoIssueChanges();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    int rule = insertRule(4);
    String issue1 = insertIssue("OPEN", null, 4, rule);
    String issue2 = insertIssue("REOPENED", null, 4, rule);

    underTest.execute();
    assertIssues(
      tuple(issue1, "TO_REVIEW", null, 4, NOW),
      tuple(issue2, "TO_REVIEW", null, 4, NOW));

    // Set a new date for NOW in order to check that issues has not been updated again
    system2.setNow(NOW + 1_000_000_000L);
    underTest.execute();
    assertIssues(
      tuple(issue1, "TO_REVIEW", null, 4, NOW),
      tuple(issue2, "TO_REVIEW", null, 4, NOW));
  }

  @Test
  public void issues_index_is_removed() throws SQLException {
    underTest.execute();

    verify(esClient).deleteIndexes("issues");
  }

  private void assertIssues(Tuple... expectedTuples) {
    assertThat(db.select("SELECT kee, status, resolution, issue_type, updated_at FROM issues")
      .stream()
      .map(map -> new Tuple(map.get("KEE"), map.get("STATUS"), map.get("RESOLUTION"), map.get("ISSUE_TYPE"), map.get("UPDATED_AT")))
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private void assertNoIssueChanges() {
    assertThat(db.countRowsOfTable("issue_changes")).isZero();
  }

  private void assertIssueChanges(Tuple... expectedTuples) {
    assertThat(db.select("SELECT issue_key, change_type, change_data, created_at, updated_at, issue_change_creation_date FROM issue_changes")
      .stream()
      .map(map -> new Tuple(map.get("ISSUE_KEY"), map.get("CHANGE_TYPE"), map.get("CHANGE_DATA"), map.get("CREATED_AT"), map.get("UPDATED_AT"),
        map.get("ISSUE_CHANGE_CREATION_DATE")))
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private String insertIssue(String status, @Nullable String resolution, int issueType, int ruleId) {
    String issueKey = randomAlphabetic(3);
    db.executeInsert(
      "ISSUES",
      "KEE", issueKey,
      "STATUS", status,
      "RESOLUTION", resolution,
      "RULE_ID", ruleId,
      "ISSUE_TYPE", issueType,
      "COMPONENT_UUID", randomAlphanumeric(10),
      "PROJECT_UUID", randomAlphanumeric(10),
      "MANUAL_SEVERITY", false,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    return issueKey;
  }

  private int insertRule(int ruleType) {
    int id = RandomUtils.nextInt();
    db.executeInsert("RULES",
      "ID", id,
      "RULE_TYPE", ruleType,
      "IS_EXTERNAL", false,
      "PLUGIN_RULE_KEY", randomAlphanumeric(3),
      "PLUGIN_NAME", randomAlphanumeric(3),
      "SCOPE", "MAIN",
      "IS_AD_HOC", false,
      "CREATED_AT", PAST,
      "UPDATED_AT", PAST);
    return id;
  }

}
