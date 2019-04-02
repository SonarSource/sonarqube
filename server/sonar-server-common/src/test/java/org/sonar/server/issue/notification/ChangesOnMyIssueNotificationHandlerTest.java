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
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

@RunWith(DataProviderRunner.class)
public class ChangesOnMyIssueNotificationHandlerTest {
  private static final String CHANGE_ON_MY_ISSUES_DISPATCHER_KEY = "ChangesOnMyIssue";
  private static final String NO_CHANGE_AUTHOR = null;

  private NotificationManager notificationManager = mock(NotificationManager.class);
  private EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);
  private ChangesOnMyIssueNotificationHandler underTest = new ChangesOnMyIssueNotificationHandler(notificationManager, emailNotificationChannel);

  @Test
  public void getMetadata_returns_same_instance_as_static_method() {
    assertThat(underTest.getMetadata().get()).isSameAs(ChangesOnMyIssueNotificationHandler.newMetadata());
  }

  @Test
  public void verify_changeOnMyIssues_notification_dispatcher_key() {
    NotificationDispatcherMetadata metadata = ChangesOnMyIssueNotificationHandler.newMetadata();

    assertThat(metadata.getDispatcherKey()).isEqualTo(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY);
  }

  @Test
  public void changeOnMyIssues_notification_is_enable_at_global_level() {
    NotificationDispatcherMetadata metadata = ChangesOnMyIssueNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(GLOBAL_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void changeOnMyIssues_notification_is_enable_at_project_level() {
    NotificationDispatcherMetadata metadata = ChangesOnMyIssueNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void getNotificationClass_is_IssueChangeNotification() {
    assertThat(underTest.getNotificationClass()).isEqualTo(IssueChangeNotification.class);
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
  public void deliver_has_no_effect_if_no_notification_has_assignee() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<IssueChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> newNotification(randomAlphabetic(5 + i), null, NO_CHANGE_AUTHOR))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    notifications.forEach(notification -> {
      verify(notification).getProjectKey();
      verify(notification).getAssignee();
      verifyNoMoreInteractions(notification);
    });
  }

  @Test
  public void deliver_has_no_effect_if_no_notification_has_change_author_different_from_assignee() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<IssueChangeNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> {
        String assignee = randomAlphabetic(4 + i);
        return newNotification(randomAlphabetic(5 + i), assignee, assignee);
      })
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    notifications.forEach(notification -> {
      verify(notification).getProjectKey();
      verify(notification, times(2)).getAssignee();
      verify(notification).getChangeAuthor();
      verifyNoMoreInteractions(notification);
    });
  }

  @Test
  @UseDataProvider("noOrDifferentChangeAuthor")
  public void deliver_checks_by_projectKey_if_notifications_have_subscribed_assignee_to_ChangesOnMyIssues_notifications(@Nullable String noOrDifferentChangeAuthor) {
    String projectKey1 = randomAlphabetic(10);
    String assignee1 = randomAlphabetic(11);
    String projectKey2 = randomAlphabetic(12);
    String assignee2 = randomAlphabetic(13);
    Set<IssueChangeNotification> notifications1 = randomSetOfNotifications(projectKey1, assignee1, noOrDifferentChangeAuthor);
    Set<IssueChangeNotification> notifications2 = randomSetOfNotifications(projectKey2, assignee2, noOrDifferentChangeAuthor);
    when(emailNotificationChannel.isActivated()).thenReturn(true);

    int deliver = underTest.deliver(Stream.concat(notifications1.stream(), notifications2.stream()).collect(toSet()));

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, projectKey1, singleton(assignee1), ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, projectKey2, singleton(assignee2), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  @UseDataProvider("noOrDifferentChangeAuthor")
  public void deliver_ignores_notifications_which_assignee_has_not_subscribed_to_ChangesOnMyIssues_notifications(@Nullable String noOrDifferentChangeAuthor) {
    String projectKey = randomAlphabetic(5);
    String assignee1 = randomAlphabetic(6);
    String assignee2 = randomAlphabetic(7);
    // assignee1 is not authorized
    Set<IssueChangeNotification> assignee1Notifications = randomSetOfNotifications(projectKey, assignee1, noOrDifferentChangeAuthor);
    // assignee2 is authorized
    Set<IssueChangeNotification> assignee2Notifications = randomSetOfNotifications(projectKey, assignee2, noOrDifferentChangeAuthor);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(notificationManager.findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, projectKey, ImmutableSet.of(assignee1, assignee2), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(ImmutableSet.of(emailRecipientOf(assignee2)));
    Set<EmailDeliveryRequest> expectedRequests = assignee2Notifications.stream()
      .map(t -> new EmailDeliveryRequest(emailOf(t.getAssignee()), t))
      .collect(toSet());
    int deliveredCount = new Random().nextInt(expectedRequests.size());
    when(emailNotificationChannel.deliver(expectedRequests)).thenReturn(deliveredCount);

    int deliver = underTest.deliver(Stream.concat(assignee1Notifications.stream(), assignee2Notifications.stream()).collect(toSet()));

    assertThat(deliver).isEqualTo(deliveredCount);
    verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, projectKey, ImmutableSet.of(assignee1, assignee2), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliver(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_ignores_notifications_which_assignee_is_the_changeAuthor() {
    String projectKey = randomAlphabetic(5);
    String assignee1 = randomAlphabetic(6);
    String assignee2 = randomAlphabetic(7);
    String assignee3 = randomAlphabetic(8);
    // assignee1 is the changeAuthor of every notification he's the assignee of
    Set<IssueChangeNotification> assignee1ChangeAuthor = randomSetOfNotifications(projectKey, assignee1, assignee1);
    // assignee2 is the changeAuthor of some notification he's the assignee of
    Set<IssueChangeNotification> assignee2ChangeAuthor = randomSetOfNotifications(projectKey, assignee2, assignee2);
    Set<IssueChangeNotification> assignee2NotChangeAuthor = randomSetOfNotifications(projectKey, assignee2, randomAlphabetic(10));
    Set<IssueChangeNotification> assignee2NoChangeAuthor = randomSetOfNotifications(projectKey, assignee2, NO_CHANGE_AUTHOR);
    // assignee3 is never the changeAuthor of the notification he's the assignee of
    Set<IssueChangeNotification> assignee3NotChangeAuthor = randomSetOfNotifications(projectKey, assignee3, randomAlphabetic(11));
    Set<IssueChangeNotification> assignee3NoChangeAuthor = randomSetOfNotifications(projectKey, assignee3, NO_CHANGE_AUTHOR);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    // assignees which are not changeAuthor have subscribed
    Set<String> assigneesChangeAuthor = ImmutableSet.of(assignee2, assignee3);
    when(notificationManager.findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, projectKey, assigneesChangeAuthor, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(ImmutableSet.of(emailRecipientOf(assignee2), emailRecipientOf(assignee3)));
    Set<EmailDeliveryRequest> expectedRequests = Stream.of(
      assignee2NotChangeAuthor.stream(), assignee2NoChangeAuthor.stream(),
      assignee3NotChangeAuthor.stream(), assignee3NoChangeAuthor.stream())
      .flatMap(t -> t)
      .map(t -> new EmailDeliveryRequest(emailOf(t.getAssignee()), t))
      .collect(toSet());
    int deliveredCount = new Random().nextInt(expectedRequests.size());
    when(emailNotificationChannel.deliver(expectedRequests)).thenReturn(deliveredCount);

    Set<IssueChangeNotification> notifications = Stream.of(
      assignee1ChangeAuthor.stream(),
      assignee2ChangeAuthor.stream(), assignee2NotChangeAuthor.stream(), assignee2NoChangeAuthor.stream(),
      assignee3NotChangeAuthor.stream(), assignee3NoChangeAuthor.stream()).flatMap(t -> t)
      .collect(toSet());
    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isEqualTo(deliveredCount);
    verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, projectKey, assigneesChangeAuthor, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliver(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @DataProvider
  public static Object[][] noOrDifferentChangeAuthor() {
    return new Object[][] {
      {NO_CHANGE_AUTHOR},
      {randomAlphabetic(15)}
    };
  }

  private static Set<IssueChangeNotification> randomSetOfNotifications(@Nullable String projectKey, @Nullable String assignee, @Nullable String changeAuthor) {
    return IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(projectKey, assignee, changeAuthor))
      .collect(Collectors.toSet());
  }

  private static IssueChangeNotification newNotification(@Nullable String projectKey, @Nullable String assignee, @Nullable String changeAuthor) {
    IssueChangeNotification notification = mock(IssueChangeNotification.class);
    when(notification.getProjectKey()).thenReturn(projectKey);
    when(notification.getAssignee()).thenReturn(assignee);
    when(notification.getChangeAuthor()).thenReturn(changeAuthor);
    return notification;
  }

  private static NotificationManager.EmailRecipient emailRecipientOf(String login) {
    return new NotificationManager.EmailRecipient(login, emailOf(login));
  }

  private static String emailOf(String login) {
    return login + "@plouf";
  }

}
