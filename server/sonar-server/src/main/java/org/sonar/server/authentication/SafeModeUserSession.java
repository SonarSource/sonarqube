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
package org.sonar.server.authentication;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.concurrent.Immutable;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.server.user.AbstractUserSession;

@Immutable
public class SafeModeUserSession extends AbstractUserSession {
  
  @Override
  protected boolean hasPermissionImpl(OrganizationPermission permission, String organizationUuid) {
    return false;
  }

  @Override
  protected Optional<String> componentUuidToProjectUuid(String componentUuid) {
    return Optional.empty();
  }

  @Override
  protected boolean hasProjectUuidPermission(String permission, String projectUuid) {
    return false;
  }

  @CheckForNull
  @Override
  public String getLogin() {
    return null;
  }

  @CheckForNull
  @Override
  public String getName() {
    return null;
  }

  @CheckForNull
  @Override
  public Integer getUserId() {
    return null;
  }

  @Override
  public Collection<GroupDto> getGroups() {
    return Collections.emptyList();
  }

  @Override
  public boolean isLoggedIn() {
    return false;
  }

  @Override
  public boolean isRoot() {
    return false;
  }

  @Override
  public boolean isSystemAdministrator() {
    return false;
  }
}
