/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.rule.Severity;
import org.sonar.db.MigrationDbTester;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class PopulateIssueStatsByRuleKeyIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateIssueStatsByRuleKey.class);

  private final PopulateIssueStatsByRuleKey underTest = new PopulateIssueStatsByRuleKey(db.database());

  @Test
  void execute_shouldPopulateStatusWithToReview() throws SQLException {
    String branchUuid = insertBranch();
    String ruleUuid1 = insertRule("java", "R1");
    String ruleUuid2 = insertRule("java", "R2");
    String ruleUuid3 = insertRule("java", "R3");
    String ruleUuid4 = insertRule("java", "R4");

    insertHotspot(branchUuid, ruleUuid1, "MAJOR", "TO_REVIEW");
    insertHotspot(branchUuid, ruleUuid1, "CRITICAL", "TO_REVIEW");
    insertHotspot(branchUuid, ruleUuid1, "BLOCKER", "REVIEWED");

    // resolved - should be ignored
    insertIssue(branchUuid, ruleUuid2, "BLOCKER", "wont fix");
    insertIssue(branchUuid, ruleUuid2, "CRITICAL", null);
    insertIssue(branchUuid, ruleUuid2, "MINOR", null);

    insertHotspot(branchUuid, ruleUuid3, "MINOR", "REVIEWED");
    insertIssue(branchUuid, ruleUuid4, "BLOCKER", "wont fix");

    underTest.execute();

    // aggregation_type, aggregation_id, rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed
    assertRows(
      tuple("PROJECT", branchUuid, "java:R1", 0L, 0L, 0L, 2L, 1L),
      tuple("PROJECT", branchUuid, "java:R2", 2L, 4L, 1L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:R3", 0L, 0L, 0L, 0L, 1L)
      );
  }

  @Test
  void execute_shouldPopulateStatusWithToReviewWithStandardModeSeverities() throws SQLException {
    String branchUuid = insertBranch();

    for (String severity : Severity.ALL) {
      String ruleUuid = insertRule("java", "R" + severity);
      insertIssue(branchUuid, ruleUuid, severity, null);
    }

    underTest.execute();

    // aggregation_type, aggregation_id, rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed
    assertRows(
      tuple("PROJECT", branchUuid, "java:RINFO", 1L, 1L, 1L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:RMINOR", 1L, 2L, 1L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:RMAJOR", 1L, 3L, 1L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:RCRITICAL", 1L, 4L, 1L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:RBLOCKER", 1L, 5L, 1L, 0L, 0L)
      );
  }

  @Test
  void execute_shouldPopulateStatusWithToReviewWithMqrModeSeverities() throws SQLException {
    String branchUuid = insertBranch();
    for (org.sonar.api.issue.impact.Severity severity : org.sonar.api.issue.impact.Severity.values()) {
      String ruleUuid = insertRule("java", "R" + severity.name());
      String issueKey = insertIssue(branchUuid, ruleUuid, "MAJOR", null);
      insertImpact(issueKey, severity.name());
    }

    underTest.execute();

    // aggregation_type, aggregation_id, rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed
    assertRows(
      tuple("PROJECT", branchUuid, "java:RINFO", 1L, 3L, 1L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:RLOW", 1L, 3L, 2L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:RMEDIUM", 1L, 3L, 3L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:RHIGH", 1L, 3L, 4L, 0L, 0L),
      tuple("PROJECT", branchUuid, "java:RBLOCKER", 1L, 3L, 5L, 0L, 0L)
      );
  }

  @Test
  void execute_whenAlreadyExecuted_shouldBeIdempotent() throws SQLException {
    underTest.execute();
    underTest.execute();

  }

  private void insertImpact(String issueKey, String severity) {
    db.executeInsert("issues_impacts",
      "issue_key", issueKey,
      "software_quality", "SECURITY",
      "severity", severity,
      "manual_severity", false);
  }

  private String insertIssue(String branchUuid, String ruleUuid, String severity, @Nullable String resolution) {
    return insertIssueOrHotspots(branchUuid, ruleUuid, severity, 1, resolution, null);
  }

  private String insertHotspot(String branchUuid, String ruleUuid, String severity, String status) {
    return insertIssueOrHotspots(branchUuid, ruleUuid, severity, 4, null, status);
  }

  private String insertIssueOrHotspots(String branchUuid, String ruleUuid, String severity, int type,
    @Nullable String resolution, @Nullable String status) {
    String issueKey = randomUUID().toString();
    db.executeInsert("issues",
      "kee", issueKey,
      "project_uuid", branchUuid,
      "rule_uuid", ruleUuid,
      "severity", severity,
      "manual_severity", false,
      "issue_type", type,
      "status", status,
      "resolution", resolution);
    return issueKey;
  }

  private String insertRule(String repo, String ruleKey) {
    String uuid = randomUUID().toString();
    db.executeInsert("rules",
      "uuid", uuid,
      "plugin_name", repo,
      "plugin_rule_key", ruleKey,
      "scope", "scope",
      "is_ad_hoc", false,
      "is_external", false);
    return uuid;
  }

  private String insertBranch() {
    String uuid = randomUUID().toString();

    db.executeInsert("project_branches",
      "uuid", uuid,
      "project_uuid", randomUUID().toString(),
      "kee", randomUUID().toString(),
      "branch_type", "BRANCH",
      "created_at", 1000,
      "updated_at", 1000,
      "exclude_from_purge", false,
      "need_issue_sync", false,
      "is_main", true);
    return uuid;
  }

  private void assertRows(Tuple... values) {
    List<Map<String, Object>> rows = db.select("""
      select aggregation_type, aggregation_id, rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed
      from issue_stats_by_rule_key
      """
    );
    assertThat(rows).extracting(
      i -> i.get("aggregation_type"),
      i -> i.get("aggregation_id"),
      i -> i.get("rule_key"),
      i -> i.get("issue_count"),
      i -> i.get("rating"),
      i -> i.get("mqr_rating"),
      i -> i.get("hotspot_count"),
      i -> i.get("hotspots_reviewed")
    ).containsOnly(values);
  }
}