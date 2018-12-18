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

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.SQLException;
import java.util.Date;
import java.util.Random;
import javax.annotation.Nullable;
import org.assertj.core.groups.Tuple;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.TestSystem2;
import org.sonar.db.CoreDbTester;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@RunWith(DataProviderRunner.class)
public class MigrateNoMoreUsedQualityGateConditionsTest {

  private static final int DIRECTION_WORST = -1;
  private static final int DIRECTION_BETTER = 1;
  private static final int DIRECTION_NONE = 0;

  private final static long PAST = 10_000_000_000L;
  private final static long NOW = 50_000_000_000L;

  @Rule
  public CoreDbTester db = CoreDbTester.createForSchema(MigrateNoMoreUsedQualityGateConditionsTest.class, "qg-schema.sql");

  private System2 system2 = new TestSystem2().setNow(NOW);

  private Random random = new Random();

  private MigrateNoMoreUsedQualityGateConditions underTest = new MigrateNoMoreUsedQualityGateConditions(db.database(), system2);

  @Test
  public void remove_conditions_using_only_warning() throws SQLException {
    long qualityGate = insertQualityGate(false);
    long ncloc = insertMetric("ncloc", DIRECTION_WORST, "INT");
    long lines = insertMetric("lines", DIRECTION_WORST, "INT");
    long issues = insertMetric("violations", DIRECTION_WORST, "INT");
    long coverage = insertMetric("coverage", DIRECTION_BETTER, "PERCENT");
    long conditionWithWarning1 = insertCondition(qualityGate, ncloc, "GT", null, "10", null);
    long conditionWithWarning2 = insertCondition(qualityGate, lines, "GT", null, "15", null);
    long conditionWithError = insertCondition(qualityGate, issues, "GT", "5", null, null);

    underTest.execute();

    assertConditions(
      tuple(conditionWithError, issues, "5", null, null));
  }

  @Test
  public void update_conditions_using_error_and_warning() throws SQLException {
    long qualityGate = insertQualityGate(false);
    long issues = insertMetric("violations", DIRECTION_WORST, "INT");
    long coverage = insertMetric("coverage", DIRECTION_BETTER, "PERCENT");
    long newLines = insertMetric("new_lines", DIRECTION_WORST, "INT");
    long conditionWithError = insertCondition(qualityGate, issues, "GT", "5", null, null);
    long conditionWithErrorAndWarning1 = insertCondition(qualityGate, coverage, "LT", "5", "10", null);
    long conditionWithErrorAndWarning2 = insertCondition(qualityGate, newLines, "GT", "7", "13", 1);

    underTest.execute();

    assertConditions(
      tuple(conditionWithError, issues, "5", null, null),
      tuple(conditionWithErrorAndWarning1, coverage, "5", null, null),
      tuple(conditionWithErrorAndWarning2, newLines, "7", null, 1L)
    );
  }

  @Test
  @UseDataProvider("metricsHavingNoLinkedLeakMetrics")
  public void delete_condition_on_not_supported_leak_period_metric(String metricKey) throws SQLException {
    long qualityGate = insertQualityGate(false);
    long metric = insertMetric(metricKey, DIRECTION_BETTER, "INT");
    insertCondition(qualityGate, metric, "LT", "5", null, 1);

    underTest.execute();

    assertThat(db.countRowsOfTable("quality_gate_conditions")).isZero();
  }

  @DataProvider
  public static Object[][] metricsHavingNoLinkedLeakMetrics() {
    return new Object[][] {
      {"statements"},
      {"functions"}
    };
  }

  @Test
  @UseDataProvider("supportedLeakPeriodMetrics")
  public void update_condition_on_supported_leak_period_metric(String metricKey, String relatedMetricKeyOnLeakPeriod) throws SQLException {
    long qualityGate = insertQualityGate(false);
    long metric = insertMetric(metricKey, DIRECTION_BETTER, "INT");
    long relatedMetricOnLeakPeriod = insertMetric(relatedMetricKeyOnLeakPeriod, DIRECTION_BETTER, "INT");
    long condition = insertCondition(qualityGate, metric, "LT", "5", null, 1);

    underTest.execute();

    assertConditions(tuple(condition, relatedMetricOnLeakPeriod, "5", null, 1L));
  }

