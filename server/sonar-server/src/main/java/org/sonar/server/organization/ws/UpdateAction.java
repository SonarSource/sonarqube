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
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_03;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ID;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateAction implements OrganizationsAction {
  private static final String ACTION = "update";

  private final UserSession userSession;
  private final OrganizationsWsSupport wsSupport;
  private final DbClient dbClient;
  private final System2 system2;

  public UpdateAction(UserSession userSession, OrganizationsWsSupport wsSupport, DbClient dbClient, System2 system2) {
    this.userSession = userSession;
    this.wsSupport = wsSupport;
    this.dbClient = dbClient;
    this.system2 = system2;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription(
        format("Update an organization.<br/>" +
          "The '%s' or '%s' must be provided.<br/>" +
          "Require 'Administer System' permission.",
            PARAM_ID, PARAM_KEY))
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setRequired(false)
      .setDescription("Organization id")
      .setExampleValue(UUID_EXAMPLE_03);

    action.createParam(PARAM_KEY)
      .setRequired(false)
      .setDescription("Organization key")
      .setExampleValue("foo-company");

    wsSupport.addOrganizationDetailsParams(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);

    String uuid = request.param(PARAM_ID);
    String key = request.param(PARAM_KEY);
    checkKeyOrUuid(uuid, key);
    String name = wsSupport.getAndCheckName(request);
    String description = wsSupport.getAndCheckDescription(request);
    String url = wsSupport.getAndCheckUrl(request);
    String avatar = wsSupport.getAndCheckAvatar(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto dto = getDto(dbSession, uuid, key);
      dto.setName(name)
        .setDescription(description)
        .setUrl(url)
        .setAvatarUrl(avatar)
        .setUpdatedAt(system2.now());
      dbClient.organizationDao().update(dbSession, dto);
      dbSession.commit();

      writeResponse(request, response, dto);
    }
  }

  private static void checkKeyOrUuid(@Nullable String uuid, @Nullable String key) {
    checkArgument(uuid != null ^ key != null, "Either '%s' or '%s' must be provided, not both", PARAM_ID, PARAM_KEY);
  }

  private OrganizationDto getDto(DbSession dbSession, @Nullable String uuid, @Nullable String key) {
    if (uuid != null) {
      return failIfEmpty(dbClient.organizationDao().selectByUuid(dbSession, uuid), "Organization not found for uuid '%s'", uuid);
    } else {
      return failIfEmpty(dbClient.organizationDao().selectByKey(dbSession, key), "Organization not found for key '%s'", key);
    }
  }

  private static OrganizationDto failIfEmpty(Optional<OrganizationDto> organizationDto, String msg, Object argument) {
    if (!organizationDto.isPresent()) {
      throw new NotFoundException(format(msg, argument));
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
