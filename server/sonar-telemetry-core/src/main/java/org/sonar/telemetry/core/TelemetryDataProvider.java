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
package org.sonar.telemetry.core;

import java.util.Map;
import java.util.Optional;

/**
 * This interface is used to provide data to the telemetry system. The telemetry system will call the methods of this interface to get the
 * data that will be sent to the telemetry server.
 * If you want to add new metric to the telemetry system, you need to create a new implementation of this interface and register it in the
 * Spring context as a bean.
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
   * For each metric either this method or {@link TelemetryDataProvider#getUuidValues()} should be implemented and used. Not both at once.
   *
   * @return the value of the data provided by this instance.
   */
  default Optional<T> getValue() {
    throw new IllegalStateException("Not implemented");
  }

  /**
   * The implementation of this method might often need to make a call to a database.
   * Similiar as {@link TelemetryDataProvider#getValue()} this method returns values of the metric. Some of the metrics
   * associate a UUID with a value. This method is used to return all the values associated with the UUIDs.
   *
   * @return map of UUIDs and their values.
   */
  default Map<String, T> getUuidValues() {
    throw new IllegalStateException("Not implemented");
  }
}
