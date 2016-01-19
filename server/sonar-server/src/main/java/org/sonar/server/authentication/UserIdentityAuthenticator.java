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
package org.sonar.server.authentication;

import com.google.common.base.Optional;
import javax.servlet.http.HttpSession;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;

public class UserIdentityAuthenticator {

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final UuidFactory uuidFactory;

  public UserIdentityAuthenticator(DbClient dbClient, UserUpdater userUpdater, UuidFactory uuidFactory) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.uuidFactory = uuidFactory;
  }

  public void authenticate(UserIdentity user, IdentityProvider provider, HttpSession session) {
    long userDbId = register(user, provider);

    // hack to disable Ruby on Rails authentication
    session.setAttribute("user_id", userDbId);
  }

  private long register(UserIdentity user, IdentityProvider provider) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      String userId = user.getId();
      Optional<UserDto> userDto = dbClient.userDao().selectByExternalIdentity(dbSession, userId, provider.getKey());
      if (userDto.isPresent() && userDto.get().isActive()) {
        userUpdater.update(dbSession, UpdateUser.create(userDto.get().getLogin())
          .setEmail(user.getEmail())
          .setName(user.getName())
          );
        return userDto.get().getId();
      }

      if (!provider.allowsUsersToSignUp()) {
        throw new NotAllowUserToSignUpException(provider);
      }
      userUpdater.create(dbSession, NewUser.create()
        .setLogin(uuidFactory.create())
        .setEmail(user.getEmail())
        .setName(user.getName())
        .setExternalIdentity(new NewUser.ExternalIdentity(provider.getKey(), userId))
        );
      return dbClient.userDao().selectOrFailByExternalIdentity(dbSession, userId, provider.getKey()).getId();

    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
