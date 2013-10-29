/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
import org.sonar.core.permission.Permission;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import static com.google.common.collect.Lists.newArrayList;

public class MockUserSession extends UserSession {

  private MockUserSession() {
    permissions = Collections.emptyList();
    projectIdByPermission = HashMultimap.create();
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

  public MockUserSession setUserId(@Nullable Integer userId) {
    super.setUserId(userId);
    return this;
  }

  public MockUserSession setGlobalPermissions(Permission... globalPermissions) {
    this.permissions = Arrays.asList(globalPermissions);
    return this;
  }

  public MockUserSession addProjectPermissions(String projectPermission, Long... projectIds) {
    this.projectPermissions.add(projectPermission);
    this.projectIdByPermission.putAll(projectPermission, newArrayList(projectIds));
    return this;
  }
}
