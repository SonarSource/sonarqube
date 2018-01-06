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
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndexer;

import static java.util.Collections.singletonList;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_LOGIN;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.server.ws.KeyExamples.KEY_ORG_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class RemoveMemberAction implements OrganizationsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final UserIndexer userIndexer;

  public RemoveMemberAction(DbClient dbClient, UserSession userSession, UserIndexer userIndexer) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.userIndexer = userIndexer;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("remove_member")
      .setDescription("Remove a member from an organization.<br>" +
        "Requires 'Administer System' permission on the specified organization.")
      .setSince("6.4")
      .setPost(true)
      .setInternal(true)
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
      OrganizationDto organization = checkFoundWithOptional(dbClient.organizationDao().selectByKey(dbSession, organizationKey),
        "Organization '%s' is not found", organizationKey);
      UserDto user = checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, login), "User '%s' is not found", login);
      userSession.checkPermission(ADMINISTER, organization);

      dbClient.organizationMemberDao().select(dbSession, organization.getUuid(), user.getId())
        .ifPresent(om -> removeMember(dbSession, organization, user));
    }
    response.noContent();
  }

  private void removeMember(DbSession dbSession, OrganizationDto organization, UserDto user) {
    ensureLastAdminIsNotRemoved(dbSession, organization, user);
    int userId = user.getId();
    String organizationUuid = organization.getUuid();
    dbClient.userPermissionDao().deleteOrganizationMemberPermissions(dbSession, organizationUuid, userId);
    dbClient.permissionTemplateDao().deleteUserPermissionsByOrganization(dbSession, organizationUuid, userId);
    dbClient.qProfileEditUsersDao().deleteByOrganizationAndUser(dbSession, organization, user);
    dbClient.userGroupDao().deleteByOrganizationAndUser(dbSession, organizationUuid, userId);
    dbClient.propertiesDao().deleteByOrganizationAndUser(dbSession, organizationUuid, userId);
    dbClient.propertiesDao().deleteByOrganizationAndMatchingLogin(dbSession, organizationUuid, user.getLogin(), singletonList(DEFAULT_ISSUE_ASSIGNEE));

    dbClient.organizationMemberDao().delete(dbSession, organizationUuid, userId);
    userIndexer.commitAndIndex(dbSession, user);
  }

  private void ensureLastAdminIsNotRemoved(DbSession dbSession, OrganizationDto organizationDto, UserDto user) {
    int remainingAdmins = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingUser(dbSession,
      organizationDto.getUuid(), ADMINISTER.getKey(), user.getId());
    checkRequest(remainingAdmins > 0, "The last administrator member cannot be removed");
  }
}
