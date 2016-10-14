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

  private static final ThreadLocal<UserSession> DELEGATE = new ThreadLocal<>();

  public UserSession get() {
    UserSession session = DELEGATE.get();
    if (session != null) {
      return session;
    }
    throw new UnauthorizedException();
  }

  public void set(UserSession session) {
    DELEGATE.set(session);
  }

  public void unload() {
    DELEGATE.remove();
  }

  public boolean hasSession() {
    return DELEGATE.get() != null;
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
  public boolean isRoot() {
    return get().isRoot();
  }

  @Override
  public UserSession checkIsRoot() {
    get().checkIsRoot();
    return this;
  }

  @Override
  public UserSession checkLoggedIn() {
    get().checkLoggedIn();
    return this;
  }

  @Override
  public UserSession checkPermission(String globalPermission) {
    get().checkPermission(globalPermission);
    return this;
  }

  @Override
  public UserSession checkGlobalPermission(String globalPermission) {
    get().checkGlobalPermission(globalPermission);
    return this;
  }

  @Override
  public UserSession checkAnyPermissions(Collection<String> globalPermissions) {
    get().checkAnyPermissions(globalPermissions);
    return this;
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
    get().checkComponentPermission(projectPermission, componentKey);
    return this;
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    get().checkComponentUuidPermission(permission, componentUuid);
    return this;
  }

  @Override
  public boolean hasComponentPermission(String permission, String componentKey) {
    return get().hasComponentPermission(permission, componentKey);
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    return get().hasComponentUuidPermission(permission, componentUuid);
  }

  @Override
  public UserSession checkOrganizationPermission(String organizationUuid, String permission) {
    get().checkOrganizationPermission(organizationUuid, permission);
    return this;
  }

  @Override
  public boolean hasOrganizationPermission(String organizationUuid, String permission) {
    return get().hasOrganizationPermission(organizationUuid, permission);
  }
}
