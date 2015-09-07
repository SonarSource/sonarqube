/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.permission.ws;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;

import static java.lang.String.format;
import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.security.DefaultGroups.isAnyone;
import static org.sonar.server.ws.WsUtils.checkFound;

public class PermissionDependenciesFinder {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public PermissionDependenciesFinder(DbClient dbClient, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  /**
   * @throws org.sonar.server.exceptions.NotFoundException if a project identifier is provided but it's not found
   */
  public Optional<ComponentDto> searchProject(DbSession dbSession, PermissionRequest request) {
    if (!request.project().isPresent()) {
      return Optional.absent();
    }

    WsProjectRef wsProjectRef = request.project().get();
    return Optional.of(componentFinder.getProjectByUuidOrKey(dbSession, wsProjectRef.uuid(), wsProjectRef.key()));
  }

  public ComponentDto getProject(DbSession dbSession, WsProjectRef projectRef) {
    return componentFinder.getProjectByUuidOrKey(dbSession, projectRef.uuid(), projectRef.key());
  }

  public String getGroupName(DbSession dbSession, PermissionRequest request) {
    GroupDto group = getGroup(dbSession, request.group());

    return group == null ? ANYONE : group.getName();
  }

  /**
   *
   * @return null if it's the anyone group
   */
  @CheckForNull
  public GroupDto getGroup(DbSession dbSession, WsGroupRef group) {
    Long groupId = group.id();
    String groupName = group.name();

    if (isAnyone(groupName)) {
      return null;
    }

    GroupDto groupDto = null;

    if (groupId != null) {
      groupDto = checkFound(dbClient.groupDao().selectById(dbSession, groupId),
        format("Group with id '%d' is not found", groupId));
    }

    if (groupName != null) {
      groupDto = checkFound(dbClient.groupDao().selectByName(dbSession, groupName),
        format("Group with name '%s' is not found", groupName));
    }

    return groupDto;
  }

  public UserDto getUser(DbSession dbSession, String userLogin) {
    return checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin),
      format("User with login '%s' is not found'", userLogin));
  }

  public PermissionTemplateDto getTemplate(DbSession dbSession, WsTemplateRef template) {
    if (template.uuid() != null) {
      return checkFound(dbClient.permissionTemplateDao().selectByUuid(dbSession, template.uuid()),
        format("Permission template with id '%s' is not found", template.uuid()));
    } else {
      return checkFound(dbClient.permissionTemplateDao().selectByName(dbSession, template.name()),
        format("Permission template with name '%s' is not found (case insensitive)", template.name()));
    }
  }
}
