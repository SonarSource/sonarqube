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
package org.sonar.server.monitoring;

import com.google.common.annotations.VisibleForTesting;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.process.ProcessProperties;

/**
 * Periodically measures the round-trip time between this node and the database by running the dialect-specific
 * validation query (the same one used by the connection pool). The connection is borrowed from the pool before
 * the timer starts, so the measurement reflects network + database round-trip only.
 * When the probe fails or performance monitoring is disabled, the gauge is set to NaN so that only a real measurement
 * is reported as a number.
 */
public class DbLatencyTask implements MonitoringTask {

  private static final Logger LOG = LoggerFactory.getLogger(DbLatencyTask.class);

  private static final String DELAY_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.db.latency.initial.delay";
  private static final String PERIOD_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.db.latency.period";
  private static final String WARN_THRESHOLD_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.db.latency.warn.threshold";
  private static final long DEFAULT_WARN_THRESHOLD_IN_MILISECONDS = 10L;
  static final long WARN_LOG_INTERVAL_IN_MILISECONDS = 5 * 60 * 1_000L;

  private final ServerMonitoringMetrics metrics;
  private final Database database;
  private final Configuration config;
  private final System2 system;
  private final LongSupplier nanoTime;
  private long lastWarnTimestamp = Long.MIN_VALUE;

  @Inject
  public DbLatencyTask(ServerMonitoringMetrics metrics, Database database, Configuration config, System2 system) {
    this(metrics, database, config, system, System::nanoTime);
  }

  @VisibleForTesting
  DbLatencyTask(ServerMonitoringMetrics metrics, Database database, Configuration config, System2 system, LongSupplier nanoTime) {
    this.metrics = metrics;
    this.database = database;
    this.config = config;
    this.system = system;
    this.nanoTime = nanoTime;
  }

  @Override
  public long getDelay() {
    return config.getLong(DELAY_IN_MILISECONDS_PROPERTY).orElse(10_000L);
  }

  @Override
  public long getPeriod() {
    return config.getLong(PERIOD_IN_MILISECONDS_PROPERTY).orElse(60_000L);
  }

  @Override
  public void run() {
    if (!isEnabled()) {
      metrics.setDbLatency(Double.NaN);
      return;
    }
    double latencySeconds;
    try {
      latencySeconds = measureLatencySeconds();
    } catch (SQLException e) {
      LOG.error("Failed to measure database latency", e);
      metrics.setDbLatency(Double.NaN);
      return;
    }
    metrics.setDbLatency(latencySeconds);
    warnIfAboveThreshold(latencySeconds);
  }

  private double measureLatencySeconds() throws SQLException {
    String validationQuery = database.getDialect().getValidationQuery();
    try (Connection connection = database.getDataSource().getConnection();
      Statement statement = connection.createStatement()) {
      long startNanos = nanoTime.getAsLong();
      try (ResultSet resultSet = statement.executeQuery(validationQuery)) {
        resultSet.next();
      }
      return (nanoTime.getAsLong() - startNanos) / 1_000_000_000.0;
    }
  }

  private void warnIfAboveThreshold(double latencySeconds) {
    long thresholdMs = config.getLong(WARN_THRESHOLD_IN_MILISECONDS_PROPERTY).orElse(DEFAULT_WARN_THRESHOLD_IN_MILISECONDS);
    double latencyMs = latencySeconds * 1_000;
    if (latencyMs <= thresholdMs) {
      return;
    }
    long now = system.now();
    if (lastWarnTimestamp == Long.MIN_VALUE || now - lastWarnTimestamp >= WARN_LOG_INTERVAL_IN_MILISECONDS) {
      lastWarnTimestamp = now;
      LOG.atWarn()
        .addArgument(() -> String.format(Locale.ROOT, "%.1f", latencyMs))
        .addArgument(thresholdMs)
        .log("Database latency is {} ms, which is above the threshold of {} ms. This may degrade SonarQube performance.");
    }
  }

  private boolean isEnabled() {
    return config.getBoolean(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey()).orElse(false);
  }
}
