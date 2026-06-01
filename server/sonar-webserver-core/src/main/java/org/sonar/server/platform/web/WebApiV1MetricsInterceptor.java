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
package org.sonar.server.platform.web;

import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.process.ProcessProperties;
import org.sonar.server.monitoring.ServerMonitoringMetrics;
import org.sonar.server.ws.ActionInterceptor;

public class WebApiV1MetricsInterceptor implements ActionInterceptor {

  private final ServerMonitoringMetrics metrics;
  private final Configuration config;
  private final ThreadLocal<Long> startNanos = new ThreadLocal<>();

  public WebApiV1MetricsInterceptor(ServerMonitoringMetrics metrics, Configuration config) {
    this.metrics = metrics;
    this.config = config;
  }

  @Override
  public void preAction(WebService.Action action, Request request) {
    if (!isEnabled()) {
      return;
    }
    startNanos.set(System.nanoTime());
  }

  @Override
  public void postAction(WebService.Action action, Request request) {
    Long start = startNanos.get();
    if (start == null) {
      return;
    }
    try {
      metrics.observeWebApiV1RequestDuration((System.nanoTime() - start) / 1_000_000_000.0, action.path());
    } finally {
      startNanos.remove();
    }
  }

  private boolean isEnabled() {
    return config.getBoolean(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey()).orElse(false);
  }
}
