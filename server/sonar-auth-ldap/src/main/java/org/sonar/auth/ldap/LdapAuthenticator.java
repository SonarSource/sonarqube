/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.auth.ldap;

import javax.annotation.Nullable;
import org.sonar.api.server.http.HttpRequest;

import static java.util.Objects.requireNonNull;

public interface LdapAuthenticator {

  /**
   * @return true if user was successfully authenticated with specified credentials, false otherwise
   * @throws RuntimeException in case of unexpected error such as connection failure
   */
  LdapAuthenticationResult doAuthenticate(LdapAuthenticator.Context context);

  final class Context {
    private String username;
    private String password;
    private HttpRequest request;

    public Context(@Nullable String username, @Nullable String password, HttpRequest request) {
      requireNonNull(request);
      this.request = request;
      this.username = username;
      this.password = password;
    }

    /**
     * Username can be null, for example when using <a href="http://www.jasig.org/cas">CAS</a>.
     */
    public String getUsername() {
      return username;
    }

    /**
     * Password can be null, for example when using <a href="http://www.jasig.org/cas">CAS</a>.
     */
    public String getPassword() {
      return password;
    }

    public HttpRequest getRequest() {
      return request;
    }
  }
}
