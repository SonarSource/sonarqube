/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.auth.saml;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.SamlMessageIdDto;
import org.springframework.security.saml2.core.Saml2Error;

@ServerSide
public class SamlMessageIdChecker {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamlMessageIdChecker.class);

  private final DbClient dbClient;
  private final Clock clock;

  public SamlMessageIdChecker(DbClient dbClient, Clock clock) {
    this.dbClient = dbClient;
    this.clock = clock;
  }

  public Optional<Saml2Error> validateMessageIdWasNotAlreadyUsed(String responseId) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      LOGGER.trace("Validating that response ID '{}' was not already used", responseId);
      if (responseIdAlreadyUsed(dbSession, responseId)) {
        return Optional.of(new Saml2Error("response_id_already_used", "A message with the same ID was already processed"));
      }
      persistResponseId(dbSession, responseId);
    }
    return Optional.empty();
  }

  private boolean responseIdAlreadyUsed(DbSession dbSession, String responseId) {
    return dbClient.samlMessageIdDao().selectByMessageId(dbSession, responseId).isPresent();
  }

  private void persistResponseId(DbSession dbSession, String responseId) {
    dbClient.samlMessageIdDao().insert(dbSession, new SamlMessageIdDto()
      .setMessageId(responseId)
      .setExpirationDate(Instant.now(clock).plus(1, ChronoUnit.DAYS).toEpochMilli()));
    dbSession.commit();
  }

}
