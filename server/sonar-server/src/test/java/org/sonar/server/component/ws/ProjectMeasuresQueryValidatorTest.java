/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.component.ws;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.server.measure.index.ProjectMeasuresQuery;

import static org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion.create;

public class ProjectMeasuresQueryValidatorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void query_with_empty_metrics_is_valid() throws Exception {
    ProjectMeasuresQueryValidator.validate(new ProjectMeasuresQuery());
  }

  @Test
  public void filter_by_ncloc_is_valid() throws Exception {
    assertValidFilterKey("ncloc");
  }

  @Test
  public void filter_by_duplicated_lines_density_is_valid() throws Exception {
    assertValidFilterKey("duplicated_lines_density");
  }

  @Test
  public void filter_by_coverage_is_valid() throws Exception {
    assertValidFilterKey("coverage");
  }

  @Test
  public void filter_by_sqale_rating_is_valid() throws Exception {
    assertValidFilterKey("sqale_rating");
  }

  @Test
  public void filter_by_reliability_rating_is_valid() throws Exception {
    assertValidFilterKey("reliability_rating");
  }

  @Test
  public void filter_by_security_rating_is_valid() throws Exception {
    assertValidFilterKey("security_rating");
  }

  @Test
  public void filter_by_alert_status_is_valid() throws Exception {
    assertValidFilterKey("alert_status");
  }

  @Test
  public void filter_by_ncloc_language_distribution_is_valid() throws Exception {
    assertValidFilterKey("ncloc_language_distribution");
  }

  @Test
  public void filter_by_new_security_rating_is_valid() throws Exception {
    assertValidFilterKey("new_security_rating");
  }

  @Test
  public void filter_by_new_maintainability_rating_is_valid() throws Exception {
    assertValidFilterKey("new_maintainability_rating");
  }

  @Test
  public void filter_by_new_coverage_is_valid() throws Exception {
    assertValidFilterKey("new_coverage");
  }

  @Test
  public void filter_by_new_duplicated_lines_density_is_valid() throws Exception {
    assertValidFilterKey("new_duplicated_lines_density");
  }

  @Test
  public void filter_by_new_lines_is_valid() throws Exception {
    assertValidFilterKey("new_lines");
  }

  @Test
  public void filter_by_new_reliability_rating_is_valid() throws Exception {
    assertValidFilterKey("new_reliability_rating");
  }

  @Test
  public void filter_by_bla_is_invalid() throws Exception {
    assertInvalidFilterKey("bla");
  }

  @Test
  public void filter_by_bla_and_new_lines_is_invalid() throws Exception {
    assertInvalidFilterKeys("Following metrics are not supported: 'bla'", "bla", "new_lines");
  }

  @Test
  public void filter_by_new_lines_and_bla_is_invalid() throws Exception {
    assertInvalidFilterKeys("Following metrics are not supported: 'bla'", "new_lines", "bla");
  }

  @Test
  public void filter_by_NeW_LiNeS_is_invalid() throws Exception {
    assertInvalidFilterKey("NeW_LiNeS");
  }

  @Test
  public void filter_by_empty_string_is_invalid() throws Exception {
    assertInvalidFilterKey("");
  }

  @Test
  public void sort_by_ncloc_is_valid() throws Exception {
    assertValidSortKey("ncloc");
  }

  @Test
  public void sort_by_duplicated_lines_density_is_valid() throws Exception {
    assertValidSortKey("duplicated_lines_density");
  }

  @Test
  public void sort_by_coverage_is_valid() throws Exception {
    assertValidSortKey("coverage");
  }

  @Test
  public void sort_by_sqale_rating_is_valid() throws Exception {
    assertValidSortKey("sqale_rating");
  }

  @Test
  public void sort_by_reliability_rating_is_valid() throws Exception {
    assertValidSortKey("reliability_rating");
  }

  @Test
  public void sort_by_security_rating_is_valid() throws Exception {
    assertValidSortKey("security_rating");
  }

  @Test
  public void sort_by_alert_status_is_valid() throws Exception {
    assertValidSortKey("alert_status");
  }

  @Test
  public void sort_by_ncloc_language_distribution_is_valid() throws Exception {
    assertValidSortKey("ncloc_language_distribution");
  }

  @Test
  public void sort_by_new_security_rating_is_valid() throws Exception {
    assertValidSortKey("new_security_rating");
  }

  @Test
  public void sort_by_new_maintainability_rating_is_valid() throws Exception {
    assertValidSortKey("new_maintainability_rating");
  }

  @Test
  public void sort_by_new_coverage_is_valid() throws Exception {
    assertValidSortKey("new_coverage");
  }

  @Test
  public void sort_by_new_duplicated_lines_density_is_valid() throws Exception {
    assertValidSortKey("new_duplicated_lines_density");
  }

  @Test
  public void sort_by_new_lines_is_valid() throws Exception {
    assertValidSortKey("new_lines");
  }

  @Test
  public void sort_by_new_reliability_rating_is_valid() throws Exception {
    assertValidSortKey("new_reliability_rating");
  }

  @Test
  public void sort_by_bla_is_invalid() throws Exception {
    assertInvalidSortKey("bla");
  }

  @Test
  public void sort_by_NeW_lInEs_is_invalid() throws Exception {
    assertInvalidSortKey("NeW_lInEs");
  }

  @Test
  public void sort_by_empty_string_is_invalid() throws Exception {
    assertInvalidSortKey("");
  }

  private void assertValidSortKey(String metricKey) {
    // do not expect an exception
    ProjectMeasuresQueryValidator.validate(new ProjectMeasuresQuery().setSort(metricKey));
  }

  private void assertInvalidSortKey(String metricKey) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Following metrics are not supported: '" + metricKey + "'");

    ProjectMeasuresQueryValidator.validate(new ProjectMeasuresQuery().setSort(metricKey));
  }

  private static void assertValidFilterKey(String... metricKeys) {
    // do not expect an exception
    validateFilterKeys(metricKeys);
  }

  private void assertInvalidFilterKey(String metricKey) {
    assertInvalidFilterKeys("Following metrics are not supported: '" + metricKey + "'", metricKey);
  }

  private void assertInvalidFilterKeys(String message, String... metricKeys) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(message);
    validateFilterKeys(metricKeys);
  }

  private static void validateFilterKeys(String... metricKeys) {
    ProjectMeasuresQuery query = new ProjectMeasuresQuery();
    Arrays.stream(metricKeys).forEachOrdered(metricKey -> query.addMetricCriterion(create(metricKey, FilterParser.Operator.LT, 80d)));
    ProjectMeasuresQueryValidator.validate(query);
  }
}
