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

public class MigratePortfoliosLiveMeasuresToMeasuresTest {

  private static final String MEASURES_MIGRATED_COLUMN = "measures_migrated";
  public static final String SELECT_MEASURE = """
    select component_uuid as "COMPONENT_UUID",
    branch_uuid as "BRANCH_UUID",
    json_value as "JSON_VALUE",
    json_value_hash as "JSON_VALUE_HASH",
    created_at as "CREATED_AT",
    updated_at as "UPDATED_AT",
    from measures where component_uuid = '%s'""";

  @Rule
  public final CoreDbTester db = CoreDbTester.createForSchema(MigratePortfoliosLiveMeasuresToMeasuresTest.class, "schema.sql");

  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final System2 system2 = mock();
  private final MigratePortfoliosLiveMeasuresToMeasures underTest = new MigratePortfoliosLiveMeasuresToMeasures(db.database(), system2);

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
  public void should_flag_portfolio_with_no_measures() throws SQLException {
    String portfolio = "portfolio_3";
    insertNotMigratedPortfolio(portfolio);

    underTest.migrate(List.of(portfolio));

    assertPortfolioMigrated(portfolio);
    assertThat(db.countRowsOfTable("measures")).isZero();
  }

  @Test
  public void should_migrate_portfolio_with_measures() throws SQLException {
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

    String unusedPortfolio = "portfolio_6";
    insertNotMigratedPortfolio(unusedPortfolio);
    insertMeasure(unusedPortfolio, nclocMetricUuid, Map.of("value", 3684));

    underTest.migrate(List.of(portfolio1, portfolio2));

    assertPortfolioMigrated(portfolio1);
    assertPortfolioMigrated(portfolio2);
    assertPortfolioNotMigrated(unusedPortfolio);
    assertThat(db.countRowsOfTable("measures")).isEqualTo(3);

    assertThat(db.select(format(SELECT_MEASURE, component1)))
      .hasSize(1)
      .extracting(t -> t.get("COMPONENT_UUID"), t -> t.get("BRANCH_UUID"), t -> t.get("JSON_VALUE"), t -> t.get("JSON_VALUE_HASH"))
      .containsOnly(tuple(component1, portfolio1, "{\"ncloc\":120.0,\"quality_gate_status\":\"ok\"}", 6033012287291512746L));

    assertThat(db.select(format(SELECT_MEASURE, component2)))
      .hasSize(1)
      .extracting(t -> t.get("COMPONENT_UUID"), t -> t.get("BRANCH_UUID"), t -> t.get("JSON_VALUE"), t -> t.get("JSON_VALUE_HASH"))
      .containsOnly(tuple(component2, portfolio1, "{\"metric_with_data\":\"some data\"}", -4524184678167636687L));
  }

  private void assertPortfolioMigrated(String portfolio) {
    assertMigrationStatus(portfolio, true);
  }

  private void assertPortfolioNotMigrated(String portfolio) {
    assertMigrationStatus(portfolio, false);
  }

  private void assertMigrationStatus(String portfolio, boolean expected) {
    List<Map<String, Object>> result = db.select(format("select %s as \"MIGRATED\" from portfolios where uuid = '%s'", MEASURES_MIGRATED_COLUMN, portfolio));
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
    db.executeInsert("portfolios",
      "uuid", portfolioUuid,
      "kee", portfolioUuid,
      "name", portfolioUuid,
      "private", true,
      "root_uuid", portfolioUuid,
      "selection_mode", "MANUAL",
      MEASURES_MIGRATED_COLUMN, false,
      "created_at", 12L,
      "updated_at", 12L
    );
  }
}
