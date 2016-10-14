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
import java.util.Locale;
import java.util.Set;
import javax.annotation.CheckForNull;

public interface UserSession {
  @CheckForNull
  String getLogin();

  @CheckForNull
  String getName();

  @CheckForNull
  Integer getUserId();

  /**
   * @deprecated does not support organizations as group names are not unique
   */
  @Deprecated
  Set<String> getUserGroups();

  boolean isLoggedIn();

  /**
   * @deprecated Always returning {@link Locale#ENGLISH}
   */
  @Deprecated
  Locale locale();

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
   * Ensures that permission is granted to user, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkPermission(String globalPermission);

  /**
   * @deprecated Only used by Views plugin.
   */
  @Deprecated
  UserSession checkGlobalPermission(String globalPermission);

  /**
   * Ensures that at least one of the global permissions is granted to user. If none are granted,
   * then throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkAnyPermissions(Collection<String> globalPermissions);

  /**
   * Does the user have the given permission ?
   */
  boolean hasPermission(String globalPermission);

  /**
   * Returns {@code true} if the permission is granted on the organization, else {@code false}.
   * Root status is not verified, so the method may return {@code false} even for root users.
   */
  boolean hasOrganizationPermission(String organizationUuid, String permission);

  /**
   * Ensures that the permission is granted to user for the specified organization,
   * otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkOrganizationPermission(String organizationUuid, String permission);

  /**
   * @deprecated Only used by Views and the Developer Cockpit plugins.
   */
  @Deprecated
  boolean hasGlobalPermission(String globalPermission);

  List<String> globalPermissions();

  /**
   * Ensures that permission is granted to user on the specified component, otherwise throws
   * a {@link org.sonar.server.exceptions.ForbiddenException}.
   * If the component doesn't exist and the user doesn't have the global permission,
   * throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkComponentPermission(String projectPermission, String componentKey);

  /**
   * Ensures that permission is granted to user, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   * If the component doesn't exist and the user doesn't have the permission, throws
   * a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkComponentUuidPermission(String permission, String componentUuid);

  /**
   * Does the user have the given permission for a component key ?
   *
   * First, check if the user has the global permission (even if the component doesn't exist)
   * If not, check is the user has the permission on the project of the component
   * If the component doesn't exist, return false
   */
  boolean hasComponentPermission(String permission, String componentKey);

  /**
   * Does the user have the given project permission for a component uuid ?

   * First, check if the user has the global permission (even if the component doesn't exist)
   * If not, check is the user has the permission on the project of the component
   * If the component doesn't exist, return false
   */
  boolean hasComponentUuidPermission(String permission, String componentUuid);
}
