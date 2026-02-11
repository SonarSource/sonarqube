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
package org.sonar.server.users;

import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonarsource.users.api.UsersQuery;
import org.sonarsource.users.api.UsersService;
import org.sonarsource.users.api.model.User;

@ServerSide
public class UsersServiceImpl implements UsersService {
  private final DbClient dbClient;
  private final AvatarResolver avatarResolver;

  public UsersServiceImpl(DbClient dbClient, AvatarResolver avatarResolver) {
    this.dbClient = dbClient;
    this.avatarResolver = avatarResolver;
  }

  @Override
  public List<User> findUsers(UsersQuery query) {
    UsersQueryValidator.validateQuery(query);

    try (DbSession dbSession = dbClient.openSession(false)) {
      List<UserDto> userDtos;

      if (query.ids() != null && !query.ids().isEmpty()) {
        userDtos = dbClient.userDao().selectByUuids(dbSession, query.ids());
      } else {
        userDtos = dbClient.userDao().selectByLogins(dbSession, query.logins());
      }

      return userDtos.stream()
        .map(dto -> UserDtoConverter.toApiUser(dto, avatarResolver))
        .toList();
    }
  }
}
