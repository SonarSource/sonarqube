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

import java.util.List;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.ResourceTypes;
import org.sonar.api.web.UserRole;
import org.sonar.db.permission.GlobalPermission;

@Immutable
public class PermissionServiceImpl implements PermissionService {
  private static final List<String> ALL_PROJECT_PERMISSIONS = List.of(
    UserRole.ADMIN,
    UserRole.CODEVIEWER,
    UserRole.ISSUE_ADMIN,
    UserRole.SECURITYHOTSPOT_ADMIN,
    UserRole.SCAN,
    UserRole.USER
  );

  private static final List<GlobalPermission> ALL_GLOBAL_PERMISSIONS = List.of(GlobalPermission.values());

  private final List<GlobalPermission> globalPermissions;
  private final List<String> projectPermissions;

  public PermissionServiceImpl(ResourceTypes resourceTypes) {
    globalPermissions = List.copyOf(ALL_GLOBAL_PERMISSIONS.stream()
      .filter(s -> !s.equals(GlobalPermission.APPLICATION_CREATOR) || resourceTypes.isQualifierPresent(Qualifiers.APP))
      .filter(s -> !s.equals(GlobalPermission.PORTFOLIO_CREATOR) || resourceTypes.isQualifierPresent(Qualifiers.VIEW))
      .toList());
    projectPermissions = List.copyOf(ALL_PROJECT_PERMISSIONS.stream()
      .filter(s -> !s.equals(GlobalPermission.APPLICATION_CREATOR.getKey()) || resourceTypes.isQualifierPresent(Qualifiers.APP))
      .filter(s -> !s.equals(GlobalPermission.PORTFOLIO_CREATOR.getKey()) || resourceTypes.isQualifierPresent(Qualifiers.VIEW))
      .toList());
  }

  /**
   * Return an immutable Set of all permissions
   */
  @Override
  public List<GlobalPermission> getGlobalPermissions() {
    return globalPermissions;
  }

  /**
   * Return an immutable Set of all project permissions
   */
  @Override
  public List<String> getAllProjectPermissions() {
    return projectPermissions;
  }
}
