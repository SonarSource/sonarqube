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
package org.sonar.server.usertoken.notification;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.db.DbClient;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;

public class TokenExpirationNotificationSender {
  private static final Logger LOG = LoggerFactory.getLogger(TokenExpirationNotificationSender.class);
  private final DbClient dbClient;
  private final TokenExpirationEmailComposer emailComposer;

  public TokenExpirationNotificationSender(DbClient dbClient, TokenExpirationEmailComposer emailComposer) {
    this.dbClient = dbClient;
    this.emailComposer = emailComposer;
  }

  public void sendNotifications() {
    if (!emailComposer.areEmailSettingsSet()) {
      LOG.debug("Emails for token expiration notification have not been sent because email settings are not configured.");
      return;
    }
    try (var dbSession = dbClient.openSession(false)) {
      var expiringTokens = dbClient.userTokenDao().selectTokensExpiredInDays(dbSession, 7);
      var expiredTokens = dbClient.userTokenDao().selectTokensExpiredInDays(dbSession, 0);
      var tokensToNotify = Stream.concat(expiringTokens.stream(), expiredTokens.stream()).toList();
      var usersToNotify = tokensToNotify.stream().map(UserTokenDto::getUserUuid).collect(Collectors.toSet());
      Map<String, String> userUuidToEmail = dbClient.userDao().selectByUuids(dbSession, usersToNotify).stream()
        .collect(Collectors.toMap(UserDto::getUuid, UserDto::getEmail));
      tokensToNotify.stream().map(token -> new TokenExpirationEmail(userUuidToEmail.get(token.getUserUuid()), token)).forEach(emailComposer::send);
    }
  }
}
