/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v82;

import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static org.apache.commons.lang.math.RandomUtils.nextInt;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteSecurityReviewRatingMeasuresTest {
  private static final String PROJECT_MEASURES_TABLE_NAME = "PROJECT_MEASURES";
  private static final String LIVE_MEASURES_TABLE_NAME = "LIVE_MEASURES";

  private static final int SECURITY_REVIEW_RATING_METRIC_ID = 200;
  private static final String SECURITY_REVIEW_RATING_METRIC_KEY = "security_review_rating";

  private static final Random RANDOM = new Random();

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteSecurityReviewRatingMeasuresTest.class, "schema.sql");

  private DataChange underTest = new DeleteSecurityReviewRatingMeasures(db.database());

  @Test
  public void should_not_fail_if_metric_not_defined() throws SQLException {
    insertMetric(1, "another metric#1");
    insertMetric(2, "another metric#2");

    String projectUuid = insertComponent("PRJ", "TRK");
    insertMeasure(4, SECURITY_REVIEW_RATING_METRIC_ID, projectUuid);
    insertLiveMeasure("uuid-4", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, projectUuid);

    underTest.execute();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(1);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(1);
  }

  @Test
  public void should_remove_security_rating_review_from_measures_and_live_measures() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(1, "another metric#1");
    insertMetric(2, "another metric#2");

    String portfolioUuid = insertComponent("PRJ", "VW");
    String subPortfolioUuid = insertComponent("PRJ", "SVW");
    String applicationUuid = insertComponent("PRJ", "APP");
    String projectUuid = insertComponent("PRJ", "TRK");

    insertMeasure(1, SECURITY_REVIEW_RATING_METRIC_ID, portfolioUuid);
    insertMeasure(2, SECURITY_REVIEW_RATING_METRIC_ID, subPortfolioUuid);
    insertMeasure(3, SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid);
    insertMeasure(4, SECURITY_REVIEW_RATING_METRIC_ID, projectUuid);

    // other random metrics
    int totalOtherMeasures = IntStream.range(5, 10 + RANDOM.nextInt(10))
      .peek(i -> insertMeasure(i, RANDOM.nextInt(100), projectUuid))
      .boxed()
      .collect(Collectors.toList())
      .size();

    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, portfolioUuid, portfolioUuid);
    insertLiveMeasure("uuid-2", SECURITY_REVIEW_RATING_METRIC_ID, subPortfolioUuid, subPortfolioUuid);
    insertLiveMeasure("uuid-3", SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid, applicationUuid);
    insertLiveMeasure("uuid-4", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, projectUuid);
    insertLiveMeasure("uuid-5", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, getRandomUuid());
    insertLiveMeasure("uuid-6", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, getRandomUuid());

    // other random metrics
    long totalOtherLiveMeasures = IntStream.range(0, 10 + RANDOM.nextInt(10))
      .peek(i -> insertLiveMeasure("uuid-other-" + i, RANDOM.nextInt(100), projectUuid, getRandomUuid()))
      .boxed()
      .collect(Collectors.toList())
      .size();

    underTest.execute();

    assertSecurityReviewRatingMeasuresDeleted();
    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(totalOtherMeasures);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(totalOtherLiveMeasures);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void should_not_fail_if_empty_tables() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(1, "another metric#1");
    insertMetric(2, "another metric#2");

    underTest.execute();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(0);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(0);
  }

  private void assertSecurityReviewRatingLiveMeasuresDeleted() {
    assertThat(db.countSql("select count(uuid) from LIVE_MEASURES where metric_id = " + SECURITY_REVIEW_RATING_METRIC_ID))
      .isEqualTo(0);
  }

  private void assertSecurityReviewRatingMeasuresDeleted() {
    assertThat(db.countSql("select count(id) from project_measures where metric_id = " + SECURITY_REVIEW_RATING_METRIC_ID))
      .isEqualTo(0);
  }

  private void insertMeasure(int id, int metricId, String componentUuid) {
    db.executeInsert("PROJECT_MEASURES",
      "ID", id,
      "METRIC_ID", metricId,
      "ANALYSIS_UUID", getRandomUuid(),
      "COMPONENT_UUID", componentUuid);
  }

  private String getRandomUuid() {
    return UUID.randomUUID().toString();
  }

  private void insertLiveMeasure(String uuid, int metricId, String projectUuid, String componentUuid) {
    db.executeInsert("LIVE_MEASURES",
      "UUID", uuid,
      "PROJECT_UUID", projectUuid,
      "COMPONENT_UUID", componentUuid,
      "METRIC_ID", metricId,
      "CREATED_AT", System.currentTimeMillis(),
      "UPDATED_AT", System.currentTimeMillis());
  }

  private void insertMetric(int id, String name) {
    db.executeInsert("METRICS",
      "ID", id,
      "NAME", name,
      "DIRECTION", 0,
      "QUALITATIVE", true);
  }

  private String insertComponent(String scope, String qualifier) {
    int id = nextInt();
    String uuid = getRandomUuid();
    db.executeInsert("COMPONENTS",
      "ID", id,
      "UUID", uuid,
      "ORGANIZATION_UUID", "default",
      "PROJECT_UUID", uuid,
      "UUID_PATH", ".",
      "ROOT_UUID", uuid,
      "PRIVATE", Boolean.toString(false),
      "SCOPE", scope,
      "QUALIFIER", qualifier);
    return uuid;
  }
}
