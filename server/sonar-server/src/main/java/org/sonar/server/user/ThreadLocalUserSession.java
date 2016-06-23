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
import org.sonar.server.exceptions.UnauthorizedException;

/**
 * Part of the current HTTP session
 */
public class ThreadLocalUserSession implements UserSession {

  private static final ThreadLocal<UserSession> THREAD_LOCAL = new ThreadLocal<>();

  public UserSession get() {
    UserSession currentUserSession = THREAD_LOCAL.get();
    if (currentUserSession != null) {
      return currentUserSession;
    }
    throw new UnauthorizedException();
  }

  public void set(UserSession session) {
    THREAD_LOCAL.set(session);
  }

  public void remove() {
    THREAD_LOCAL.remove();
  }

  public boolean hasSession() {
    return THREAD_LOCAL.get() != null;
  }

  @Override
  @CheckForNull
  public String getLogin() {
    return get().getLogin();
  }

  @Override
  @CheckForNull
  public String getName() {
    return get().getName();
  }

  @Override
  @CheckForNull
  public Integer getUserId() {
    return get().getUserId();
  }

  @Override
  public Set<String> getUserGroups() {
    return get().getUserGroups();
  }

  @Override
  public boolean isLoggedIn() {
    return get().isLoggedIn();
  }

  @Override
  public Locale locale() {
    return get().locale();
  }

  @Override
  public UserSession checkLoggedIn() {
    return get().checkLoggedIn();
  }

  @Override
  public UserSession checkPermission(String globalPermission) {
    return get().checkPermission(globalPermission);
  }

  @Override
  public UserSession checkGlobalPermission(String globalPermission) {
    return get().checkGlobalPermission(globalPermission);
  }

  @Override
  public UserSession checkAnyPermissions(Collection<String> globalPermissions) {
    return get().checkAnyPermissions(globalPermissions);
  }

  @Override
  public boolean hasPermission(String globalPermission) {
    return get().hasPermission(globalPermission);
  }

  @Override
  public boolean hasGlobalPermission(String globalPermission) {
    return get().hasGlobalPermission(globalPermission);
  }

  @Override
  public List<String> globalPermissions() {
    return get().globalPermissions();
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, String componentKey) {
    return get().checkComponentPermission(projectPermission, componentKey);
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    return get().checkComponentUuidPermission(permission, componentUuid);
  }

  @Override
  public boolean hasComponentPermission(String permission, String componentKey) {
    return get().hasComponentPermission(permission, componentKey);
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    return get().hasComponentUuidPermission(permission, componentUuid);
  }
}
