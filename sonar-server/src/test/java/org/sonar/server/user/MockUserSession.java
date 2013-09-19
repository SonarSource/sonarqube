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

import org.sonar.core.permission.GlobalPermission;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

public class MockUserSession extends UserSession {

  private MockUserSession() {
    globalPermissions = Collections.emptyList();
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

  public MockUserSession setPermissions(GlobalPermission... perm) {
    globalPermissions = Arrays.asList(perm);
    return this;
  }
}
