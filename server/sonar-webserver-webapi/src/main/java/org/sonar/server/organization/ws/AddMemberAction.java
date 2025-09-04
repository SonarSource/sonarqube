/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Optional.ofNullable;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonar.server.organization.ws.MemberUpdater.MemberType;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Organizations.AddMemberWsResponse;
import org.sonarqube.ws.Organizations.User;

public class AddMemberAction implements OrganizationsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final AvatarResolver avatarResolver;
  private final MemberUpdater memberUpdater;

  private static final String PARAM_ORGANIZATION = "organization";
  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_TYPE = "type";

  public AddMemberAction(DbClient dbClient, UserSession userSession, AvatarResolver avatarResolver, MemberUpdater memberUpdater) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.avatarResolver = avatarResolver;
    this.memberUpdater = memberUpdater;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("add_member")
      .setDescription("Add a user as a member of an organization.<br>" +
        "Requires 'Administer System' permission on the specified organization.")
      .setSince("6.4")
      .setPost(true)
      .setInternal(true)
      .setResponseExample(getClass().getResource("add_member-example.json"))
      .setHandler(this);

    action
      .createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setRequired(true);

    action
      .createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("ray.bradbury");

    action
      .createParam(PARAM_TYPE)
      .setDefaultValue(MemberType.STANDARD.name())
      .setDescription("Type of user");

  }

  public void handle(Request request, Response response) throws Exception {
    String organizationKey = request.mandatoryParam(PARAM_ORGANIZATION);
    String login = request.mandatoryParam(PARAM_LOGIN);
    MemberType memberType = request.paramAsEnum(PARAM_TYPE, MemberType.class);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, organizationKey), "Organization '%s' is not found",
        organizationKey);
      userSession.checkIsSystemAdministrator();
      UserDto user = checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' is not found", login);
      memberUpdater.addMember(dbSession, organization, user, memberType);
      dbSession.commit();

      int groups = dbClient.groupMembershipDao().countGroups(dbSession, GroupMembershipQuery.builder()
        .organizationUuid(organization.getUuid())
        .membership(IN)
        .build(), user.getUuid());
      AddMemberWsResponse wsResponse = buildResponse(user, groups);
      writeProtobuf(wsResponse, request, response);
    }
  }

  private AddMemberWsResponse buildResponse(UserDto user, int groups) {
    AddMemberWsResponse.Builder response = AddMemberWsResponse.newBuilder();
    User.Builder wsUser = User.newBuilder()
      .setLogin(user.getLogin())
      .setGroupCount(groups);
    ofNullable(emptyToNull(user.getEmail())).ifPresent(text -> wsUser.setAvatar(avatarResolver.create(user)));
    ofNullable(user.getName()).ifPresent(wsUser::setName);
    response.setUser(wsUser);
    return response.build();
  }
}
