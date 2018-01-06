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
package org.sonar.api.security;

import org.sonar.api.ExtensionPoint;
import org.sonar.api.server.ServerSide;

/**
 * @since 1.12
 * @see SecurityRealm
 * @deprecated replaced by Authenticator in version 3.1
 */
@Deprecated
@ServerSide
@ExtensionPoint
public interface LoginPasswordAuthenticator {

  /**
   * @throws RuntimeException if the authenticator can not be initialized at sonar server startup, eg. if the connection to LDAP server is refused
   * @deprecated in 2.14, but was left for backward compatibility - when this authenticator is not a part of {@link SecurityRealm}, otherwise has no effect and not invoked
   */
  @Deprecated
  void init();

  /**
   * @return true, if user was successfully authenticated with specified username and password, false otherwise
   * @throws RuntimeException in case of unexpected error such as connection failure
   */
  boolean authenticate(String username, String password);

}
