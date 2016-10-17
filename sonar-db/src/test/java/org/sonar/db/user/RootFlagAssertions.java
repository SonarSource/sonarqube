/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class RootFlagAssertions {
  private final DbTester db;

  public RootFlagAssertions(DbTester db) {
    this.db = db;
  }

  public void verifyUnchanged(UserDto user) {
    verify(user, user.isRoot(), user.getUpdatedAt());
  }

  public void verify(UserDto userDto, boolean root, long updatedAt) {
    UserDto dto = db.getDbClient().userDao().selectByLogin(db.getSession(), userDto.getLogin());
    assertThat(dto.isRoot())
      .as("Root flag of user '%s' is same as when created", userDto.getLogin())
      .isEqualTo(root);
    assertThat(dto.getUpdatedAt())
      .as("UpdatedAt of user '%s' has not changed since created")
      .isEqualTo(updatedAt);
  }

  public void verify(UserDto userDto, boolean root) {
    UserDto dto = db.getDbClient().userDao().selectByLogin(db.getSession(), userDto.getLogin());
    assertThat(dto.isRoot())
      .as("Root flag of user '%s' is '%s'", userDto.getLogin(), root)
      .isEqualTo(root);
    assertThat(dto.getUpdatedAt())
      .as("UpdatedAt of user '%s' has changed since insertion", userDto.getLogin())
      .isNotEqualTo(userDto.getUpdatedAt());
  }

  public void verify(String login, boolean root) {
    assertThat(db.getDbClient().userDao().selectByLogin(db.getSession(), login).isRoot())
      .as("Root flag of user '%s' is '%s'", login, root)
      .isEqualTo(root);
  }
}
