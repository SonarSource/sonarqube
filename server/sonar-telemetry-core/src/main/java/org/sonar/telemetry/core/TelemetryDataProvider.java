/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.telemetry.core;

import java.util.Map;
import java.util.Optional;

/**
 * This interface is used to provide data to the telemetry system. The telemetry system will call the methods of this interface to get the
 * data that will be sent to the telemetry server.
 * If you want to add a new metric to the telemetry system, you need to create a new implementation of this interface, or for convenience to subclass {@link AbstractTelemetryDataProvider} (recommended),
 * and register it in the Spring context as a bean.
 *
 * @param <T> type of the value provided by this instance. Should be either {@link Boolean}, {@link String},
 * {@link Integer} or {@link Float}.
 */
public interface TelemetryDataProvider<T> {

  /**
   * @return the key of the metric that will be used to store the value of the data provided by this instance. The combination of
   * metric key and dimension needs to be universally unique. The metric key needs to be written in snake_case.
   */
  String getMetricKey();

  /**
   * @return the dimension ("category") of the data provided by this instance. The combination of metric key and dimension needs to be
   * universally unique.
   */
  Dimension getDimension();

  /**
   * @return returns the granularity of this telemetry metric.
   * @see Granularity
   */
  Granularity getGranularity();

  /**
   * @return the type of the data provided by this instance.
   */
  TelemetryDataType getType();

  /**
   * The implementation of this method might often need to make a call to a database.
   * For each metric either this method or {@link TelemetryDataProvider#getValues()} should be used. Not both at once.
   * <p>
   * Only taken into account for {@link Dimension#INSTALLATION}, and only when {@link #getValues()} is empty. It is ignored for
   * {@link Dimension#USER}, {@link Dimension#PROJECT} and {@link Dimension#LANGUAGE}.
   * <p>
   * When neither this method nor {@link #getValues()} returns a value, nothing is sent for {@link Granularity#ADHOC} metrics,
   * and a {@code null} value is sent for the other granularities.
   *
   * @return the value of the data provided by this instance.
   */
  default Optional<T> getValue() {
    return Optional.empty();
  }

  /**
   * The implementation of this method might often need to make a call to a database.
   * Similar as {@link TelemetryDataProvider#getValue()} this method returns values of the metric. Some of the metrics
   * associate a key with a value. This method is used to return all the values associated with the keys.
   * <p>
   * The meaning of the keys depends on the {@link #getDimension() dimension}:
   * <ul>
   *   <li>{@link Dimension#USER}: user UUID</li>
   *   <li>{@link Dimension#PROJECT}: project UUID</li>
   *   <li>{@link Dimension#LANGUAGE}: language key</li>
   *   <li>{@link Dimension#INSTALLATION}: sub-key of the metric. There is only one installation, identified in the header of the
   *   telemetry payload, so each entry is sent as a separate installation metric whose key is
   *   {@code <metric key>.<entry key>}. This allows a single provider to send several related metrics, whatever the
   *   {@link #getGranularity() granularity}. For example, with metric key {@code issue_count} and entries {@code bug -> 3}
   *   and {@code vulnerability -> 1}, the metrics {@code issue_count.bug} and {@code issue_count.vulnerability} are sent.
   *   When non-empty, it takes precedence over {@link #getValue()}.</li>
   * </ul>
   * Providers support only the dimensions listed above. The other dimensions ({@link Dimension#ANALYSIS},
   * {@link Dimension#FIX_SUGGESTION}) are sent through dedicated messages, and a provider declaring one of them is rejected.
   *
   * @return map of keys and their values.
   */
  default Map<String, T> getValues() {
    return Map.of();
  }

  /**
   * This method will be executed for every telemetry provider after telemetry metrics are sent. This is important for some telemetry
   * providers, more specifically, those adhoc metrics which are usually to be sent as once off values as these values are typically
   * stored in memory. The most common use case would be to clear the data.
   */
  default void after() {
    // this method does nothing by default it is used to perform cleanup tasks if needed
  }
}
