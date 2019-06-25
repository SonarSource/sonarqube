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
package org.sonar.server.platform.db.migration.version.v79;

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.CoreDbTester;

import static com.google.common.primitives.Longs.asList;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class RemoveQGConditionsOnSecurityReviewRatingTest {

  private static final String TABLE_QUALITY_GATE_CONDITIONS = "quality_gate_conditions";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(RemoveQGConditionsOnSecurityReviewRatingTest.class, "quality_gate_conditions.sql");

  private RemoveQGConditionsOnSecurityReviewRating underTest = new RemoveQGConditionsOnSecurityReviewRating(db.database());

  @Test
  public void migrate() throws SQLException {
    long securityReviewRatingMetric = insertMetric("security_review_rating");
    long otherMetric = insertMetric("other");
    insertQualityGateCondition(securityReviewRatingMetric);
    insertQualityGateCondition(securityReviewRatingMetric);
    long conditionOnOtherMetric = insertQualityGateCondition(otherMetric);

    underTest.execute();

    verifyConditionIds(asList(conditionOnOtherMetric));
  }

  @Test
  public void do_nothing_when_no_condition_on_security_review_rating() throws SQLException {
    long otherMetric = insertMetric("other");
    long condition1 = insertQualityGateCondition(otherMetric);
    long condition2 = insertQualityGateCondition(otherMetric);

    underTest.execute();

    verifyConditionIds(asList(condition1, condition2));
  }

  @Test
  public void do_nothing_when_no_condition() throws SQLException {
    insertMetric("other");

    underTest.execute();

    assertThat(db.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isZero();
  }

  @Test
  public void migration_is_reentrant() throws SQLException {
    long securityReviewRatingMetric = insertMetric("security_review_rating");
    long otherMetric = insertMetric("other");
    insertQualityGateCondition(securityReviewRatingMetric);
    insertQualityGateCondition(securityReviewRatingMetric);
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
