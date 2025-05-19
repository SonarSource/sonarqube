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
package org.sonar.telemetry.metrics;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.TelemetryDataProvider;
import org.sonar.telemetry.core.schema.InstallationMetric;
import org.sonar.telemetry.core.schema.LanguageMetric;
import org.sonar.telemetry.core.schema.Metric;
import org.sonar.telemetry.core.schema.ProjectMetric;
import org.sonar.telemetry.core.schema.UserMetric;

public class TelemetryMetricsMapper {

  private TelemetryMetricsMapper() {
  }

  public static Set<Metric> mapFromDataProvider(TelemetryDataProvider<?> provider) {
    switch (provider.getDimension()) {
      case INSTALLATION -> {
        return mapInstallationMetric(provider);
      }
      case PROJECT -> {
        return mapProjectMetric(provider);
      }
      case USER -> {
        return mapUserMetric(provider);
      }
      case LANGUAGE -> {
        return mapLanguageMetric(provider);
      }
      default -> throw new IllegalArgumentException("Dimension: " + provider.getDimension() + " not yet implemented.");
    }
  }

  private static Set<Metric> mapInstallationMetric(TelemetryDataProvider<?> provider) {
    // Case 1: the provider has implemented getValues() and it is non‐empty
    var multiValues = provider.getValues();
    if (!multiValues.isEmpty()) {
      return multiValues.entrySet().stream()
        .map(entry -> new InstallationMetric(
          provider.getMetricKey() + "." + entry.getKey(),
          entry.getValue(),
          provider.getType(),
          provider.getGranularity()))
        .collect(Collectors.toSet());
    }

    // Case 2: the provider has implemented getValue() and it is non‐empty
    var singleValue = provider.getValue();
    if (singleValue.isPresent()) {
      return Collections.singleton(new InstallationMetric(
        provider.getMetricKey(),
        singleValue.orElse(null),
        provider.getType(),
        provider.getGranularity()));
    }

    // Case 3: the provider has not implemented getValue() or getValues(), or both are empty
    if (provider.getGranularity() == Granularity.ADHOC) {
      return Collections.emptySet();
    } else {
      // It's not clear whether we actually want to send a null metric in this case, but we do for now to be consistent with the previous implementation.
      return Collections.singleton(new InstallationMetric(
        provider.getMetricKey(),
        null,
        provider.getType(),
        provider.getGranularity()));
    }
  }

  // Note: Dimension.USER does not currently support getValue(). But we just silently ignore it if a provider tries to use it.
  private static Set<Metric> mapUserMetric(TelemetryDataProvider<?> provider) {
    return provider.getValues().entrySet().stream()
      .map(entry -> new UserMetric(
        provider.getMetricKey(),
        entry.getValue(),
        entry.getKey(),
        provider.getType(),
        provider.getGranularity()))
      .collect(Collectors.toSet());
  }

  // Note: Dimension.PROJECT does not currently support getValue(). But we just silently ignore it if a provider tries to use it.
  private static Set<Metric> mapProjectMetric(TelemetryDataProvider<?> provider) {
    return provider.getValues().entrySet().stream()
      .map(entry -> new ProjectMetric(
        provider.getMetricKey(),
        entry.getValue(),
        entry.getKey(),
        provider.getType(),
        provider.getGranularity()))
      .collect(Collectors.toSet());
  }

  // Note: Dimension.LANGUAGE does not currently support getValue(). But we just silently ignore it if a provider tries to use it.
  private static Set<Metric> mapLanguageMetric(TelemetryDataProvider<?> provider) {
    return provider.getValues().entrySet().stream()
      .map(entry -> new LanguageMetric(
        provider.getMetricKey(),
        entry.getValue(),
        entry.getKey(),
        provider.getType(),
        provider.getGranularity()))
      .collect(Collectors.toSet());
  }
}
