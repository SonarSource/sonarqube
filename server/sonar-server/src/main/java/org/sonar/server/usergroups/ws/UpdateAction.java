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
package org.sonar.server.usergroups.ws;

import java.util.Objects;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.user.UserGroupValidation;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupMembershipQuery;
import org.sonar.db.user.UserMembershipQuery;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.platform.PersistentSettings;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsUserGroups;

import static org.sonar.api.CoreProperties.CORE_DEFAULT_GROUP;
import static org.sonar.api.user.UserGroupValidation.GROUP_NAME_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.GroupWsSupport.DESCRIPTION_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_DESCRIPTION;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.toProtobuf;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UpdateAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;
  private final PersistentSettings persistentSettings;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public UpdateAction(DbClient dbClient, UserSession userSession, GroupWsSupport support, PersistentSettings persistentSettings,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
    this.persistentSettings = persistentSettings;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("update")
      .setDescription("Update a group.")
      .setHandler(this)
      .setPost(true)
      .setResponseExample(getClass().getResource("example-update.json"))
      .setSince("5.2");

    action.createParam(PARAM_GROUP_ID)
      .setDescription("Identifier of the group.")
      .setExampleValue("42")
      .setRequired(true);

    action.createParam(PARAM_GROUP_NAME)
      .setDescription(String.format("New optional name for the group. A group name cannot be larger than %d characters and must be unique. " +
        "The value 'anyone' (whatever the case) is reserved and cannot be used. If value is empty or not defined, then name is not changed.", GROUP_NAME_MAX_LENGTH))
      .setExampleValue("sonar-users");

    action.createParam(PARAM_GROUP_DESCRIPTION)
      .setDescription(String.format("New optional description for the group. A group description cannot be larger than %d characters. " +
        "If value is not defined, then description is not changed.", DESCRIPTION_MAX_LENGTH))
      .setExampleValue("Default group for new users");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      long groupId = request.mandatoryParamAsLong(PARAM_GROUP_ID);
      GroupDto group = dbClient.groupDao().selectById(dbSession, groupId);
      checkFound(group, "Could not find a user group with id '%s'.", groupId);
      Optional<OrganizationDto> org = dbClient.organizationDao().selectByUuid(dbSession, group.getOrganizationUuid());
      checkFoundWithOptional(org, "Could not find organization with id '%s'.", group.getOrganizationUuid());
      userSession.checkOrganizationPermission(org.get().getUuid(), GlobalPermissions.SYSTEM_ADMIN);

      boolean changed = false;
      String newName = request.param(PARAM_GROUP_NAME);
      if (newName != null) {
        changed = true;
        UserGroupValidation.validateGroupName(newName);
        support.checkNameDoesNotExist(dbSession, group.getOrganizationUuid(), newName);

        String oldName = group.getName();
        group.setName(newName);
        updateDefaultGroupIfNeeded(dbSession, org.get(), oldName, newName);
      }

      String description = request.param(PARAM_GROUP_DESCRIPTION);
      if (description != null) {
        changed = true;
        group.setDescription(support.validateDescription(description));
      }

      if (changed) {
        dbClient.groupDao().update(dbSession, group);
        dbSession.commit();
      }

      writeResponse(dbSession, request, response, org.get(), group);
    }
  }

  private void updateDefaultGroupIfNeeded(DbSession dbSession, OrganizationDto org, String oldName, String newName) {
    // The feature "default group" relies on a property. As organization properties are
    // not implemented yet, default groups are not supported on non-default organizations
    if (defaultOrganizationProvider.get().getUuid().equals(org.getUuid())) {
      String defaultGroupName = persistentSettings.getString(CORE_DEFAULT_GROUP);
      if (Objects.equals(defaultGroupName, oldName)) {
        persistentSettings.saveProperty(dbSession, CORE_DEFAULT_GROUP, newName);
      }
    }
  }

  private void writeResponse(DbSession dbSession, Request request, Response response, OrganizationDto organization, GroupDto group) {
    UserMembershipQuery query = UserMembershipQuery.builder()
      .groupId(group.getId())
      .membership(GroupMembershipQuery.IN)
      .build();
    int membersCount = dbClient.groupMembershipDao().countMembers(dbSession, query);

    WsUserGroups.UpdateResponse.Builder respBuilder = WsUserGroups.UpdateResponse.newBuilder();
    respBuilder.setGroup(toProtobuf(organization, group, membersCount));
    writeProtobuf(respBuilder.build(), request, response);
  }
}
