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
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public abstract class AbstractUserSession<T extends AbstractUserSession> implements UserSession {
  protected static final String INSUFFICIENT_PRIVILEGES_MESSAGE = "Insufficient privileges";

  protected Integer userId;
  protected String login;
  protected Set<String> userGroups = Sets.newHashSet(DefaultGroups.ANYONE);
  protected List<String> globalPermissions = Collections.emptyList();
  protected HashMultimap<String, String> projectKeyByPermission = HashMultimap.create();
  protected HashMultimap<String, String> projectUuidByPermission = HashMultimap.create();
  protected Map<String, String> projectUuidByComponentUuid = newHashMap();
  protected List<String> projectPermissionsCheckedByKey = newArrayList();
  protected List<String> projectPermissionsCheckedByUuid = newArrayList();
  protected String name;
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

  protected T setLogin(@Nullable String s) {
    this.login = Strings.emptyToNull(s);
    return clazz.cast(this);
  }

  @Override
  @CheckForNull
  public String getName() {
    return name;
  }

  protected T setName(@Nullable String s) {
    this.name = Strings.emptyToNull(s);
    return clazz.cast(this);
  }

  @Override
  @CheckForNull
  public Integer getUserId() {
    return userId;
  }

  protected T setUserId(@Nullable Integer userId) {
    this.userId = userId;
    return clazz.cast(this);
  }

  @Override
  public Set<String> getUserGroups() {
    return userGroups;
  }

  protected T setUserGroups(@Nullable String... userGroups) {
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
    this.locale = Objects.firstNonNull(l, Locale.ENGLISH);
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
  public UserSession checkGlobalPermission(String globalPermission) {
    return checkGlobalPermission(globalPermission, null);
  }

  @Override
  public UserSession checkGlobalPermission(String globalPermission, @Nullable String errorMessage) {
    if (!hasGlobalPermission(globalPermission)) {
      throw new ForbiddenException(errorMessage != null ? errorMessage : INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public boolean hasGlobalPermission(String globalPermission) {
    return globalPermissions().contains(globalPermission);
  }

  @Override
  public UserSession checkProjectPermission(String projectPermission, String projectKey) {
    if (!hasProjectPermission(projectPermission, projectKey)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public UserSession checkProjectUuidPermission(String projectPermission, String projectUuid) {
    if (!hasProjectPermissionByUuid(projectPermission, projectUuid)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
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
}
