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
package org.sonar.server.usergroups.ws;

import java.util.Set;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineGroupWsParameters;

public class DeleteAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupService groupService;
  private final ManagedInstanceService managedInstanceService;

  public DeleteAction(DbClient dbClient, UserSession userSession, GroupService groupService, ManagedInstanceService managedInstanceService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.groupService = groupService;
    this.managedInstanceService = managedInstanceService;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("delete")
      .setDescription(format("Delete a group. The default groups cannot be deleted.<br/>" +
        "'%s' must be provided.<br />" +
        "Requires the following permission: 'Administer System'.", PARAM_GROUP_NAME))
      .setHandler(this)
      .setSince("5.2")
      .setPost(true)
      .setChangelog(
        new Change("10.0", "Parameter 'id' is removed. Use 'name' instead."),
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));

    defineGroupWsParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkPermission(GlobalPermission.ADMINISTER);
      GroupDto group = findGroupOrThrow(request, dbSession);
      checkIfInstanceAndGroupAreManaged(dbSession, group);
      groupService.delete(dbSession, group);

      dbSession.commit();
      response.noContent();
    }
  }

  private void checkIfInstanceAndGroupAreManaged(DbSession dbSession, GroupDto group) {
    boolean isGroupManaged = managedInstanceService.getGroupUuidToManaged(dbSession, Set.of(group.getUuid())).getOrDefault(group.getUuid(), false);
    if (isGroupManaged) {
      throw BadRequestException.create("Deleting managed groups is not allowed.");
    }
  }

  private GroupDto findGroupOrThrow(Request request, DbSession dbSession) {
    String groupName = request.mandatoryParam(PARAM_GROUP_NAME);
    return groupService.findGroup(dbSession, groupName)
      .orElseThrow(() -> new NotFoundException(format("No group with name '%s'", groupName)));
  }
}
