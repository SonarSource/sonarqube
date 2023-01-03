/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures;

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteSecurityReviewRatingProjectMeasuresTest {
  private static final String PROJECT_MEASURES_TABLE = "project_measures";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteSecurityReviewRatingProjectMeasuresTest.class, "schema.sql");

  private DataChange underTest = new DeleteSecurityReviewRatingProjectMeasures(db.database());

  @Test
  public void removes_project_measure_for_review_rating_and_review_rating_effort_metrics() throws SQLException {
    String codeSmellMetric = insertMetric(1L, "security_review_rating");
    insertProjectMeasure(codeSmellMetric);
    String reviewRatingEffort = insertMetric(2L, "security_review_rating_effort");
    insertProjectMeasure(reviewRatingEffort);
    String anotherMetric = insertMetric(3L, "another_metric");
    String anotherMetricProjectMeasure = insertProjectMeasure(anotherMetric);

    underTest.execute();
    verifyProjectMeasureIds(singletonList(anotherMetricProjectMeasure));
  }


  private String insertMetric(Long id, String key) {
    db.executeInsert("metrics",
      "id", id,
      "uuid", id,
      "name", key);

    return (String) db.selectFirst(format("select uuid as \"uuid\" from metrics where name='%s'", key)).get("uuid");
  }

  private String insertProjectMeasure(String metricUuid) {
    double projectMeasureUUid = RandomUtils.nextDouble();
    Map<String, Object> values = new HashMap<>(ImmutableMap.of("uuid", projectMeasureUUid, "metric_uuid", metricUuid,
      "analysis_uuid", "analysis_uuid", "component_uuid", "component_uuid"));
    db.executeInsert(PROJECT_MEASURES_TABLE, values);
    String sql = format("select uuid as \"uuid\" from %s where metric_uuid='%s'", PROJECT_MEASURES_TABLE, metricUuid);
    return (String) db
      .selectFirst(sql)
      .get("uuid");
  }

  private void verifyProjectMeasureIds(List<String> expectedProjectMeasureIds) {
    List<Map<String, Object>> results = db.select("select uuid from  " + PROJECT_MEASURES_TABLE);
    assertThat(results.stream()
      .map(map -> (String) map.get("UUID"))
      .collect(toList()))
      .containsExactlyInAnyOrderElementsOf(expectedProjectMeasureIds);
  }
}
