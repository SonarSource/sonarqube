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

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.process.ProcessProperties;
import org.sonar.server.monitoring.ServerMonitoringMetrics;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WebApiV2MetricsInterceptorTest {

  private final ServerMonitoringMetrics metrics = mock(ServerMonitoringMetrics.class);
  private final MapSettings settings = new MapSettings();

  private WebApiV2MetricsInterceptor underTest;

  @BeforeEach
  void before() {
    underTest = new WebApiV2MetricsInterceptor(metrics, settings.asConfig());
  }

  @Test
  void preHandle_afterCompletion_whenEnabled_recordsDuration() {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), true);
    MockHttpServletRequest request = newRequest("GET", "/api/v2/users", "/api/v2/users");
    HttpServletResponse response = new MockHttpServletResponse();

    underTest.preHandle(request, response, new Object());
    underTest.afterCompletion(request, response, new Object(), null);

    verify(metrics, atLeastOnce()).observeWebApiV2RequestDuration(anyDouble(), eq("/api/v2/users"), eq("GET"));
  }

  @Test
  void preHandle_afterCompletion_whenDisabled_recordsNothing() {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), false);
    MockHttpServletRequest request = newRequest("GET", "/api/v2/users", "/api/v2/users");
    HttpServletResponse response = new MockHttpServletResponse();

    underTest.preHandle(request, response, new Object());
    underTest.afterCompletion(request, response, new Object(), null);

    verify(metrics, never()).observeWebApiV2RequestDuration(anyDouble(), anyString(), anyString());
  }

  @Test
  void afterCompletion_withoutPreHandle_doesNothing() {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), true);
    MockHttpServletRequest request = newRequest("GET", "/api/v2/users", "/api/v2/users");
    HttpServletResponse response = new MockHttpServletResponse();

    underTest.afterCompletion(request, response, new Object(), null);

    verify(metrics, never()).observeWebApiV2RequestDuration(anyDouble(), anyString(), anyString());
  }

  @Test
  void preHandle_afterCompletion_whenPatternAttributeMissing_usesUnknownSentinel() {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), true);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v2/users/foo");
    HttpServletResponse response = new MockHttpServletResponse();

    underTest.preHandle(request, response, new Object());
    underTest.afterCompletion(request, response, new Object(), null);

    verify(metrics, atLeastOnce()).observeWebApiV2RequestDuration(anyDouble(), eq("unknown"), eq("POST"));
  }

  private static MockHttpServletRequest newRequest(String method, String uri, String bestMatchingPattern) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, bestMatchingPattern);
    return request;
  }
}
