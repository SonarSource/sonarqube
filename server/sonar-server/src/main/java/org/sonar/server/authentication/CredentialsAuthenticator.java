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

import static org.sonar.db.user.UserDto.encryptPassword;

import javax.servlet.http.HttpServletRequest;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;

public class CredentialsAuthenticator {

  private final DbClient dbClient;
  private final RealmAuthenticator externalAuthenticator;

  public CredentialsAuthenticator(DbClient dbClient, RealmAuthenticator externalAuthenticator) {
    this.dbClient = dbClient;
    this.externalAuthenticator = externalAuthenticator;
  }

  public UserDto authenticate(String userLogin, String userPassword, HttpServletRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return authenticate(dbSession, userLogin, userPassword, request);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private UserDto authenticate(DbSession dbSession, String userLogin, String userPassword, HttpServletRequest request) {
    UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin);
    if (user != null && user.isLocal()) {
      return authenticateFromDb(user, userPassword);
    } else if (externalAuthenticator.isExternalAuthenticationUsed()) {
      return externalAuthenticator.authenticate(userLogin, userPassword, request);
    } else {
      throw new UnauthorizedException();
    }
  }

  private UserDto authenticateFromDb(UserDto userDto, String userPassword) {
    String cryptedPassword = userDto.getCryptedPassword();
    String salt = userDto.getSalt();
    if (cryptedPassword == null || salt == null
      || !cryptedPassword.equals(encryptPassword(userPassword, salt))) {
      throw new UnauthorizedException();
    }
    return userDto;
  }

}
