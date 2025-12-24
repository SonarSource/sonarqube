/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.purge;

import java.util.Set;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.core.util.UuidFactory;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.MessageSerializer;
import org.sonar.telemetry.core.TelemetryClient;
import org.sonar.telemetry.core.schema.BaseMessage;
import org.sonar.telemetry.core.schema.Metric;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

@ComputeEngineSide
public class PurgeTelemetry {

  private final TelemetryClient telemetryClient;
  private final UuidFactory uuidFactory;
  private final Server server;
  private final PrQgEnforcementTelemetries prQgEnforcementTelemetries;
  private final Configuration config;

  public PurgeTelemetry(Server server, UuidFactory uuidFactory, Configuration configuration, TelemetryClient telemetryClient,
    PrQgEnforcementTelemetries prQgEnforcementTelemetries) {
    this.server = server;
    this.uuidFactory = uuidFactory;
    this.config = configuration;
    this.telemetryClient = telemetryClient;
    this.prQgEnforcementTelemetries = prQgEnforcementTelemetries;
  }

  public void sendTelemetry() {
    Set<Metric> metrics = getMetrics();
    resetMetrics();
    if (!config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)) {
      return;
    }
    if (metrics.isEmpty()) {
      return;
    }
    BaseMessage baseMessage = new BaseMessage.Builder()
      .setMessageUuid(uuidFactory.create())
      .setInstallationId(server.getId())
      .setDimension(Dimension.INSTALLATION)
      .setMetrics(metrics)
      .build();

    String jsonString = MessageSerializer.serialize(baseMessage);
    telemetryClient.uploadMetricAsync(jsonString);
  }

  private Set<Metric> getMetrics() {
    return prQgEnforcementTelemetries.getMetrics();
  }

  private void resetMetrics() {
    prQgEnforcementTelemetries.resetMetrics();
  }
}
