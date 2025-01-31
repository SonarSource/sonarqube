/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v108;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.core.metric.SoftwareQualitiesMetrics;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

class MigrateBranchesLiveMeasuresToMeasuresIT {

  private static final String MEASURES_MIGRATED_COLUMN = "measures_migrated";
  public static final String SELECT_MEASURE = "select component_uuid, branch_uuid, json_value, json_value_hash, created_at, updated_at " +
    "from measures where component_uuid = '%s'";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigrateBranchesLiveMeasuresToMeasures.class);

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final System2 system2 = mock();
  private final DataChange underTest = new MigrateBranchesLiveMeasuresToMeasures(db.database(), system2);

  @Test
  void shall_complete_when_tables_are_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  void migration_does_nothing_if_live_measures_table_is_missing() {
    db.executeDdl("drop table live_measures");
    db.assertTableDoesNotExist("live_measures");
    String branch = "branch_3";
    insertNotMigratedBranch(branch);

    assertThatCode(underTest::execute)
      .doesNotThrowAnyException();
  }

  @Test
  void log_the_item_uuid_when_the_migration_fails() {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String branch1 = "branch_1";
    insertNotMigratedBranch(branch1);
    insertMeasure(branch1, nclocMetricUuid, Map.of("value", 120));

    db.executeDdl("drop table measures");
    db.assertTableDoesNotExist("measures");

    assertThatExceptionOfType(SQLException.class)
      .isThrownBy(underTest::execute);

    assertThat(logTester.logs(Level.ERROR))
      .contains("Migration of branch branch_1 failed");
  }

  @Test
  void shall_not_migrate_when_branch_is_already_flagged() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String qgStatusMetricUuid = insertMetric("quality_gate_status", "STRING");
    String metricWithDataUuid = insertMetric("metric_with_data", "DATA");
    String branch1 = "branch_1";
    insertMigratedBranch(branch1);
    insertMeasure(branch1, nclocMetricUuid, Map.of("value", 120));
    insertMeasure(branch1, qgStatusMetricUuid, Map.of("text_value", "ok"));
    insertMeasure(branch1, metricWithDataUuid, Map.of("measure_data", "some data".getBytes(StandardCharsets.UTF_8)));

    insertMigratedBranch("branch_2");
    insertMeasure("branch_2", nclocMetricUuid, Map.of("value", 14220));

    underTest.execute();

    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  void should_flag_branch_with_no_measures() throws SQLException {
    String branch = "branch_3";
    insertNotMigratedBranch(branch);

    underTest.execute();

    assertBranchMigrated(branch);
    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  void should_migrate_branch_with_measures() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String qgStatusMetricUuid = insertMetric("quality_gate_status", "STRING");
    String metricWithDataUuid = insertMetric("metric_with_data", "DATA");

    String branch1 = "branch_4";
    insertNotMigratedBranch(branch1);
    String component1 = uuidFactory.create();
    String component2 = uuidFactory.create();
    insertMeasure(branch1, component1, nclocMetricUuid, Map.of("value", 120));
    insertMeasure(branch1, component1, qgStatusMetricUuid, Map.of("text_value", "ok"));
    insertMeasure(branch1, component2, metricWithDataUuid, Map.of("measure_data", "some data".getBytes(StandardCharsets.UTF_8)));

    String branch2 = "branch_5";
    insertNotMigratedBranch(branch2);
    insertMeasure(branch2, nclocMetricUuid, Map.of("value", 64));

    String migratedBranch = "branch_6";
    insertMigratedBranch(migratedBranch);
    insertMeasure(migratedBranch, nclocMetricUuid, Map.of("value", 3684));

    underTest.execute();

    assertBranchMigrated(branch1);
    assertBranchMigrated(branch2);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(3);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("component_uuid"), t -> t.get("branch_uuid"), t -> t.get("json_value"), t -> t.get("json_value_hash"))
      .containsOnly(tuple(component1, branch1, "{\"ncloc\":120.0,\"quality_gate_status\":\"ok\"}", 6033012287291512746L));

    assertThat(db.select(format(SELECT_MEASURE, component2)))
      .hasSize(1)
      .extracting(t -> t.get("component_uuid"), t -> t.get("branch_uuid"), t -> t.get("json_value"), t -> t.get("json_value_hash"))
      .containsOnly(tuple(component2, branch1, "{\"metric_with_data\":\"some data\"}", -4524184678167636687L));
  }

  @Test
  void should_not_migrate_measures_planned_for_deletion() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    Set<String> deletedMetricUuid = DeleteSoftwareQualityRatingFromProjectMeasures.SOFTWARE_QUALITY_METRICS_TO_DELETE.stream().map(e -> insertMetric(e, "INT"))
      .collect(Collectors.toSet());

    String branch1 = "branch_4";
    insertNotMigratedBranch(branch1);
    String component1 = uuidFactory.create();
    String component2 = uuidFactory.create();
    insertMeasure(branch1, component1, nclocMetricUuid, Map.of("value", 120));
    deletedMetricUuid.forEach(metricUuid -> insertMeasure(branch1, component1, metricUuid, Map.of("value", 120)));
    deletedMetricUuid.forEach(metricUuid -> insertMeasure(branch1, component2, metricUuid, Map.of("value", 120)));

    underTest.execute();

    assertBranchMigrated(branch1);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("component_uuid"), t -> t.get("branch_uuid"), t -> t.get("json_value"), t -> t.get("json_value_hash"))
      .containsOnly(tuple(component1, branch1, "{\"ncloc\":120.0}", -1557106439558598045L));

    assertThat(db.select(format(SELECT_MEASURE, component2)))
      .isEmpty();
  }

  @Test
  void should_include_new_measures_based_on_previous_available_measures() throws SQLException {
    Set<String> metricsToMigrate = MeasureMigration.MIGRATION_MAP.keySet().stream().map(e -> insertMetric(e, "DATA"))
      .collect(Collectors.toSet());

    String branch = "branch_4";
    insertNotMigratedBranch(branch);
    String component1 = uuidFactory.create();
    metricsToMigrate.forEach(metricUuid -> insertMeasure(branch, component1, metricUuid,
      Map.of("measure_data", "{\"LOW\":3,\"MEDIUM\":0,\"HIGH\":4,\"total\":7}".getBytes(StandardCharsets.UTF_8))));

    underTest.execute();

    assertBranchMigrated(branch);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);

    List<Map<String, Object>> measuresFromDB = db.select(format(SELECT_MEASURE, component1));
    assertThat(measuresFromDB).hasSize(1);

    Gson gson = new Gson();
    Map<String, Object> jsonValue = gson.fromJson((String) measuresFromDB.get(0).get("json_value"), Map.class);

    Map<String, Object> expectedExistingMetrics = MeasureMigration.MIGRATION_MAP.keySet().stream().collect(
      Collectors.toMap(s -> s, s -> "{\"LOW\":3,\"MEDIUM\":0,\"HIGH\":4,\"total\":7}"));

    Map<String, Object> expectedNewMetrics = MeasureMigration.MIGRATION_MAP.values().stream().collect(
      Collectors.toMap(s -> s, s -> 7.0));

    assertThat(jsonValue).containsAllEntriesOf(expectedExistingMetrics).containsAllEntriesOf(expectedNewMetrics);
  }

  @Test
  void should_migrate_other_measures_when_there_is_an_error_converting_previous_measures() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String maintainabilityMetricUuid = insertMetric(CoreMetrics.MAINTAINABILITY_ISSUES_KEY, "DATA");
    String reliabilityMetricUuid = insertMetric(CoreMetrics.RELIABILITY_ISSUES_KEY, "DATA");
    String securityMetricUuid = insertMetric(CoreMetrics.SECURITY_ISSUES_KEY, "DATA");

    String branch = "branch_4";
    insertNotMigratedBranch(branch);
    String component1 = uuidFactory.create();
    insertMeasure(branch, component1, nclocMetricUuid, Map.of("value", 120));
    // total is not a number
    insertMeasure(branch, component1, maintainabilityMetricUuid, Map.of("measure_data", "{\"LOW\":3,\"MEDIUM\":0,\"HIGH\":4,\"total\":\"ABC\"}"
      .getBytes(StandardCharsets.UTF_8)));
    // total cannot fit in a long
    insertMeasure(branch, component1, reliabilityMetricUuid, Map.of("measure_data", "{\"LOW\":3,\"MEDIUM\":0,\"HIGH\":4,\"total\":98723987498723987429874928748748}"
      .getBytes(StandardCharsets.UTF_8)));
    insertMeasure(branch, component1, securityMetricUuid, Map.of("measure_data", "{\"LOW\":3,\"MEDIUM\":0,\"HIGH\":4,\"total\":37}".getBytes(StandardCharsets.UTF_8)));

    logTester.setLevel(Level.DEBUG);
    underTest.execute();

    assertBranchMigrated(branch);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);

    List<Map<String, Object>> measuresFromDB = db.select(format(SELECT_MEASURE, component1));
    assertThat(measuresFromDB).hasSize(1);

    Gson gson = new Gson();
    Map<String, Object> jsonValue = gson.fromJson((String) measuresFromDB.get(0).get("json_value"), Map.class);

    Map<String, Object> expectedExistingMetrics = Map.of(
      "ncloc", 120.0,
      CoreMetrics.MAINTAINABILITY_ISSUES_KEY, "{\"LOW\":3,\"MEDIUM\":0,\"HIGH\":4,\"total\":\"ABC\"}",
      CoreMetrics.RELIABILITY_ISSUES_KEY, "{\"LOW\":3,\"MEDIUM\":0,\"HIGH\":4,\"total\":98723987498723987429874928748748}",
      CoreMetrics.SECURITY_ISSUES_KEY, "{\"LOW\":3,\"MEDIUM\":0,\"HIGH\":4,\"total\":37}");

    Map<String, Object> expectedNewMetrics = Map.of(SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES_KEY, 37.0);

    assertThat(jsonValue).hasSize(5).containsAllEntriesOf(expectedExistingMetrics).containsAllEntriesOf(expectedNewMetrics);
    assertThat(logTester.logs(Level.DEBUG)).contains("Failed to migrate metric reliability_issues with value {\"LOW\":3,\"MEDIUM\":0," +
      "\"HIGH\":4,\"total\":98723987498723987429874928748748}");
  }

  @Test
  void should_not_migrate_large_measures() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String metricWithDataUuid = insertMetric("metric_with_data", "DATA");
    String metricWithLargeDataUuid = insertMetric("metric_with_large_data", "DATA");

    String branch1 = "branch_1";
    insertNotMigratedBranch(branch1);
    String component1 = uuidFactory.create();
    insertMeasure(branch1, component1, nclocMetricUuid, Map.of("value", 120));
    byte[] largeValue = createLargeValue(999_999);
    insertMeasure(branch1, component1, metricWithDataUuid, Map.of("measure_data", largeValue));
    insertMeasure(branch1, component1, metricWithLargeDataUuid, Map.of("measure_data", createLargeValue(1_000_000)));

    underTest.execute();

    assertBranchMigrated(branch1);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("component_uuid"), t -> t.get("branch_uuid"),
        t -> t.get("json_value"), t -> t.get("json_value_hash"))
      .containsOnly(tuple(component1, branch1, "{\"ncloc\":120.0,\"metric_with_data\":\"" +
        new String(largeValue, StandardCharsets.UTF_8) + "\"}", -8442559321192521885L));
  }

  @Test
  void should_not_migrate_not_persisted_metrics() throws SQLException {
    String devCostMetricUuid = insertMetric(CoreMetrics.DEVELOPMENT_COST_KEY, "STRING");
    String nclocDataMetricUuid = insertMetric(CoreMetrics.NCLOC_DATA_KEY, "STRING");
    String executableLinesDataMetricUuid = insertMetric(CoreMetrics.EXECUTABLE_LINES_DATA_KEY, "STRING");

    String branch1 = "branch_1";
    insertNotMigratedBranch(branch1);
    String component1 = uuidFactory.create();
    insertMeasure(branch1, component1, devCostMetricUuid, Map.of("text_value", "123"));
    insertMeasure(branch1, component1, nclocDataMetricUuid, Map.of("text_value", "456"));
    insertMeasure(branch1, component1, executableLinesDataMetricUuid, Map.of("text_value", "789"));

    underTest.execute();

    assertBranchMigrated(branch1);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("component_uuid"), t -> t.get("branch_uuid"),
        t -> t.get("json_value"), t -> t.get("json_value_hash"))
      .containsOnly(tuple(component1, branch1, "{\"development_cost\":\"123\"}", -4081454374503046534L));
  }

  private byte[] createLargeValue(int size) {
    byte[] value = new byte[size];
    Arrays.fill(value, (byte) 'a');
    return value;
  }

  private void assertBranchMigrated(String branch) {
    List<Map<String, Object>> result = db.select(format("select %s as \"MIGRATED\" from project_branches where uuid = '%s'", MEASURES_MIGRATED_COLUMN, branch));
    assertThat(result)
      .hasSize(1)
      .extracting(t -> t.get("MIGRATED"))
      .containsOnly(true);
  }

  private String insertMetric(String metricName, String valueType) {
    String metricUuid = uuidFactory.create();
    db.executeInsert("metrics",
      "uuid", metricUuid,
      "name", metricName,
      "val_type", valueType);
    return metricUuid;
  }

  private void insertMeasure(String branchUuid, String metricUuid, Map<String, Object> data) {
    insertMeasure(branchUuid, uuidFactory.create(), metricUuid, data);
  }

  private void insertMeasure(String branchUuid, String componentUuid, String metricUuid, Map<String, Object> data) {
    Map<String, Object> dataMap = new HashMap<>(data);
    dataMap.put("uuid", uuidFactory.create());
    dataMap.put("component_uuid", componentUuid);
    dataMap.put("project_uuid", branchUuid);
    dataMap.put("metric_uuid", metricUuid);
    dataMap.put("created_at", 12L);
    dataMap.put("updated_at", 12L);

    db.executeInsert("live_measures", dataMap);
  }

  private void insertNotMigratedBranch(String branchUuid) {
    insertBranch(branchUuid, false);
  }

  private void insertMigratedBranch(String branchUuid) {
    insertBranch(branchUuid, true);
  }

  private void insertBranch(String branchUuid, boolean migrated) {
    db.executeInsert("project_branches",
      "uuid", branchUuid,
      "kee", branchUuid,
      "branch_type", "LONG",
      "project_uuid", uuidFactory.create(),
      MEASURES_MIGRATED_COLUMN, migrated,
      "need_issue_sync", false,
      "is_main", true,
      "created_at", 12L,
      "updated_at", 12L);
  }

}
