/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

class PopulateIssueStatsByRuleKeyForPortfoliosAndAppsIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(PopulateIssueStatsByRuleKeyForPortfoliosAndApps.class);

  private final PopulateIssueStatsByRuleKeyForPortfoliosAndApps underTest =
    new PopulateIssueStatsByRuleKeyForPortfoliosAndApps(db.database());

  @Test
  void execute_shouldAggregateApplicationProjectsIssueStats() throws SQLException {
    // Insert applications and portfolios
    String appUuid1 = "app-uuid1";
    insertComponent("APP", appUuid1);

    String appUuid2 = "app-uuid2";
    insertComponent("APP", appUuid2);

    String portfolioUuid = "portfolio-uuid";
    insertComponent("VW", portfolioUuid);

    // insert projects
    String projectUuid1 = "projectUuid1";
    String projectUuid2 = "projectUuid2";
    String projectUuid3 = "projectUuid3";
    String projectUuid4 = "projectUuid4";
    String projectUuid5 = "projectUuid5";
    String projectUuid6 = "projectUuid6";
    String projectUuid7 = "projectUuid7";
    String projectUuid8 = "projectUuid8";
    insertComponent("TRK", projectUuid1);
    insertComponent("TRK", projectUuid2);
    insertComponent("TRK", projectUuid3);
    insertComponent("TRK", projectUuid4);
    insertComponent("TRK", projectUuid5);
    insertComponent("TRK", projectUuid6);
    insertComponent("TRK", projectUuid7);
    insertComponent("TRK", projectUuid8);

    // projects 1-3 belong to app 1
    insertProjectCopy(projectUuid1, appUuid1);
    insertProjectCopy(projectUuid2, appUuid1);
    insertProjectCopy(projectUuid3, appUuid1);

    // projects 4-6 belong to app 2
    insertProjectCopy(projectUuid4, appUuid2);
    insertProjectCopy(projectUuid5, appUuid2);
    insertProjectCopy(projectUuid6, appUuid2);

    // projects 7 & 8 belong to the portfolio
    insertProjectCopy(projectUuid7, portfolioUuid);
    insertProjectCopy(projectUuid8, portfolioUuid);

    // insert issue_stats rows for each project
    // project1
    insertIntoIssueStats(projectUuid1, "PROJECT", "java:001", 10, 2, 4, 0, 0);
    insertIntoIssueStats(projectUuid1, "PROJECT", "java:002", 0, 1, 3, 10, 10);
    insertIntoIssueStats(projectUuid1, "PROJECT", "java:003", 25, 2, 2, 0, 0);

    // project 2
    insertIntoIssueStats(projectUuid2, "PROJECT", "java:001", 100, 3, 3, 0, 0);
    insertIntoIssueStats(projectUuid2, "PROJECT", "java:002", 0, 1, 3, 3, 27);
    insertIntoIssueStats(projectUuid2, "PROJECT", "java:003", 6, 1, 1, 0, 0);

    // project 3
    insertIntoIssueStats(projectUuid3, "PROJECT", "java:001", 17, 1, 3, 0, 0);
    insertIntoIssueStats(projectUuid3, "PROJECT", "java:002", 0, 1, 3, 103, 7);
    insertIntoIssueStats(projectUuid3, "PROJECT", "java:003", 52, 4, 2, 0, 0);

    // project 4
    insertIntoIssueStats(projectUuid4, "PROJECT", "java:001", 17, 1, 1, 0, 0);
    insertIntoIssueStats(projectUuid4, "PROJECT", "java:002", 0, 1, 3, 24, 7);
    insertIntoIssueStats(projectUuid4, "PROJECT", "java:003", 52, 4, 2, 0, 0);

    // project 5
    insertIntoIssueStats(projectUuid5, "PROJECT", "java:001", 5, 4, 1, 0, 0);
    insertIntoIssueStats(projectUuid5, "PROJECT", "java:002", 0, 1, 1, 16, 0);
    insertIntoIssueStats(projectUuid5, "PROJECT", "java:003", 3, 4, 3, 0, 0);

    // project 6
    insertIntoIssueStats(projectUuid6, "PROJECT", "java:001", 14, 3, 1, 0, 0);
    insertIntoIssueStats(projectUuid6, "PROJECT", "java:002", 0, 1, 1, 0, 12);
    insertIntoIssueStats(projectUuid6, "PROJECT", "java:003", 2, 4, 1, 0, 0);

    // project 7
    insertIntoIssueStats(projectUuid7, "PROJECT", "java:001", 20, 1, 1, 0, 0);
    insertIntoIssueStats(projectUuid7, "PROJECT", "java:002", 0, 1, 1, 10, 2);
    insertIntoIssueStats(projectUuid7, "PROJECT", "java:003", 2, 4, 1, 0, 0);

    // project 8
    insertIntoIssueStats(projectUuid8, "PROJECT", "java:001", 1, 2, 1, 0, 0);
    insertIntoIssueStats(projectUuid8, "PROJECT", "java:002", 0, 1, 2, 1, 1);
    insertIntoIssueStats(projectUuid8, "PROJECT", "java:003", 3, 4, 3, 0, 0);

    // verify that the migration aggregates the issues stats correctly
    underTest.execute();

    assertRowsForAggregation(appUuid1, "APPLICATION",
      tuple("APPLICATION", appUuid1, "java:001", 127L, 3L, 4L, 0L, 0L),
      tuple("APPLICATION", appUuid1, "java:002", 0L, 1L, 3L, 116L, 44L),
      tuple("APPLICATION", appUuid1, "java:003", 83L, 4L, 2L, 0L, 0L)
    );

    assertRowsForAggregation(appUuid2, "APPLICATION",
      tuple("APPLICATION", appUuid2, "java:001", 36L, 4L, 1L, 0L, 0L),
      tuple("APPLICATION", appUuid2, "java:002", 0L, 1L, 3L, 40L, 19L),
      tuple("APPLICATION", appUuid2, "java:003", 57L, 4L, 3L, 0L, 0L)
    );

    assertRowsForAggregation(portfolioUuid, "PORTFOLIO",
      tuple("PORTFOLIO", portfolioUuid, "java:001", 21L, 2L, 1L, 0L, 0L),
      tuple("PORTFOLIO", portfolioUuid, "java:002", 0L, 1L, 2L, 11L, 3L),
      tuple("PORTFOLIO", portfolioUuid, "java:003", 5L, 4L, 3L, 0L, 0L)
    );
  }

  private void insertComponent(String qualifier, String uuid) {
    db.executeInsert("components",
      "uuid", uuid,
      "branch_uuid", uuid,
      "kee", uuid + "_kee",
      "uuid_path", ".",
      "qualifier", qualifier,
      "scope", "PRJ",
      "enabled", true,
      "private", false);
  }

  private void insertProjectCopy(String branchUuid, String rootUuid) {
    db.executeInsert("components",
      "uuid", branchUuid + "_copy_uuid",
      "branch_uuid", rootUuid,
      "kee", branchUuid + "_copy_kee",
      "qualifier", "TRK",
      "uuid_path", "." + rootUuid + "." + branchUuid,
      "copy_component_uuid", branchUuid,
      "scope", "PRJ",
      "enabled", true,
      "private", false);
  }

  private void insertIntoIssueStats(String aggregationId, String aggregationType, String ruleKey, int issueCount, int rating,
    int mqrRating, int hotspotCount, int hotspotsReviewed) {
    db.executeInsert("issue_stats_by_rule_key",
      "aggregation_type", aggregationType,
      "aggregation_id", aggregationId,
      "rule_key", ruleKey,
      "issue_count", issueCount,
      "rating", rating,
      "mqr_rating", mqrRating,
      "hotspot_count", hotspotCount,
      "hotspots_reviewed", hotspotsReviewed
    );
  }

  private void assertRowsForAggregation(String aggregationId, String aggregationType, Tuple... values) {
    List<Map<String, Object>> rows = db.select("""
      select aggregation_type, aggregation_id, rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed
      from issue_stats_by_rule_key
      where aggregation_id = '%s'
      and aggregation_type = '%s'
      """.formatted(aggregationId, aggregationType));

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