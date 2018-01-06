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
package org.sonar.server.permission;

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.ProjectPermissions;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.ws.WsUtils.checkRequest;

public abstract class PermissionChange {

  public enum Operation {
    ADD, REMOVE
  }

  private final Operation operation;
  private final String organizationUuid;
  private final String permission;
  private final ProjectId projectId;

  public PermissionChange(Operation operation, String organizationUuid, String permission, @Nullable ProjectId projectId) {
    this.operation = requireNonNull(operation);
    this.organizationUuid = requireNonNull(organizationUuid);
    this.permission = requireNonNull(permission);
    this.projectId = projectId;
    if (projectId == null) {
      checkRequest(GlobalPermissions.ALL.contains(permission), "Invalid global permission '%s'. Valid values are %s", permission, GlobalPermissions.ALL);
    } else {
      checkRequest(ProjectPermissions.ALL.contains(permission), "Invalid project permission '%s'. Valid values are %s", permission, ProjectPermissions.ALL);
    }
  }

  public Operation getOperation() {
    return operation;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public String getPermission() {
    return permission;
  }

  public Optional<ProjectId> getProjectId() {
    return Optional.ofNullable(projectId);
  }

  /**
   * Shortcut based on {@link #getProjectId()}
   */
  @CheckForNull
  public String getProjectUuid() {
    return projectId == null ? null : projectId.getUuid();
  }

  /**
   * Shortcut based on {@link #getProjectId()}
   */
  @CheckForNull
  public Long getNullableProjectId() {
    return projectId == null ? null : projectId.getId();
  }
}
