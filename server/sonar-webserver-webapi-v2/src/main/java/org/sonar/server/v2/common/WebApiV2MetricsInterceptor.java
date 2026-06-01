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
package org.sonar.server.v2.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.process.ProcessProperties;
import org.sonar.server.monitoring.ServerMonitoringMetrics;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

public record WebApiV2MetricsInterceptor(ServerMonitoringMetrics metrics, Configuration config) implements HandlerInterceptor {

  private static final String ATTR_START_NANOS = "sonarqube.metrics.startNanos";
  private static final String UNKNOWN_PATTERN = "unknown";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (isEnabled()) {
      request.setAttribute(ATTR_START_NANOS, System.nanoTime());
    }
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) {
    Object startAttr = request.getAttribute(ATTR_START_NANOS);
    if (startAttr instanceof Long start) {
      double durationSeconds = (System.nanoTime() - start) / 1_000_000_000.0;
      metrics.observeWebApiV2RequestDuration(durationSeconds, getEndpointPattern(request), request.getMethod());
    }
  }

  private boolean isEnabled() {
    return config.getBoolean(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey()).orElse(false);
  }

  private static String getEndpointPattern(HttpServletRequest request) {
    Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
    return pattern != null ? pattern.toString() : UNKNOWN_PATTERN;
  }
}
