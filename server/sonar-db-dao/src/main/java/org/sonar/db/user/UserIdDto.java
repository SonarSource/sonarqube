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
package org.sonar.db.user;

import static java.util.Objects.requireNonNull;

public class UserIdDto implements UserId {
  private String uuid;
  private String login;

  public UserIdDto(String uuid, String login) {
    this.uuid = uuid;
    this.login = requireNonNull(login);
  }

  public UserIdDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public UserIdDto setLogin(String login) {
    this.login = login;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public String getLogin() {
    return login;
  }

  public static UserIdDto from(UserDto dto) {
    return new UserIdDto(dto.getUuid(), dto.getLogin());
  }

  @Override
  public String toString() {
    return "login='" + login + "'";
  }
}
