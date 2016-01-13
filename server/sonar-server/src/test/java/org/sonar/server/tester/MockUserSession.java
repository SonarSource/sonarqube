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
package org.sonar.server.tester;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.server.user.AbstractUserSession;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

public class MockUserSession extends AbstractUserSession<MockUserSession> implements UserSession {
  private Map<String, String> projectKeyByComponentKey = newHashMap();

  protected MockUserSession() {
    super(MockUserSession.class);
  }

  public MockUserSession(String login) {
    this();
    setLogin(Preconditions.checkNotNull(login));
  }

  public MockUserSession(MockUserSession ruleUserSession) {
    this();
    this.userId = ruleUserSession.userId;
    this.login = ruleUserSession.login;
    this.userGroups = ruleUserSession.userGroups;
    this.globalPermissions = ruleUserSession.globalPermissions;
    this.projectKeyByPermission = ruleUserSession.projectKeyByPermission;
    this.projectUuidByPermission = ruleUserSession.projectUuidByPermission;
    this.projectUuidByComponentUuid = ruleUserSession.projectUuidByComponentUuid;
    this.projectPermissionsCheckedByKey = ruleUserSession.projectPermissionsCheckedByKey;
    this.name = ruleUserSession.name;
    this.locale = ruleUserSession.locale;
  }

  @Override
  public boolean isLoggedIn() {
    return true;
  }

  public MockUserSession setGlobalPermissions(String... globalPermissions) {
    this.globalPermissions = Arrays.asList(globalPermissions);
    return this;
  }

  @Override
  public MockUserSession setLogin(@Nullable String s) {
    return super.setLogin(s);
  }

  @Override
  public MockUserSession setName(@Nullable String s) {
    return super.setName(s);
  }

  @Override
  public MockUserSession setUserId(@Nullable Integer userId) {
    return super.setUserId(userId);
  }

  @Override
  public MockUserSession setUserGroups(@Nullable String... userGroups) {
    return super.setUserGroups(userGroups);
  }

  @Override
  public MockUserSession setLocale(@Nullable Locale l) {
    return super.setLocale(l);
  }

  /**
   * Deprecated, please use {@link #addProjectUuidPermissions}
   */
  @Deprecated
  public MockUserSession addProjectPermissions(String projectPermission, String... projectKeys) {
    this.projectPermissionsCheckedByKey.add(projectPermission);
    this.projectKeyByPermission.putAll(projectPermission, newArrayList(projectKeys));
    for (String projectKey : projectKeys) {
      this.projectKeyByComponentKey.put(projectKey, projectKey);
    }
    return this;
  }

  public MockUserSession addProjectUuidPermissions(String projectPermission, String... projectUuids) {
    this.projectPermissionsCheckedByUuid.add(projectPermission);
    this.projectUuidByPermission.putAll(projectPermission, newArrayList(projectUuids));
    for (String projectUuid : projectUuids) {
      this.projectUuidByComponentUuid.put(projectUuid, projectUuid);
    }
    return this;
  }

  /**
   * Deprecated, please use {@link #addComponentUuidPermission}
   */
  @Deprecated
  public MockUserSession addComponentPermission(String projectPermission, String projectKey, String componentKey) {
    this.projectKeyByComponentKey.put(componentKey, projectKey);
    addProjectPermissions(projectPermission, projectKey);
    return this;
  }

  public MockUserSession addComponentUuidPermission(String projectPermission, String projectUuid, String componentUuid) {
    this.projectUuidByComponentUuid.put(componentUuid, projectUuid);
    addProjectUuidPermissions(projectPermission, projectUuid);
    return this;
  }

  @Override
  public List<String> globalPermissions() {
    return globalPermissions;
  }

  @Override
  public boolean hasComponentPermission(String permission, String componentKey) {
    String projectKey = projectKeyByComponentKey.get(componentKey);
    return hasPermission(permission) || (projectKey != null && hasProjectPermission(permission, projectKey));
  }

  private boolean hasProjectPermission(String permission, String projectKey) {
    return projectPermissionsCheckedByKey.contains(permission) && projectKeyByPermission.get(permission).contains(projectKey);
  }

  @Override
  public boolean hasComponentUuidPermission(String permission, String componentUuid) {
    String projectUuid = projectUuidByComponentUuid.get(componentUuid);
    return hasPermission(permission) || (projectUuid != null && hasProjectPermissionByUuid(permission, projectUuid));
  }

  private boolean hasProjectPermissionByUuid(String permission, String projectUuid) {
    return projectPermissionsCheckedByUuid.contains(permission) && projectUuidByPermission.get(permission).contains(projectUuid);
  }
}