  @Test
  @UseDataProvider("supportedLeakPeriodMetrics")
  public void remove_condition_on_supported_leak_period_metric_when_condition_already_exists(String metricKey, String relatedMetricKeyOnLeakPeriod) throws SQLException {
    long qualityGate = insertQualityGate(false);
    long metric = insertMetric(metricKey, DIRECTION_BETTER, "INT");
    long condition = insertCondition(qualityGate, metric, "LT", "10", null, 1);
    long relatedMetricOnLeakPeriod = insertMetric(relatedMetricKeyOnLeakPeriod, DIRECTION_BETTER, "INT");
    long leakCondition = insertCondition(qualityGate, relatedMetricOnLeakPeriod, "LT", "5", null, 1);

    underTest.execute();

    assertConditions(tuple(leakCondition, relatedMetricOnLeakPeriod, "5", null, 1L));
  }

  @DataProvider
  public static Object[][] supportedLeakPeriodMetrics() {
    return new Object[][] {
      {"branch_coverage", "new_branch_coverage"},
      {"conditions_to_cover", "new_conditions_to_cover"},
      {"coverage", "new_coverage"},
      {"line_coverage", "new_line_coverage"},
      {"lines_to_cover", "new_lines_to_cover"},
      {"uncovered_conditions", "new_uncovered_conditions"},
      {"uncovered_lines", "new_uncovered_lines"},
      {"duplicated_blocks", "new_duplicated_blocks"},
      {"duplicated_lines", "new_duplicated_lines"},
      {"duplicated_lines_density", "new_duplicated_lines_density"},
      {"blocker_violations", "new_blocker_violations"},
      {"critical_violations", "new_critical_violations"},
      {"info_violations", "new_info_violations"},
      {"violations", "new_violations"},
      {"major_violations", "new_major_violations"},
      {"minor_violations", "new_minor_violations"},
      {"sqale_index", "new_technical_debt"},
      {"code_smells", "new_code_smells"},
      {"sqale_rating", "new_maintainability_rating"},
      {"sqale_debt_ratio", "new_sqale_debt_ratio"},
      {"bugs", "new_bugs"},
      {"reliability_rating", "new_reliability_rating"},
      {"reliability_remediation_effort", "new_reliability_remediation_effort"},
      {"vulnerabilities", "new_vulnerabilities"},
      {"security_rating", "new_security_rating"},
      {"security_remediation_effort", "new_security_remediation_effort"},
      {"lines", "new_lines"},
    };
  }

  @Test
  public void update_condition_using_leak_period_metric_when_condition_on_new_metric_exists_but_using_bad_operator() throws SQLException {
    long qualityGate = insertQualityGate(false);
    long linesToCover = insertMetric("lines_to_cover", DIRECTION_WORST, "INT");
    long newLinesToCover = insertMetric("new_lines_to_cover", DIRECTION_WORST, "INT");
    // This condition should be migrated to use new_lines_to_cover metric
    long conditionOnLinesToCover = insertCondition(qualityGate, linesToCover, "GT", "10", null, 1);
    // This condition should be removed as using a no more supported operator
    long conditionOnNewLinesToCover = insertCondition(qualityGate, newLinesToCover, "EQ", "5", null, 1);

    underTest.execute();

    assertConditions(
      tuple(conditionOnLinesToCover, newLinesToCover, "10", null, 1L));
  }

  @Test
  @UseDataProvider("noMoreSupportedMetricTypes")
  public void delete_condition_on_no_more_supported_metric_types(String metricKey, String metricType) throws SQLException {
    long qualityGate = insertQualityGate(false);
    long metric = insertMetric(metricKey, DIRECTION_BETTER, metricType);
    long condition = insertCondition(qualityGate, metric, "LT", "5", null, null);

    underTest.execute();

    assertThat(db.countRowsOfTable("quality_gate_conditions")).isZero();
  }

