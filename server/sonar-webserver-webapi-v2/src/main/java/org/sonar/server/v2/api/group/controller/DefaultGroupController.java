/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.v2.api.group.controller;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.group.response.RestGroupResponse;

public class DefaultGroupController implements GroupController {

  private static final String GROUP_NOT_FOUND_MESSAGE = "Group '%s' not found";
  private final GroupService groupService;
  private final DbClient dbClient;
  private final UserSession userSession;

  public DefaultGroupController(GroupService groupService, DbClient dbClient, UserSession userSession) {
    this.groupService = groupService;
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public RestGroupResponse fetchGroup(String id) {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    try (DbSession session = dbClient.openSession(false)) {
      return groupService.findGroupByUuid(session, id)
        .map(DefaultGroupController::groupDtoToResponse)
        .orElseThrow(() -> new NotFoundException(String.format(GROUP_NOT_FOUND_MESSAGE, id)));
    }
  }

  private static RestGroupResponse groupDtoToResponse(GroupDto group) {
    return new RestGroupResponse(group.getUuid(), group.getName(), group.getDescription());
  }
}
