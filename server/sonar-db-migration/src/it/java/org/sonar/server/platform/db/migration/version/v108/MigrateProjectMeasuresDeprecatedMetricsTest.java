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
package org.sonar.server.platform.db.migration.version.v108;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.core.util.UuidFactoryImpl;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.api.measures.CoreMetrics.MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_ISSUES_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_ISSUES_KEY;
import static org.sonar.server.platform.db.migration.version.v108.MeasureMigration.MIGRATION_MAP;

class MigrateProjectMeasuresDeprecatedMetricsTest {
  private static final String ANALYSIS_UUID_1 = UuidFactoryImpl.INSTANCE.create();
  private static final String ANALYSIS_UUID_2 = UuidFactoryImpl.INSTANCE.create();
  @RegisterExtension
  private final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MigrateProjectMeasuresDeprecatedMetrics.class);
  private final MigrateProjectMeasuresDeprecatedMetrics underTest = new MigrateProjectMeasuresDeprecatedMetrics(db.database(),
    UuidFactoryImpl.INSTANCE);
  private Map<String, String> metricsToMigrate;
  private Map<String, String> replacementMetrics;

  @BeforeEach
  void init() {
    metricsToMigrate = insertMetricsToMigrate();
    replacementMetrics = insertReplacementMetrics();
  }

  @Test
  void execute_shouldCreateNewMetrics() throws SQLException {
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(MAINTAINABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "1");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(RELIABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "3");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(SECURITY_ISSUES_KEY), ANALYSIS_UUID_1, "5");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(NEW_MAINTAINABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "11");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(NEW_RELIABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "13");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(NEW_SECURITY_ISSUES_KEY), ANALYSIS_UUID_1, "15");
    underTest.execute();

    assertThat(db.select("select metric_uuid, value from project_measures where metric_uuid in (%s)"
      .formatted(replacementMetrics.values().stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")))))
        .hasSize(6)
        .map(m -> Map.of("metric_uuid", m.get("metric_uuid"), "value", ((Number) m.get("value")).longValue()))
        .contains((Map.of("metric_uuid", replacementMetrics.get(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY), "value", 1L)))
        .contains((Map.of("metric_uuid", replacementMetrics.get(SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY), "value", 3L)))
        .contains((Map.of("metric_uuid", replacementMetrics.get(SOFTWARE_QUALITY_SECURITY_ISSUES_KEY), "value", 5L)))
        .contains((Map.of("metric_uuid", replacementMetrics.get(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY), "value", 11L)))
        .contains((Map.of("metric_uuid", replacementMetrics.get(NEW_SOFTWARE_QUALITY_RELIABILITY_ISSUES_KEY), "value", 13L)))
        .contains((Map.of("metric_uuid", replacementMetrics.get(NEW_SOFTWARE_QUALITY_SECURITY_ISSUES_KEY), "value", 15L)));
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(MAINTAINABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "1");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(RELIABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "3");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(SECURITY_ISSUES_KEY), ANALYSIS_UUID_1, "5");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(NEW_MAINTAINABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "11");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(NEW_RELIABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "13");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(NEW_SECURITY_ISSUES_KEY), ANALYSIS_UUID_1, "15");
    underTest.execute();
    underTest.execute();
    assertThat(db.select("select * from project_measures"))
      .hasSize(MIGRATION_MAP.size() * 2);
  }

  @Test
  void execute_whenValueCannotBeConverted_shouldCreateOtherNewMetrics() throws SQLException {
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(MAINTAINABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "1");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(RELIABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "NOT_VALID_NUMBER");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(SECURITY_ISSUES_KEY), ANALYSIS_UUID_1, "5");
    underTest.execute();

    assertThat(db.select("select metric_uuid, value from project_measures where metric_uuid in (%s)"
      .formatted(replacementMetrics.values().stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")))))
        .hasSize(2)
        .map(m -> Map.of("metric_uuid", m.get("metric_uuid"), "value", ((Number) m.get("value")).longValue()))
        .contains((Map.of("metric_uuid", replacementMetrics.get(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY), "value", 1L)))
        .contains((Map.of("metric_uuid", replacementMetrics.get(SOFTWARE_QUALITY_SECURITY_ISSUES_KEY), "value", 5L)));
  }

  @Test
  void execute_whenWasPartiallyMigrated_shouldContinueWithOtherAnalysis() throws SQLException {

    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(MAINTAINABILITY_ISSUES_KEY), ANALYSIS_UUID_1, "1");
    createProjectMeasureForNewMetric(replacementMetrics.get(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY), ANALYSIS_UUID_1, 1);

    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(MAINTAINABILITY_ISSUES_KEY), ANALYSIS_UUID_2, "4");
    createProjectMeasureForDeprecatedMetric(metricsToMigrate.get(SECURITY_ISSUES_KEY), ANALYSIS_UUID_2, "5");
    underTest.execute();

    assertThat(db.select("select metric_uuid, value, analysis_uuid from project_measures where metric_uuid in (%s)"
      .formatted(replacementMetrics.values().stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")))))
        .hasSize(3)
        .map(m -> Map.of("metric_uuid", m.get("metric_uuid"), "value", ((Number) m.get("value")).longValue(), "analysis_uuid", m.get("analysis_uuid")))
        .contains((Map.of("metric_uuid", replacementMetrics.get(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY), "value", 1L, "analysis_uuid", ANALYSIS_UUID_1)))
        .contains((Map.of("metric_uuid", replacementMetrics.get(SOFTWARE_QUALITY_MAINTAINABILITY_ISSUES_KEY), "value", 4L, "analysis_uuid", ANALYSIS_UUID_2)))
        .contains((Map.of("metric_uuid", replacementMetrics.get(SOFTWARE_QUALITY_SECURITY_ISSUES_KEY), "value", 5L, "analysis_uuid",
          ANALYSIS_UUID_2)));
  }

  private void createProjectMeasureForDeprecatedMetric(String metricUuid, String analysisUuid, String totalIssues) {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("TEXT_VALUE", "{\"LOW\":X,\"MEDIUM\":Y,\"HIGH\":Z,\"total\":" + totalIssues + "}"),
      Map.entry("ANALYSIS_UUID", analysisUuid),
      Map.entry("METRIC_UUID", metricUuid),
      Map.entry("COMPONENT_UUID", UuidFactoryImpl.INSTANCE.create()));
    db.executeInsert("project_measures", map);
  }

  private void createProjectMeasureForNewMetric(String metricUuid, String analysisUuid, int totalIssues) {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("VALUE", totalIssues),
      Map.entry("ANALYSIS_UUID", analysisUuid),
      Map.entry("METRIC_UUID", metricUuid),
      Map.entry("COMPONENT_UUID", UuidFactoryImpl.INSTANCE.create()));
    db.executeInsert("project_measures", map);
  }

  private Map<String, String> insertMetricsToMigrate() {
    Map<String, String> createdMetrics = new HashMap<>();
    MIGRATION_MAP.keySet().forEach(metricKey -> createdMetrics.put(metricKey, insertMetric(metricKey)));
    return createdMetrics;
  }

  private Map<String, String> insertReplacementMetrics() {
    Map<String, String> createdMetrics = new HashMap<>();
    MIGRATION_MAP.values().forEach(metricKey -> createdMetrics.put(metricKey, insertMetric(metricKey)));
    return createdMetrics;
  }

  private String insertMetric(String key) {
    String uuid = UuidFactoryImpl.INSTANCE.create();
    Map<String, Object> map = Map.ofEntries(
      Map.entry("UUID", uuid),
      Map.entry("NAME", key));
    db.executeInsert("metrics", map);
    return uuid;
  }
}
