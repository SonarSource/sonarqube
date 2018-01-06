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
package org.sonar.server.user;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.api.user.User;
import org.sonar.api.user.UserFinder;
import org.sonar.api.user.UserQuery;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;

/**
 * @since 3.6
 */
public class DefaultUserFinder implements UserFinder {

  private final DbClient dbClient;

  public DefaultUserFinder(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  @CheckForNull
  public User findByLogin(String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto dto = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
      return dto != null ? dto.toUser() : null;
    }
  }

  @Override
  public List<User> findByLogins(List<String> logins) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<UserDto> dtos = dbClient.userDao().selectByLogins(dbSession, logins);
      return toUsers(dtos);
    }
  }

  @Override
  public List<User> find(UserQuery query) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<UserDto> dtos = dbClient.userDao().selectUsers(dbSession, query);
      return toUsers(dtos);
    }
  }

  private static List<User> toUsers(Collection<UserDto> dtos) {
    List<User> users = Lists.newArrayList();
    for (UserDto dto : dtos) {
      users.add(dto.toUser());
    }
    return users;
  }
}
