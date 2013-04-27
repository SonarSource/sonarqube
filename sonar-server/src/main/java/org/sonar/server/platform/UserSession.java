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
package org.sonar.server.platform;

import com.google.common.base.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UserSession {

  private static final ThreadLocal<UserSession> threadLocal = new ThreadLocal<UserSession>();
  private static final UserSession ANONYMOUS = new UserSession(null, null);

  private final Integer userId;
  private final String login;

  public UserSession(@Nullable Integer userId, @Nullable String login) {
    this.userId = userId;
    this.login = login;
  }

  @CheckForNull
  public String login() {
    return login;
  }

  @CheckForNull
  public Integer userId() {
    return userId;
  }

  public boolean isLoggedIn() {
    return userId != null;
  }

  /**
   * @return never null
   */
  public static UserSession get() {
    return Objects.firstNonNull(threadLocal.get(), ANONYMOUS);
  }

  static void set(@Nullable UserSession session) {
    threadLocal.set(session);
  }

}

