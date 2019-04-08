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
import org.sonar.server.notification.NotificationManager.EmailRecipient;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static com.google.common.collect.ImmutableSet.of;
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

public class MyNewIssuesNotificationHandlerTest {
  private static final String MY_NEW_ISSUES_DISPATCHER_KEY = "SQ-MyNewIssues";
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);

  private MyNewIssuesNotificationHandler underTest = new MyNewIssuesNotificationHandler(notificationManager, emailNotificationChannel);

  @Test
  public void getMetadata_returns_same_instance_as_static_method() {
    assertThat(underTest.getMetadata().get()).isSameAs(MyNewIssuesNotificationHandler.newMetadata());
  }

  @Test
  public void verify_myNewIssues_notification_dispatcher_key() {
    NotificationDispatcherMetadata metadata = MyNewIssuesNotificationHandler.newMetadata();

    assertThat(metadata.getDispatcherKey()).isEqualTo(MY_NEW_ISSUES_DISPATCHER_KEY);
  }

  @Test
  public void myNewIssues_notification_is_enable_at_global_level() {
    NotificationDispatcherMetadata metadata = MyNewIssuesNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(GLOBAL_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void myNewIssues_notification_is_enable_at_project_level() {
    NotificationDispatcherMetadata metadata = MyNewIssuesNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void getNotificationClass_is_MyNewIssuesNotification() {
    assertThat(underTest.getNotificationClass()).isEqualTo(MyNewIssuesNotification.class);
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
    Set<MyNewIssuesNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> mock(MyNewIssuesNotification.class))
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
    Set<MyNewIssuesNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> newNotification(null, null))
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
    Set<MyNewIssuesNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> newNotification(randomAlphabetic(5 + i), null))
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
  public void deliver_has_no_effect_if_no_notification_has_subscribed_assignee_to_MyNewIssue_notifications() {
    String projectKey = randomAlphabetic(12);
    String assignee = randomAlphabetic(10);
    MyNewIssuesNotification notification = newNotification(projectKey, assignee);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey, of(assignee), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emptySet());

    int deliver = underTest.deliver(Collections.singleton(notification));

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey, of(assignee), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_ignores_notification_without_projectKey() {
    String projectKey = randomAlphabetic(10);
    Set<MyNewIssuesNotification> withProjectKey = IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(projectKey, randomAlphabetic(11 + i)))
      .collect(toSet());
    Set<MyNewIssuesNotification> noProjectKey = IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(null, randomAlphabetic(11 + i)))
      .collect(toSet());
    Set<MyNewIssuesNotification> noProjectKeyNoAssignee = randomSetOfNotifications(null, null);
    Set<EmailRecipient> authorizedRecipients = withProjectKey.stream()
      .map(n -> new EmailRecipient(n.getAssignee(), n.getAssignee() + "@foo"))
      .collect(toSet());
    Set<EmailDeliveryRequest> expectedRequests = withProjectKey.stream()
      .map(n -> new EmailDeliveryRequest(n.getAssignee() + "@foo", n))
      .collect(toSet());
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<String> assignees = withProjectKey.stream().map(MyNewIssuesNotification::getAssignee).collect(toSet());
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey, assignees, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(authorizedRecipients);

    Set<MyNewIssuesNotification> notifications = Stream.of(withProjectKey.stream(), noProjectKey.stream(), noProjectKeyNoAssignee.stream())
      .flatMap(t -> t)
      .collect(toSet());
    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey, assignees, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_ignores_notification_without_assignee() {
    String projectKey = randomAlphabetic(10);
    Set<MyNewIssuesNotification> withAssignee = IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(projectKey, randomAlphabetic(11 + i)))
      .collect(toSet());
    Set<MyNewIssuesNotification> noAssignee = randomSetOfNotifications(projectKey, null);
    Set<MyNewIssuesNotification> noProjectKeyNoAssignee = randomSetOfNotifications(null, null);
    Set<EmailRecipient> authorizedRecipients = withAssignee.stream()
      .map(n -> new EmailRecipient(n.getAssignee(), n.getAssignee() + "@foo"))
      .collect(toSet());
    Set<EmailDeliveryRequest> expectedRequests = withAssignee.stream()
      .map(n -> new EmailDeliveryRequest(n.getAssignee() + "@foo", n))
      .collect(toSet());
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<String> assignees = withAssignee.stream().map(MyNewIssuesNotification::getAssignee).collect(toSet());
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey, assignees, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(authorizedRecipients);

    Set<MyNewIssuesNotification> notifications = Stream.of(withAssignee.stream(), noAssignee.stream(), noProjectKeyNoAssignee.stream())
      .flatMap(t -> t)
      .collect(toSet());
    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey, assignees, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_checks_by_projectKey_if_notifications_have_subscribed_assignee_to_MyNewIssue_notifications() {
    String projectKey1 = randomAlphabetic(10);
    String assignee1 = randomAlphabetic(11);
    String projectKey2 = randomAlphabetic(12);
    String assignee2 = randomAlphabetic(13);
    Set<MyNewIssuesNotification> notifications1 = randomSetOfNotifications(projectKey1, assignee1);
    Set<MyNewIssuesNotification> notifications2 = randomSetOfNotifications(projectKey2, assignee2);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey1, of(assignee1), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emptySet());
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey2, of(assignee2),ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emptySet());

    int deliver = underTest.deliver(Stream.concat(notifications1.stream(), notifications2.stream()).collect(toSet()));

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey1, of(assignee1), ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey2, of(assignee2), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_ignores_notifications_which_assignee_has_no_subscribed_to_MyNewIssue_notifications() {
    String projectKey = randomAlphabetic(5);
    String assignee1 = randomAlphabetic(6);
    String assignee2 = randomAlphabetic(7);
    Set<String> assignees = of(assignee1, assignee2);
    // assignee1 is not authorized
    Set<MyNewIssuesNotification> assignee1Notifications = randomSetOfNotifications(projectKey, assignee1);
    // assignee2 is authorized
    Set<MyNewIssuesNotification> assignee2Notifications = randomSetOfNotifications(projectKey, assignee2);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey, assignees, ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(of(emailRecipientOf(assignee2)));
    Set<EmailDeliveryRequest> expectedRequests = assignee2Notifications.stream()
      .map(t -> new EmailDeliveryRequest(emailOf(t.getAssignee()), t))
      .collect(toSet());
    int deliveredCount = new Random().nextInt(expectedRequests.size());
    when(emailNotificationChannel.deliverAll(expectedRequests)).thenReturn(deliveredCount);

    int deliver = underTest.deliver(Stream.concat(assignee1Notifications.stream(), assignee2Notifications.stream()).collect(toSet()));

    assertThat(deliver).isEqualTo(deliveredCount);
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey, assignees, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_returns_sum_of_delivery_counts_when_multiple_projects() {
    String projectKey1 = randomAlphabetic(5);
    String projectKey2 = randomAlphabetic(6);
    String projectKey3 = randomAlphabetic(7);
    String assignee1 = randomAlphabetic(8);
    String assignee2 = randomAlphabetic(9);
    String assignee3 = randomAlphabetic(10);
    // assignee1 has subscribed to project1 only, no notification on project3
    Set<MyNewIssuesNotification> assignee1Project1 = randomSetOfNotifications(projectKey1, assignee1);
    Set<MyNewIssuesNotification> assignee1Project2 = randomSetOfNotifications(projectKey2, assignee1);
    // assignee2 is subscribed to project1 and project2, notifications on all projects
    Set<MyNewIssuesNotification> assignee2Project1 = randomSetOfNotifications(projectKey1, assignee2);
    Set<MyNewIssuesNotification> assignee2Project2 = randomSetOfNotifications(projectKey2, assignee2);
    Set<MyNewIssuesNotification> assignee2Project3 = randomSetOfNotifications(projectKey3, assignee2);
    // assignee3 is subscribed to project2 only, no notification on project1
    Set<MyNewIssuesNotification> assignee3Project2 = randomSetOfNotifications(projectKey2, assignee3);
    Set<MyNewIssuesNotification> assignee3Project3 = randomSetOfNotifications(projectKey3, assignee3);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey1, of(assignee1, assignee2), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(of(emailRecipientOf(assignee1), emailRecipientOf(assignee2)));
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey2, of(assignee1, assignee2, assignee3), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(of(emailRecipientOf(assignee2), emailRecipientOf(assignee3)));
    when(notificationManager.findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey3, of(assignee2, assignee3), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(emptySet());
    Set<EmailDeliveryRequest> expectedRequests = Stream.of(
      assignee1Project1.stream(), assignee2Project1.stream(), assignee2Project2.stream(), assignee3Project2.stream())
      .flatMap(t -> t)
      .map(t -> new EmailDeliveryRequest(emailOf(t.getAssignee()), t))
      .collect(toSet());
    int deliveredCount = new Random().nextInt(expectedRequests.size());
    when(emailNotificationChannel.deliverAll(expectedRequests)).thenReturn(deliveredCount);

    Set<MyNewIssuesNotification> notifications = Stream.of(
      assignee1Project1.stream(), assignee1Project2.stream(),
      assignee2Project1.stream(), assignee2Project2.stream(),
      assignee2Project3.stream(), assignee3Project2.stream(), assignee3Project3.stream())
      .flatMap(t -> t)
      .collect(toSet());
    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isEqualTo(deliveredCount);
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey1, of(assignee1, assignee2), ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey2, of(assignee1, assignee2, assignee3), ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(MY_NEW_ISSUES_DISPATCHER_KEY, projectKey3, of(assignee2, assignee3), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(expectedRequests);
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  private static Set<MyNewIssuesNotification> randomSetOfNotifications(@Nullable String projectKey, @Nullable String assignee) {
    return IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(projectKey, assignee))
      .collect(Collectors.toSet());
  }

  private static MyNewIssuesNotification newNotification(@Nullable String projectKey, @Nullable String assignee) {
    MyNewIssuesNotification notification = mock(MyNewIssuesNotification.class);
    when(notification.getProjectKey()).thenReturn(projectKey);
    when(notification.getAssignee()).thenReturn(assignee);
    return notification;
  }

  private static EmailRecipient emailRecipientOf(String assignee1) {
    return new EmailRecipient(assignee1, emailOf(assignee1));
  }

  private static String emailOf(String assignee1) {
    return assignee1 + "@bar";
  }
}
