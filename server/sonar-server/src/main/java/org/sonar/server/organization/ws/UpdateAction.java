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

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations;

import static java.lang.String.format;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_AVATAR_URL;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_DESCRIPTION;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_NAME;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_URL;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateAction implements OrganizationsWsAction {
  private static final String ACTION = "update";

  private final UserSession userSession;
  private final OrganizationsWsSupport wsSupport;
  private final DbClient dbClient;
  private final OrganizationFlags organizationFlags;

  public UpdateAction(UserSession userSession, OrganizationsWsSupport wsSupport, DbClient dbClient,
    OrganizationFlags organizationFlags) {
    this.userSession = userSession;
    this.wsSupport = wsSupport;
    this.dbClient = dbClient;
    this.organizationFlags = organizationFlags;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Update an organization.<br/>" +
        "Require 'Administer System' permission. Organization support must be enabled.")
      .setInternal(true)
      .setSince("6.2")
      .setChangelog(new Change("7.4", "Maximal number of character of name is 300 characters"))
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(true)
      .setDescription("Organization key")
      .setExampleValue("foo-company");

    wsSupport.addOrganizationDetailsParams(action, false);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      organizationFlags.checkEnabled(dbSession);

      String key = request.mandatoryParam(PARAM_KEY);

      UpdateOrganizationRequest updateRequest = new UpdateOrganizationRequest(
        request.getParam(PARAM_NAME, (rqt, paramKey) -> wsSupport.getAndCheckName(rqt)),
        request.getParam(PARAM_DESCRIPTION, (rqt, paramKey) -> emptyAsNull(wsSupport.getAndCheckDescription(rqt))),
        request.getParam(PARAM_URL, (rqt, paramKey) -> emptyAsNull(wsSupport.getAndCheckUrl(rqt))),
        request.getParam(PARAM_AVATAR_URL, (rqt, paramKey) -> emptyAsNull(wsSupport.getAndCheckAvatar(rqt))));

      OrganizationDto dto = getDto(dbSession, key);

      userSession.checkPermission(ADMINISTER, dto);

      dto.setName(updateRequest.getName().or(dto::getName))
        .setDescription(updateRequest.getDescription().or(dto::getDescription))
        .setUrl(updateRequest.getUrl().or(dto::getUrl))
        .setAvatarUrl(updateRequest.getAvatar().or(dto::getAvatarUrl));
      dbClient.organizationDao().update(dbSession, dto);
      dbSession.commit();

      writeResponse(request, response, dto);
    }
  }

  @CheckForNull
  private static String emptyAsNull(@Nullable String value) {
    if (value == null || value.isEmpty()) {
      return null;
    }
    return value;
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

  private static final class UpdateOrganizationRequest {
    private final Request.Param<String> name;
    private final Request.Param<String> description;
    private final Request.Param<String> url;
    private final Request.Param<String> avatar;

    private UpdateOrganizationRequest(Request.Param<String> name,
      Request.Param<String> description,
      Request.Param<String> url,
      Request.Param<String> avatar) {
      this.name = name;
      this.description = description;
      this.url = url;
      this.avatar = avatar;
    }

    public Request.Param<String> getName() {
      return name;
    }

    public Request.Param<String> getDescription() {
      return description;
    }

    public Request.Param<String> getUrl() {
      return url;
    }

    public Request.Param<String> getAvatar() {
      return avatar;
    }
  }

}
