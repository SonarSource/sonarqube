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

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.exceptions.UnauthorizedException;

import static org.sonar.db.user.UserDto.encryptPassword;
import static org.sonar.server.authentication.event.AuthenticationEvent.*;

public class CredentialsAuthenticator {

  private final DbClient dbClient;
  private final RealmAuthenticator externalAuthenticator;
  private final AuthenticationEvent authenticationEvent;

  public CredentialsAuthenticator(DbClient dbClient, RealmAuthenticator externalAuthenticator, AuthenticationEvent authenticationEvent) {
    this.dbClient = dbClient;
    this.externalAuthenticator = externalAuthenticator;
    this.authenticationEvent = authenticationEvent;
  }

  public UserDto authenticate(String userLogin, String userPassword, HttpServletRequest request, Method method) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return authenticate(dbSession, userLogin, userPassword, request, method);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private UserDto authenticate(DbSession dbSession, String userLogin, String userPassword, HttpServletRequest request, Method method) {
    UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin);
    if (user != null && user.isLocal()) {
      UserDto userDto = authenticateFromDb(user, userPassword);
      authenticationEvent.login(request, userLogin, Source.local(method));
      return userDto;
    }
    Optional<UserDto> userDto = externalAuthenticator.authenticate(userLogin, userPassword, request, method);
    if (userDto.isPresent()) {
      return userDto.get();
    }
    throw new UnauthorizedException();
  }

  private static UserDto authenticateFromDb(UserDto userDto, String userPassword) {
    String cryptedPassword = userDto.getCryptedPassword();
    String salt = userDto.getSalt();
    if (cryptedPassword == null || salt == null
      || !cryptedPassword.equals(encryptPassword(userPassword, salt))) {
      throw new UnauthorizedException();
    }
    return userDto;
  }

}
