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
package org.sonar.server.user;

import java.util.Collection;
import java.util.Optional;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.user.GroupDto;

import static java.util.Collections.emptySet;

/**
 * A session that holds no permission, belongs to no group, and has no identity provider behind it:
 * every authorization question is answered no.
 *
 * <p>Shared by the sessions with no {@code users} row behind them — safe mode and the service callers
 * of {@link AbstractServiceUserSession} — so that a permission added to {@link UserSession} has a
 * single place to be denied. Subclasses decide whether they count as logged in and active.
 */
public abstract class PermissionlessUserSession extends AbstractUserSession {

  @Override
  public Collection<GroupDto> getGroups() {
    return emptySet();
  }

  @Override
  public boolean shouldResetPassword() {
    return false;
  }

  @Override
  public Optional<IdentityProvider> getIdentityProvider() {
    return Optional.empty();
  }

  @Override
  public Optional<ExternalIdentity> getExternalIdentity() {
    return Optional.empty();
  }

  @Override
  public boolean isSystemAdministrator() {
    return false;
  }

  @Override
  public boolean isAuthenticatedBrowserSession() {
    return false;
  }

  @Override
  protected boolean hasPermissionImpl(GlobalPermission permission) {
    return false;
  }

  @Override
  protected Optional<String> componentUuidToEntityUuid(String componentUuid) {
    return Optional.empty();
  }

  @Override
  protected boolean hasEntityUuidPermission(ProjectPermission permission, String entityUuid) {
    return false;
  }

  @Override
  protected boolean hasChildProjectsPermission(ProjectPermission permission, String applicationUuid) {
    return false;
  }

  @Override
  protected boolean hasPortfolioChildProjectsPermission(ProjectPermission permission, String portfolioUuid) {
    return false;
  }

  @Override
  public boolean hasComponentUuidPermission(ProjectPermission permission, String componentUuid) {
    return false;
  }
}
