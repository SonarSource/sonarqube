/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.edition.ws;

import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.measure.index.ProjectMeasuresIndex;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Editions.FormDataResponse;

public class FormDataAction implements EditionsWsAction {
  private final UserSession userSession;
  private final Server server;
  private final ProjectMeasuresIndex measuresIndex;

  public FormDataAction(UserSession userSession, Server server, ProjectMeasuresIndex measuresIndex) {
    this.userSession = userSession;
    this.server = server;
    this.measuresIndex = measuresIndex;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("form_data")
      .setSince("6.7")
      .setPost(false)
      .setDescription("Provide data to prefill license request forms: the server ID and the total number of lines of code.")
      .setResponseExample(getClass().getResource("example-edition-form_data.json"))
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession
      .checkLoggedIn()
      .checkIsSystemAdministrator();

    String serverId = server.getId();
    long nloc = measuresIndex.searchTelemetryStatistics().getNcloc();

    FormDataResponse responsePayload = FormDataResponse.newBuilder()
      .setNcloc(nloc)
      .setServerId(serverId)
      .build();
    WsUtils.writeProtobuf(responsePayload, request, response);
  }
}
