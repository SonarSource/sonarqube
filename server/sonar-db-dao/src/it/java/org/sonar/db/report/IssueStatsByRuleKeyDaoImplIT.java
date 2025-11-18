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
package org.sonar.db.report;

import io.sonarcloud.compliancereports.dao.IssueStats;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static java.util.UUID.fromString;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class IssueStatsByRuleKeyDaoImplIT {
  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final IssueStatsByRuleKeyDaoImpl underTest = new IssueStatsByRuleKeyDaoImpl(db.getSession());

  @Test
  void shouldGetIssuesByAggregationId() {

    try (var session = db.getSession().getSqlSession(); var sqlSession = session.getConnection()) {
      insertSampleIssueStats(sqlSession);
      session.commit();

      var results = underTest.getIssueStatsForProject(fromString("b728478a-470f-4cb2-8a19-9302632e049f"));
      assertThat(results).hasSize(2);
      assertThat(results)
        .anySatisfy(issueStats -> {
          assertThat(issueStats.ruleKey()).isEqualTo("githubactions:S7630");
          assertThat(issueStats.issueCount()).isEqualTo(6);
          assertThat(issueStats.rating()).isEqualTo(5);
          assertThat(issueStats.hotspotCount()).isZero();
          assertThat(issueStats.hotspotsReviewed()).isZero();
        })
        .anySatisfy(issueStats -> {
          assertThat(issueStats.ruleKey()).isEqualTo("githubactions:S7640");
          assertThat(issueStats.issueCount()).isEqualTo(3);
          assertThat(issueStats.rating()).isEqualTo(3);
          assertThat(issueStats.hotspotCount()).isEqualTo(1);
          assertThat(issueStats.hotspotsReviewed()).isEqualTo(1);
        });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void shouldInsertIssueStatsForProject() throws SQLException {
    var issueStatsList = List.of(
      new IssueStats("githubactions:S7630", 6, 5, 0, 0),
      new IssueStats("githubactions:S7640", 3, 3, 1, 1)
    );

    underTest.insertIssueStatsForProject(fromString("b728478a-470f-4cb2-8a19-9302632e049f"), issueStatsList);

    try (var session = db.getSession().getSqlSession(); var sqlSession = session.getConnection()) {
      var resultSet = sqlSession.prepareStatement(
        "SELECT rule_key, issue_count, rating, hotspot_count, hotspots_reviewed " +
          "FROM issue_stats_by_rule_key " +
          "WHERE aggregation_type='PROJECT' AND aggregation_id='b728478a-470f-4cb2-8a19-9302632e049f'"
      ).executeQuery();

      var results = new ArrayList<IssueStats>();
      while (resultSet.next()) {
        results.add(new IssueStats(
          resultSet.getString("rule_key"),
          resultSet.getInt("issue_count"),
          resultSet.getInt("rating"),
          resultSet.getInt("hotspot_count"),
          resultSet.getInt("hotspots_reviewed")
        ));
      }

      assertThat(results).hasSize(2);
      assertThat(results)
        .anySatisfy(issueStats -> {
          assertThat(issueStats.ruleKey()).isEqualTo("githubactions:S7630");
          assertThat(issueStats.issueCount()).isEqualTo(6);
          assertThat(issueStats.rating()).isEqualTo(5);
          assertThat(issueStats.hotspotCount()).isZero();
          assertThat(issueStats.hotspotsReviewed()).isZero();
        })
        .anySatisfy(issueStats -> {
          assertThat(issueStats.ruleKey()).isEqualTo("githubactions:S7640");
          assertThat(issueStats.issueCount()).isEqualTo(3);
          assertThat(issueStats.rating()).isEqualTo(3);
          assertThat(issueStats.hotspotCount()).isEqualTo(1);
          assertThat(issueStats.hotspotsReviewed()).isEqualTo(1);
        });
    }
  }

  @Test
  void shouldDeleteAllIssueStatsForProject() throws SQLException {
    try (var session = db.getSession().getSqlSession(); var sqlSession = session.getConnection()) {
      insertSampleIssueStats(sqlSession);
      session.commit();

      underTest.deleteAllIssueStatsForProject(fromString("b728478a-470f-4cb2-8a19-9302632e049f"));

      var resultSet = sqlSession.prepareStatement(
        "SELECT COUNT(*) AS total " +
          "FROM issue_stats_by_rule_key " +
          "WHERE aggregation_type='PROJECT' AND aggregation_id='b728478a-470f-4cb2-8a19-9302632e049f'"
      ).executeQuery();

      if (resultSet.next()) {
        int total = resultSet.getInt("total");
        assertThat(total).isZero();
      } else {
        throw new IllegalStateException("No result returned from count query");
      }
    }
  }

  private static void insertSampleIssueStats(Connection sqlSession) throws SQLException {
    sqlSession.prepareStatement(
      "INSERT INTO issue_stats_by_rule_key (aggregation_type, aggregation_id, rule_key, issue_count, rating, hotspot_count, hotspots_reviewed) " +
      "VALUES ('PROJECT', 'b728478a-470f-4cb2-8a19-9302632e049f', 'githubactions:S7630', 6, 5, 0, 0)," +
      "('PROJECT', 'b728478a-470f-4cb2-8a19-9302632e049f', 'githubactions:S7640', 3, 3, 1, 1)"
    ).execute();
  }
}
