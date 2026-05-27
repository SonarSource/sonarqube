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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.process.ProcessProperties;
import org.sonar.server.monitoring.ServerMonitoringMetrics;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebApiV1MetricsInterceptorTest {

  private final ServerMonitoringMetrics metrics = mock(ServerMonitoringMetrics.class);
  private final MapSettings settings = new MapSettings();

  private WebApiV1MetricsInterceptor underTest;

  @BeforeEach
  void before() {
    underTest = new WebApiV1MetricsInterceptor(metrics, settings.asConfig());
  }

  @Test
  void preAction_postAction_whenEnabled_recordsDuration() {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), true);
    WebService.Action action = mockAction("api/issues/search");
    Request request = mock(Request.class);

    underTest.preAction(action, request);
    underTest.postAction(action, request);

    verify(metrics, atLeastOnce()).observeWebApiV1RequestDuration(anyDouble(), eq("api/issues/search"));
  }

  @Test
  void preAction_postAction_whenDisabled_recordsNothing() {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), false);
    WebService.Action action = mockAction("api/issues/search");
    Request request = mock(Request.class);

    underTest.preAction(action, request);
    underTest.postAction(action, request);

    verify(metrics, never()).observeWebApiV1RequestDuration(anyDouble(), anyString());
  }

  @Test
  void postAction_withoutPreAction_doesNothing() {
    settings.setProperty(ProcessProperties.Property.PERFORMANCE_MONITORING_ENABLED.getKey(), true);
    WebService.Action action = mockAction("api/issues/search");
    Request request = mock(Request.class);

    underTest.postAction(action, request);

    verify(metrics, never()).observeWebApiV1RequestDuration(anyDouble(), anyString());
  }

  @Test
  void preAction_postAction_whenPropertyMissing_defaultsToEnabled() {
    WebService.Action action = mockAction("api/issues/search");
    Request request = mock(Request.class);

    underTest.preAction(action, request);
    underTest.postAction(action, request);

    verify(metrics, atLeastOnce()).observeWebApiV1RequestDuration(anyDouble(), eq("api/issues/search"));
  }

  private static WebService.Action mockAction(String path) {
    WebService.Action action = mock(WebService.Action.class);
    when(action.path()).thenReturn(path);
    return action;
  }
}
