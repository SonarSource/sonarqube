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
package org.sonar.ce.user;

import java.util.Collection;
import java.util.List;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.user.UserSession;

/**
 * Implementation of {@link UserSession} which provide not implementation of any method.
 * <p>
 * Any use of {@link UserSession} in the Compute Engine will raise an error.
 * </p>
 */
public class CeUserSession implements UserSession {

  private static final String UOE_MESSAGE = "UserSession must not be used from within the Compute Engine";

  @Override
  public String getLogin() {
    throw notImplemented();
  }

  @Override
  public String getName() {
    throw notImplemented();
  }

  @Override
  public Integer getUserId() {
    throw notImplemented();
  }

  @Override
  public Collection<GroupDto> getGroups() {
    throw notImplemented();
  }

  @Override
  public boolean isLoggedIn() {
    throw notImplemented();
  }

  @Override
  public boolean isRoot() {
    throw notImplemented();
  }

  @Override
  public UserSession checkIsRoot() {
    throw notImplemented();
  }

  @Override
  public UserSession checkLoggedIn() {
    throw notImplemented();
  }

  @Override
  public boolean hasPermission(OrganizationPermission permission, String organizationUuid) {
    throw notImplemented();
  }

  @Override
  public UserSession checkPermission(OrganizationPermission permission, String organizationUuid) {
    throw notImplemented();
  }

  @Override
  public boolean hasPermission(OrganizationPermission permission, OrganizationDto organization) {
    throw notImplemented();
  }

  @Override
  public UserSession checkPermission(OrganizationPermission permission, OrganizationDto organization) {
    throw notImplemented();
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, ComponentDto component) {
    throw notImplemented();
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    throw notImplemented();
  }

  @Override
  public boolean isSystemAdministrator() {
    throw notImplemented();
  }

  @Override
  public UserSession checkIsSystemAdministrator() {
    throw notImplemented();
  }

  @Override
  public boolean hasComponentPermission(String permission, ComponentDto component) {
    throw notImplemented();
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    throw notImplemented();
  }

  @Override
  public List<ComponentDto> keepAuthorizedComponents(String permission, Collection<ComponentDto> components) {
    throw notImplemented();
  }

  private static RuntimeException notImplemented() {
    throw new UnsupportedOperationException(UOE_MESSAGE);
  }
}
