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
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionChange;

public class PermissionWsCommons {

  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public PermissionWsCommons(DbClient dbClient, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  String searchGroupName(DbSession dbSession, @Nullable String groupNameParam, @Nullable Long groupId) {
    if (groupNameParam != null) {
      return groupNameParam;
    }

    GroupDto group = dbClient.groupDao().selectById(dbSession, groupId);
    if (group == null) {
      throw new NotFoundException(String.format("Group with id '%d' is not found", groupId));
    }

    return group.getName();
  }

  PermissionChange buildUserPermissionChange(DbSession dbSession, PermissionRequest request) {
    PermissionChange permissionChange = new PermissionChange()
      .setPermission(request.permission())
      .setUserLogin(request.userLogin());
    addProjectToPermissionChange(dbSession, permissionChange, request);

    return permissionChange;
  }

  PermissionChange buildGroupPermissionChange(DbSession dbSession, PermissionRequest request) {
    String groupName = searchGroupName(dbSession, request.groupName(), request.groupId());

    PermissionChange permissionChange = new PermissionChange()
      .setPermission(request.permission())
      .setGroupName(groupName);
    addProjectToPermissionChange(dbSession, permissionChange, request);

    return permissionChange;
  }

  private void addProjectToPermissionChange(DbSession dbSession, PermissionChange permissionChange, PermissionRequest request) {
    if (request.hasProject()) {
      ComponentDto project = componentFinder.getProjectByUuidOrKey(dbSession, request.projectUuid(), request.projectKey());
      permissionChange.setComponentKey(project.key());
    }
  }

  Optional<ComponentDto> searchProject(PermissionRequest request) {
    if (!request.hasProject()) {
      return Optional.absent();
    }

    DbSession dbSession = dbClient.openSession(false);
    try {
      return Optional.of(componentFinder.getProjectByUuidOrKey(dbSession, request.projectUuid(), request.projectKey()));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
