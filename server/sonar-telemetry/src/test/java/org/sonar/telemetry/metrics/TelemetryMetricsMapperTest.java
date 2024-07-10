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
package org.sonar.telemetry.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.sonar.telemetry.Dimension;
import org.sonar.telemetry.Granularity;
import org.sonar.telemetry.TelemetryDataProvider;
import org.sonar.telemetry.TelemetryDataType;
import org.sonar.telemetry.metrics.schema.InstallationMetric;
import org.sonar.telemetry.metrics.schema.LanguageMetric;
import org.sonar.telemetry.metrics.schema.Metric;
import org.sonar.telemetry.metrics.schema.ProjectMetric;
import org.sonar.telemetry.metrics.schema.UserMetric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class TelemetryMetricsMapperTest {

  @Test
  void mapFromDataProvider_whenInstallationProvider() {
    TelemetryDataProvider<String> provider = new TestTelemetryBean(Dimension.INSTALLATION);

    Set<Metric> metrics = TelemetryMetricsMapper.mapFromDataProvider(provider);
    List<InstallationMetric> userMetrics = retrieveList(metrics);

    assertThat(userMetrics)
      .extracting(InstallationMetric::getKey, InstallationMetric::getType, InstallationMetric::getValue, InstallationMetric::getGranularity)
      .containsExactlyInAnyOrder(
        tuple("telemetry-bean-a", TelemetryDataType.STRING, "value", Granularity.DAILY)
      );
  }

  @Test
  void mapFromDataProvider_whenUserProvider() {
    TelemetryDataProvider<String> provider = new TestTelemetryBean(Dimension.USER);

    Set<Metric> metrics = TelemetryMetricsMapper.mapFromDataProvider(provider);
    List<UserMetric> list = retrieveList(metrics);

    assertThat(list)
      .extracting(UserMetric::getKey, UserMetric::getType, UserMetric::getUserUuid, UserMetric::getValue, UserMetric::getGranularity)
      .containsExactlyInAnyOrder(
        expected()
      );
  }

  @Test
  void mapFromDataProvider_whenLanguageProvider() {
    TelemetryDataProvider<String> provider = new TestTelemetryBean(Dimension.LANGUAGE);

    Set<Metric> metrics = TelemetryMetricsMapper.mapFromDataProvider(provider);
    List<LanguageMetric> list = retrieveList(metrics);

    assertThat(list)
      .extracting(LanguageMetric::getKey, LanguageMetric::getType, LanguageMetric::getLanguage, LanguageMetric::getValue, LanguageMetric::getGranularity)
      .containsExactlyInAnyOrder(
        expected()
      );
  }

  @Test
  void mapFromDataProvider_whenProjectProvider() {
    TelemetryDataProvider<String> provider = new TestTelemetryBean(Dimension.PROJECT);

    Set<Metric> metrics = TelemetryMetricsMapper.mapFromDataProvider(provider);
    List<ProjectMetric> list = retrieveList(metrics);

    assertThat(list)
      .extracting(ProjectMetric::getKey, ProjectMetric::getType, ProjectMetric::getProjectUuid, ProjectMetric::getValue, ProjectMetric::getGranularity)
      .containsExactlyInAnyOrder(
        expected()
      );
  }

  private static Tuple[] expected() {
    return new Tuple[]
      {
        tuple("telemetry-bean-a", TelemetryDataType.STRING, "key-1", "value-1", Granularity.DAILY),
        tuple("telemetry-bean-a", TelemetryDataType.STRING, "key-2", "value-2", Granularity.DAILY)
      };
  }

  private static <T extends Metric> List<T> retrieveList(Set<Metric> metrics) {
    return new ArrayList<>((Set<T>) metrics);
  }

}
