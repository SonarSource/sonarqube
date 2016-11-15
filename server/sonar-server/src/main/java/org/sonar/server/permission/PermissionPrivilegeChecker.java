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
package org.sonar.server.permission;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.web.UserRole;
import org.sonar.server.user.UserSession;

import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;

public class PermissionPrivilegeChecker {
  private PermissionPrivilegeChecker() {
    // static methods only
  }

  public static void checkGlobalAdmin(UserSession userSession, String organizationUuid) {
    userSession
      .checkLoggedIn()
      .checkOrganizationPermission(organizationUuid, SYSTEM_ADMIN);
  }

  /**
   * @deprecated does not support organizations. Replaced by {@link #checkProjectAdmin(UserSession, String, Optional)}
   */
  @Deprecated
  public static void checkProjectAdminUserByComponentKey(UserSession userSession, @Nullable String componentKey) {
    userSession.checkLoggedIn();
    if (componentKey == null || !userSession.hasComponentPermission(UserRole.ADMIN, componentKey)) {
      userSession.checkPermission(SYSTEM_ADMIN);
    }
  }

  /**
   * Checks that user is administrator of the specified project, or of the specified organization if project is not
   * defined.
   * @throws org.sonar.server.exceptions.ForbiddenException if user is not administrator
   */
  public static void checkProjectAdmin(UserSession userSession, String organizationUuid, Optional<ProjectId> projectId) {
    userSession.checkLoggedIn();
    if (!projectId.isPresent() || !userSession.hasComponentUuidPermission(UserRole.ADMIN, projectId.get().getUuid())) {
      userSession.checkOrganizationPermission(organizationUuid, SYSTEM_ADMIN);
    }
  }

  /**
   * Checks that user is administrator of the specified project, or of system if project is not
   * defined.
   * @throws org.sonar.server.exceptions.ForbiddenException if user is not administrator
   * @deprecated does not support organizations. Replaced by {@link #checkProjectAdmin(UserSession, String, Optional)}
   */
  @Deprecated
  public static void checkProjectAdmin(UserSession userSession, Optional<ProjectId> projectId) {
    userSession.checkLoggedIn();
    if (!projectId.isPresent() || !userSession.hasComponentUuidPermission(UserRole.ADMIN, projectId.get().getUuid())) {
      userSession.checkPermission(SYSTEM_ADMIN);
    }
  }
}
