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
package org.sonar.ce.user;

import java.util.Collection;
import java.util.List;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.GroupDto;
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
    return notImplemented();
  }

  @Override
  public String getName() {
    return notImplemented();
  }

  @Override
  public Integer getUserId() {
    return notImplemented();
  }

  @Override
  public Collection<GroupDto> getGroups() {
    return notImplemented();
  }

  @Override
  public boolean isLoggedIn() {
    return notImplementedBooleanMethod();
  }

  @Override
  public boolean isRoot() {
    return notImplementedBooleanMethod();
  }

  @Override
  public UserSession checkIsRoot() {
    return notImplemented();
  }

  @Override
  public UserSession checkLoggedIn() {
    return notImplemented();
  }

  @Override
  public boolean hasPermission(String globalPermission) {
    return notImplementedBooleanMethod();
  }

  @Override
  public boolean hasOrganizationPermission(String organizationUuid, String permission) {
    return notImplementedBooleanMethod();
  }

  @Override
  public UserSession checkOrganizationPermission(String organizationUuid, String permission) {
    return notImplemented();
  }

  @Override
  public List<String> globalPermissions() {
    return notImplemented();
  }

  @Override
  public UserSession checkComponentPermission(String projectPermission, ComponentDto component) {
    return notImplemented();
  }

  @Override
  public UserSession checkComponentUuidPermission(String permission, String componentUuid) {
    return notImplemented();
  }

  @Override
  public boolean hasComponentPermission(String permission, ComponentDto component) {
    return notImplementedBooleanMethod();
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    return notImplementedBooleanMethod();
  }

  private static <T> T notImplemented() {
    throw new UnsupportedOperationException(UOE_MESSAGE);
  }

  private static boolean notImplementedBooleanMethod() {
    throw new UnsupportedOperationException(UOE_MESSAGE);
  }
}
