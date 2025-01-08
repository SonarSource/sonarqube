/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbTester;
import org.sonar.db.user.SamlMessageIdDto;
import org.springframework.security.saml2.core.Saml2Error;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;

public class SamlMessageIdCheckerIT {

  public static final SamlMessageIdDto MESSAGE_1 = new SamlMessageIdDto().setMessageId("MESSAGE_1").setExpirationDate(1_000_000_000L);

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final Clock clock = Clock.fixed(Instant.EPOCH, ZoneId.systemDefault());

  private final SamlMessageIdChecker underTest = new SamlMessageIdChecker(db.getDbClient(), clock);

  @Test
  void check_fails_when_message_id_already_exist() {
    insertMessageInDb();

    Optional<Saml2Error> validationErrors = underTest.validateMessageIdWasNotAlreadyUsed("MESSAGE_1");
    assertThat(validationErrors).isPresent();
    Saml2Error saml2Error = validationErrors.orElseThrow();
    assertThat(saml2Error.getErrorCode()).isEqualTo("response_id_already_used");
    assertThat(saml2Error.getDescription()).isEqualTo("A message with the same ID was already processed");
  }

  @Test
  void check_do_not_fail_when_message_id_is_new_and_insert_saml_message_in_db() {
    insertMessageInDb();

    Optional<Saml2Error> validationErrors = underTest.validateMessageIdWasNotAlreadyUsed("MESSAGE_2");
    assertThat(validationErrors).isEmpty();

    SamlMessageIdDto result = db.getDbClient().samlMessageIdDao().selectByMessageId(db.getSession(), "MESSAGE_2").orElseThrow();
    assertThat(result.getMessageId()).isEqualTo("MESSAGE_2");
    assertThat(Instant.ofEpochMilli(result.getExpirationDate())).isEqualTo(Instant.EPOCH.plus(1, DAYS));
  }

  private void insertMessageInDb() {
    db.getDbClient().samlMessageIdDao().insert(db.getSession(), MESSAGE_1);
    db.commit();
  }

}
