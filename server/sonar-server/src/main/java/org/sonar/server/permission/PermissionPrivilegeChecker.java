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
import org.sonar.api.web.UserRole;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.user.UserSession;

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class PermissionPrivilegeChecker {
  private PermissionPrivilegeChecker() {
    // static methods only
  }

  public static void checkGlobalAdmin(UserSession userSession, String organizationUuid) {
    userSession
      .checkLoggedIn()
      .checkPermission(OrganizationPermission.ADMINISTER, organizationUuid);
  }

  /**
   * Checks that user is administrator of the specified project, or of the specified organization if project is not
   * defined.
   * @throws org.sonar.server.exceptions.ForbiddenException if user is not administrator
   */
  public static void checkProjectAdmin(UserSession userSession, String organizationUuid, Optional<ProjectId> projectId) {
    userSession.checkLoggedIn();

    if (userSession.hasPermission(OrganizationPermission.ADMINISTER, organizationUuid)) {
      return;
    }

    if (projectId.isPresent()) {
      userSession.checkComponentUuidPermission(UserRole.ADMIN, projectId.get().getUuid());
    } else {
      throw insufficientPrivilegesException();
    }
  }
}
