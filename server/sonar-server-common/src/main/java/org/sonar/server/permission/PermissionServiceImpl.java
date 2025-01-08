/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.server.component.ComponentTypes;
import org.sonar.api.web.UserRole;
import org.sonar.db.permission.GlobalPermission;

@Immutable
public class PermissionServiceImpl implements PermissionService {
  public static final Set<String> ALL_PROJECT_PERMISSIONS = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
    UserRole.ADMIN,
    UserRole.CODEVIEWER,
    UserRole.ISSUE_ADMIN,
    UserRole.SECURITYHOTSPOT_ADMIN,
    UserRole.SCAN,
    UserRole.USER
  )));

  private static final List<GlobalPermission> ALL_GLOBAL_PERMISSIONS = List.of(GlobalPermission.values());

  private final List<GlobalPermission> globalPermissions;
  private final List<String> projectPermissions;

  public PermissionServiceImpl(ComponentTypes componentTypes) {
    globalPermissions = List.copyOf(ALL_GLOBAL_PERMISSIONS.stream()
      .filter(s -> !s.equals(GlobalPermission.APPLICATION_CREATOR) || componentTypes.isQualifierPresent(ComponentQualifiers.APP))
      .filter(s -> !s.equals(GlobalPermission.PORTFOLIO_CREATOR) || componentTypes.isQualifierPresent(ComponentQualifiers.VIEW))
      .toList());
    projectPermissions = List.copyOf(ALL_PROJECT_PERMISSIONS.stream()
      .filter(s -> !s.equals(GlobalPermission.APPLICATION_CREATOR.getKey()) || componentTypes.isQualifierPresent(ComponentQualifiers.APP))
      .filter(s -> !s.equals(GlobalPermission.PORTFOLIO_CREATOR.getKey()) || componentTypes.isQualifierPresent(ComponentQualifiers.VIEW))
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
