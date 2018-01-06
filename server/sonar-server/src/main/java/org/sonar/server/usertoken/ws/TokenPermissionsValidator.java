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
package org.sonar.server.usertoken.ws;

import javax.annotation.Nullable;
import org.sonar.server.user.UserSession;

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

class TokenPermissionsValidator {
  private TokenPermissionsValidator() {
    // prevent instantiation
  }

  static void validate(UserSession userSession, @Nullable String requestLogin) {
    userSession.checkLoggedIn();
    if (!userSession.isSystemAdministrator() && !isLoggedInUser(userSession, requestLogin)) {
      throw insufficientPrivilegesException();
    }
  }

  private static boolean isLoggedInUser(UserSession userSession, @Nullable String requestLogin) {
    return requestLogin != null && requestLogin.equals(userSession.getLogin());
  }
}
