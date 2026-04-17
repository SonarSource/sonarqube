/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.security;

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
import org.sonar.server.user.UserSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * UserSession implementation that delegates to Spring Security's SecurityContext.
 * This makes SecurityContext the single source of truth for user identity in /api/v2/*.
 *
 * <p>For backwards compatibility with code that expects ThreadLocalUserSession,
 * this wrapper allows legacy code to continue working while the actual user data
 * lives in SecurityContext.</p>
 *
 * <p><strong>Architecture:</strong> All methods delegate to the original UserSession
 * stored in UserSessionAuthentication within SecurityContext. This eliminates
 * dual ThreadLocal storage.</p>
 */
public class SecurityContextBackedUserSession implements UserSession {

  /**
   * Get the UserSession from SecurityContext.
   * This extracts the actual UserSession stored in the SonarUserDetails principal.
   */
  private static UserSession delegate() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
      throw new UnauthorizedException("Authentication is required");
    }

    // Extract UserSession from SonarUserDetails principal
    Object principal = authentication.getPrincipal();
    if (principal instanceof SonarUserDetails sonarUserDetails) {
      return sonarUserDetails.getUserSession();
    }

    throw new UnauthorizedException("UserSession not found in authentication principal");
  }

  @Override
  @CheckForNull
  public String getLogin() {
    return delegate().getLogin();
  }

  @Override
  @CheckForNull
  public String getUuid() {
    return delegate().getUuid();
  }

  @Override
  @CheckForNull
  public String getName() {
    return delegate().getName();
  }

  @Override
  @CheckForNull
  public Long getLastSonarlintConnectionDate() {
    return delegate().getLastSonarlintConnectionDate();
  }

  @Override
  public Collection<GroupDto> getGroups() {
    return delegate().getGroups();
  }

  @Override
  public boolean shouldResetPassword() {
    return delegate().shouldResetPassword();
  }

  @Override
  public Optional<IdentityProvider> getIdentityProvider() {
    return delegate().getIdentityProvider();
  }

  @Override
  public Optional<ExternalIdentity> getExternalIdentity() {
    return delegate().getExternalIdentity();
  }

  @Override
  public boolean isLoggedIn() {
    return delegate().isLoggedIn();
  }

  @Override
  public UserSession checkLoggedIn() {
    delegate().checkLoggedIn();
    return this;
  }

  @Override
  public boolean hasPermission(GlobalPermission permission) {
    return delegate().hasPermission(permission);
  }

  @Override
  public UserSession checkPermission(GlobalPermission permission) {
    delegate().checkPermission(permission);
    return this;
  }

  @Override
  public boolean hasComponentPermission(ProjectPermission permission, ComponentDto component) {
    return delegate().hasComponentPermission(permission, component);
  }

  @Override
  public boolean hasEntityPermission(ProjectPermission permission, EntityDto entity) {
    return delegate().hasEntityPermission(permission, entity);
  }

  @Override
  public boolean hasEntityPermission(ProjectPermission permission, String entityUuid) {
    return delegate().hasEntityPermission(permission, entityUuid);
  }

  @Override
  public boolean hasChildProjectsPermission(ProjectPermission permission, ComponentDto component) {
    return delegate().hasChildProjectsPermission(permission, component);
  }

  @Override
  public boolean hasChildProjectsPermission(ProjectPermission permission, EntityDto application) {
    return delegate().hasChildProjectsPermission(permission, application);
  }

  @Override
  public boolean hasPortfolioChildProjectsPermission(ProjectPermission permission, ComponentDto component) {
    return delegate().hasPortfolioChildProjectsPermission(permission, component);
  }

  @Override
  public boolean hasComponentUuidPermission(ProjectPermission permission, String componentUuid) {
    return delegate().hasComponentUuidPermission(permission, componentUuid);
  }

  @Override
  public List<ComponentDto> keepAuthorizedComponents(ProjectPermission permission, Collection<ComponentDto> components) {
    return delegate().keepAuthorizedComponents(permission, components);
  }

  @Override
  public <T extends EntityDto> List<T> keepAuthorizedEntities(ProjectPermission permission, Collection<T> entities) {
    return delegate().keepAuthorizedEntities(permission, entities);
  }

  @Override
  public UserSession checkComponentPermission(ProjectPermission projectPermission, ComponentDto component) {
    delegate().checkComponentPermission(projectPermission, component);
    return this;
  }

  @Override
  public UserSession checkEntityPermission(ProjectPermission projectPermission, EntityDto entity) {
    delegate().checkEntityPermission(projectPermission, entity);
    return this;
  }

  @Override
  public UserSession checkEntityPermissionOrElseThrowResourceForbiddenException(ProjectPermission projectPermission, EntityDto entity) {
    delegate().checkEntityPermissionOrElseThrowResourceForbiddenException(projectPermission, entity);
    return this;
  }

  @Override
  public UserSession checkChildProjectsPermission(ProjectPermission projectPermission, ComponentDto project) {
    delegate().checkChildProjectsPermission(projectPermission, project);
    return this;
  }

  @Override
  public UserSession checkChildProjectsPermission(ProjectPermission projectPermission, EntityDto application) {
    delegate().checkChildProjectsPermission(projectPermission, application);
    return this;
  }

  @Override
  @Deprecated
  public UserSession checkComponentUuidPermission(ProjectPermission permission, String componentUuid) {
    delegate().checkComponentUuidPermission(permission, componentUuid);
    return this;
  }

  @Override
  public boolean isSystemAdministrator() {
    return delegate().isSystemAdministrator();
  }

  @Override
  public UserSession checkIsSystemAdministrator() {
    delegate().checkIsSystemAdministrator();
    return this;
  }

  @Override
  public boolean isActive() {
    return delegate().isActive();
  }

  @Override
  public boolean isAuthenticatedBrowserSession() {
    return delegate().isAuthenticatedBrowserSession();
  }
}
