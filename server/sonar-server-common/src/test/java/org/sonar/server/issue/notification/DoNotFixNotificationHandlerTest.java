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
package org.sonar.server.issue.notification;

import com.google.common.collect.ImmutableSet;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sonar.api.issue.Issue;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

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

@RunWith(DataProviderRunner.class)
public class DoNotFixNotificationHandlerTest {
  private static final String DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY = "NewFalsePositiveIssue";
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);
  private DoNotFixNotificationHandler underTest = new DoNotFixNotificationHandler(notificationManager, emailNotificationChannel);

  @Test
  public void getMetadata_returns_same_instance_as_static_method() {
    assertThat(underTest.getMetadata().get()).isSameAs(DoNotFixNotificationHandler.newMetadata());
  }

  @Test
  public void verify_changeOnMyIssues_notification_dispatcher_key() {
    NotificationDispatcherMetadata metadata = DoNotFixNotificationHandler.newMetadata();

    assertThat(metadata.getDispatcherKey()).isEqualTo(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY);
  }

  @Test
  public void changeOnMyIssues_notification_is_disabled_at_global_level() {
    NotificationDispatcherMetadata metadata = DoNotFixNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(GLOBAL_NOTIFICATION)).isEqualTo("false");
  }

  @Test
  public void changeOnMyIssues_notification_is_enable_at_project_level() {
    NotificationDispatcherMetadata metadata = DoNotFixNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void getNotificationClass_is_IssueChangeNotification() {
    assertThat(underTest.getNotificationClass()).isEqualTo(IssueChangeNotification.class);
  }

  @Test
  public void deliver_has_no_effect_if_emailNotificationChannel_is_disabled() {
    when(emailNotificationChannel.isActivated()).thenReturn(false);
    Set<IssueChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> mock(IssueChangeNotification.class))
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
    Set<IssueChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> newNotification(null, null, null))
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
  public void deliver_has_no_effect_if_no_notification_has_change_author() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<IssueChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> newNotification(randomAlphabetic(5 + i), null, null))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    notifications.forEach(notification -> {
      verify(notification).getProjectKey();
      verify(notification).getChangeAuthor();
      verifyNoMoreInteractions(notification);
    });
  }

  @Test
  public void deliver_has_no_effect_if_no_notification_has_new_resolution() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<IssueChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> newNotification(randomAlphabetic(5 + i), randomAlphabetic(4 + i), null))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    notifications.forEach(notification -> {
      verify(notification).getProjectKey();
      verify(notification).getChangeAuthor();
      verify(notification).getNewResolution();
      verifyNoMoreInteractions(notification);
    });
  }

  @Test
  @UseDataProvider("notFPorWontFixResolution")
  public void deliver_has_no_effect_if_no_notification_has_FP_or_wont_fix_resolution(String newResolution) {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<IssueChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> newNotification(randomAlphabetic(5 + i), randomAlphabetic(4 + i), newResolution))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    notifications.forEach(notification -> {
      verify(notification).getProjectKey();
      verify(notification).getChangeAuthor();
      verify(notification).getNewResolution();
      verifyNoMoreInteractions(notification);
    });
  }

  @DataProvider
  public static Object[][] notFPorWontFixResolution() {
    return new Object[][] {
      {""},
      {randomAlphabetic(9)},
      {Issue.RESOLUTION_FIXED},
      {Issue.RESOLUTION_REMOVED}
    };
  }

  @Test
  @UseDataProvider("FPorWontFixResolution")
  public void deliver_checks_by_projectKey_if_notifications_have_subscribed_assignee_to_FPorWontFix_notifications(String newResolution) {
    String projectKey1 = randomAlphabetic(10);
    String changeAuthor1 = randomAlphabetic(11);
    String projectKey2 = randomAlphabetic(12);
    String changeAuthor2 = randomAlphabetic(13);
    Set<IssueChangeNotification> notifications1 = randomSetOfNotifications(projectKey1, changeAuthor1, newResolution);
    Set<IssueChangeNotification> notifications2 = randomSetOfNotifications(projectKey2, changeAuthor2, newResolution);
    when(emailNotificationChannel.isActivated()).thenReturn(true);

    int deliver = underTest.deliver(Stream.concat(notifications1.stream(), notifications2.stream()).collect(toSet()));

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, projectKey1, ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, projectKey2, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  @UseDataProvider("FPorWontFixResolution")
  public void deliver_does_not_send_email_request_for_notifications_a_subscriber_is_the_changeAuthor_of(String newResolution) {
    String projectKey = randomAlphabetic(5);
    String subscriber1 = randomAlphabetic(6);
    String subscriber2 = randomAlphabetic(7);
    String subscriber3 = randomAlphabetic(8);
    String otherChangeAuthor = randomAlphabetic(9);
    // subscriber1 is the changeAuthor of some notifications
    Set<IssueChangeNotification> subscriber1Notifications = randomSetOfNotifications(projectKey, subscriber1, newResolution);
    // subscriber2 is the changeAuthor of some notifications
    Set<IssueChangeNotification> subscriber2Notifications = randomSetOfNotifications(projectKey, subscriber2, newResolution);
    // subscriber3 has no notification
    Set<IssueChangeNotification> otherChangeAuthorNotifications = randomSetOfNotifications(projectKey, otherChangeAuthor, newResolution);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<String> subscribers = ImmutableSet.of(subscriber1, subscriber2, subscriber3);
    when(notificationManager.findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, projectKey, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(subscribers.stream().map(DoNotFixNotificationHandlerTest::emailRecipientOf).collect(toSet()));
    Set<EmailDeliveryRequest> expectedRequests = Stream.of(
      subscriber1Notifications.stream().flatMap(notif -> Stream.of(subscriber2, subscriber3).map(login -> new EmailDeliveryRequest(emailOf(login), notif))),
      subscriber2Notifications.stream().flatMap(notif -> Stream.of(subscriber1, subscriber3).map(login -> new EmailDeliveryRequest(emailOf(login), notif))),
      otherChangeAuthorNotifications.stream().flatMap(notif -> Stream.of(subscriber1, subscriber2, subscriber3).map(login -> new EmailDeliveryRequest(emailOf(login), notif))))
      .flatMap(t -> t)
      .collect(toSet());
    int deliveredCount = new Random().nextInt(expectedRequests.size());
    when(emailNotificationChannel.deliverAll(expectedRequests)).thenReturn(deliveredCount);

    Set<IssueChangeNotification> notifications = Stream.of(
      subscriber1Notifications.stream(),
      subscriber2Notifications.stream(),
      otherChangeAuthorNotifications.stream())
      .flatMap(t -> t)
      .collect(toSet());
    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isEqualTo(deliveredCount);
    verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, projectKey, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @DataProvider
  public static Object[][] FPorWontFixResolution() {
    return new Object[][] {
      {Issue.RESOLUTION_FALSE_POSITIVE},
      {Issue.RESOLUTION_WONT_FIX}
    };
  }

  private static Set<IssueChangeNotification> randomSetOfNotifications(@Nullable String projectKey, @Nullable String changeAuthor, @Nullable String newResolution) {
    return IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(projectKey, changeAuthor, newResolution))
      .collect(Collectors.toSet());
  }

  private static IssueChangeNotification newNotification(@Nullable String projectKey, @Nullable String changeAuthor, @Nullable String newResolution) {
    IssueChangeNotification notification = mock(IssueChangeNotification.class);
    when(notification.getProjectKey()).thenReturn(projectKey);
    when(notification.getChangeAuthor()).thenReturn(changeAuthor);
    when(notification.getNewResolution()).thenReturn(newResolution);
    return notification;
  }

  private static NotificationManager.EmailRecipient emailRecipientOf(String assignee1) {
    return new NotificationManager.EmailRecipient(assignee1, emailOf(assignee1));
  }

  private static String emailOf(String assignee1) {
    return assignee1 + "@baffe";
  }

}
