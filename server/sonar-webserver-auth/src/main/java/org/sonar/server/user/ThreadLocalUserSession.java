/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
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
  @CheckForNull
  public Integer getUserId() {
    return get().getUserId();
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
  public UserSession checkIsRoot() {
    return get().checkIsRoot();
  }

  @Override
  public boolean isRoot() {
    return get().isRoot();
  }

  @Override
  public UserSession checkLoggedIn() {
    get().checkLoggedIn();
    return this;
  }

  @Override
  public boolean hasPermission(OrganizationPermission permission, String organizationUuid) {
    return get().hasPermission(permission, organizationUuid);
  }

  @Override
  public UserSession checkPermission(OrganizationPermission permission, String organizationUuid) {
    get().checkPermission(permission, organizationUuid);
    return this;
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, ComponentDto component) {
    get().checkComponentPermission(projectPermission, component);
    return this;
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
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
  public boolean hasComponentPermission(String permission, ComponentDto component) {
    return get().hasComponentPermission(permission, component);
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    return get().hasComponentUuidPermission(permission, componentUuid);
  }

  @Override
  public UserSession checkPermission(OrganizationPermission permission, OrganizationDto organization) {
    get().checkPermission(permission, organization);
    return this;
  }

  @Override
  public boolean hasPermission(OrganizationPermission permission, OrganizationDto organization) {
    return get().hasPermission(permission, organization);
  }

  @Override
  public List<ComponentDto> keepAuthorizedComponents(String permission, Collection<ComponentDto> components) {
    return get().keepAuthorizedComponents(permission, components);
  }

  @Override
  public boolean hasMembership(OrganizationDto organizationDto) {
    return get().hasMembership(organizationDto);
  }

  @Override
  public UserSession checkMembership(OrganizationDto organization) {
    get().checkMembership(organization);
    return this;
  }
}
