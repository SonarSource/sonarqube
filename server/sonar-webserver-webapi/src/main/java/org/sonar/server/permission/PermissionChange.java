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
package org.sonar.server.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GlobalPermission;

import static java.util.Objects.requireNonNull;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public abstract class PermissionChange {

  public enum Operation {
    ADD, REMOVE
  }

  private final Operation operation;
  private final String permission;
  private final ComponentDto project;
  protected final PermissionService permissionService;

  protected PermissionChange(Operation operation, String permission, @Nullable ComponentDto project, PermissionService permissionService) {
    this.operation = requireNonNull(operation);
    this.permission = requireNonNull(permission);
    this.project = project;
    this.permissionService = permissionService;
    if (project == null) {
      checkRequest(permissionService.getGlobalPermissions().stream().anyMatch(p -> p.getKey().equals(permission)),
        "Invalid global permission '%s'. Valid values are %s", permission,
        permissionService.getGlobalPermissions().stream().map(GlobalPermission::getKey).collect(toList()));
    } else {
      checkRequest(permissionService.getAllProjectPermissions().contains(permission), "Invalid project permission '%s'. Valid values are %s", permission,
        permissionService.getAllProjectPermissions());
    }
  }

  public Operation getOperation() {
    return operation;
  }

  public String getPermission() {
    return permission;
  }

  @CheckForNull
  public ComponentDto getProject() {
    return project;
  }

  @CheckForNull
  public String getProjectName() {
    return project == null ? null : project.name();
  }

  @CheckForNull
  public String getProjectUuid() {
    return project == null ? null : project.uuid();
  }
}
