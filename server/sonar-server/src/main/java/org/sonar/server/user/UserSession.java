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
package org.sonar.server.user;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public interface UserSession {
  @CheckForNull
  String getLogin();

  @CheckForNull
  String getName();

  @CheckForNull
  Integer getUserId();

  Set<String> getUserGroups();

  boolean isLoggedIn();

  Locale locale();

  /**
   * Ensures that user is logged in otherwise throws {@link org.sonar.server.exceptions.UnauthorizedException}.
   */
  UserSession checkLoggedIn();

  /**
   * Ensures that user implies the specified global permission, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkGlobalPermission(String globalPermission);

  /**
   * Ensures that user implies the specified global permission, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException} with
   * the specified error message.
   */
  UserSession checkGlobalPermission(String globalPermission, @Nullable String errorMessage);

  /**
   * Does the user have the given permission ?
   */
  boolean hasGlobalPermission(String globalPermission);

  List<String> globalPermissions();

  /**
   * Ensures that user implies the specified project permission, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkProjectPermission(String projectPermission, String projectKey);

  /**
   * Ensures that user implies the specified project permission, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkProjectUuidPermission(String projectPermission, String projectUuid);

  /**
   * Does the user have the given project permission ?
   */
  boolean hasProjectPermission(String permission, String projectKey);

  /**
   * Does the user have the given project permission ?
   */
  boolean hasProjectPermissionByUuid(String permission, String projectUuid);

  /**
   * Ensures that user implies the specified project permission on a component, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkComponentPermission(String projectPermission, String componentKey);

  /**
   * Ensures that user implies the specified component permission on a component, otherwise throws a {@link org.sonar.server.exceptions.ForbiddenException}.
   */
  UserSession checkComponentUuidPermission(String permission, String componentUuid);

  /**
   * Does the user have the given project permission for a component key ?
   */
  boolean hasComponentPermission(String permission, String componentKey);

  /**
   * Does the user have the given project permission for a component uuid ?
   */
  boolean hasComponentUuidPermission(String permission, String componentUuid);
}
