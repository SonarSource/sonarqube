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
package org.sonar.server.issue.notification;

import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MQRAndStandardModesExistNotificationHandlerTest {
  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
  private final EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);

  private final MQRAndStandardModesExistNotificationHandler underTest = new MQRAndStandardModesExistNotificationHandler(dbClient, emailNotificationChannel);

  @BeforeEach
  public void wire_mocks() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.authorizationDao()).thenReturn(authorizationDao);
  }

  @Test
  void toEmailDeliveryRequests_whenHasAdmins_shouldSendExpectedNotification() {
    when(authorizationDao.selectGlobalAdministerEmailSubscribers(dbSession))
      .thenReturn(Set.of(new EmailSubscriberDto().setEmail("email@email.com"), new EmailSubscriberDto().setEmail("email2@email.com")));

    Assertions.assertThat(underTest.toEmailDeliveryRequests(List.of(new MQRAndStandardModesExistNotification(true))))
      .extracting(EmailNotificationChannel.EmailDeliveryRequest::recipientEmail, EmailNotificationChannel.EmailDeliveryRequest::notification)
      .containsExactly(tuple("email@email.com", new MQRAndStandardModesExistNotification(true)),
        tuple("email2@email.com", new MQRAndStandardModesExistNotification(true)));
  }

  @Test
  void toEmailDeliveryRequests_whenHasNoAdmins_shouldNotSendNotification() {
    when(authorizationDao.selectGlobalAdministerEmailSubscribers(dbSession))
      .thenReturn(Set.of());

    Assertions.assertThat(underTest.toEmailDeliveryRequests(List.of(new MQRAndStandardModesExistNotification(true))))
      .isEmpty();
  }

}
