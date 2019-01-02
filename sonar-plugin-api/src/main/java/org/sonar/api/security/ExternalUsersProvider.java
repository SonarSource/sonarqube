/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.security;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

/**
 * Note that prefix "do" for names of methods is reserved for future enhancements, thus should not be used in subclasses.
 *
 * @see SecurityRealm
 * @since 2.14
 */
public abstract class ExternalUsersProvider {

  /**
   * This method is overridden by old versions of plugins such as LDAP 1.1. It should not be overridden anymore.
   *
   * @param username the username
   * @return details for specified user, or null if such user doesn't exist
   * @throws RuntimeException in case of unexpected error such as connection failure
   * @deprecated replaced by {@link #doGetUserDetails(org.sonar.api.security.ExternalUsersProvider.Context)} since v. 3.1
   */
  @Deprecated
  public UserDetails doGetUserDetails(@Nullable String username) {
    return null;
  }

  /**
   * Override this method in order load user information.
   *
   * @return the user, or null if user doesn't exist
   * @throws RuntimeException in case of unexpected error such as connection failure
   * @since 3.1
   */
  public UserDetails doGetUserDetails(Context context) {
    return doGetUserDetails(context.getUsername());
  }

  public static final class Context {
    private String username;
    private HttpServletRequest request;

    public Context(@Nullable String username, HttpServletRequest request) {
      this.username = username;
      this.request = request;
    }

    public String getUsername() {
      return username;
    }

    public HttpServletRequest getRequest() {
      return request;
    }
  }
}
