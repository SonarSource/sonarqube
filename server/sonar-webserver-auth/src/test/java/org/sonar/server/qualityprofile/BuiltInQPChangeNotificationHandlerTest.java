/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.qualityprofile;

import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class BuiltInQPChangeNotificationHandlerTest {
  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);
  private AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
  private EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);

  private BuiltInQPChangeNotificationHandler underTest = new BuiltInQPChangeNotificationHandler(dbClient, emailNotificationChannel);

  @Before
  public void wire_mocks() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.authorizationDao()).thenReturn(authorizationDao);
  }

  @Test
  public void getMetadata_returns_empty() {
    assertThat(underTest.getMetadata()).isEmpty();
  }

  @Test
  public void getNotificationClass_is_BuiltInQPChangeNotification() {
    assertThat(underTest.getNotificationClass()).isEqualTo(BuiltInQPChangeNotification.class);
  }

  @Test
  public void deliver_has_no_effect_if_emailNotificationChannel_is_disabled() {
    when(emailNotificationChannel.isActivated()).thenReturn(false);
    Set<BuiltInQPChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> mock(BuiltInQPChangeNotification.class))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    verifyZeroInteractions(dbClient);
    notifications.forEach(Mockito::verifyZeroInteractions);
  }

  @Test
  public void deliver_has_no_effect_if_there_is_no_global_administer_email_subscriber() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<BuiltInQPChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> mock(BuiltInQPChangeNotification.class))
      .collect(toSet());
    when(authorizationDao.selectQualityProfileAdministratorLogins(dbSession))
      .thenReturn(emptySet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    verify(dbClient).openSession(false);
    verify(dbClient).authorizationDao();
    verifyNoMoreInteractions(dbClient);
    verify(authorizationDao).selectQualityProfileAdministratorLogins(dbSession);
    verifyNoMoreInteractions(authorizationDao);
    notifications.forEach(Mockito::verifyZeroInteractions);
  }

  @Test
  public void deliver_create_emailRequest_for_each_notification_and_for_each_global_administer_email_subscriber() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<BuiltInQPChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> mock(BuiltInQPChangeNotification.class))
      .collect(toSet());
    Set<EmailSubscriberDto> emailSubscribers = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> EmailSubscriberDto.create("login_" + i, true, "login_" + i + "@foo"))
      .collect(toSet());
    when(authorizationDao.selectQualityProfileAdministratorLogins(dbSession))
      .thenReturn(emailSubscribers);
    Set<EmailNotificationChannel.EmailDeliveryRequest> expectedRequests = notifications.stream()
      .flatMap(notification -> emailSubscribers.stream().map(subscriber -> new EmailNotificationChannel.EmailDeliveryRequest(subscriber.getEmail(), notification)))
      .collect(toSet());
    int deliveries = new Random().nextInt(expectedRequests.size());
    when(emailNotificationChannel.deliverAll(expectedRequests)).thenReturn(deliveries);

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isEqualTo(deliveries);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
    verify(dbClient).openSession(false);
    verify(dbClient).authorizationDao();
    verifyNoMoreInteractions(dbClient);
    verify(authorizationDao).selectQualityProfileAdministratorLogins(dbSession);
    verifyNoMoreInteractions(authorizationDao);
    notifications.forEach(Mockito::verifyZeroInteractions);
  }

}
