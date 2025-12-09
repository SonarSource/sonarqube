/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.GroupDto;
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
    throw new UnauthorizedException("User is not authenticated");
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
  public Long getLastSonarlintConnectionDate() {
    return get().getLastSonarlintConnectionDate();
  }

  @Override
  @CheckForNull
  public String getLogin() {
    return get().getLogin();
  }

  @Override
  @CheckForNull
  public String getUuid() {
    return get().getUuid();
  }

  @Override
  @CheckForNull
  public String getName() {
    return get().getName();
  }

  @Override
  public Collection<GroupDto> getGroups() {
    return get().getGroups();
  }

  @Override
  public Optional<IdentityProvider> getIdentityProvider() {
    return get().getIdentityProvider();
  }

  @Override
  public Optional<ExternalIdentity> getExternalIdentity() {
    return get().getExternalIdentity();
  }

  @Override
  public boolean isLoggedIn() {
    return get().isLoggedIn();
  }

  @Override
  public UserSession checkLoggedIn() {
    get().checkLoggedIn();
    return this;
  }

  @Override
  public boolean shouldResetPassword() {
    return get().shouldResetPassword();
  }

  @Override
  public boolean hasPermission(GlobalPermission permission) {
    return get().hasPermission(permission);
  }

  @Override
  public UserSession checkPermission(GlobalPermission permission) {
    get().checkPermission(permission);
    return this;
  }

  @Override
  public UserSession checkComponentPermission(ProjectPermission projectPermission, ComponentDto component) {
    get().checkComponentPermission(projectPermission, component);
    return this;
  }

  @Override
  public UserSession checkEntityPermission(ProjectPermission projectPermission, EntityDto entity) {
    get().checkEntityPermission(projectPermission, entity);
    return this;
  }

  @Override
  public UserSession checkEntityPermissionOrElseThrowResourceForbiddenException(ProjectPermission projectPermission, EntityDto entity) {
    get().checkEntityPermissionOrElseThrowResourceForbiddenException(projectPermission, entity);
    return this;
  }

  @Override
  public UserSession checkChildProjectsPermission(ProjectPermission projectPermission, ComponentDto component) {
    get().checkChildProjectsPermission(projectPermission, component);
    return this;
  }

  @Override
  public UserSession checkChildProjectsPermission(ProjectPermission projectPermission, EntityDto application) {
    get().checkChildProjectsPermission(projectPermission, application);
    return this;
  }

  @Override
  public UserSession checkComponentUuidPermission(ProjectPermission permission, String componentUuid) {
    get().checkComponentUuidPermission(permission, componentUuid);
    return this;
  }

  @Override
  public boolean isSystemAdministrator() {
    return get().isSystemAdministrator();
  }

  @Override
  public UserSession checkIsSystemAdministrator() {
    get().checkIsSystemAdministrator();
    return this;
  }

  @Override
  public boolean isActive() {
    return get().isActive();
  }

  @Override
  public boolean isAuthenticatedBrowserSession() {
    return get().isAuthenticatedBrowserSession();
  }

  @Override
  public boolean hasComponentPermission(ProjectPermission permission, ComponentDto component) {
    return get().hasComponentPermission(permission, component);
  }

  @Override
  public boolean hasEntityPermission(ProjectPermission permission, EntityDto entity) {
    return get().hasEntityPermission(permission, entity);
  }

  @Override
  public boolean hasEntityPermission(ProjectPermission permission, String entityUuid) {
    return get().hasEntityPermission(permission, entityUuid);
  }

  @Override
  public boolean hasChildProjectsPermission(ProjectPermission permission, ComponentDto component) {
    return get().hasChildProjectsPermission(permission, component);
  }

  @Override
  public boolean hasChildProjectsPermission(ProjectPermission permission, EntityDto application) {
    return get().hasChildProjectsPermission(permission, application);
  }

  @Override
  public boolean hasPortfolioChildProjectsPermission(ProjectPermission permission, ComponentDto portfolio) {
    return get().hasPortfolioChildProjectsPermission(permission, portfolio);
  }

  @Override
  public boolean hasComponentUuidPermission(ProjectPermission permission, String componentUuid) {
    return get().hasComponentUuidPermission(permission, componentUuid);
  }

  @Override
  public List<ComponentDto> keepAuthorizedComponents(ProjectPermission permission, Collection<ComponentDto> components) {
    return get().keepAuthorizedComponents(permission, components);
  }

  @Override
  public <T extends EntityDto> List<T> keepAuthorizedEntities(ProjectPermission permission, Collection<T> entities) {
    return get().keepAuthorizedEntities(permission, entities);
  }
}
