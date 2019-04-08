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
package org.sonar.server.qualitygate.notification;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

public class QGChangeNotificationHandlerTest {
  private static final String QG_CHANGE_DISPATCHER_KEY = "NewAlerts";
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);
  private QGChangeNotificationHandler underTest = new QGChangeNotificationHandler(notificationManager, emailNotificationChannel);

  @Test
  public void getMetadata_returns_same_instance_as_static_method() {
    assertThat(underTest.getMetadata().get()).isSameAs(QGChangeNotificationHandler.newMetadata());
  }

  @Test
  public void verify_qgChange_notification_dispatcher_key() {
    NotificationDispatcherMetadata metadata = QGChangeNotificationHandler.newMetadata();

    assertThat(metadata.getDispatcherKey()).isEqualTo(QG_CHANGE_DISPATCHER_KEY);
  }

  @Test
  public void qgChange_notification_is_enable_at_global_level() {
    NotificationDispatcherMetadata metadata = QGChangeNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(GLOBAL_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void qgChange_notification_is_enable_at_project_level() {
    NotificationDispatcherMetadata metadata = QGChangeNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void getNotificationClass_is_QGChangeNotification() {
    assertThat(underTest.getNotificationClass()).isEqualTo(QGChangeNotification.class);
  }

  @Test
  public void deliver_has_no_effect_if_notifications_is_empty() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    int deliver = underTest.deliver(Collections.emptyList());

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager, emailNotificationChannel);
  }

  @Test
  public void deliver_has_no_effect_if_emailNotificationChannel_is_disabled() {
    when(emailNotificationChannel.isActivated()).thenReturn(false);
    Set<QGChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> mock(QGChangeNotification.class))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    notifications.forEach(Mockito::verifyZeroInteractions);
  }

  @Test
  public void deliver_has_no_effect_if_no_notification_has_projectKey() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<QGChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> newNotification(null))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    notifications.forEach(notification -> {
      verify(notification).getProjectKey();
      verifyNoMoreInteractions(notification);
    });
  }

  @Test
  public void deliver_has_no_effect_if_no_notification_has_subscribed_recipients_to_QGChange_notifications() {
    String projectKey = randomAlphabetic(12);
    QGChangeNotification notification = newNotification(projectKey);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(notificationManager.findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emptySet());

    int deliver = underTest.deliver(Collections.singleton(notification));

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_ignores_notification_without_projectKey() {
    String projectKey = randomAlphabetic(10);
    Set<QGChangeNotification> withProjectKey = IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(projectKey))
      .collect(toSet());
    Set<QGChangeNotification> noProjectKey = IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(null))
      .collect(toSet());
    Set<NotificationManager.EmailRecipient> emailRecipients = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> "user_" + i)
      .map(login -> new NotificationManager.EmailRecipient(login, emailOf(login)))
      .collect(toSet());
    Set<EmailNotificationChannel.EmailDeliveryRequest> expectedRequests = emailRecipients.stream()
      .flatMap(emailRecipient -> withProjectKey.stream().map(notif -> new EmailNotificationChannel.EmailDeliveryRequest(emailRecipient.getEmail(), notif)))
      .collect(toSet());
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(notificationManager.findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emailRecipients);

    Set<QGChangeNotification> notifications = Stream.of(withProjectKey.stream(), noProjectKey.stream())
      .flatMap(t -> t)
      .collect(toSet());
    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_checks_by_projectKey_if_notifications_have_subscribed_assignee_to_QGChange_notifications() {
    String projectKey1 = randomAlphabetic(10);
    String projectKey2 = randomAlphabetic(11);
    Set<QGChangeNotification> notifications1 = randomSetOfNotifications(projectKey1);
    Set<QGChangeNotification> notifications2 = randomSetOfNotifications(projectKey2);
    when(emailNotificationChannel.isActivated()).thenReturn(true);

    Set<NotificationManager.EmailRecipient> emailRecipients1 = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> "user1_" + i)
      .map(login -> new NotificationManager.EmailRecipient(login, emailOf(login)))
      .collect(toSet());
    Set<NotificationManager.EmailRecipient> emailRecipients2 = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> "user2_" + i)
      .map(login -> new NotificationManager.EmailRecipient(login, emailOf(login)))
      .collect(toSet());
    when(notificationManager.findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey1, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emailRecipients1);
    when(notificationManager.findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey2, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emailRecipients2);
    Set<EmailNotificationChannel.EmailDeliveryRequest> expectedRequests = Stream.concat(
      emailRecipients1.stream()
        .flatMap(emailRecipient -> notifications1.stream().map(notif -> new EmailNotificationChannel.EmailDeliveryRequest(emailRecipient.getEmail(), notif))),
      emailRecipients2.stream()
        .flatMap(emailRecipient -> notifications2.stream().map(notif -> new EmailNotificationChannel.EmailDeliveryRequest(emailRecipient.getEmail(), notif))))
      .collect(toSet());

    int deliver = underTest.deliver(Stream.concat(notifications1.stream(), notifications2.stream()).collect(toSet()));

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey1, ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey2, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_send_notifications_to_all_subscribers_of_all_projects() {
    String projectKey1 = randomAlphabetic(10);
    String projectKey2 = randomAlphabetic(11);
    Set<QGChangeNotification> notifications1 = randomSetOfNotifications(projectKey1);
    Set<QGChangeNotification> notifications2 = randomSetOfNotifications(projectKey2);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(notificationManager.findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey1, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emptySet());
    when(notificationManager.findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey2, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emptySet());

    int deliver = underTest.deliver(Stream.concat(notifications1.stream(), notifications2.stream()).collect(toSet()));

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey1, ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(QG_CHANGE_DISPATCHER_KEY, projectKey2, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  private static Set<QGChangeNotification> randomSetOfNotifications(@Nullable String projectKey) {
    return IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(projectKey))
      .collect(Collectors.toSet());
  }

  private static QGChangeNotification newNotification(@Nullable String projectKey) {
    QGChangeNotification notification = mock(QGChangeNotification.class);
    when(notification.getProjectKey()).thenReturn(projectKey);
    return notification;
  }

  private static String emailOf(String assignee1) {
    return assignee1 + "@giraffe";
  }

}
