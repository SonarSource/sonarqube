/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sonar.core.permission.ProjectPermissions;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;

import static org.apache.commons.lang.StringUtils.defaultString;

public abstract class AbstractUserSession implements UserSession {
  private static final String INSUFFICIENT_PRIVILEGES_MESSAGE = "Insufficient privileges";
  private static final ForbiddenException INSUFFICIENT_PRIVILEGES_EXCEPTION = new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
  private static final String AUTHENTICATION_IS_REQUIRED_MESSAGE = "Authentication is required";

  @Override
  public UserSession checkIsRoot() {
    if (!isRoot()) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public final UserSession checkLoggedIn() {
    if (!isLoggedIn()) {
      throw new UnauthorizedException(AUTHENTICATION_IS_REQUIRED_MESSAGE);
    }
    return this;
  }

  @Override
  public final boolean hasPermission(OrganizationPermission permission, OrganizationDto organization) {
    return hasPermission(permission, organization.getUuid());
  }

  @Override
  public final boolean hasPermission(OrganizationPermission permission, String organizationUuid) {
    return isRoot() || hasPermissionImpl(permission, organizationUuid);
  }

  @Override
  public final UserSession checkPermission(OrganizationPermission permission, OrganizationDto organization) {
    return checkPermission(permission, organization.getUuid());
  }

  @Override
  public final UserSession checkPermission(OrganizationPermission permission, String organizationUuid) {
    if (!hasPermission(permission, organizationUuid)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  protected abstract boolean hasPermissionImpl(OrganizationPermission permission, String organizationUuid);

  @Override
  public final boolean hasComponentPermission(String permission, ComponentDto component) {
    if (isRoot()) {
      return true;
    }
    String projectUuid = defaultString(component.getMainBranchProjectUuid(), component.projectUuid());
    return hasProjectUuidPermission(permission, projectUuid);
  }

  @Override
  public final boolean hasComponentUuidPermission(String permission, String componentUuid) {
    if (isRoot()) {
      return true;
    }
    Optional<String> projectUuid = componentUuidToProjectUuid(componentUuid);
    return projectUuid
      .map(s -> hasProjectUuidPermission(permission, s))
      .orElse(false);
  }

  protected abstract Optional<String> componentUuidToProjectUuid(String componentUuid);

  protected abstract boolean hasProjectUuidPermission(String permission, String projectUuid);

  @Override
  public final UserSession checkComponentPermission(String projectPermission, ComponentDto component) {
    if (!hasComponentPermission(projectPermission, component)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  @Override
  public final UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    if (!hasComponentUuidPermission(permission, componentUuid)) {
      throw new ForbiddenException(INSUFFICIENT_PRIVILEGES_MESSAGE);
    }
    return this;
  }

  public static ForbiddenException insufficientPrivilegesException() {
    return INSUFFICIENT_PRIVILEGES_EXCEPTION;
  }

  @Override
  public final List<ComponentDto> keepAuthorizedComponents(String permission, Collection<ComponentDto> components) {
    if (isRoot()) {
      return new ArrayList<>(components);
    }
    return doKeepAuthorizedComponents(permission, components);
  }

  /**
   * Naive implementation, to be overridden if needed
   */
  protected List<ComponentDto> doKeepAuthorizedComponents(String permission, Collection<ComponentDto> components) {
    boolean allowPublicComponent = ProjectPermissions.PUBLIC_PERMISSIONS.contains(permission);
    return components.stream()
      .filter(c -> (allowPublicComponent && !c.isPrivate()) || hasComponentPermission(permission, c))
      .collect(MoreCollectors.toList());
  }

  @Override
  public final UserSession checkIsSystemAdministrator() {
    if (!isSystemAdministrator()) {
      throw insufficientPrivilegesException();
    }
    return this;
  }
}
