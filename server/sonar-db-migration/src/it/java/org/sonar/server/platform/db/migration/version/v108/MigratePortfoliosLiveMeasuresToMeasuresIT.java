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

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DataChange;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

class MigratePortfoliosLiveMeasuresToMeasuresIT {

  private static final String MEASURES_MIGRATED_COLUMN = "measures_migrated";
  public static final String SELECT_MEASURE = "select component_uuid, branch_uuid, json_value, json_value_hash, created_at, updated_at " +
    "from measures where component_uuid = '%s'";

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigratePortfoliosLiveMeasuresToMeasures.class);

  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final System2 system2 = mock();
  private final DataChange underTest = new MigratePortfoliosLiveMeasuresToMeasures(db.database(), system2);

  @Test
  void shall_complete_when_tables_are_empty() throws SQLException {
    underTest.execute();

    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  void migration_does_nothing_if_live_measures_table_is_missing() {
    db.executeDdl("drop table live_measures");
    db.assertTableDoesNotExist("live_measures");
    String branch = "portfolio_1";
    insertNotMigratedPortfolio(branch);

    assertThatCode(underTest::execute)
      .doesNotThrowAnyException();
  }

  @Test
  void log_the_item_uuid_when_the_migration_fails() {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String portfolio = "portfolio_1";
    insertNotMigratedPortfolio(portfolio);
    insertMeasure(portfolio, nclocMetricUuid, Map.of("value", 120));

    db.executeDdl("drop table measures");
    db.assertTableDoesNotExist("measures");

    assertThatExceptionOfType(SQLException.class)
      .isThrownBy(underTest::execute);

    assertThat(logTester.logs(Level.ERROR))
      .contains("Migration of portfolio portfolio_1 failed");
  }

  @Test
  void shall_not_migrate_when_portfolio_is_already_flagged() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String qgStatusMetricUuid = insertMetric("quality_gate_status", "STRING");
    String metricWithDataUuid = insertMetric("metric_with_data", "DATA");
    String portfolio1 = "portfolio_1";
    insertMigratedPortfolio(portfolio1);
    insertMeasure(portfolio1, nclocMetricUuid, Map.of("value", 120));
    insertMeasure(portfolio1, qgStatusMetricUuid, Map.of("text_value", "ok"));
    insertMeasure(portfolio1, metricWithDataUuid, Map.of("measure_data", "some data".getBytes(StandardCharsets.UTF_8)));

    insertMigratedPortfolio("portfolio_2");
    insertMeasure("portfolio_2", nclocMetricUuid, Map.of("value", 14220));

    underTest.execute();

    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  void should_flag_portfolio_with_no_measures() throws SQLException {
    String portfolio = "portfolio_3";
    insertNotMigratedPortfolio(portfolio);

    underTest.execute();

    assertPortfolioMigrated(portfolio);
    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  void should_migrate_portfolio_with_measures() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    String qgStatusMetricUuid = insertMetric("quality_gate_status", "STRING");
    String metricWithDataUuid = insertMetric("metric_with_data", "DATA");

    String portfolio1 = "portfolio_4";
    insertNotMigratedPortfolio(portfolio1);
    String component1 = uuidFactory.create();
    String component2 = uuidFactory.create();
    insertMeasure(portfolio1, component1, nclocMetricUuid, Map.of("value", 120));
    insertMeasure(portfolio1, component1, qgStatusMetricUuid, Map.of("text_value", "ok"));
    insertMeasure(portfolio1, component2, metricWithDataUuid, Map.of("measure_data", "some data".getBytes(StandardCharsets.UTF_8)));

    String portfolio2 = "portfolio_5";
    insertNotMigratedPortfolio(portfolio2);
    insertMeasure(portfolio2, nclocMetricUuid, Map.of("value", 64));

    String migratedPortfolio = "portfolio_6";
    insertMigratedPortfolio(migratedPortfolio);
    insertMeasure(migratedPortfolio, nclocMetricUuid, Map.of("value", 3684));

    underTest.execute();

    assertPortfolioMigrated(portfolio1);
    assertPortfolioMigrated(portfolio2);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(3);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("component_uuid"), t -> t.get("branch_uuid"), t -> t.get("json_value"), t -> t.get("json_value_hash"))
      .containsOnly(tuple(component1, portfolio1, "{\"ncloc\":120.0,\"quality_gate_status\":\"ok\"}", 6033012287291512746L));

    assertThat(db.select(format(SELECT_MEASURE, component2)))
      .hasSize(1)
      .extracting(t -> t.get("component_uuid"), t -> t.get("branch_uuid"), t -> t.get("json_value"), t -> t.get("json_value_hash"))
      .containsOnly(tuple(component2, portfolio1, "{\"metric_with_data\":\"some data\"}", -4524184678167636687L));
  }

  @Test
  void should_not_migrate_measures_planned_for_deletion() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");
    Set<String> deletedMetricUuid = DeleteSoftwareQualityRatingFromProjectMeasures.SOFTWARE_QUALITY_METRICS_TO_DELETE.stream().map(e -> insertMetric(e, "INT"))
      .collect(Collectors.toSet());

    String portfolio1 = "portfolio_4";
    insertNotMigratedPortfolio(portfolio1);
    String component1 = uuidFactory.create();
    String component2 = uuidFactory.create();
    insertMeasure(portfolio1, component1, nclocMetricUuid, Map.of("value", 120));
    deletedMetricUuid.forEach(metricUuid -> insertMeasure(portfolio1, component1, metricUuid, Map.of("value", 120)));
    deletedMetricUuid.forEach(metricUuid -> insertMeasure(portfolio1, component2, metricUuid, Map.of("value", 120)));

    underTest.execute();

    assertPortfolioMigrated(portfolio1);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("component_uuid"), t -> t.get("branch_uuid"), t -> t.get("json_value"), t -> t.get("json_value_hash"))
      .containsOnly(tuple(component1, portfolio1, "{\"ncloc\":120.0}", -1557106439558598045L));
  }

  @Test
  void should_duplicate_measures_on_reentry_when_index_does_not_exist() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");

    String portfolio1 = "portfolio_1";
    insertNotMigratedPortfolio(portfolio1);
    String component1 = uuidFactory.create();
    insertMeasure(portfolio1, component1, nclocMetricUuid, Map.of("value", 120));

    insertMigratedMeasure(portfolio1, component1);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);

    underTest.execute();

    assertThat(db.countRowsOfTable("measures")).isEqualTo(2);
  }

  @Test
  void should_remove_migrated_measures_on_reentry_when_index_exists() throws SQLException {
    String nclocMetricUuid = insertMetric("ncloc", "INT");

    String portfolio1 = "portfolio_1";
    insertNotMigratedPortfolio(portfolio1);
    String component1 = uuidFactory.create();
    insertMeasure(portfolio1, component1, nclocMetricUuid, Map.of("value", 120));

    insertMigratedMeasure(portfolio1, component1);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);

    var createIndexMigration = new CreateIndexOnMeasuresTable(db.database());
    createIndexMigration.execute();

    underTest.execute();

    assertThat(db.countRowsOfTable("measures")).isEqualTo(1);
  }

  private void assertPortfolioMigrated(String portfolio) {
    List<Map<String, Object>> result = db.select(format("select %s as \"MIGRATED\" from portfolios where uuid = '%s'", MEASURES_MIGRATED_COLUMN, portfolio));
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

  private void insertMeasure(String portfolioUuid, String metricUuid, Map<String, Object> data) {
    insertMeasure(portfolioUuid, uuidFactory.create(), metricUuid, data);
  }

  private void insertMeasure(String portfolioUuid, String componentUuid, String metricUuid, Map<String, Object> data) {
    Map<String, Object> dataMap = new HashMap<>(data);
    dataMap.put("uuid", uuidFactory.create());
    dataMap.put("component_uuid", componentUuid);
    dataMap.put("project_uuid", portfolioUuid);
    dataMap.put("metric_uuid", metricUuid);
    dataMap.put("created_at", 12L);
    dataMap.put("updated_at", 12L);

    db.executeInsert("live_measures", dataMap);
  }

  private void insertNotMigratedPortfolio(String portfolioUuid) {
    insertPortfolio(portfolioUuid, false);
  }

  private void insertMigratedPortfolio(String portfolioUuid) {
    insertPortfolio(portfolioUuid, true);
  }

  private void insertPortfolio(String portfolioUuid, boolean migrated) {
    db.executeInsert("portfolios",
      "uuid", portfolioUuid,
      "kee", portfolioUuid,
      "name", portfolioUuid,
      "private", true,
      "root_uuid", portfolioUuid,
      "selection_mode", "MANUAL",
      MEASURES_MIGRATED_COLUMN, migrated,
      "created_at", 12L,
      "updated_at", 12L);
  }

  private void insertMigratedMeasure(String branch, String componentUuid) {
    db.executeInsert("measures",
      "component_uuid", componentUuid,
      "branch_uuid", branch,
      "json_value", "{\"any\":\"thing\"}",
      "json_value_hash", 1234,
      "created_at", 12,
      "updated_at", 12);
  }
}
