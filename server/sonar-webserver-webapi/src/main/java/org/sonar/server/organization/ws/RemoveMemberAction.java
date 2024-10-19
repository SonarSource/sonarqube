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

import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;

public class RemoveMemberAction implements OrganizationsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final MemberUpdater memberUpdater;

  private static final String PARAM_ORGANIZATION = "organization";
  private static final String PARAM_LOGIN = "login";

  public RemoveMemberAction(DbClient dbClient, UserSession userSession, MemberUpdater memberUpdater) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.memberUpdater = memberUpdater;
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
      .setRequired(true);

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
      userSession.checkPermission(ADMINISTER, organization);

      UserDto user = checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, login), "User '%s' is not found", login);
      memberUpdater.removeMember(dbSession, organization, user);
    }
    response.noContent();
  }

}
