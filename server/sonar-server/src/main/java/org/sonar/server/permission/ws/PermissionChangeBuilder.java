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
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.usergroups.ws.WsGroupRef;

import static org.sonar.server.permission.ws.PermissionRequestValidator.validatePermission;

public class PermissionChangeBuilder {

  private final PermissionDependenciesFinder dependenciesFinder;

  public PermissionChangeBuilder(PermissionDependenciesFinder dependenciesFinder) {
    this.dependenciesFinder = dependenciesFinder;
  }

  public PermissionChange buildUserPermissionChange(DbSession dbSession, String permission, Optional<WsProjectRef> projectRef, String login) {
    PermissionChange permissionChange = new PermissionChange()
      .setPermission(permission)
      .setUserLogin(login);
    addProjectToPermissionChange(dbSession, permissionChange, projectRef);

    return permissionChange;
  }

  public PermissionChange buildGroupPermissionChange(DbSession dbSession, String permission, Optional<WsProjectRef> projectRef, WsGroupRef groupRef) {
    validatePermission(permission, projectRef);
    String groupName = dependenciesFinder.getGroupName(dbSession, groupRef);

    PermissionChange permissionChange = new PermissionChange()
      .setPermission(permission)
      .setGroupName(groupName);
    addProjectToPermissionChange(dbSession, permissionChange, projectRef);

    return permissionChange;
  }

  private void addProjectToPermissionChange(DbSession dbSession, PermissionChange permissionChange, Optional<WsProjectRef> projectRef) {
    Optional<ComponentDto> project = dependenciesFinder.searchProject(dbSession, projectRef);
    if (project.isPresent()) {
      permissionChange.setComponentKey(project.get().key());
    }
  }
}
