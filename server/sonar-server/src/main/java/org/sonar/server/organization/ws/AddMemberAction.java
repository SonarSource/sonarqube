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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.issue.ws.AvatarResolver;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndexer;
import org.sonar.server.usergroups.DefaultGroupFinder;
import org.sonarqube.ws.Organizations.AddMemberWsResponse;
import org.sonarqube.ws.Organizations.User;

import static com.google.common.base.Strings.emptyToNull;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_LOGIN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.server.ws.KeyExamples.KEY_ORG_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class AddMemberAction implements OrganizationsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final UserIndexer userIndexer;
  private final DefaultGroupFinder defaultGroupFinder;
  private final AvatarResolver avatarResolver;

  public AddMemberAction(DbClient dbClient, UserSession userSession, UserIndexer userIndexer, DefaultGroupFinder defaultGroupFinder, AvatarResolver avatarResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.userIndexer = userIndexer;
    this.defaultGroupFinder = defaultGroupFinder;
    this.avatarResolver = avatarResolver;
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
      .setRequired(true)
      .setExampleValue(KEY_ORG_EXAMPLE_001);

    action
      .createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("ray.bradbury");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String organizationKey = request.mandatoryParam(PARAM_ORGANIZATION);
    String login = request.mandatoryParam(PARAM_LOGIN);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, organizationKey), "Organization '%s' is not found",
        organizationKey);
      UserDto user = checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' is not found", login);
      addMember(dbSession, organization, user);

      int groups = dbClient.groupMembershipDao().countGroups(dbSession, GroupMembershipQuery.builder()
        .organizationUuid(organization.getUuid())
        .membership(IN)
        .build(), user.getId());
      AddMemberWsResponse wsResponse = buildResponse(user, groups);
      writeProtobuf(wsResponse, request, response);
    }
  }

  private void addMember(DbSession dbSession, OrganizationDto organization, UserDto user) {
    userSession.checkPermission(OrganizationPermission.ADMINISTER, organization);
    if (isMemberOf(dbSession, organization, user)) {
      return;
    }

    dbClient.organizationMemberDao().insert(dbSession, new OrganizationMemberDto()
      .setOrganizationUuid(organization.getUuid())
      .setUserId(user.getId()));
    dbClient.userGroupDao().insert(dbSession,
      new UserGroupDto().setGroupId(defaultGroupFinder.findDefaultGroup(dbSession, organization.getUuid()).getId()).setUserId(user.getId()));
    userIndexer.commitAndIndex(dbSession, user);
  }

  private AddMemberWsResponse buildResponse(UserDto user, int groups) {
    AddMemberWsResponse.Builder response = AddMemberWsResponse.newBuilder();
    User.Builder wsUser = User.newBuilder()
      .setLogin(user.getLogin())
      .setName(user.getName())
      .setGroupCount(groups);
    setNullable(emptyToNull(user.getEmail()), text -> wsUser.setAvatar(avatarResolver.create(user)));
    response.setUser(wsUser);
    return response.build();
  }

  private boolean isMemberOf(DbSession dbSession, OrganizationDto organizationDto, UserDto userDto) {
    return dbClient.organizationMemberDao().select(dbSession, organizationDto.getUuid(), userDto.getId()).isPresent();
  }

}
