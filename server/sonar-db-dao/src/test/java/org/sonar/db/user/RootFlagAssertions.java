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
package org.sonar.db.user;

import java.util.Map;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class RootFlagAssertions {
  private final DbTester db;

  public RootFlagAssertions(DbTester db) {
    this.db = db;
  }

  public void verify(UserDto userDto, boolean root, long updatedAt) {
    Map<String, Object> row = db.selectFirst("select is_root as \"isRoot\", updated_at as \"updatedAt\" from users where login = '" + userDto.getLogin() + "'");
    Object isRoot = row.get("isRoot");
    assertThat(isRoot)
      .as("Root flag of user '%s' is same as when created", userDto.getLogin())
      .isEqualTo(isRoot instanceof Long ? toLong(root) : root);
    assertThat(row.get("updatedAt"))
      .as("UpdatedAt of user '%s' has not changed since created")
      .isEqualTo(updatedAt);
  }

  public void verify(UserDto userDto, boolean root) {
    Map<String, Object> row = db.selectFirst("select is_root as \"isRoot\", updated_at as \"updatedAt\" from users where login = '" + userDto.getLogin() + "'");
    Object isRoot = row.get("isRoot");
    assertThat(isRoot)
      .as("Root flag of user '%s' is '%s'", userDto.getLogin(), root)
      .isEqualTo(isRoot instanceof Long ? toLong(root) : root);
    assertThat(row.get("updatedAt"))
      .as("UpdatedAt of user '%s' has changed since insertion", userDto.getLogin())
      .isNotEqualTo(userDto.getUpdatedAt());
  }

  private static Long toLong(boolean root) {
    return root ? 1L : 0L;
  }

  public void verify(String login, boolean root) {
    assertThat(db.getDbClient().userDao().selectByLogin(db.getSession(), login).isRoot())
      .as("Root flag of user '%s' is '%s'", login, root)
      .isEqualTo(root);
  }
}
