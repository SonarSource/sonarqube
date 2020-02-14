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
import org.junit.Before;
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
  private static final int SECURITY_REVIEW_RATING_EFFORT_METRIC_ID = 201;
  private static final String SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY = "security_review_rating_effort";

  private static final int OTHER_METRIC_ID_1 = 1;
  private static final int OTHER_METRIC_ID_2 = 2;
  private static final int OTHER_METRIC_MEASURES_COUNT = 20;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteSecurityReviewRatingMeasuresTest.class, "schema.sql");

  private DataChange underTest = new DeleteSecurityReviewRatingMeasures(db.database());

  @Before
  public void before() {
    insertMetric(OTHER_METRIC_ID_1, "another metric#1");
    insertMetric(OTHER_METRIC_ID_2, "another metric#2");
  }

  @Test
  public void not_fail_if_metrics_not_defined() throws SQLException {
    String projectUuid = insertComponent("PRJ", "TRK");
    insertMeasure(1, SECURITY_REVIEW_RATING_METRIC_ID, projectUuid);
    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, projectUuid);

    underTest.execute();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(1);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(1);
  }

  @Test
  public void not_fail_if_security_review_rating_effort_metric_not_found() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);

    String applicationUuid = insertComponent("PRJ", "TRK");

    insertMeasure(1, SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid);
    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid, applicationUuid);

    generateOtherMetricMeasures(2, applicationUuid);
    generateOtherMetricsLiveMeasures(applicationUuid);

    underTest.execute();

    assertSecurityReviewRatingMeasuresDeleted();
    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void remove_security_rating_review_from_measures_and_live_measures_projects() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);

    String applicationUuid = insertComponent("PRJ", "TRK");

    insertMeasure(1, SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid);
    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid, applicationUuid);

    generateOtherMetricMeasures(2, applicationUuid);
    generateOtherMetricsLiveMeasures(applicationUuid);

    underTest.execute();

    assertSecurityReviewRatingMeasuresDeleted();
    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void remove_security_rating_review_from_measures_and_live_measures_for_portfolios() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);

    String portfolioUuid = insertComponent("PRJ", "VW");
    String subPortfolioUuid = insertComponent("PRJ", "SVW");

    insertMeasure(1, SECURITY_REVIEW_RATING_METRIC_ID, portfolioUuid);
    insertMeasure(2, SECURITY_REVIEW_RATING_METRIC_ID, subPortfolioUuid);

    insertMeasure(3, SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, portfolioUuid);
    insertMeasure(4, SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, subPortfolioUuid);

    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, portfolioUuid, portfolioUuid);
    insertLiveMeasure("uuid-2", SECURITY_REVIEW_RATING_METRIC_ID, subPortfolioUuid, subPortfolioUuid);

    insertLiveMeasure("uuid-3", SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, portfolioUuid, portfolioUuid);
    insertLiveMeasure("uuid-4", SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, subPortfolioUuid, subPortfolioUuid);


    generateOtherMetricMeasures(5, portfolioUuid);
    generateOtherMetricsLiveMeasures(portfolioUuid);

    underTest.execute();

    assertSecurityReviewRatingMeasuresDeleted();
    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void remove_security_rating_review_from_measures_and_live_measures_applications() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);

    String applicationUuid = insertComponent("PRJ", "APP");

    insertMeasure(1, SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid);
    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid, applicationUuid);

    generateOtherMetricMeasures(2, applicationUuid);
    generateOtherMetricsLiveMeasures(applicationUuid);

    underTest.execute();

    assertSecurityReviewRatingMeasuresDeleted();
    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void remove_security_rating_review_from_measures_and_live_measures_mixed() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);

    String portfolioUuid = insertComponent("PRJ", "VW");
    String subPortfolioUuid = insertComponent("PRJ", "SVW");
    String applicationUuid = insertComponent("PRJ", "APP");
    String projectUuid = insertComponent("PRJ", "TRK");

    insertMeasure(1, SECURITY_REVIEW_RATING_METRIC_ID, portfolioUuid);
    insertMeasure(2, SECURITY_REVIEW_RATING_METRIC_ID, subPortfolioUuid);
    insertMeasure(3, SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid);
    insertMeasure(4, SECURITY_REVIEW_RATING_METRIC_ID, projectUuid);

    insertMeasure(5, SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, portfolioUuid);
    insertMeasure(6, SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, subPortfolioUuid);

    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, portfolioUuid, portfolioUuid);
    insertLiveMeasure("uuid-2", SECURITY_REVIEW_RATING_METRIC_ID, subPortfolioUuid, subPortfolioUuid);
    insertLiveMeasure("uuid-3", SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid, applicationUuid);
    insertLiveMeasure("uuid-4", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, projectUuid);
    insertLiveMeasure("uuid-5", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, getRandomUuid());
    insertLiveMeasure("uuid-6", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, getRandomUuid());

    insertLiveMeasure("uuid-7", SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, portfolioUuid, portfolioUuid);
    insertLiveMeasure("uuid-8", SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, subPortfolioUuid, subPortfolioUuid);

    generateOtherMetricMeasures(7, projectUuid);
    generateOtherMetricsLiveMeasures(projectUuid);

    underTest.execute();

    assertSecurityReviewRatingMeasuresDeleted();
    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void not_fail_if_empty_tables() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);

    underTest.execute();

    assertThat(db.countRowsOfTable(PROJECT_MEASURES_TABLE_NAME)).isEqualTo(0);
    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(0);
  }

  private void generateOtherMetricsLiveMeasures(String componentUuid) {
    IntStream.range(0, OTHER_METRIC_MEASURES_COUNT)
      .peek(i -> insertLiveMeasure("uuid-other-" + i, i, componentUuid, getRandomUuid()))
      .boxed()
      .collect(Collectors.toList());
  }

  private void generateOtherMetricMeasures(int startId, String componentUuid) {
    IntStream.range(startId, startId + OTHER_METRIC_MEASURES_COUNT)
      .peek(i -> insertMeasure(i, new Random().nextBoolean() ? OTHER_METRIC_ID_1 : OTHER_METRIC_ID_2, componentUuid))
      .boxed()
      .collect(Collectors.toList());
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
