/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations;

import static java.lang.String.format;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateAction implements OrganizationsAction {
  private static final String ACTION = "update";

  private final UserSession userSession;
  private final OrganizationsWsSupport wsSupport;
  private final DbClient dbClient;

  public UpdateAction(UserSession userSession, OrganizationsWsSupport wsSupport, DbClient dbClient) {
    this.userSession = userSession;
    this.wsSupport = wsSupport;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Update an organization.<br/>" +
        "Require 'Administer System' permission.")
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Organization key")
      .setExampleValue("foo-company");

    wsSupport.addOrganizationDetailsParams(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    String key = request.mandatoryParam(PARAM_KEY);
    String name = wsSupport.getAndCheckName(request);
    String description = wsSupport.getAndCheckDescription(request);
    String url = wsSupport.getAndCheckUrl(request);
    String avatar = wsSupport.getAndCheckAvatar(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto dto = getDto(dbSession, key);

      userSession.checkOrganizationPermission(dto.getUuid(), SYSTEM_ADMIN);

      dto.setName(name)
        .setDescription(description)
        .setUrl(url)
        .setAvatarUrl(avatar);
      dbClient.organizationDao().update(dbSession, dto);
      dbSession.commit();

      writeResponse(request, response, dto);
    }
  }

  private OrganizationDto getDto(DbSession dbSession, String key) {
    Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByKey(dbSession, key);
    if (!organizationDto.isPresent()) {
      throw new NotFoundException(format("Organization not found for key '%s'", (Object) key));
    }
    return organizationDto.get();
  }

  private void writeResponse(Request request, Response response, OrganizationDto dto) {
    writeProtobuf(
      Organizations.UpdateWsResponse.newBuilder().setOrganization(wsSupport.toOrganization(dto)).build(),
      request,
      response);
  }
}
