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

import javax.annotation.Nullable;
import org.sonar.api.config.Configuration;
import org.sonar.api.web.UserRole;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.user.UserSession;

import java.util.Objects;

import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE;
import static org.sonar.api.CoreProperties.CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY;
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
   * Checks that user is administrator of the specified project
   * @throws org.sonar.server.exceptions.ForbiddenException if user is not administrator
   */
  public static void checkProjectAdmin(UserSession userSession, Configuration config, @Nullable EntityDto entity, String organizationUuid) {
    userSession.checkLoggedIn();
    Objects.requireNonNull(organizationUuid);

    if (userSession.hasPermission(OrganizationPermission.ADMINISTER, organizationUuid)) {
      return;
    }

    boolean allowChangingPermissionsByProjectAdmins = config.getBoolean(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_PROPERTY)
      .orElse(CORE_ALLOW_PERMISSION_MANAGEMENT_FOR_PROJECT_ADMINS_DEFAULT_VALUE);
    if (entity != null && allowChangingPermissionsByProjectAdmins) {
      userSession.checkEntityPermission(UserRole.ADMIN, entity);
    } else {
      throw insufficientPrivilegesException();
    }
  }
}
