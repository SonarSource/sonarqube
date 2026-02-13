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
package org.sonar.db.report;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;
import org.sonarsource.compliancereports.dao.AggregationType;
import org.sonarsource.compliancereports.dao.IssueStats;

import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.sonarsource.compliancereports.dao.AggregationType.PROJECT;

class IssueStatsByRuleKeyDaoImplIT {
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final IssueStatsByRuleKeyDaoImpl underTest = new IssueStatsByRuleKeyDaoImpl(db.getDbClient());

  @Test
  void shouldGetIssuesByAggregationId() throws SQLException {
    insertSampleIssueStats();

    assertThat(underTest.getIssueStats("b728478a-470f-4cb2-8a19-9302632e049f", PROJECT))
      .extracting(IssueStats::ruleKey, IssueStats::issueCount, IssueStats::rating, IssueStats::mqrRating,
        IssueStats::hotspotCount, IssueStats::hotspotsReviewed)
      .containsOnly(
        tuple("githubactions:S7630", 6, 5, 4, 0, 0),
        tuple("githubactions:S7640", 3, 3, 3, 1, 1)
      );
  }

  @Test
  void shouldDeleteAndInsertLargeSetOfIssueStatsForProject() throws SQLException {
    int size = 1000;
    List<IssueStats> issueStatsList = IntStream.range(0, size)
      .mapToObj(i -> new IssueStats("githubactions:S" + i, 6, 5, 3, 0, 0))
      .toList();

    underTest.deleteAndInsertIssueStats("b728478a-470f-4cb2-8a19-9302632e049f", PROJECT, issueStatsList);
    assertThat(getIssueStats()).hasSize(size);
  }

  @Test
  void shouldDeleteAndInsertIssueStatsForProject() throws SQLException {
    var issueStatsList = List.of(
      new IssueStats("githubactions:S7630", 6, 5, 3, 0, 0),
      new IssueStats("githubactions:S7640", 3, 3, 5, 1, 1)
    );

    insertSampleIssueStats();

    underTest.deleteAndInsertIssueStats("b728478a-470f-4cb2-8a19-9302632e049f", PROJECT, issueStatsList);
    assertThat(getIssueStats())
      .extracting(IssueStats::ruleKey, IssueStats::issueCount, IssueStats::rating, IssueStats::mqrRating,
        IssueStats::hotspotCount, IssueStats::hotspotsReviewed)
      .containsOnly(
        tuple("githubactions:S7630", 6, 5, 3, 0, 0),
        tuple("githubactions:S7640", 3, 3, 5, 1, 1)
      );
  }

  private List<IssueStats> getIssueStats() throws SQLException {
    var statement = db.getSession().getConnection().prepareStatement(
      "SELECT rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed " +
        "FROM issue_stats_by_rule_key " +
        "WHERE aggregation_type='PROJECT' AND aggregation_id='b728478a-470f-4cb2-8a19-9302632e049f'"
    );

    var issueStats = new ArrayList<IssueStats>();
    try (var resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        issueStats.add(new IssueStats(
          resultSet.getString("rule_key"),
          resultSet.getInt("issue_count"),
          resultSet.getInt("rating"),
          resultSet.getInt("mqr_rating"),
          resultSet.getInt("hotspot_count"),
          resultSet.getInt("hotspots_reviewed")
        ));
      }
    }
    return issueStats;
  }

  @Test
  void shouldDeleteAllIssueStatsForProject() throws SQLException {
    insertSampleIssueStats();

    underTest.deleteAndInsertIssueStats("b728478a-470f-4cb2-8a19-9302632e049f", AggregationType.PROJECT, List.of());
    assertThat(getIssueStats()).isEmpty();
  }

  private void insertSampleIssueStats() throws SQLException {
    try (var c = db.openConnection(); var statement = c.prepareStatement(
      """
        INSERT INTO issue_stats_by_rule_key (aggregation_type, aggregation_id, rule_key, issue_count, rating, mqr_rating, hotspot_count, hotspots_reviewed)
        VALUES ('PROJECT', 'b728478a-470f-4cb2-8a19-9302632e049f', 'githubactions:S7630', 6, 5, 4, 0, 0),
        ('PROJECT', 'b728478a-470f-4cb2-8a19-9302632e049f', 'githubactions:S7640', 3, 3, 3, 1, 1)
        """
    )) {
      statement.execute();
    }
  }
}
