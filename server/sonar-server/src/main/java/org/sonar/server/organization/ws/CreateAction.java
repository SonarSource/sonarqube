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
package org.sonar.server.organization.ws;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.organization.OrganizationCreation;
import org.sonar.server.organization.OrganizationFlags;
import org.sonar.server.organization.OrganizationValidation;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations.CreateWsResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.organization.OrganizationCreation.NewOrganization.newOrganizationBuilder;
import static org.sonar.server.organization.OrganizationValidation.KEY_MAX_LENGTH;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_KEY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements OrganizationsWsAction {
  private static final String ACTION = "create";

  private final Configuration config;
  private final UserSession userSession;
  private final DbClient dbClient;
  private final OrganizationsWsSupport wsSupport;
  private final OrganizationValidation organizationValidation;
  private final OrganizationCreation organizationCreation;
  private final OrganizationFlags organizationFlags;

  public CreateAction(Configuration config, UserSession userSession, DbClient dbClient, OrganizationsWsSupport wsSupport,
    OrganizationValidation organizationValidation, OrganizationCreation organizationCreation, OrganizationFlags organizationFlags) {
    this.config = config;
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.organizationValidation = organizationValidation;
    this.organizationCreation = organizationCreation;
    this.organizationFlags = organizationFlags;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Create an organization.<br />" +
        "Requires 'Administer System' permission unless any logged in user is allowed to create an organization (see appropriate setting). Organization support must be enabled.")
      .setResponseExample(getClass().getResource("create-example.json"))
      .setInternal(true)
      .setSince("6.2")
      .setHandler(this);

    action.createParam(PARAM_KEY)
      .setRequired(false)
      .setMaximumLength(KEY_MAX_LENGTH)
      .setDescription("Key of the organization. <br />" +
        "The key is unique to the whole SonarQube. <br/>" +
        "When not specified, the key is computed from the name. <br />" +
        "Otherwise, it must be between 2 and 32 chars long. All chars must be lower-case letters (a to z), digits or dash (but dash can neither be trailing nor heading)")
      .setExampleValue("foo-company");

    wsSupport.addOrganizationDetailsParams(action, true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (config.getBoolean(CorePropertyDefinitions.ORGANIZATIONS_ANYONE_CAN_CREATE).orElse(false)) {
      userSession.checkLoggedIn();
    } else {
      userSession.checkIsSystemAdministrator();
    }

    String name = wsSupport.getAndCheckMandatoryName(request);
    String requestKey = getAndCheckKey(request);
    String key = useOrGenerateKey(requestKey, name);
    String description = wsSupport.getAndCheckDescription(request);
    String url = wsSupport.getAndCheckUrl(request);
    String avatar = wsSupport.getAndCheckAvatar(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      organizationFlags.checkEnabled(dbSession);
      UserDto currentUser = dbClient.userDao().selectActiveUserByLogin(dbSession, userSession.getLogin());
      OrganizationDto organization = organizationCreation.create(
        dbSession,
        currentUser,
        newOrganizationBuilder()
          .setName(name)
          .setKey(key)
          .setDescription(description)
          .setUrl(url)
          .setAvatarUrl(avatar)
          .build());

      writeResponse(request, response, organization);
    } catch (OrganizationCreation.KeyConflictException e) {
      checkArgument(requestKey == null, "Key '%s' is already used. Specify another one.", key);
      checkArgument(requestKey != null, "Key '%s' generated from name '%s' is already used. Specify one.", key, name);
    }
  }

  @CheckForNull
  private String getAndCheckKey(Request request) {
    String rqstKey = request.param(PARAM_KEY);
    if (rqstKey != null) {
      return organizationValidation.checkKey(rqstKey);
    }
    return rqstKey;
  }

  private String useOrGenerateKey(@Nullable String key, String name) {
    if (key == null) {
      return organizationValidation.generateKeyFrom(name);
    }
    return key;
  }

  private void writeResponse(Request request, Response response, OrganizationDto dto) {
    writeProtobuf(
      CreateWsResponse.newBuilder().setOrganization(wsSupport.toOrganization(dto)).build(),
      request,
      response);
  }

}
