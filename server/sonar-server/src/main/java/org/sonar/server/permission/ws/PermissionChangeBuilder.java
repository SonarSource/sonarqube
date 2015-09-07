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
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.permission.PermissionChange;

public class PermissionChangeBuilder {

  private final PermissionDependenciesFinder dependenciesFinder;

  public PermissionChangeBuilder(PermissionDependenciesFinder dependenciesFinder) {
    this.dependenciesFinder = dependenciesFinder;
  }

  public PermissionChange buildUserPermissionChange(DbSession dbSession, PermissionRequest request) {
    PermissionChange permissionChange = new PermissionChange()
      .setPermission(request.permission())
      .setUserLogin(request.userLogin());
    addProjectToPermissionChange(dbSession, permissionChange, request);

    return permissionChange;
  }

  public PermissionChange buildGroupPermissionChange(DbSession dbSession, PermissionRequest request) {
    String groupName = dependenciesFinder.getGroupName(dbSession, request);

    PermissionChange permissionChange = new PermissionChange()
      .setPermission(request.permission())
      .setGroupName(groupName);
    addProjectToPermissionChange(dbSession, permissionChange, request);

    return permissionChange;
  }

  private void addProjectToPermissionChange(DbSession dbSession, PermissionChange permissionChange, PermissionRequest request) {
    Optional<ComponentDto> project = dependenciesFinder.searchProject(dbSession, request);
    if (project.isPresent()) {
      permissionChange.setComponentKey(project.get().key());
    }
  }
}
