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
package org.sonar.server.platform.db.migration;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.MessageSerializer;
import org.sonar.telemetry.core.TelemetryClient;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.core.schema.BaseMessage;
import org.sonar.telemetry.core.schema.InstallationMetric;
import org.sonar.telemetry.core.schema.Metric;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

@ServerSide
public class DatabaseMigrationTelemetry implements Startable {

  public static final String KEY_DB_MIGRATION_SUCCESS = "db_migration_success";
  public static final String KEY_DB_MIGRATION_COMPLETED_STEPS = "db_migration_completed_steps";
  public static final String KEY_DB_MIGRATION_TOTAL_TIME_MS = "db_migration_total_time_ms";
  public static final String KEY_DB_MIGRATION_STEP_DURATION = "db_migration_step_duration.";

  private final Server server;
  private final UuidFactory uuidFactory;
  private final DatabaseMigrationLoggerContext databaseMigrationLoggerContext;
  private final TelemetryClient telemetryClient;
  private final Configuration configuration;

  public DatabaseMigrationTelemetry(Server server, UuidFactory uuidFactory, DatabaseMigrationLoggerContext databaseMigrationLoggerContext,
    TelemetryClient telemetryClient, Configuration configuration) {
    this.server = server;
    this.uuidFactory = uuidFactory;
    this.databaseMigrationLoggerContext = databaseMigrationLoggerContext;
    this.telemetryClient = telemetryClient;
    this.configuration = configuration;
  }

  @Override
  public void start() {
    sendTelemetry();
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void sendTelemetry() {
    if (!configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)) {
      return;
    }

    if (databaseMigrationLoggerContext.getMigrationData().isEmpty()) {
      return;
    }

    Set<Metric> metrics = getMetrics();

    BaseMessage baseMessage = new BaseMessage.Builder()
      .setMessageUuid(uuidFactory.create())
      .setInstallationId(server.getId())
      .setDimension(Dimension.INSTALLATION)
      .setMetrics(metrics)
      .build();

    String jsonString = MessageSerializer.serialize(baseMessage);
    telemetryClient.uploadMetricAsync(jsonString);
  }

  @NotNull
  private Set<Metric> getMetrics() {
    Set<Metric> metrics = new HashSet<>();

    InstallationMetric migrationTotalTimeMetric = getMigrationTotalTimeMetric();
    InstallationMetric migrationCompletedStepsMetric = getMigrationCompletedStepsMetric();
    InstallationMetric migrationSuccessMetric = getMigrationSuccessMetric();
    Set<InstallationMetric> migrationMetrics = getMigrationMetrics();

    metrics.add(migrationTotalTimeMetric);
    metrics.add(migrationCompletedStepsMetric);
    metrics.add(migrationSuccessMetric);
    metrics.addAll(migrationMetrics);
    return metrics;
  }

  private Set<InstallationMetric> getMigrationMetrics() {
    return databaseMigrationLoggerContext.getMigrationData().stream().map(
      data -> new InstallationMetric(
      KEY_DB_MIGRATION_STEP_DURATION + data.step(),
      data.durationInMs(),
      TelemetryDataType.INTEGER,
      Granularity.ADHOC
    )).collect(Collectors.toSet());
  }

  private InstallationMetric getMigrationSuccessMetric() {
    return new InstallationMetric(
      KEY_DB_MIGRATION_SUCCESS,
      databaseMigrationLoggerContext.getMigrationData().stream().allMatch(DatabaseMigrationLoggerContext.MigrationData::success),
      TelemetryDataType.BOOLEAN,
      Granularity.ADHOC
    );
  }

  private InstallationMetric getMigrationCompletedStepsMetric() {
    return new InstallationMetric(
      KEY_DB_MIGRATION_COMPLETED_STEPS,
      databaseMigrationLoggerContext.getMigrationData().size(),
      TelemetryDataType.INTEGER,
      Granularity.ADHOC
    );
  }

  private InstallationMetric getMigrationTotalTimeMetric() {
    long sum = databaseMigrationLoggerContext.getMigrationData().stream().mapToLong(DatabaseMigrationLoggerContext.MigrationData::durationInMs).sum();
    return new InstallationMetric(
      KEY_DB_MIGRATION_TOTAL_TIME_MS,
      sum,
      TelemetryDataType.INTEGER,
      Granularity.ADHOC
    );
  }

}
