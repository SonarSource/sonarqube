/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v89;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class PopulateQualityGateConditionsMinimumEffectiveLinesDefaultValueTest {

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(
    PopulateQualityGateConditionsMinimumEffectiveLinesDefaultValueTest.class, "schema.sql");

  private final PopulateQualityGateConditionsMinimumEffectiveLinesDefaultValue underTest = new PopulateQualityGateConditionsMinimumEffectiveLinesDefaultValue(db.database());

  @Test
  public void migrates_all_previously_ignored_metrics_with_20_as_effective_size() throws SQLException {
    insertMetric(1L, CoreMetrics.NEW_BLOCKS_DUPLICATED_KEY);
    insertMetric(2L, CoreMetrics.NEW_DUPLICATED_LINES_DENSITY_KEY);
    insertMetric(3L, CoreMetrics.NEW_DUPLICATED_LINES_KEY);
    insertMetric(4L, CoreMetrics.NEW_BRANCH_COVERAGE_KEY);
    insertMetric(5L, CoreMetrics.NEW_LINE_COVERAGE_KEY);
    insertMetric(6L, CoreMetrics.NEW_COVERAGE_KEY);

    insertQualityGateConditions(4L, 1L);
    insertQualityGateConditions(5L, 2L);
    insertQualityGateConditions(6L, 3L);
    insertQualityGateConditions(10L, 4L);
    insertQualityGateConditions(11L, 5L);
    insertQualityGateConditions(12L, 6L);
    insertQualityGateConditions(13L, 1L);
    insertQualityGateConditions(21L, 2L);
    insertQualityGateConditions(22L, 3L);

    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", "uuid1", 20L),
      tuple("uuid5", "uuid2", 20L),
      tuple("uuid6", "uuid3", 20L),
      tuple("uuid10", "uuid4", 20L),
      tuple("uuid11", "uuid5", 20L),
      tuple("uuid12", "uuid6", 20L),
      tuple("uuid13", "uuid1", 20L),
      tuple("uuid21", "uuid2", 20L),
      tuple("uuid22", "uuid3", 20L));
  }

  @Test
  public void migrates_all_previously_non_ignored_metrics_with_0_as_effective_size() throws SQLException {
    insertMetric(1L, CoreMetrics.NEW_BLOCKER_VIOLATIONS_KEY);
    insertMetric(2L, CoreMetrics.NEW_BUGS_KEY);
    insertMetric(3L, CoreMetrics.NEW_CODE_SMELLS_KEY);
    insertMetric(4L, CoreMetrics.NEW_DEVELOPMENT_COST_KEY);
    insertMetric(5L, CoreMetrics.NEW_UNCOVERED_LINES_KEY);

    insertQualityGateConditions(4L, 1L);
    insertQualityGateConditions(5L, 2L);
    insertQualityGateConditions(6L, 3L);
    insertQualityGateConditions(10L, 4L);
    insertQualityGateConditions(11L, 5L);
    insertQualityGateConditions(13L, 1L);
    insertQualityGateConditions(21L, 2L);
    insertQualityGateConditions(22L, 3L);

    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", "uuid1", 0L),
      tuple("uuid5", "uuid2", 0L),
      tuple("uuid6", "uuid3", 0L),
      tuple("uuid10", "uuid4", 0L),
      tuple("uuid11", "uuid5", 0L),
      tuple("uuid13", "uuid1", 0L),
      tuple("uuid21", "uuid2", 0L),
      tuple("uuid22", "uuid3", 0L));
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    insertMetric(1L, CoreMetrics.BRANCH_COVERAGE_KEY);
    insertMetric(2L, CoreMetrics.BLOCKER_VIOLATIONS_KEY);
    insertMetric(3L, CoreMetrics.CODE_SMELLS_KEY);
    insertMetric(6L, CoreMetrics.NEW_COVERAGE_KEY);

    insertQualityGateConditions(4L, 1L);
    insertQualityGateConditions(5L, 2L);
    insertQualityGateConditions(6L, 3L);
    insertQualityGateConditions(12L, 6L);

    underTest.execute();
    // re-entrant
    underTest.execute();

    assertThatTableContains(
      tuple("uuid4", "uuid1", 0L),
      tuple("uuid5", "uuid2", 0L),
      tuple("uuid6", "uuid3", 0L),
      tuple("uuid12", "uuid6", 20L)

    );
  }

  private void assertThatTableContains(Tuple... tuples) {
    List<Map<String, Object>> select = db.select("select uuid, metric_uuid, minimum_effective_lines from quality_gate_conditions");
    assertThat(select).extracting(m -> m.get("UUID"), m -> m.get("METRIC_UUID"), m -> m.get("MINIMUM_EFFECTIVE_LINES"))
      .containsExactlyInAnyOrder(tuples);
  }

  private void insertMetric(Long id, String name) {
    db.executeInsert("metrics",
      "id", id,
      "uuid", "uuid" + id,
      "name", name);
  }

  private void insertQualityGateConditions(Long id, Long metricId) {
    db.executeInsert("quality_gate_conditions",
      "qgate_uuid", "uuid" + 0,
      "uuid", "uuid" + id,
      "metric_uuid", "uuid" + metricId);
  }
}