  @DataProvider
  public static Object[][] noMoreSupportedMetricTypes() {
    return new Object[][] {
      {"bool_type", "BOOL"},
      {"development_cost", "STRING"},
      {"last_change_on_maintainability_rating", "DATA"},
      {"class_complexity_distribution", "DISTRIB"}
    };
  }

  @Test
  @UseDataProvider("conditionsOnNoMoreSupportedOperators")
  public void delete_condition_on_no_more_supported_operators(String metricKey, int direction, String operator) throws SQLException {
    long qualityGate = insertQualityGate(false);
    long metric = insertMetric(metricKey, direction, "INT");
    long condition = insertCondition(qualityGate, metric, operator, "5", null, null);

    underTest.execute();

    assertThat(db.countRowsOfTable("quality_gate_conditions")).isZero();
  }

  @DataProvider
  public static Object[][] conditionsOnNoMoreSupportedOperators() {
    return new Object[][] {
      {"function_complexity_distribution", DIRECTION_NONE, "EQ"},
      {"file_complexity_distribution", DIRECTION_NONE, "NE"},
      {"blockers", DIRECTION_WORST, "LT"},
      {"coverage", DIRECTION_BETTER, "GT"}
    };
  }

  @Test
  @UseDataProvider("conditionsOnSupportedOperators")
  public void do_not_delete_condition_on_supported_operators(String metricKey, int direction, String operator) throws SQLException {
    long qualityGate = insertQualityGate(false);
    long metric = insertMetric(metricKey, direction, "INT");
    long condition = insertCondition(qualityGate, metric, operator, "5", null, null);

    underTest.execute();

    assertConditions(condition);
  }

  @DataProvider
  public static Object[][] conditionsOnSupportedOperators() {
    return new Object[][] {
      {"function_complexity_distribution", DIRECTION_NONE, "LT"},
      {"file_complexity_distribution", DIRECTION_NONE, "GT"},
      {"blockers", DIRECTION_BETTER, "LT"},
      {"coverage", DIRECTION_WORST, "GT"}
    };
  }

  private void assertConditions(Long... expectedIds) {
    assertThat(db.select("SELECT id FROM quality_gate_conditions")
      .stream()
      .map(row -> (long) row.get("ID"))
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedIds);
  }

  private void assertConditions(Tuple... expectedTuples) {
    assertThat(db.select("SELECT id, metric_id, value_error, value_warning, period FROM quality_gate_conditions")
      .stream()
      .map(row -> new Tuple(row.get("ID"), row.get("METRIC_ID"), row.get("VALUE_ERROR"), row.get("VALUE_WARNING"), row.get("PERIOD")))
      .collect(toList()))
        .containsExactlyInAnyOrder(expectedTuples);
  }

  private long insertQualityGate(boolean isBuiltIn) {
    long id = random.nextInt(1_000_000);
    db.executeInsert("QUALITY_GATES",
      "UUID", id,
      "ID", id,
      "NAME", "name " + id,
      "IS_BUILT_IN", isBuiltIn,
      "CREATED_AT", new Date(PAST),
      "UPDATED_AT", new Date(PAST));
    return id;
  }

  private long insertMetric(String name, int direction, String metricType) {
    long id = random.nextInt(1_000_000);
    db.executeInsert("METRICS",
      "ID", id,
      "NAME", name,
      "VAL_TYPE", metricType,
      "DIRECTION", direction);
    return id;
  }

  private long insertCondition(long qualityGateId, long metricId, String operator, @Nullable String error, @Nullable String warning, @Nullable Integer period) {
    long id = random.nextInt(1_000_000);
    db.executeInsert("QUALITY_GATE_CONDITIONS",
      "ID", id,
      "QGATE_ID", qualityGateId,
      "METRIC_ID", metricId,
      "OPERATOR", operator,
      "VALUE_ERROR", error,
      "VALUE_WARNING", warning,
      "PERIOD", period,
      "CREATED_AT", new Date(PAST),
      "UPDATED_AT", new Date(PAST));
    return id;
  }

}
