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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;

public abstract class AbstractUserSession<T extends AbstractUserSession> implements UserSession {
  protected static final String INSUFFICIENT_PRIVILEGES_MESSAGE = "Insufficient privileges";
  private static final ForbiddenException INSUFFICIENT_PRIVILEGES_EXCEPTION = new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);

  protected UserDto userDto;
  protected Integer userId;
  protected String login;
  protected String name;

  protected Set<String> userGroups = Sets.newHashSet(DefaultGroups.ANYONE);
  protected List<String> globalPermissions = Collections.emptyList();
  protected HashMultimap<String, String> projectKeyByPermission = HashMultimap.create();
  protected HashMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  protected Map<String, String> projectUuidByComponentUuid = newHashMap();
  protected List<String> projectPermissionsCheckedByKey = newArrayList();
  protected List<String> projectPermissionsCheckedByUuid = newArrayList();

  protected Locale locale = Locale.ENGLISH;

  private final Class<T> clazz;

  protected AbstractUserSession(Class<T> clazz) {
    this.clazz = clazz;
  }


  @Override
  @CheckForNull
  public String getLogin() {
    return login;
  }

  public T setLogin(@Nullable String s) {
    this.login = Strings.emptyToNull(s);
    return clazz.cast(this);
  }

  @Override
  @CheckForNull
  public String getName() {
    return name;
  }

  public T setName(@Nullable String s) {
    this.name = Strings.emptyToNull(s);
    return clazz.cast(this);
  }

  @Override
  @CheckForNull
  public Integer getUserId() {
    return userId;
  }

  public T setUserId(@Nullable Integer userId) {
    this.userId = userId;
    return clazz.cast(this);
  }

  @Override
  public Set<String> getUserGroups() {
    return userGroups;
  }

  public T setUserGroups(@Nullable String... userGroups) {
    if (userGroups != null) {
      this.userGroups.addAll(Arrays.asList(userGroups));
    }
    return clazz.cast(this);
  }

  @Override
  public boolean isLoggedIn() {
    return login != null;
  }

  @Override
  public Locale locale() {
    return locale;
  }

  protected T setLocale(@Nullable Locale l) {
    this.locale = MoreObjects.firstNonNull(l, Locale.ENGLISH);
    return clazz.cast(this);
  }

  @Override
  public UserSession checkLoggedIn() {
    if (login == null) {
      throw new UnauthorizedException("Authentication is required");
    }
    return this;
  }

  @Override
  public UserSession checkPermission(String globalPermission) {
    if (!hasPermission(globalPermission)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public UserSession checkGlobalPermission(String globalPermission) {
    return checkPermission(globalPermission);
  }

  @Override
  public UserSession checkAnyPermissions(Collection<String> globalPermissionsToTest) {
    List<String> userGlobalPermissions = globalPermissions();
    for (String userGlobalPermission : userGlobalPermissions) {
      if (globalPermissionsToTest.contains(userGlobalPermission)) {
        return this;
      }
    }

    throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
  }

  @Override
  public boolean hasPermission(String globalPermission) {
    return globalPermissions().contains(globalPermission);
  }

  @Override
  public boolean hasGlobalPermission(String globalPermission) {
    return hasPermission(globalPermission);
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, String componentKey) {
    if (!hasComponentPermission(projectPermission, componentKey)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    if (!hasComponentUuidPermission(permission, componentUuid)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  public static ForbiddenException insufficientPrivilegesException() {
    return INSUFFICIENT_PRIVILEGES_EXCEPTION;
  }
}
