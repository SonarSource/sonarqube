/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.ui.ws;

import org.sonar.api.platform.Server;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.measure.SumNclocDbQuery;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Navigation;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class MarketplaceAction implements NavigationWsAction {
  private final UserSession userSession;
  private final Server server;
  private final DbClient dbClient;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public MarketplaceAction(UserSession userSession, Server server, DbClient dbClient, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.userSession = userSession;
    this.server = server;
    this.dbClient = dbClient;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("marketplace")
      .setSince("7.2")
      .setPost(false)
      .setDescription("Provide data to prefill license request forms: the server ID and the total number of lines of code.")
      .setResponseExample(getClass().getResource("marketplace-example.json"))
      .setInternal(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    userSession
      .checkLoggedIn()
      .checkIsSystemAdministrator();

    Navigation.MarketplaceResponse responsePayload = Navigation.MarketplaceResponse.newBuilder()
      .setNcloc(computeNcloc())
      .setServerId(server.getId())
      .build();
    writeProtobuf(responsePayload, request, response);
  }

  private long computeNcloc() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      SumNclocDbQuery query = SumNclocDbQuery.builder()
        .setOnlyPrivateProjects(false)
        .setOrganizationUuid(defaultOrganizationProvider.get().getUuid())
        .build();
      return dbClient.liveMeasureDao().sumNclocOfBiggestLongLivingBranch(dbSession, query);
    }
  }
}
