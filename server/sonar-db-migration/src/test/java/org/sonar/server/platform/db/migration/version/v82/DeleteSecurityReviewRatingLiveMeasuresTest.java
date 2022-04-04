/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

public class DeleteSecurityReviewRatingLiveMeasuresTest {
  private static final String LIVE_MEASURES_TABLE_NAME = "LIVE_MEASURES";

  private static final int SECURITY_REVIEW_RATING_METRIC_ID = 200;
  private static final String SECURITY_REVIEW_RATING_METRIC_KEY = "security_review_rating";
  private static final int SECURITY_REVIEW_RATING_EFFORT_METRIC_ID = 201;
  private static final String SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY = "security_review_rating_effort";

  private static final int OTHER_METRIC_ID_1 = 1;
  private static final int OTHER_METRIC_ID_2 = 2;
  private static final int OTHER_METRIC_MEASURES_COUNT = 20;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteSecurityReviewRatingLiveMeasuresTest.class, "schema.sql");

  private final DataChange underTest = new DeleteSecurityReviewRatingLiveMeasures(db.database());

  @Before
  public void before() {
    insertMetric(OTHER_METRIC_ID_1, "another metric#1");
    insertMetric(OTHER_METRIC_ID_2, "another metric#2");
  }

  @Test
  public void not_fail_if_metrics_not_defined() throws SQLException {
    String projectUuid = insertComponent("TRK");
    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, projectUuid, projectUuid);

    underTest.execute();

    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(1);
  }

  @Test
  public void not_fail_if_security_review_rating_effort_metric_not_found() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);

    String applicationUuid = insertComponent("TRK");

    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid, applicationUuid);

    generateOtherMetricsLiveMeasures(applicationUuid);

    underTest.execute();

    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void remove_security_rating_review_from_live_measures_projects() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);

    String applicationUuid = insertComponent("TRK");

    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid, applicationUuid);

    generateOtherMetricsLiveMeasures(applicationUuid);

    underTest.execute();

    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void remove_security_rating_review_from_live_measures_for_portfolios() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);

    String portfolioUuid = insertComponent("VW");
    String subPortfolioUuid = insertComponent("SVW");

    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, portfolioUuid, portfolioUuid);
    insertLiveMeasure("uuid-2", SECURITY_REVIEW_RATING_METRIC_ID, subPortfolioUuid, subPortfolioUuid);

    insertLiveMeasure("uuid-3", SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, portfolioUuid, portfolioUuid);
    insertLiveMeasure("uuid-4", SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, subPortfolioUuid, subPortfolioUuid);

    generateOtherMetricsLiveMeasures(portfolioUuid);

    underTest.execute();

    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void remove_security_rating_review_from_live_measures_applications() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);
    insertMetric(SECURITY_REVIEW_RATING_EFFORT_METRIC_ID, SECURITY_REVIEW_RATING_EFFORT_METRIC_KEY);

    String applicationUuid = insertComponent("APP");

    insertLiveMeasure("uuid-1", SECURITY_REVIEW_RATING_METRIC_ID, applicationUuid, applicationUuid);

    generateOtherMetricsLiveMeasures(applicationUuid);

    underTest.execute();

    assertSecurityReviewRatingLiveMeasuresDeleted();

    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isEqualTo(OTHER_METRIC_MEASURES_COUNT);

    // should not fail if called twice
    underTest.execute();
  }

  @Test
  public void not_fail_if_empty_tables() throws SQLException {
    insertMetric(SECURITY_REVIEW_RATING_METRIC_ID, SECURITY_REVIEW_RATING_METRIC_KEY);

    underTest.execute();

    assertThat(db.countRowsOfTable(LIVE_MEASURES_TABLE_NAME)).isZero();
  }

  private void generateOtherMetricsLiveMeasures(String componentUuid) {
    IntStream.range(0, OTHER_METRIC_MEASURES_COUNT)
      .peek(i -> insertLiveMeasure("uuid-other-" + i, i, componentUuid, getRandomUuid()))
      .boxed()
      .collect(Collectors.toList());
  }

  private void assertSecurityReviewRatingLiveMeasuresDeleted() {
    assertThat(db.countSql("select count(uuid) from LIVE_MEASURES where metric_id = " + SECURITY_REVIEW_RATING_METRIC_ID))
      .isZero();
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

  private String insertComponent(String qualifier) {
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
      "SCOPE", "PRJ",
      "QUALIFIER", qualifier);
    return uuid;
  }
}
