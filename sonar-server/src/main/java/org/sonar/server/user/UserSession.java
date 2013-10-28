/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.platform.Platform;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Part of the current HTTP session
 */
public class UserSession {

  private static final ThreadLocal<UserSession> THREAD_LOCAL = new ThreadLocal<UserSession>();
  public static final UserSession ANONYMOUS = new UserSession();
  private static final Logger LOG = LoggerFactory.getLogger(UserSession.class);

  private Integer userId;
  private String login;
  private Locale locale = Locale.ENGLISH;
  List<String> globalPermissions = null;

  HashMultimap<String, Long> projectIdByPermission = HashMultimap.create();
  List<String> projectPermissions = newArrayList();

  UserSession() {
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
  public Integer userId() {
    return userId;
  }

  UserSession setUserId(@Nullable Integer userId) {
    this.userId = userId;
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
    if (!hasGlobalPermission(globalPermission)) {
      throw new ForbiddenException("Insufficient privileges");
    }
    return this;
  }

  /**
   * Does the user have the given permission ?
   */
  public boolean hasGlobalPermission(String globalPermission) {
    return globalPermissions().contains(globalPermission);
  }

  List<String> globalPermissions() {
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
  public UserSession checkProjectPermission(String projectPermission, Long componentId) {
    if (!hasProjectPermission(projectPermission, componentId)) {
      throw new ForbiddenException("Insufficient privileges");
    }
    return this;
  }

  /**
   * Does the user have the given project permission ?
   */
  public boolean hasProjectPermission(String permission, Long componentId) {
    if (!projectPermissions.contains(permission)) {
      Collection<Long> projectIds = authorizationDao().selectAuthorizedRootProjectsIds(userId, permission);
      for (Long id : projectIds) {
        projectIdByPermission.put(permission, id);
      }
      projectPermissions.add(permission);
    }
    return projectIdByPermission.get(permission).contains(componentId);
  }

  AuthorizationDao authorizationDao() {
    return Platform.component(AuthorizationDao.class);
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
