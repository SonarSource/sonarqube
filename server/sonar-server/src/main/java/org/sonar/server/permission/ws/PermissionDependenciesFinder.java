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
package org.sonar.server.permission.ws;

import com.google.common.base.Optional;
import javax.annotation.CheckForNull;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.usergroups.ws.UserGroupFinder;
import org.sonar.server.usergroups.ws.WsGroupRef;

import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.api.security.DefaultGroups.isAnyone;
import static org.sonar.server.ws.WsUtils.checkFound;

public class PermissionDependenciesFinder {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final UserGroupFinder userGroupFinder;
  private final ResourceTypes resourceTypes;

  public PermissionDependenciesFinder(DbClient dbClient, ComponentFinder componentFinder, UserGroupFinder userGroupFinder, ResourceTypes resourceTypes) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.userGroupFinder = userGroupFinder;
    this.resourceTypes = resourceTypes;
  }

  /**
   * @throws org.sonar.server.exceptions.NotFoundException if a project identifier is provided but it's not found
   */
  public Optional<ComponentDto> searchProject(DbSession dbSession, Optional<WsProjectRef> optionalWsProjectRef) {
    if (!optionalWsProjectRef.isPresent()) {
      return Optional.absent();
    }

    WsProjectRef wsProjectRef = optionalWsProjectRef.get();
    return Optional.of(componentFinder.getRootComponentOrModuleByUuidOrKey(dbSession, wsProjectRef.uuid(), wsProjectRef.key(), resourceTypes));
  }

  public ComponentDto getRootComponentOrModule(DbSession dbSession, WsProjectRef projectRef) {
    return componentFinder.getRootComponentOrModuleByUuidOrKey(dbSession, projectRef.uuid(), projectRef.key(), resourceTypes);
  }

  public String getGroupName(DbSession dbSession, WsGroupRef groupRef) {
    GroupDto group = getGroup(dbSession, groupRef);

    return group == null ? ANYONE : group.getName();
  }

  /**
   *
   * @return null if it's the anyone group
   */
  @CheckForNull
  public GroupDto getGroup(DbSession dbSession, WsGroupRef group) {
    if (isAnyone(group.name())) {
      return null;
    }

    return userGroupFinder.getGroup(dbSession, group);
  }

  public UserDto getUser(DbSession dbSession, String userLogin) {
    return checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin),
      "User with login '%s' is not found'", userLogin);
  }

  public PermissionTemplateDto getTemplate(DbSession dbSession, WsTemplateRef template) {
    if (template.uuid() != null) {
      return checkFound(
        dbClient.permissionTemplateDao().selectByUuid(dbSession, template.uuid()),
        "Permission template with id '%s' is not found", template.uuid());
    } else {
      return checkFound(
        dbClient.permissionTemplateDao().selectByName(dbSession, template.name()),
        "Permission template with name '%s' is not found (case insensitive)", template.name());
    }
  }
}
