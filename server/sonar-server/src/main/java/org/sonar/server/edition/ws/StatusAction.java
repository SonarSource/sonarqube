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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.EditionManagementState;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Editions;

public class StatusAction implements EditionsWsAction {
  private final UserSession userSession;
  private final EditionManagementState editionManagementState;

  public StatusAction(UserSession userSession, EditionManagementState editionManagementState) {
    this.userSession = userSession;
    this.editionManagementState = editionManagementState;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("status")
      .setSince("6.7")
      .setPost(false)
      .setDescription("Provide status of SonarSource commercial edition of the current SonarQube. Requires 'Administer System' permission.")
      .setResponseExample(getClass().getResource("example-edition-status.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession
      .checkLoggedIn()
      .checkIsSystemAdministrator();

    Editions.StatusResponse.Builder responseBuilder = Editions.StatusResponse.newBuilder()
      .setCurrentEditionKey(editionManagementState.getCurrentEditionKey().orElse(""))
      .setNextEditionKey(editionManagementState.getPendingEditionKey().orElse(""))
      .setInstallationStatus(Editions.InstallationStatus.valueOf(editionManagementState.getPendingInstallationStatus().name()));
    editionManagementState.getInstallErrorMessage().ifPresent(responseBuilder::setInstallError);

    WsUtils.writeProtobuf(responseBuilder.build(), request, response);
  }
}
