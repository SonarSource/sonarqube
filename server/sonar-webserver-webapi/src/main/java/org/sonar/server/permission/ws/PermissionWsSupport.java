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
package org.sonar.server.permission.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserId;
import org.sonar.db.user.UserIdDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.GroupUuidOrAnyone;
import org.sonar.server.permission.ws.template.WsTemplateRef;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.ws.GroupWsRef;
import org.sonar.server.usergroups.ws.GroupWsSupport;
import org.sonarqube.ws.client.permission.PermissionsWsParameters;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdmin;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;

public class PermissionWsSupport {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final GroupWsSupport groupWsSupport;
  private final Configuration configuration;

  public PermissionWsSupport(DbClient dbClient, Configuration configuration, ComponentFinder componentFinder, GroupWsSupport groupWsSupport) {
    this.dbClient = dbClient;
    this.configuration = configuration;
    this.componentFinder = componentFinder;
    this.groupWsSupport = groupWsSupport;
  }

  public void checkPermissionManagementAccess(UserSession userSession, @Nullable ComponentDto project) {
    checkProjectAdmin(userSession, configuration, project);
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
    UserDto dto = ofNullable(dbClient.userDao().selectActiveUserByLogin(dbSession, login))
      .orElseThrow(() -> new NotFoundException(format("User with login '%s' is not found'", login)));
    return new UserIdDto(dto.getUuid(), dto.getLogin());
  }

  public PermissionTemplateDto findTemplate(DbSession dbSession, WsTemplateRef ref) {
    String uuid = ref.uuid();
    String name = ref.name();
    if (uuid != null) {
      return checkFound(
        dbClient.permissionTemplateDao().selectByUuid(dbSession, uuid),
        "Permission template with id '%s' is not found", uuid);
    } else {
      checkNotNull(name);
      return checkFound(
        dbClient.permissionTemplateDao().selectByName(dbSession, name),
        "Permission template with name '%s' is not found (case insensitive)", name);
    }
  }

}
