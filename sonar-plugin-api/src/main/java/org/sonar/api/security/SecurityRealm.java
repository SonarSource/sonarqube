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

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * @since 2.14
 */
@ServerSide
@ExtensionPoint
public abstract class SecurityRealm {

  /**
   * @return unique name of this realm, e.g. "ldap"
   */
  public String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Invoked during server startup and can be used to initialize internal state.
   */
  public void init() {
  }

  /**
   * @return {@link LoginPasswordAuthenticator} associated with this realm, never null
   * @deprecated replaced by doGetAuthenticator in version 3.1
   */
  @Deprecated
  public LoginPasswordAuthenticator getLoginPasswordAuthenticator() {
    return null;
  }

  /**
   * @since 3.1
   */
  public Authenticator doGetAuthenticator() {
    // this method is not overridden when deprecated getLoginPasswordAuthenticator
    // is used
    return new Authenticator() {
      @Override
      public boolean doAuthenticate(Context context) {
        return getLoginPasswordAuthenticator().authenticate(context.getUsername(), context.getPassword());
      }
    };
  }

  /**
   * @return {@link ExternalUsersProvider} associated with this realm, null if not supported
   */
  public ExternalUsersProvider getUsersProvider() {
    return null;
  }

  /**
   * @return {@link ExternalGroupsProvider} associated with this realm, null if not supported
   */
  public ExternalGroupsProvider getGroupsProvider() {
    return null;
  }
}
