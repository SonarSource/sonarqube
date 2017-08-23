/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.health.Health;
import org.sonar.server.health.HealthChecker;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsSystem;

public abstract class AbstractHealthAction implements SystemWsAction {
  private final HealthChecker healthChecker;

  public AbstractHealthAction(HealthChecker healthChecker) {
    this.healthChecker = healthChecker;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("health")
      .setDescription("Provide health status of the current SonarQube instance." +
        "<p>status: the health status" +
        " <ul>" +
        " <li>GREEN: SonarQube is fully operational</li>" +
        " <li>YELLOW: SonarQube is operational but something must be fixed to be fully operational</li>" +
        " <li>RED: SonarQube is not operational</li>" +
        " </ul>" +
        "</p>")
      .setSince("6.6")
      .setResponseExample(Resources.getResource(this.getClass(), "example-health.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    performAuthenticationChecks();

    Health check = healthChecker.check();
    WsSystem.HealthResponse.Builder responseBuilder = WsSystem.HealthResponse.newBuilder()
      .setHealth(WsSystem.Health.valueOf(check.getStatus().name()));
    WsSystem.Cause.Builder causeBuilder = WsSystem.Cause.newBuilder();
    check.getCauses().forEach(str -> responseBuilder.addCauses(causeBuilder.clear().setMessage(str).build()));

    WsUtils.writeProtobuf(responseBuilder.build(), request, response);
  }

  abstract void performAuthenticationChecks();
}
