/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.authentication;

import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

public class UserAuthResult {

  public enum AuthType {
    SSO,
    JWT,
    TOKEN,
    GITHUB_WEBHOOK,
    BASIC
  }

  UserDto userDto;
  UserTokenDto tokenDto;
  AuthType authType;

  public UserAuthResult() {
  }

  public UserAuthResult(UserDto userDto, AuthType authType) {
    this.userDto = userDto;
    this.authType = authType;
  }

  public UserAuthResult(UserDto userDto, UserTokenDto tokenDto, AuthType authType) {
    this.userDto = userDto;
    this.tokenDto = tokenDto;
    this.authType = authType;
  }

  private UserAuthResult(AuthType authType) {
    this.authType = authType;
  }

  public static UserAuthResult withGithubWebhook() {
    return new UserAuthResult(AuthType.GITHUB_WEBHOOK);
  }

  public UserDto getUserDto() {
    return userDto;
  }

  public AuthType getAuthType() {
    return authType;
  }

  public UserTokenDto getTokenDto() {
    return tokenDto;
  }
}
