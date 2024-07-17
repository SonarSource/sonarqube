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
package org.sonar.telemetry.metrics.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sonar.db.telemetry.TelemetryMetricsSentDto;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;

public class SentMetricsStorage {
  private final Map<Dimension, Map<String, TelemetryMetricsSentDto>> dimensionMetricKeyMap = new EnumMap<>(Dimension.class);

  public SentMetricsStorage(List<TelemetryMetricsSentDto> dtoList) {
    dtoList.forEach(dto -> dimensionMetricKeyMap
      .computeIfAbsent(Dimension.fromValue(dto.getDimension()), k -> new HashMap<>())
      .put(dto.getMetricKey(), dto));
  }

  public Optional<TelemetryMetricsSentDto> getMetricsSentDto(Dimension dimension, String metricKey) {
    Map<String, TelemetryMetricsSentDto> metricKeyMap = dimensionMetricKeyMap.get(dimension);
    if (metricKeyMap != null && metricKeyMap.containsKey(metricKey)) {
      return Optional.of(metricKeyMap.get(metricKey));
    }

    return Optional.empty();
  }

  public boolean shouldSendMetric(Dimension dimension, String metricKey, Granularity granularity) {
    Map<String, TelemetryMetricsSentDto> metricKeyMap = dimensionMetricKeyMap.get(dimension);
    boolean exists = metricKeyMap != null && metricKeyMap.containsKey(metricKey);

    if (!exists) {
      return true;
    }

    TelemetryMetricsSentDto dto = metricKeyMap.get(metricKey);
    Instant lastSentTime = Instant.ofEpochMilli(dto.getLastSent());
    Instant now = Instant.now();

    LocalDateTime lastSentDateTime = LocalDateTime.ofInstant(lastSentTime, ZoneId.systemDefault());
    LocalDateTime nowDateTime = LocalDateTime.ofInstant(now, ZoneId.systemDefault());

    switch (granularity) {
      case DAILY -> {
        return ChronoUnit.DAYS.between(lastSentDateTime, nowDateTime) > 0;
      } case WEEKLY -> {
        return ChronoUnit.WEEKS.between(lastSentDateTime, nowDateTime) > 0;
      } case MONTHLY -> {
        return ChronoUnit.MONTHS.between(lastSentDateTime, nowDateTime) > 0;
      } default -> throw new IllegalArgumentException("Unknown granularity: " + granularity);
    }
  }
}
