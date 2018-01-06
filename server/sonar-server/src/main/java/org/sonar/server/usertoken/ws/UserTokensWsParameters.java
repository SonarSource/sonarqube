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

public class UserTokensWsParameters {
  public static final String CONTROLLER = "api/user_tokens";
  public static final String ACTION_GENERATE = "generate";
  public static final String ACTION_REVOKE = "revoke";
  public static final String ACTION_SEARCH = "search";

  public static final String PARAM_LOGIN = "login";
  public static final String PARAM_NAME = "name";

  private UserTokensWsParameters() {
    // constants only
  }
}
