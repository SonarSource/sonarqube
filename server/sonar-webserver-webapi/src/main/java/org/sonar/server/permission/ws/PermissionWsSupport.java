/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.permission.GroupUuidOrAnyone;
import org.sonar.server.permission.ProjectUuid;
import org.sonar.server.permission.UserId;
import org.sonar.server.permission.ws.template.WsTemplateRef;
import org.sonar.server.usergroups.ws.GroupWsRef;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonarqube.ws.client.permission.PermissionsWsParameters;

import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;

public class PermissionWsSupport {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final GroupWsSupport groupWsSupport;

  public PermissionWsSupport(DbClient dbClient, ComponentFinder componentFinder, GroupWsSupport groupWsSupport) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.groupWsSupport = groupWsSupport;
  }

  public Optional<ProjectUuid> findProjectUuid(DbSession dbSession, Request request) {
    return findProject(dbSession, request)
      .map(ProjectUuid::new);
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

  public GroupUuidOrAnyone findGroup(DbSession dbSession, Request request) {
    String groupUuid = request.param(PARAM_GROUP_ID);
    String groupName = request.param(PARAM_GROUP_NAME);
    GroupWsRef groupRef = GroupWsRef.create(groupUuid, groupName);
    return groupWsSupport.findGroupOrAnyone(dbSession, groupRef);
  }

  public UserId findUser(DbSession dbSession, String login) {
    UserDto dto = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
    checkFound(dto, "User with login '%s' is not found'", login);
    return new UserId(dto.getUuid(), dto.getLogin());
  }

  public PermissionTemplateDto findTemplate(DbSession dbSession, WsTemplateRef ref) {
    if (ref.uuid() != null) {
      return checkFound(
        dbClient.permissionTemplateDao().selectByUuid(dbSession, ref.uuid()),
        "Permission template with id '%s' is not found", ref.uuid());
    } else {
      return checkFound(
        dbClient.permissionTemplateDao().selectByName(dbSession, ref.name()),
        "Permission template with name '%s' is not found (case insensitive)", ref.name());
    }
  }

}
