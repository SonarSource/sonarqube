/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.adhoc;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.CoreDbTester;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

public class MigrateBranchesLiveMeasuresToMeasuresTest {

  private static final String MEASURES_MIGRATED_COLUMN = "measures_migrated";
  public static final String SELECT_MEASURE = """
    select component_uuid as "COMPONENT_UUID",
    branch_uuid as "BRANCH_UUID",
    json_value as "JSON_VALUE",
    json_value_hash as "JSON_VALUE_HASH",
    created_at as "CREATED_AT",
    updated_at as "UPDATED_AT",
    from measures
    where component_uuid = '%s'""";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(MigrateBranchesLiveMeasuresToMeasuresTest.class, "schema.sql");

  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final System2 system2 = mock();
  private final MigrateBranchesLiveMeasuresToMeasures underTest = new MigrateBranchesLiveMeasuresToMeasures(db.database(), system2);

  @Test
  public void shall_do_nothing_when_called_by_execute() {
    assertThatCode(underTest::execute)
      .doesNotThrowAnyException();

    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  public void shall_complete_when_tables_are_empty() throws SQLException {
    underTest.migrate(List.of("unused"));

    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  public void should_flag_branch_with_no_measures() throws SQLException {
    String branch = "branch_3";
    insertNotMigratedBranch(branch);

    underTest.migrate(List.of(branch));

    assertBranchMigrated(branch);
    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  public void should_migrate_branch_if_previous_attempt_failed() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String qgStatusMetricUuid = insertMetric("quality_gate_status", "STRING");
    String metricWithDataUuid = insertMetric("metric_with_data", "DATA");

    String branch1 = "branch_7";
    insertNotMigratedBranch(branch1);
    String component1 = uuidFactory.create();
    String component2 = uuidFactory.create();
    insertMeasure(branch1, component1, nclocMetricUuid, Map.of("value", 120));
    insertMeasure(branch1, component1, qgStatusMetricUuid, Map.of("text_value", "ok"));
    insertMeasure(branch1, component2, metricWithDataUuid, Map.of("measure_data", "some data".getBytes(StandardCharsets.UTF_8)));

    insertMigratedMeasure(branch1, component1);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);

    underTest.migrate(List.of(branch1));

    assertBranchMigrated(branch1);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(2);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("COMPONENT_UUID"), t -> t.get("BRANCH_UUID"), t -> t.get("JSON_VALUE"), t -> t.get("JSON_VALUE_HASH"))
      .containsOnly(tuple(component1, branch1, "{\"ncloc\":120.0,\"quality_gate_status\":\"ok\"}", 6033012287291512746L));

    assertThat(db.select(format(SELECT_MEASURE, component2)))
      .hasSize(1)
      .extracting(t -> t.get("COMPONENT_UUID"), t -> t.get("BRANCH_UUID"), t -> t.get("JSON_VALUE"), t -> t.get("JSON_VALUE_HASH"))
      .containsOnly(tuple(component2, branch1, "{\"metric_with_data\":\"some data\"}", -4524184678167636687L));
  }

  @Test
  public void should_migrate_branch_with_measures() throws SQLException {
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

    String unusedBranch = "branch_6";
    insertNotMigratedBranch(unusedBranch);
    insertMeasure(unusedBranch, nclocMetricUuid, Map.of("value", 3684));

    underTest.migrate(List.of(branch1, branch2));

    assertBranchMigrated(branch1);
    assertBranchMigrated(branch2);
    assertBranchNotMigrated(unusedBranch);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(3);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("COMPONENT_UUID"), t -> t.get("BRANCH_UUID"), t -> t.get("JSON_VALUE"), t -> t.get("JSON_VALUE_HASH"))
      .containsOnly(tuple(component1, branch1, "{\"ncloc\":120.0,\"quality_gate_status\":\"ok\"}", 6033012287291512746L));

    assertThat(db.select(format(SELECT_MEASURE, component2)))
      .hasSize(1)
      .extracting(t -> t.get("COMPONENT_UUID"), t -> t.get("BRANCH_UUID"), t -> t.get("JSON_VALUE"), t -> t.get("JSON_VALUE_HASH"))
      .containsOnly(tuple(component2, branch1, "{\"metric_with_data\":\"some data\"}", -4524184678167636687L));
  }

  private void assertBranchMigrated(String branch) {
    assertMigrationStatus(branch, true);
  }

  private void assertBranchNotMigrated(String branch) {
    assertMigrationStatus(branch, false);
  }

  private void assertMigrationStatus(String branch, boolean expected) {
    List<Map<String, Object>> result = db.select(format("select %s as \"MIGRATED\" from project_branches where uuid = '%s'", MEASURES_MIGRATED_COLUMN, branch));
    assertThat(result)
      .hasSize(1)
      .extracting(t -> t.get("MIGRATED"))
      .containsOnly(expected);
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

  private void insertMigratedMeasure(String branch, String componentUuid) {
    db.executeInsert("measures",
      "component_uuid", componentUuid,
      "branch_uuid", branch,
      "json_value", "{\"any\":\"thing\"}",
      "json_value_hash", "1234",
      "created_at", 12,
      "updated_at", 12);
  }

  private void insertNotMigratedBranch(String branchUuid) {
    db.executeInsert("project_branches",
      "uuid", branchUuid,
      "kee", branchUuid,
      "branch_type", "LONG",
      "project_uuid", uuidFactory.create(),
      MEASURES_MIGRATED_COLUMN, false,
      "need_issue_sync", false,
      "created_at", 12L,
      "updated_at", 12L
    );
  }


}
