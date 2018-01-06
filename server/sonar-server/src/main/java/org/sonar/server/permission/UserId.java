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
package org.sonar.server.permission;

import javax.annotation.concurrent.Immutable;
import org.sonar.db.user.UserDto;

import static java.util.Objects.requireNonNull;

/**
 * Reference a user by his technical (db) id or functional login.
 * This is temporary class as long as services and DAOs do not
 * use only technical id.
 */
@Immutable
public class UserId {

  private final int id;
  private final String login;

  public UserId(int userId, String login) {
    this.id = userId;
    this.login = requireNonNull(login);
  }

  public int getId() {
    return id;
  }

  public String getLogin() {
    return login;
  }

  public static UserId from(UserDto dto) {
    return new UserId(dto.getId(), dto.getLogin());
  }
}
