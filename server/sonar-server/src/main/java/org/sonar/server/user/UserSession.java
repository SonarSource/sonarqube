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
package org.sonar.server.user;

import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;

public interface UserSession {
  @CheckForNull
  String getLogin();

  @CheckForNull
  String getName();

  @CheckForNull
  Integer getUserId();

  /**
   * The groups that the logged-in user is member of. An empty
   * collection is returned if user is anonymous.
   */
  Collection<GroupDto> getGroups();

  /**
   * Whether the user is logged-in or anonymous.
   */
  boolean isLoggedIn();

  boolean isRoot();

  /**
   * Ensures that user is root in otherwise throws {@link org.sonar.server.exceptions.UnauthorizedException}.
   */
  UserSession checkIsRoot();

  /**
   * Ensures that user is logged in otherwise throws {@link org.sonar.server.exceptions.UnauthorizedException}.
   */
  UserSession checkLoggedIn();

  /**
   * Does the user have the given permission ?
  
   * @deprecated in 6.3 because if doesn't support organizations
   * @see org.sonar.core.permission.GlobalPermissions
   * @see #isRoot()
   * @see #hasOrganizationPermission(String, String)
   */
  @Deprecated
  boolean hasPermission(String globalPermission);

  /**
   * Returns {@code true} if the permission is granted on the organization, else {@code false}.
   * Root status is not verified, so the method may return {@code false} even for root users.
   *
   * @see org.sonar.core.permission.GlobalPermissions
   */
  boolean hasOrganizationPermission(String organizationUuid, String permission);

  /**
   * Ensures that the permission is granted to user for the specified organization,
   * otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkOrganizationPermission(String organizationUuid, String permission);

  /**
   * @deprecated in 6.3 because of organizations. No replacement yet.
   */
  @Deprecated
  List<String> globalPermissions();

  /**
   * Ensures that permission is granted to user, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   * If the component doesn't exist and the user doesn't have the permission, throws
   * a {@link org.sonar.server.exceptions.ForbiddenException}.
   *
   * @see org.sonar.api.web.UserRole for list of project permissions
   */
  UserSession checkComponentPermission(String projectPermission, ComponentDto component);

  /**
   * Ensures that permission is granted to user, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   * If the component doesn't exist and the user doesn't have the permission, throws
   * a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkComponentUuidPermission(String permission, String componentUuid);

  /**
   * Whether the user has the permission on the component. Returns {@code false}
   * if the component does not exist in database.
   */
  boolean hasComponentPermission(String permission, ComponentDto component);

  /**
   * Does the user have the given project permission for a component uuid ?
  
   * First, check if the user has the global permission (even if the component doesn't exist)
   * If not, check is the user has the permission on the project of the component
   * If the component doesn't exist, return false
   */
  boolean hasComponentUuidPermission(String permission, String componentUuid);
}
