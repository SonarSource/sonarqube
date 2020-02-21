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

import static com.google.common.primitives.Longs.asList;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class DeleteQgateConditionsUsingSecurityHotspotMetricsTest {

  private static final String TABLE_QUALITY_GATE_CONDITIONS = "quality_gate_conditions";

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(DeleteQgateConditionsUsingSecurityHotspotMetricsTest.class, "schema.sql");

  private DataChange underTest = new DeleteQgateConditionsUsingSecurityHotspotMetrics(db.database());

  @Test
  public void remove_conditions_on_security_hotspots() throws SQLException {
    long securityHotspotsMetric = insertMetric("security_hotspots");
    insertQualityGateCondition(securityHotspotsMetric);
    long newSecurityHotspotsMetric = insertMetric("new_security_hotspots");
    insertQualityGateCondition(newSecurityHotspotsMetric);
    long nclocMetric = insertMetric("ncloc");
    long conditionOnNcloc = insertQualityGateCondition(nclocMetric);

    underTest.execute();

    verifyConditionIds(singletonList(conditionOnNcloc));
  }

  @Test
  public void do_not_remove_any_condition_when_no_security_hotspot_metrics() throws SQLException {
    long nclocMetric = insertMetric("ncloc");
    long conditionOnNcloc = insertQualityGateCondition(nclocMetric);
    long issuesMetric = insertMetric("issues");
    long conditionOnIssues = insertQualityGateCondition(issuesMetric);

    underTest.execute();

    verifyConditionIds(asList(conditionOnNcloc, conditionOnIssues));
  }

  @Test
  public void do_nothing_when_no_condition() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isZero();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    long securityHotspotsMetric = insertMetric("security_hotspots");
    insertQualityGateCondition(securityHotspotsMetric);
    long newSecurityHotspotsMetric = insertMetric("new_security_hotspots");
    insertQualityGateCondition(newSecurityHotspotsMetric);
    long otherMetric = insertMetric("ncloc");
    long conditionOnOtherMetric = insertQualityGateCondition(otherMetric);

    underTest.execute();
    underTest.execute();

    verifyConditionIds(asList(conditionOnOtherMetric));
  }

  private void verifyConditionIds(List<Long> expectedConditionIds) {
    List<Map<String, Object>> results = db.select("select ID from  " + TABLE_QUALITY_GATE_CONDITIONS);
    assertThat(results.stream()
      .map(map -> (long) map.get("ID"))
      .collect(toList()))
        .containsExactlyInAnyOrderElementsOf(expectedConditionIds);
  }

  private long insertQualityGateCondition(long metricId) {
    long qualityGateId = RandomUtils.nextInt();
    Map<String, Object> values = new HashMap<>(ImmutableMap.of("QGATE_ID", qualityGateId, "METRIC_ID", metricId, "OPERATOR", "GT"));
    values.put("VALUE_ERROR", RandomUtils.nextInt());
    db.executeInsert(TABLE_QUALITY_GATE_CONDITIONS, values);
    String sql = format("select id as \"id\" from %s where qgate_id='%s' and metric_id='%s'", TABLE_QUALITY_GATE_CONDITIONS, qualityGateId, metricId);
    return (Long) db
      .selectFirst(sql)
      .get("id");
  }

  private long insertMetric(String key) {
    db.executeInsert("metrics", "NAME", key);
    return (Long) db.selectFirst(format("select id as \"id\" from metrics where name='%s'", key)).get("id");
  }
}
