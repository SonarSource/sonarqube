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

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.metrics.schema.InstallationMetric;
import org.sonar.telemetry.metrics.schema.LanguageMetric;
import org.sonar.telemetry.metrics.schema.Metric;
import org.sonar.telemetry.metrics.schema.ProjectMetric;
import org.sonar.telemetry.metrics.schema.UserMetric;

public class TelemetryMetricsMapper {

  private TelemetryMetricsMapper() {
  }

  public static Set<Metric> mapFromDataProvider(TelemetryDataProvider<?> provider) {
    switch (provider.getDimension()) {
      case INSTALLATION -> {
        return mapInstallationMetric(provider);
      } case PROJECT -> {
        return mapProjectMetric(provider);
      } case USER -> {
        return mapUserMetric(provider);
      } case LANGUAGE -> {
        return mapLanguageMetric(provider);
      } default -> throw new IllegalArgumentException("Dimension: " + provider.getDimension() + " not yet implemented.");
    }
  }

  private static Set<Metric> mapInstallationMetric(TelemetryDataProvider<?> provider) {
    Optional<?> optionalValue = provider.getValue();
    if (provider.getGranularity() == Granularity.ADHOC && optionalValue.isEmpty()) {
      return Collections.emptySet();
    }

    return Collections.singleton(new InstallationMetric(
      provider.getMetricKey(),
      optionalValue.orElse(null),
      provider.getType(),
      provider.getGranularity()
    ));
  }

  private static Set<Metric> mapUserMetric(TelemetryDataProvider<?> provider) {
    return provider.getUuidValues().entrySet().stream()
      .map(entry -> new UserMetric(
        provider.getMetricKey(),
        entry.getValue(),
        entry.getKey(),
        provider.getType(),
        provider.getGranularity()
      )).collect(Collectors.toSet());
  }

  private static Set<Metric> mapProjectMetric(TelemetryDataProvider<?> provider) {
    return provider.getUuidValues().entrySet().stream()
      .map(entry -> new ProjectMetric(
        provider.getMetricKey(),
        entry.getValue(),
        entry.getKey(),
        provider.getType(),
        provider.getGranularity()
      )).collect(Collectors.toSet());
  }

  private static Set<Metric> mapLanguageMetric(TelemetryDataProvider<?> provider) {
    return provider.getUuidValues().entrySet().stream()
      .map(entry -> new LanguageMetric(
        provider.getMetricKey(),
        entry.getValue(),
        entry.getKey(),
        provider.getType(),
        provider.getGranularity()
      )).collect(Collectors.toSet());
  }

}
