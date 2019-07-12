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
package org.sonar.server.organization.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationHelper;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class PreventUserDeletionAction implements OrganizationsWsAction {

  private static final String ACTION = "prevent_user_deletion";

  private final DbClient dbClient;
  private final UserSession userSession;

  public PreventUserDeletionAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction(ACTION)
      .setPost(false)
      .setDescription("List organizations that prevent the deletion of the authenticated user.")
      .setResponseExample(getClass().getResource("prevent_user_deletion-example.json"))
      .setInternal(true)
      .setSince("7.9")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      int userId = userSession.getUserId();
      List<OrganizationDto> organizationsThatPreventUserDeletion = new OrganizationHelper(dbClient).selectOrganizationsWithLastAdmin(dbSession, userId);

      Organizations.PreventUserDeletionWsResponse wsResponse = buildResponse(organizationsThatPreventUserDeletion);
      writeProtobuf(wsResponse, request, response);
    }
  }

  private Organizations.PreventUserDeletionWsResponse buildResponse(List<OrganizationDto> organizations) {
    Organizations.PreventUserDeletionWsResponse.Builder response = Organizations.PreventUserDeletionWsResponse.newBuilder();
    Organizations.PreventUserDeletionWsResponse.Organization.Builder wsOrganization = Organizations.PreventUserDeletionWsResponse.Organization.newBuilder();
    organizations.forEach(o -> {
        wsOrganization.clear();
        response.addOrganizations(toOrganization(wsOrganization, o));
      });
    return response.build();
  }

  private static Organizations.PreventUserDeletionWsResponse.Organization.Builder toOrganization(
    Organizations.PreventUserDeletionWsResponse.Organization.Builder builder, OrganizationDto organization) {
    return builder
      .setName(organization.getName())
      .setKey(organization.getKey());
  }
}
