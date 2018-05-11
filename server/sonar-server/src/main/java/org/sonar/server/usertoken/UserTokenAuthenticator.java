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
package org.sonar.server.usertoken;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserTokenDto;

import static java.util.Optional.empty;
import static java.util.Optional.of;

public class UserTokenAuthenticator {
  private final TokenGenerator tokenGenerator;
  private final DbClient dbClient;

  public UserTokenAuthenticator(TokenGenerator tokenGenerator, DbClient dbClient) {
    this.tokenGenerator = tokenGenerator;
    this.dbClient = dbClient;
  }

  /**
   * Returns the user uuid if the token hash is found, else {@code Optional.absent()}.
   * The returned uuid is not validated. If database is corrupted (table USER_TOKENS badly purged
   * for instance), then the uuid may not relate to a valid user.
   */
  public java.util.Optional<String> authenticate(String token) {
    String tokenHash = tokenGenerator.hash(token);
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserTokenDto userToken = dbClient.userTokenDao().selectByTokenHash(dbSession, tokenHash);
      if (userToken == null) {
        return empty();
      }
      return of(userToken.getUserUuid());
    }
  }
}
