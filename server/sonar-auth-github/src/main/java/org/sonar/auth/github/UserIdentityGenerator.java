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
package org.sonar.auth.github;

import static java.lang.String.format;
import static org.sonar.auth.github.GitHubSettings.LOGIN_STRATEGY_PROVIDER_ID;
import static org.sonar.auth.github.GitHubSettings.LOGIN_STRATEGY_UNIQUE;

class UserIdentityGenerator {

  private UserIdentityGenerator() {
    // Only static method
  }

  static String generateLogin(GsonUser gsonUser, String loginStrategy) {
    switch (loginStrategy) {
      case LOGIN_STRATEGY_PROVIDER_ID:
        return gsonUser.getLogin();
      case LOGIN_STRATEGY_UNIQUE:
        return generateUniqueLogin(gsonUser);
      default:
        throw new IllegalStateException(format("Login strategy not supported : %s", loginStrategy));
    }
  }

  static String generateName(GsonUser gson) {
    String name = gson.getName();
    return name == null || name.isEmpty() ? gson.getLogin() : name;
  }

  private static String generateUniqueLogin(GsonUser gsonUser) {
    return format("%s@%s", gsonUser.getLogin(), GitHubIdentityProvider.KEY);
  }
}
