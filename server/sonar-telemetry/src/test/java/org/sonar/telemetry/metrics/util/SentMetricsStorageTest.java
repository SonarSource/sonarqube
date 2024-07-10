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

import com.tngtech.java.junit.dataprovider.DataProvider;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.db.telemetry.TelemetryMetricsSentDto;
import org.sonar.telemetry.Dimension;
import org.sonar.telemetry.Granularity;

public class SentMetricsStorageTest {

  public static final String METRIC_1 = "metric-1";
  public static final String METRIC_2 = "metric-2";
  public static final String METRIC_3 = "metric-3";
  public static final String METRIC_4 = "metric-4";

  @DataProvider
  public static Object[][] data() {
    return new Object[][]{
      // Dimension: INSTALLATION
      {Dimension.INSTALLATION, METRIC_1, Granularity.DAILY, false}, // 1 minute ago
      {Dimension.INSTALLATION, METRIC_1, Granularity.WEEKLY, false}, // 1 minute ago
      {Dimension.INSTALLATION, METRIC_1, Granularity.MONTHLY, false}, // 1 minute ago

      // Dimension: USER
      {Dimension.USER, METRIC_2, Granularity.DAILY, true}, // 2 days ago
      {Dimension.USER, METRIC_2, Granularity.WEEKLY, false}, // 2 days ago
      {Dimension.USER, METRIC_2, Granularity.MONTHLY, false}, // 2 days ago

      // Dimension: PROJECT
      {Dimension.PROJECT, METRIC_3, Granularity.DAILY, true}, // 10 days ago
      {Dimension.PROJECT, METRIC_3, Granularity.WEEKLY, true}, // 10 days ago
      {Dimension.PROJECT, METRIC_3, Granularity.MONTHLY, false}, // 10 days ago

      // Dimension: LANGUAGE
      {Dimension.LANGUAGE, METRIC_4, Granularity.DAILY, true}, // 40 days ago
      {Dimension.LANGUAGE, METRIC_4, Granularity.WEEKLY, true}, // 40 days ago
      {Dimension.LANGUAGE, METRIC_4, Granularity.MONTHLY, true},  // 40 days ago

      // Non-existing metrics that should be sent, as they are sent for the first time
      {Dimension.INSTALLATION, "metric-5", Granularity.DAILY, true},
      {Dimension.USER, "metric-6", Granularity.WEEKLY, true},
      {Dimension.PROJECT, "metric-7", Granularity.MONTHLY, true}
    };
  }

  @ParameterizedTest
  @MethodSource("data")
  void shouldSendMetric(Dimension dimension, String metricKey, Granularity granularity, boolean expectedResult) {
    SentMetricsStorage storage = new SentMetricsStorage(getDtos());
    boolean actualResult = storage.shouldSendMetric(dimension, metricKey, granularity);
    Assertions.assertEquals(expectedResult, actualResult);
  }

  private List<TelemetryMetricsSentDto> getDtos() {
    TelemetryMetricsSentDto dto1 = new TelemetryMetricsSentDto(METRIC_1, Dimension.INSTALLATION.getValue());
    dto1.setLastSent(Instant.now().minus(1, ChronoUnit.MINUTES).toEpochMilli()); // 1 minute ago

    TelemetryMetricsSentDto dto2 = new TelemetryMetricsSentDto(METRIC_2, Dimension.USER.getValue());
    dto2.setLastSent(Instant.now().minus(2, ChronoUnit.DAYS).toEpochMilli()); // 2 days ago

    TelemetryMetricsSentDto dto3 = new TelemetryMetricsSentDto(METRIC_3, Dimension.PROJECT.getValue());
    dto3.setLastSent(Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli()); // 10 days ago

    TelemetryMetricsSentDto dto4 = new TelemetryMetricsSentDto(METRIC_4, Dimension.LANGUAGE.getValue());
    dto4.setLastSent(Instant.now().minus(40, ChronoUnit.DAYS).toEpochMilli()); // 40 days ago

    return Arrays.asList(dto1, dto2, dto3, dto4);
  }

}
