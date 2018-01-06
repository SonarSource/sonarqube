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
package org.sonar.server.permission.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.permission.ProjectId;
import org.sonar.server.permission.UserId;
import org.sonar.server.permission.ws.template.WsTemplateRef;
import org.sonar.server.usergroups.ws.GroupIdOrAnyone;
import org.sonar.server.usergroups.ws.GroupWsRef;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonarqube.ws.client.permission.PermissionsWsParameters;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;

public class PermissionWsSupport {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final GroupWsSupport groupWsSupport;

  public PermissionWsSupport(DbClient dbClient, ComponentFinder componentFinder, GroupWsSupport groupWsSupport) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.groupWsSupport = groupWsSupport;
  }

  public OrganizationDto findOrganization(DbSession dbSession, @Nullable String organizationKey) {
    return groupWsSupport.findOrganizationByKey(dbSession, organizationKey);
  }

  public Optional<ProjectId> findProjectId(DbSession dbSession, Request request) {
    return findProject(dbSession, request)
      .map(ProjectId::new);
  }

  public Optional<ComponentDto> findProject(DbSession dbSession, Request request) {
    String uuid = request.param(PermissionsWsParameters.PARAM_PROJECT_ID);
    String key = request.param(PermissionsWsParameters.PARAM_PROJECT_KEY);
    if (uuid != null || key != null) {
      ProjectWsRef ref = ProjectWsRef.newWsProjectRef(uuid, key);
      return Optional.of(componentFinder.getRootComponentByUuidOrKey(dbSession, ref.uuid(), ref.key()));
    }
    return Optional.empty();
  }

  public ComponentDto getRootComponentOrModule(DbSession dbSession, ProjectWsRef projectRef) {
    return componentFinder.getRootComponentByUuidOrKey(dbSession, projectRef.uuid(), projectRef.key());
  }

  public GroupIdOrAnyone findGroup(DbSession dbSession, Request request) {
    Integer groupId = request.paramAsInt(PARAM_GROUP_ID);
    String orgKey = request.param(PARAM_ORGANIZATION);
    String groupName = request.param(PARAM_GROUP_NAME);
    GroupWsRef groupRef = GroupWsRef.create(groupId, orgKey, groupName);
    return groupWsSupport.findGroupOrAnyone(dbSession, groupRef);
  }

  public UserId findUser(DbSession dbSession, String login) {
    UserDto dto = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
    checkFound(dto, "User with login '%s' is not found'", login);
    return new UserId(dto.getId(), dto.getLogin());
  }

  public PermissionTemplateDto findTemplate(DbSession dbSession, WsTemplateRef ref) {
    if (ref.uuid() != null) {
      return checkFound(
        dbClient.permissionTemplateDao().selectByUuid(dbSession, ref.uuid()),
        "Permission template with id '%s' is not found", ref.uuid());
    } else {
      OrganizationDto org = findOrganization(dbSession, ref.getOrganization());
      return checkFound(
        dbClient.permissionTemplateDao().selectByName(dbSession, org.getUuid(), ref.name()),
        "Permission template with name '%s' is not found (case insensitive) in organization with key '%s'", ref.name(), org.getKey());
    }
  }

  public void checkMembership(DbSession dbSession, OrganizationDto organization, UserId user) {
    checkArgument(dbClient.organizationMemberDao().select(dbSession, organization.getUuid(), user.getId()).isPresent(),
      "User '%s' is not member of organization '%s'", user.getLogin(), organization.getKey());
  }
}
