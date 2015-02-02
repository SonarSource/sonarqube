/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.user;

import com.google.common.collect.HashMultimap;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.user.AuthorizationDao;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Mockito.mock;

public class MockUserSession extends UserSession {

  private final AuthorizationDao authorizationDao;

  private final ResourceDao resourceDao;

  private MockUserSession() {
    globalPermissions = Collections.emptyList();
    projectKeyByPermission = HashMultimap.create();
    projectUuidByPermission = HashMultimap.create();
    authorizationDao = mock(AuthorizationDao.class);
    resourceDao = mock(ResourceDao.class);
  }

  public static MockUserSession set() {
    MockUserSession session = create();
    UserSession.set(session);
    return session;
  }

  public static MockUserSession create() {
    return new MockUserSession();
  }

  public MockUserSession setLocale(@Nullable Locale locale) {
    super.setLocale(locale);
    return this;
  }

  public MockUserSession setLogin(@Nullable String login) {
    super.setLogin(login);
    return this;
  }

  public MockUserSession setName(@Nullable String name) {
    super.setName(name);
    return this;
  }

  public MockUserSession setUserId(@Nullable Integer userId) {
    super.setUserId(userId);
    return this;
  }

  public MockUserSession setUserGroups(@Nullable String... userGroups) {
    super.setUserGroups(userGroups);
    return this;
  }

  public MockUserSession setGlobalPermissions(String... globalPermissions) {
    this.globalPermissions = Arrays.asList(globalPermissions);
    return this;
  }

  /**
   * Deprecated, please use {@link #addProjectUuidPermissions}
   */
  @Deprecated
  public MockUserSession addProjectPermissions(String projectPermission, String... projectKeys) {
    this.projectPermissions.add(projectPermission);
    this.projectKeyByPermission.putAll(projectPermission, newArrayList(projectKeys));
    return this;
  }

  public MockUserSession addProjectUuidPermissions(String projectPermission, String... projectUuids) {
    this.projectPermissions.add(projectPermission);
    this.projectUuidByPermission.putAll(projectPermission, newArrayList(projectUuids));
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
  AuthorizationDao authorizationDao() {
    return authorizationDao;
  }

  @Override
  ResourceDao resourceDao() {
    return resourceDao;
  }
}
