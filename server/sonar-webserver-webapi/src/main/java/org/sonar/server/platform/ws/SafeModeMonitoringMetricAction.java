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
package org.sonar.server.platform.ws;

import com.google.common.io.Resources;
import com.google.common.net.HttpHeaders;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.monitoring.MonitoringWsAction;
import org.sonar.server.user.BearerPasscode;
import org.sonar.server.user.SystemPasscode;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SafeModeMonitoringMetricAction implements MonitoringWsAction {

  protected static final Gauge isWebUpGauge = Gauge.build().name("sonarqube_health_web_status")
    .help("Tells whether Web process is up or down. 1 for up, 0 for down").register();

  private final SystemPasscode systemPasscode;
  private final BearerPasscode bearerPasscode;

  public SafeModeMonitoringMetricAction(SystemPasscode systemPasscode, BearerPasscode bearerPasscode) {
    this.systemPasscode = systemPasscode;
    this.bearerPasscode = bearerPasscode;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("metrics")
      .setSince("9.3")
      .setDescription("""
        Return monitoring metrics in Prometheus format.\s
        Support content type 'text/plain' (default) and 'application/openmetrics-text'.
        this endpoint can be access using a Bearer token, that needs to be defined in sonar.properties with the 'sonar.web.systemPasscode' key.""")
      .setResponseExample(Resources.getResource(this.getClass(), "monitoring-metrics.txt"))
      .setHandler(this);
    isWebUpGauge.set(1D);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {

    if (!systemPasscode.isValid(request) && !isSystemAdmin() && !bearerPasscode.isValid(request)) {
      throw new ForbiddenException("Insufficient privileges");
    }

    String requestContentType = request.getHeaders().get("accept");
    String contentType = TextFormat.chooseContentType(requestContentType);

    response.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    response.stream().setStatus(200);

    try (Writer writer = new OutputStreamWriter(response.stream().output(), UTF_8)) {
      TextFormat.writeFormat(contentType, writer, CollectorRegistry.defaultRegistry.metricFamilySamples());
      writer.flush();
    }
  }

  public boolean isSystemAdmin() {
    // No authenticated user in safe mode
    return false;
  }

}
