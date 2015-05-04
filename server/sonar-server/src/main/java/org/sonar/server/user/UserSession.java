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

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.platform.Platform;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Part of the current HTTP session
 */
public class UserSession {

  public static final UserSession ANONYMOUS = new UserSession();

  private static final ThreadLocal<UserSession> THREAD_LOCAL = new ThreadLocal<UserSession>();
  private static final Logger LOG = Loggers.get(UserSession.class);
  private static final String INSUFFICIENT_PRIVILEGES_MESSAGE = "Insufficient privileges";

  private Integer userId;
  private String login;
  private String name;
  private Set<String> userGroups;
  private Locale locale = Locale.ENGLISH;
  List<String> globalPermissions = null;

  HashMultimap<String, String> projectKeyByPermission = HashMultimap.create();
  HashMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  Map<String, String> projectKeyByComponentKey = newHashMap();
  Map<String, String> projectUuidByComponentUuid = newHashMap();
  List<String> projectPermissions = newArrayList();

  UserSession() {
    // Do not forget that when forceAuthentication is set to true, the Anyone group should not be set (but this will be check when
    // authentication will be done in Java)
    userGroups = newHashSet(DefaultGroups.ANYONE);
  }

  @CheckForNull
  public String login() {
    return login;
  }

  UserSession setLogin(@Nullable String s) {
    this.login = Strings.emptyToNull(s);
    return this;
  }

  @CheckForNull
  public String name() {
    return name;
  }

  UserSession setName(@Nullable String s) {
    this.name = Strings.emptyToNull(s);
    return this;
  }

  @CheckForNull
  public Integer userId() {
    return userId;
  }

  UserSession setUserId(@Nullable Integer userId) {
    this.userId = userId;
    return this;
  }

  public Set<String> userGroups() {
    return userGroups;
  }

  UserSession setUserGroups(@Nullable String... userGroups) {
    if (userGroups != null) {
      this.userGroups.addAll(Arrays.asList(userGroups));
    }
    return this;
  }

  public boolean isLoggedIn() {
    return login != null;
  }

  public Locale locale() {
    return locale;
  }

  UserSession setLocale(@Nullable Locale l) {
    this.locale = Objects.firstNonNull(l, Locale.ENGLISH);
    return this;
  }

  public UserSession checkLoggedIn() {
    if (login == null) {
      throw new UnauthorizedException("Authentication is required");
    }
    return this;
  }

  /**
   * Ensures that user implies the specified global permission. If not a {@link org.sonar.server.exceptions.ForbiddenException} is thrown.
   */
  public UserSession checkGlobalPermission(String globalPermission) {
    return checkGlobalPermission(globalPermission, null);
  }

  public UserSession checkGlobalPermission(String globalPermission, @Nullable String errorMessage) {
    if (!hasGlobalPermission(globalPermission)) {
      throw new ForbiddenException(errorMessage != null ? errorMessage : INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  /**
   * Does the user have the given permission ?
   */
  public boolean hasGlobalPermission(String globalPermission) {
    return globalPermissions().contains(globalPermission);
  }

  public List<String> globalPermissions() {
    if (globalPermissions == null) {
      List<String> permissionKeys = authorizationDao().selectGlobalPermissions(login);
      globalPermissions = new ArrayList<String>();
      for (String permissionKey : permissionKeys) {
        if (!GlobalPermissions.ALL.contains(permissionKey)) {
          LOG.warn("Ignoring unknown permission {} for user {}", permissionKey, login);
        } else {
          globalPermissions.add(permissionKey);
        }
      }
    }
    return globalPermissions;
  }

  /**
   * Ensures that user implies the specified project permission. If not a {@link org.sonar.server.exceptions.ForbiddenException} is thrown.
   */
  public UserSession checkProjectPermission(String projectPermission, String projectKey) {
    if (!hasProjectPermission(projectPermission, projectKey)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  /**
   * Ensures that user implies the specified project permission. If not a {@link org.sonar.server.exceptions.ForbiddenException} is thrown.
   */
  public UserSession checkProjectUuidPermission(String projectPermission, String projectUuid) {
    if (!hasProjectPermissionByUuid(projectPermission, projectUuid)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  /**
   * Does the user have the given project permission ?
   */
  public boolean hasProjectPermission(String permission, String projectKey) {
    if (!projectPermissions.contains(permission)) {
      Collection<String> projectKeys = authorizationDao().selectAuthorizedRootProjectsKeys(userId, permission);
      for (String key : projectKeys) {
        projectKeyByPermission.put(permission, key);
      }
      projectPermissions.add(permission);
    }
    return projectKeyByPermission.get(permission).contains(projectKey);
  }

  /**
   * Does the user have the given project permission ?
   */
  public boolean hasProjectPermissionByUuid(String permission, String projectUuid) {
    if (!projectPermissions.contains(permission)) {
      Collection<String> projectUuids = authorizationDao().selectAuthorizedRootProjectsUuids(userId, permission);
      for (String key : projectUuids) {
        projectUuidByPermission.put(permission, key);
      }
      projectPermissions.add(permission);
    }
    return projectUuidByPermission.get(permission).contains(projectUuid);
  }

  /**
   * Ensures that user implies the specified project permission on a component. If not a {@link org.sonar.server.exceptions.ForbiddenException} is thrown.
   */
  public UserSession checkComponentPermission(String projectPermission, String componentKey) {
    if (!hasComponentPermission(projectPermission, componentKey)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  /**
   * Ensures that user implies the specified component permission on a component. If not a {@link org.sonar.server.exceptions.ForbiddenException} is thrown.
   */
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    if (!hasComponentUuidPermission(permission, componentUuid)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  /**
   * Does the user have the given project permission for a component key ?
   */
  public boolean hasComponentPermission(String permission, String componentKey) {
    String projectKey = projectKeyByComponentKey.get(componentKey);
    if (projectKey == null) {
      ResourceDto project = resourceDao().getRootProjectByComponentKey(componentKey);
      if (project == null) {
        return false;
      }
      projectKey = project.getKey();
    }
    boolean hasComponentPermission = hasProjectPermission(permission, projectKey);
    if (hasComponentPermission) {
      projectKeyByComponentKey.put(componentKey, projectKey);
      return true;
    }
    return false;
  }

  /**
   * Does the user have the given project permission for a component uuid ?
   */
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    String projectUuid = projectUuidByComponentUuid.get(componentUuid);
    if (projectUuid == null) {
      ResourceDto project = resourceDao().getResource(componentUuid);
      if (project == null) {
        return false;
      }
      projectUuid = project.getProjectUuid();
    }
    boolean hasComponentPermission = hasProjectPermissionByUuid(permission, projectUuid);
    if (hasComponentPermission) {
      projectUuidByComponentUuid.put(componentUuid, projectUuid);
      return true;
    }
    return false;
  }

  AuthorizationDao authorizationDao() {
    return Platform.component(AuthorizationDao.class);
  }

  ResourceDao resourceDao() {
    return Platform.component(ResourceDao.class);
  }

  public static UserSession get() {
    return Objects.firstNonNull(THREAD_LOCAL.get(), ANONYMOUS);
  }

  static void set(UserSession session) {
    THREAD_LOCAL.set(session);
  }

  static void remove() {
    THREAD_LOCAL.remove();
  }

  static boolean hasSession() {
    return THREAD_LOCAL.get() != null;
  }
}
