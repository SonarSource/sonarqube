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
package org.sonar.server.platform.ws;

import jakarta.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.common.platform.LivenessChecker;

public class LivenessActionSupport {

  private final LivenessChecker livenessChecker;

  public LivenessActionSupport(LivenessChecker livenessChecker) {
    this.livenessChecker = livenessChecker;
  }

  void define(WebService.NewController controller, SystemWsAction handler) {
    controller.createAction("liveness")
      .setDescription("Provide liveness of SonarQube, meant to be used for a liveness probe on Kubernetes" +
        "<p>Require 'Administer System' permission or authentication with passcode</p>" +
        "<p>When SonarQube is fully started, liveness check for database connectivity, Compute Engine status," +
        " and, except for DataCenter Edition, if ElasticSearch is Green or Yellow</p>"+
        "<p>When SonarQube is on Safe Mode (for example when a database migration is running), liveness check only for database connectivity</p>"+
        "<p> " +
        " <ul>" +
        " <li>HTTP 204: this SonarQube node is alive</li>" +
        " <li>Any other HTTP code: this SonarQube node is not alive, and should be reschedule</li>" +
        " </ul>" +
        "</p>")
      .setSince("9.1")
      .setInternal(true)
      .setChangelog(new Change("10.8", "The endpoint doesn't log an error anymore when the liveness check fails. " +
        "The failed HTTP status code changed from 500 to 503."))
      .setContentType(Response.ContentType.NO_CONTENT)
      .setHandler(handler);
  }

  void checkliveness(Response response) {
    if (livenessChecker.liveness()) {
      response.noContent();
    } else {
      response.stream().setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }
  }

}
