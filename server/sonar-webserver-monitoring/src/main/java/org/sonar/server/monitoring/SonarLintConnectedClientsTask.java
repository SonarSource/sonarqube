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
package org.sonar.server.monitoring;

import org.sonar.api.config.Configuration;
import org.sonar.server.pushapi.sonarlint.SonarLintClientsRegistry;

public class SonarLintConnectedClientsTask implements MonitoringTask {

  private static final String DELAY_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.other.initial.delay";
  private static final String PERIOD_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.other.period";

  private final ServerMonitoringMetrics serverMonitoringMetrics;
  private final SonarLintClientsRegistry sonarLintClientsRegistry;
  private final Configuration config;

  public SonarLintConnectedClientsTask(ServerMonitoringMetrics serverMonitoringMetrics, SonarLintClientsRegistry sonarLintClientsRegistry,
    Configuration configuration) {
    this.serverMonitoringMetrics = serverMonitoringMetrics;
    this.sonarLintClientsRegistry = sonarLintClientsRegistry;
    this.config = configuration;
  }

  @Override
  public void run() {
    serverMonitoringMetrics.setNumberOfConnectedSonarLintClients(sonarLintClientsRegistry.countConnectedClients());
  }

  @Override
  public long getDelay() {
    return config.getLong(DELAY_IN_MILISECONDS_PROPERTY).orElse(10_000L);
  }

  @Override
  public long getPeriod() {
    return config.getLong(PERIOD_IN_MILISECONDS_PROPERTY).orElse(10_000L);
  }
}
