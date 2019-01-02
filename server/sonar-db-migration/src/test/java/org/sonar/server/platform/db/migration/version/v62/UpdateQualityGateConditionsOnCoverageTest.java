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
package org.sonar.server.platform.db.migration.version.v62;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class UpdateQualityGateConditionsOnCoverageTest {

  private static final String TABLE_QUALITY_GATES = "quality_gates";
  private static final String TABLE_QUALITY_GATE_CONDITIONS = "quality_gate_conditions";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(UpdateQualityGateConditionsOnCoverageTest.class, "schema.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private UpdateQualityGateConditionsOnCoverage underTest = new UpdateQualityGateConditionsOnCoverage(dbTester.database());

  @Test
  public void move_overall_coverage_condition_to_coverage() throws SQLException {
    Map<String, Long> metricIdsByMetricKeys = insertSampleMetrics();
    long qualityGateId = insertQualityGate("default");
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("overall_coverage"), null, "GT", "10", null);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(1);
    verifyConditions(qualityGateId, new QualityGateCondition("coverage", null, "GT", "10", null));
  }

  @Test
  public void move_overall_coverage_condition_to_coverage_when_overall_coverage_exists_condition_on_overall_coverage_exists() throws SQLException {
    Map<String, Long> metricIdsByMetricKeys = insertSampleMetrics();
    long qualityGateId = insertQualityGate("default");
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("overall_coverage"), null, "GT", "10", null);
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("coverage"), null, "LT", null, "20");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(1);
    verifyConditions(qualityGateId, new QualityGateCondition("coverage", null, "GT", "10", null));
  }

  @Test
  public void remove_it_coverage_condition_when_overall_coverage_condition_exists_and_no_coverage_condition() throws Exception {
    Map<String, Long> metricIdsByMetricKeys = insertSampleMetrics();
    long qualityGateId = insertQualityGate("default");
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("overall_coverage"), null, "GT", "10", null);
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("it_coverage"), null, "LT", null, "20");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(1);
    verifyConditions(qualityGateId, new QualityGateCondition("coverage", null, "GT", "10", null));
  }

  @Test
  public void keep_coverage_condition_when_no_overall_and_it_coverage() throws SQLException {
    Map<String, Long> metricIdsByMetricKeys = insertSampleMetrics();
    long qualityGateId = insertQualityGate("default");
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("coverage"), null, "GT", "10", null);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(1);
    verifyConditions(qualityGateId, new QualityGateCondition("coverage", null, "GT", "10", null));
  }

  @Test
  public void remove_it_coverage_condition_when_coverage_condition_exists_and_no_overall_coverage_condition() throws SQLException {
    Map<String, Long> metricIdsByMetricKeys = insertSampleMetrics();
    long qualityGateId = insertQualityGate("default");
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("coverage"), null, "GT", "10", null);
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("it_coverage"), null, "LT", null, "20");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(1);
    verifyConditions(qualityGateId, new QualityGateCondition("coverage", null, "GT", "10", null));
  }

  @Test
  public void move_it_coverage_condition_to_coverage_when_only_it_coverage_condition() throws SQLException {
    Map<String, Long> metricIdsByMetricKeys = insertSampleMetrics();
    long qualityGateId = insertQualityGate("default");
    insertQualityGateCondition(qualityGateId, metricIdsByMetricKeys.get("it_coverage"), null, "GT", "10", null);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(1);
    verifyConditions(qualityGateId, new QualityGateCondition("coverage", null, "GT", "10", null));
  }

  @Test
  public void move_new_coverage_conditions() throws SQLException {
    Map<String, Long> metricIdsByMetricKeys = insertMetrics("new_coverage", "new_overall_coverage", "new_it_coverage");
    long qualityGate1 = insertQualityGate("qualityGate1");
    insertQualityGateCondition(qualityGate1, metricIdsByMetricKeys.get("new_coverage"), 1L, "GT", "10", null);
    insertQualityGateCondition(qualityGate1, metricIdsByMetricKeys.get("new_overall_coverage"), 1L, "GT", "7", "15");
    insertQualityGateCondition(qualityGate1, metricIdsByMetricKeys.get("new_it_coverage"), 2L, "LT", "8", null);
    long qualityGate2 = insertQualityGate("qualityGate2");
    insertQualityGateCondition(qualityGate2, metricIdsByMetricKeys.get("new_overall_coverage"), 2L, "GT", "15", null);
    insertQualityGateCondition(qualityGate2, metricIdsByMetricKeys.get("new_it_coverage"), 2L, "GT", null, "5");
    long qualityGate3 = insertQualityGate("qualityGate3");
    insertQualityGateCondition(qualityGate3, metricIdsByMetricKeys.get("new_it_coverage"), 3L, "GT", null, "5");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(3);
    verifyConditions(qualityGate1, new QualityGateCondition("new_coverage", 1L, "GT", "7", "15"));
    verifyConditions(qualityGate2, new QualityGateCondition("new_coverage", 2L, "GT", "15", null));
    verifyConditions(qualityGate3, new QualityGateCondition("new_coverage", 3L, "GT", null, "5"));
  }

  @DataProvider
  public static Object[][] metricKeys() {
    List<String> metrics = ImmutableList.of(
      "coverage", "lines_to_cover", "uncovered_lines", "line_coverage", "conditions_to_cover", "uncovered_conditions", "branch_coverage");
    Object[][] res = new Object[metrics.size()][3];
    int i = 0;
    for (String metricKey : metrics) {
      res[i][0] = metricKey;
      res[i][1] = "overall_" + metricKey;
      res[i][2] = "it_" + metricKey;
      i++;
    }
    return res;
  }

  @Test
  @UseDataProvider("metricKeys")
  public void move_conditions_from_many_quality_gates_on_all_metrics(String coverageKey, String overallCoverageKey, String itCoverageKey) throws SQLException {
    Map<String, Long> metricIdsByMetricKeys = insertMetrics(coverageKey, overallCoverageKey, itCoverageKey, "other");
    long qualityGate1 = insertQualityGate("qualityGate1");
    insertQualityGateCondition(qualityGate1, metricIdsByMetricKeys.get(coverageKey), null, "GT", "10", null);
    insertQualityGateCondition(qualityGate1, metricIdsByMetricKeys.get(overallCoverageKey), null, "GT", "7", "15");
    insertQualityGateCondition(qualityGate1, metricIdsByMetricKeys.get(itCoverageKey), null, "LT", "8", null);
    long qualityGate2 = insertQualityGate("qualityGate2");
    insertQualityGateCondition(qualityGate2, metricIdsByMetricKeys.get(overallCoverageKey), null, "GT", "15", null);
    insertQualityGateCondition(qualityGate2, metricIdsByMetricKeys.get(itCoverageKey), null, "GT", null, "5");
    long qualityGate3 = insertQualityGate("qualityGate3");
    insertQualityGateCondition(qualityGate3, metricIdsByMetricKeys.get(itCoverageKey), null, "GT", null, "5");
    insertQualityGateCondition(qualityGate3, metricIdsByMetricKeys.get("other"), null, "GT", "11", null);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(4);
    verifyConditions(qualityGate1, new QualityGateCondition(coverageKey, null, "GT", "7", "15"));
    verifyConditions(qualityGate2, new QualityGateCondition(coverageKey, null, "GT", "15", null));
    verifyConditions(qualityGate3, new QualityGateCondition(coverageKey, null, "GT", null, "5"), new QualityGateCondition("other", null, "GT", "11", null));
  }

  @Test
  public void does_not_update_conditions_on_none_related_coverage_metrics() throws Exception {
    insertMetrics();
    long metric1 = insertMetric("metric1");
    long metric2 = insertMetric("metric2");
    long qualityGate1 = insertQualityGate("qualityGate1");
    insertQualityGateCondition(qualityGate1, metric1, null, "GT", "10", null);
    long qualityGate2 = insertQualityGate("qualityGate2");
    insertQualityGateCondition(qualityGate2, metric2, null, "LT", null, "20");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(2);
    verifyConditions(qualityGate1, new QualityGateCondition("metric1", null, "GT", "10", null));
    verifyConditions(qualityGate2, new QualityGateCondition("metric2", null, "LT", null, "20"));
  }

  @Test
  public void move_conditions_linked_to_same_metric() throws Exception {
    insertMetric("coverage");
    long metricId = insertMetric("overall_coverage");
    long qualityGate = insertQualityGate("qualityGate");
    insertQualityGateCondition(qualityGate, metricId, null, "GT", "7", "15");
    insertQualityGateCondition(qualityGate, metricId, 1L, "GT", "10", null);

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isEqualTo(2);
    verifyConditions(qualityGate,
      new QualityGateCondition("coverage", null, "GT", "7", "15"),
      new QualityGateCondition("coverage", 1L, "GT", "10", null));
  }

  @Test
  public void does_nothing_when_no_quality_gates() throws Exception {
    insertMetrics("coverage", "new_coverage", "overall_coverage");

    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isZero();
  }

  @Test
  public void does_nothing_when_no_metrics() throws Exception {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable(TABLE_QUALITY_GATE_CONDITIONS)).isZero();
  }

  private Map<String, Long> insertSampleMetrics() {
    return insertMetrics("coverage", "overall_coverage", "it_coverage");
  }

  private Map<String, Long> insertMetrics(String... metrics) {
    Map<String, Long> metricIdsByMetricKeys = new HashMap<>();
    for (String metricKey : metrics) {
      metricIdsByMetricKeys.put(metricKey, insertMetric(metricKey));
    }
    return metricIdsByMetricKeys;
  }

  private long insertMetric(String key) {
    dbTester.executeInsert("metrics", "NAME", key);
    return (Long) dbTester.selectFirst(format("select id as \"id\" from metrics where name='%s'", key)).get("id");
  }

  private long insertQualityGate(String qualityGate) {
    dbTester.executeInsert(TABLE_QUALITY_GATES, "NAME", qualityGate);
    return (Long) dbTester.selectFirst(format("select id as \"id\" from %s where name='%s'", TABLE_QUALITY_GATES, qualityGate)).get("id");
  }

  private long insertQualityGateCondition(long qualityGateId, long metricId, @Nullable Long period, String operator, @Nullable String error, @Nullable String warning) {
    Map<String, Object> values = new HashMap<>(ImmutableMap.of("QGATE_ID", qualityGateId, "METRIC_ID", metricId, "OPERATOR", operator));
    if (period != null) {
      values.put("PERIOD", period);
    }
    if (error != null) {
      values.put("VALUE_ERROR", error);
    }
    if (warning != null) {
      values.put("VALUE_WARNING", warning);
    }
    dbTester.executeInsert(TABLE_QUALITY_GATE_CONDITIONS, values);
    String sql = format("select id as \"id\" from %s where qgate_id='%s' and metric_id='%s'", TABLE_QUALITY_GATE_CONDITIONS, qualityGateId, metricId);
    sql += period == null ? "" : format(" and period='%s'", period);
    return (Long) dbTester
      .selectFirst(sql)
      .get("id");
  }

  private void verifyConditions(long qualityGateId, QualityGateCondition... expectedConditions) {
    List<Map<String, Object>> results = dbTester.select(
      format("select m.name as \"metricKey\", qgc.period as \"period\", qgc.operator as \"operator\", qgc.value_error as \"error\", qgc.value_warning as \"warning\" from %s qgc " +
        "inner join metrics m on m.id=qgc.metric_id " +
        "where qgc.qgate_id = '%s'", TABLE_QUALITY_GATE_CONDITIONS, qualityGateId));
    List<QualityGateCondition> conditions = results.stream().map(QualityGateCondition::new).collect(Collectors.toList());
    assertThat(conditions).containsOnly(expectedConditions);
  }

  private static class QualityGateCondition {
    String metricKey;
    Long period;
    String operator;
    String valueError;
    String valueWarning;

    public QualityGateCondition(String metricKey, @Nullable Long period, String operator, @Nullable String valueError, @Nullable String valueWarning) {
      this.metricKey = metricKey;
      this.period = period;
      this.operator = operator;
      this.valueError = valueError;
      this.valueWarning = valueWarning;
    }

    QualityGateCondition(Map<String, Object> map) {
      this.metricKey = (String) map.get("metricKey");
      this.period = (Long) map.get("period");
      this.operator = (String) map.get("operator");
      this.valueError = (String) map.get("error");
      this.valueWarning = (String) map.get("warning");
    }

    public String getMetricKey() {
      return metricKey;
    }

    @CheckForNull
    public Long getPeriod() {
      return period;
    }

    public String getOperator() {
      return operator;
    }

    @CheckForNull
    public String getValueError() {
      return valueError;
    }

    @CheckForNull
    public String getValueWarning() {
      return valueWarning;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      QualityGateCondition that = (QualityGateCondition) o;
      return new EqualsBuilder()
        .append(metricKey, that.getMetricKey())
        .append(period, that.getPeriod())
        .append(operator, that.getOperator())
        .append(valueError, that.getValueError())
        .append(valueWarning, that.getValueWarning())
        .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(15, 31)
        .append(metricKey)
        .append(period)
        .append(operator)
        .append(valueError)
        .append(valueWarning)
        .toHashCode();
    }

    @Override
    public String toString() {
      return "QualityGateCondition{" +
        "metricKey='" + metricKey + '\'' +
        ", period=" + period +
        ", operator=" + operator +
        ", valueError='" + valueError + '\'' +
        ", valueWarning='" + valueWarning + '\'' +
        '}';
    }
  }

}
