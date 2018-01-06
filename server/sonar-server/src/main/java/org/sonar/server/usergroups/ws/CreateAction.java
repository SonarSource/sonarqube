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
package org.sonar.server.usergroups.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.user.UserGroupValidation;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.UserGroups;

import static java.lang.String.format;
import static org.sonar.api.user.UserGroupValidation.GROUP_NAME_MAX_LENGTH;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.DESCRIPTION_MAX_LENGTH;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_DESCRIPTION;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;
import static org.sonar.server.usergroups.ws.GroupWsSupport.toProtobuf;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CreateAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;

  public CreateAction(DbClient dbClient, UserSession userSession, GroupWsSupport support) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
  }

  @Override
  public void define(NewController controller) {
    NewAction action = controller.createAction("create")
      .setDescription("Create a group.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this)
      .setPost(true)
      .setResponseExample(getClass().getResource("create-example.json"))
      .setSince("5.2");

    action.createParam(PARAM_ORGANIZATION_KEY)
      .setDescription("Key of organization. If unset then default organization is used.")
      .setExampleValue("my-org")
      .setSince("6.2")
      .setInternal(true);

    action.createParam(PARAM_GROUP_NAME)
      .setRequired(true)
      .setMaximumLength(GROUP_NAME_MAX_LENGTH)
      .setDescription(format("Name for the new group. A group name cannot be larger than %d characters and must be unique. " +
        "The value 'anyone' (whatever the case) is reserved and cannot be used.", GROUP_NAME_MAX_LENGTH))
      .setExampleValue("sonar-users");

    action.createParam(PARAM_GROUP_DESCRIPTION)
      .setMaximumLength(DESCRIPTION_MAX_LENGTH)
      .setDescription(format("Description for the new group. A group description cannot be larger than %d characters.", DESCRIPTION_MAX_LENGTH))
      .setExampleValue("Default group for new users");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = support.findOrganizationByKey(dbSession, request.param(PARAM_ORGANIZATION_KEY));
      userSession.checkPermission(ADMINISTER, organization);
      GroupDto group = new GroupDto()
        .setOrganizationUuid(organization.getUuid())
        .setName(request.mandatoryParam(PARAM_GROUP_NAME))
        .setDescription(request.param(PARAM_GROUP_DESCRIPTION));

      // validations
      UserGroupValidation.validateGroupName(group.getName());
      support.checkNameDoesNotExist(dbSession, group.getOrganizationUuid(), group.getName());

      dbClient.groupDao().insert(dbSession, group);
      dbSession.commit();

      writeResponse(request, response, organization, group);
    }
  }

  private void writeResponse(Request request, Response response, OrganizationDto organization, GroupDto group) {
    UserGroups.CreateWsResponse.Builder respBuilder = UserGroups.CreateWsResponse.newBuilder();
    // 'default' is always false as it's not possible to create a default group
    respBuilder.setGroup(toProtobuf(organization, group, 0, false));
    writeProtobuf(respBuilder.build(), request, response);
  }
}
