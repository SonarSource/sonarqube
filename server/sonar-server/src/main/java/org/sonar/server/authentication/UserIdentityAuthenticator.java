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

import javax.servlet.http.HttpSession;
import org.sonar.api.server.authentication.IdentityProvider;
import org.sonar.api.server.authentication.UnauthorizedException;
import org.sonar.api.server.authentication.UserIdentity;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserUpdater;

import static java.lang.String.format;

public class UserIdentityAuthenticator {

  private final DbClient dbClient;
  private final UserUpdater userUpdater;

  public UserIdentityAuthenticator(DbClient dbClient, UserUpdater userUpdater) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
  }

  public void authenticate(UserIdentity user, IdentityProvider provider, HttpSession session) {
    long userDbId = register(user, provider);

    // hack to disable Ruby on Rails authentication
    session.setAttribute("user_id", userDbId);
  }

  private long register(UserIdentity user, IdentityProvider provider) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      String userLogin = user.getLogin();
      UserDto userDto = dbClient.userDao().selectByLogin(dbSession, userLogin);
      if (userDto != null && userDto.isActive()) {
        userUpdater.update(dbSession, UpdateUser.create(userDto.getLogin())
          .setEmail(user.getEmail())
          .setName(user.getName())
          .setExternalIdentity(new ExternalIdentity(provider.getKey(), user.getProviderLogin()))
          .setPassword(null));
        return userDto.getId();
      }

      if (!provider.allowsUsersToSignUp()) {
        throw new UnauthorizedException(format("'%s' users are not allowed to sign up", provider.getKey()));
      }

      String email = user.getEmail();
      if (email != null && dbClient.userDao().doesEmailExist(dbSession, email)) {
        throw new UnauthorizedException(format(
          "You can't sign up because email '%s' is already used by an existing user. This means that you probably already registered with another account.", email));
      }

      userUpdater.create(dbSession, NewUser.create()
        .setLogin(userLogin)
        .setEmail(user.getEmail())
        .setName(user.getName())
        .setExternalIdentity(new ExternalIdentity(provider.getKey(), user.getProviderLogin()))
        );
      return dbClient.userDao().selectOrFailByLogin(dbSession, userLogin).getId();

    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
