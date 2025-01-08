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
package org.sonar.server.monitoring;

import java.lang.management.ManagementFactory;
import org.sonar.api.config.Configuration;

public class WebUptimeTask implements MonitoringTask {

  private static final String DELAY_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.webuptime.initial.delay";
  private static final String PERIOD_IN_MILISECONDS_PROPERTY = "sonar.server.monitoring.webuptime.period";

  private final ServerMonitoringMetrics metrics;
  private final Configuration config;

  public WebUptimeTask(ServerMonitoringMetrics metrics, Configuration configuration) {
    this.metrics = metrics;
    this.config = configuration;
  }

  @Override
  public long getDelay() {
    return config.getLong(DELAY_IN_MILISECONDS_PROPERTY).orElse(10_000L);
  }

  @Override
  public long getPeriod() {
    return config.getLong(PERIOD_IN_MILISECONDS_PROPERTY).orElse(5_000L);
  }

  @Override
  public void run() {
    long javaUptimeInMilliseconds = ManagementFactory.getRuntimeMXBean().getUptime();
    metrics.setWebUptimeMinutes(javaUptimeInMilliseconds / (1000 * 60));
  }
}
