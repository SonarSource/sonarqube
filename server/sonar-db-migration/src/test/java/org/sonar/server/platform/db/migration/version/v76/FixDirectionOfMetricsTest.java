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
package org.sonar.server.platform.db.migration.version.v76;

import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class FixDirectionOfMetricsTest {

  private static final String TESTS_METRIC_NAME = "tests";
  private static final String CONDITIONS_TO_COVER_METRIC_NAME = "conditions_to_cover";
  private static final String NEW_CONDITIONS_TO_COVER_METRIC_NAME = "new_conditions_to_cover";
  private static final String UNRELATED_METRIC_NAME = "unrelated";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(FixDirectionOfMetricsTest.class, "metrics.sql");

  private static long idCounter = 1;

  private FixDirectionOfMetrics underTest = new FixDirectionOfMetrics(db.database());

  @Test
  @UseDataProvider("interestingCombinationsOfInitialMetricsAndValues")
  public void fixes_direction_of_relevant_metrics(Map<String, Integer> initialMetricsAndValues) throws SQLException {
    insertMetric(TESTS_METRIC_NAME, initialMetricsAndValues.get(TESTS_METRIC_NAME));
    insertMetric(CONDITIONS_TO_COVER_METRIC_NAME, initialMetricsAndValues.get(CONDITIONS_TO_COVER_METRIC_NAME));
    insertMetric(NEW_CONDITIONS_TO_COVER_METRIC_NAME, initialMetricsAndValues.get(NEW_CONDITIONS_TO_COVER_METRIC_NAME));
    insertMetric(UNRELATED_METRIC_NAME, initialMetricsAndValues.get(UNRELATED_METRIC_NAME));

    underTest.execute();

    assertThat(selectMetricDirection(TESTS_METRIC_NAME)).isEqualTo(1L);
    assertThat(selectMetricDirection(CONDITIONS_TO_COVER_METRIC_NAME)).isEqualTo(-1L);
    assertThat(selectMetricDirection(NEW_CONDITIONS_TO_COVER_METRIC_NAME)).isEqualTo(-1L);
    assertThat(selectMetricDirection(UNRELATED_METRIC_NAME)).isEqualTo((long) initialMetricsAndValues.get(UNRELATED_METRIC_NAME));
  }

  @DataProvider
  public static Object[][] interestingCombinationsOfInitialMetricsAndValues() {
    List<Map<String, Integer>> mappings = new ArrayList<>();
    int[] directions = {-1, 0, 1};
    for (int unrelatedDirection : directions) {
      // the target metrics with old (wrong) direction, and an unrelated metric with any direction
      mappings.add(ImmutableMap.of(
        TESTS_METRIC_NAME, -1,
        CONDITIONS_TO_COVER_METRIC_NAME, 0,
        NEW_CONDITIONS_TO_COVER_METRIC_NAME, 0,
        UNRELATED_METRIC_NAME, unrelatedDirection));

      // the target metrics with correct direction, and an unrelated metric with any direction
      mappings.add(ImmutableMap.of(
        TESTS_METRIC_NAME, 1,
        CONDITIONS_TO_COVER_METRIC_NAME, -1,
        NEW_CONDITIONS_TO_COVER_METRIC_NAME, -1,
        UNRELATED_METRIC_NAME, unrelatedDirection));
    }
    return mappings.stream().map(m -> new Object[] {m}).toArray(Object[][]::new);
  }

  private void insertMetric(String name, int direction) {
    long id = idCounter++;
    db.executeInsert("METRICS",
      "ID", id,
      "NAME", name,
      "DIRECTION", direction);
  }

  private long selectMetricDirection(String metricName) {
    return (long) db.selectFirst("SELECT direction FROM metrics WHERE name = '" + metricName + "'").get("DIRECTION");
  }
}
