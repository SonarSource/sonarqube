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
import com.google.common.collect.ListMultimap;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Change;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Project;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Rule;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.UserChange;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.server.issue.notification.IssuesChangesNotificationBuilderTesting.newProject;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

@RunWith(DataProviderRunner.class)
public class FPOrWontFixNotificationHandlerTest {
  private static final String DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY = "NewFalsePositiveIssue";
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);
  private IssuesChangesNotificationSerializer serializerMock = mock(IssuesChangesNotificationSerializer.class);
  private IssuesChangesNotificationSerializer serializer = spy(new IssuesChangesNotificationSerializer());
  private Class<Set<EmailDeliveryRequest>> requestSetType = (Class<Set<EmailDeliveryRequest>>) (Class<?>) Set.class;
  private FPOrWontFixNotificationHandler underTest = new FPOrWontFixNotificationHandler(notificationManager, emailNotificationChannel, serializer);

  @Test
  public void getMetadata_returns_same_instance_as_static_method() {
    assertThat(underTest.getMetadata().get()).isSameAs(FPOrWontFixNotificationHandler.newMetadata());
  }

  @Test
  public void verify_fpOrWontFixIssues_notification_dispatcher_key() {
    NotificationDispatcherMetadata metadata = FPOrWontFixNotificationHandler.newMetadata();

    assertThat(metadata.getDispatcherKey()).isEqualTo(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY);
  }

  @Test
  public void fpOrWontFixIssues_notification_is_disabled_at_global_level() {
    NotificationDispatcherMetadata metadata = FPOrWontFixNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(GLOBAL_NOTIFICATION)).isEqualTo("false");
  }

  @Test
  public void fpOrWontFixIssues_notification_is_enable_at_project_level() {
    NotificationDispatcherMetadata metadata = FPOrWontFixNotificationHandler.newMetadata();

    assertThat(metadata.getProperty(PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void getNotificationClass_is_IssueChangeNotification() {
    assertThat(underTest.getNotificationClass()).isEqualTo(IssuesChangesNotification.class);
  }

  @Test
  public void deliver_has_no_effect_if_emailNotificationChannel_is_disabled() {
    when(emailNotificationChannel.isActivated()).thenReturn(false);
    Set<IssuesChangesNotification> notifications = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> mock(IssuesChangesNotification.class))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    notifications.forEach(Mockito::verifyZeroInteractions);
  }

  @Test
  public void deliver_parses_every_notification_in_order() {
    Set<IssuesChangesNotification> notifications = IntStream.range(0, 5 + new Random().nextInt(10))
      .mapToObj(i -> mock(IssuesChangesNotification.class))
      .collect(toSet());
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    when(serializerMock.from(any(IssuesChangesNotification.class))).thenReturn(mock(IssuesChangesNotificationBuilder.class));
    FPOrWontFixNotificationHandler underTest = new FPOrWontFixNotificationHandler(notificationManager, emailNotificationChannel, serializerMock);

    underTest.deliver(notifications);

    notifications.forEach(notification -> verify(serializerMock).from(notification));
  }

  @Test
  public void deliver_fails_with_IAE_if_serializer_throws_IAE() {
    Set<IssuesChangesNotification> notifications = IntStream.range(0, 3 + new Random().nextInt(10))
      .mapToObj(i -> mock(IssuesChangesNotification.class))
      .collect(toSet());
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    IllegalArgumentException expected = new IllegalArgumentException("faking serializer#from throwing a IllegalArgumentException");
    when(serializerMock.from(any(IssuesChangesNotification.class)))
      .thenReturn(mock(IssuesChangesNotificationBuilder.class))
      .thenReturn(mock(IssuesChangesNotificationBuilder.class))
      .thenThrow(expected);
    FPOrWontFixNotificationHandler underTest = new FPOrWontFixNotificationHandler(notificationManager, emailNotificationChannel, serializerMock);

    try {
      underTest.deliver(notifications);
      fail("should have throws IAE");
    } catch (IllegalArgumentException e) {
      verify(serializerMock, times(3)).from(any(IssuesChangesNotification.class));
      assertThat(e).isSameAs(expected);
    }
  }

  @Test
  public void deliver_has_no_effect_if_no_issue_has_new_resolution() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Change changeMock = mock(Change.class);
    Set<IssuesChangesNotification> notifications = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(randomIssues(t -> t.setNewResolution(null)).collect(toSet()), changeMock))
      .map(serializer::serialize)
      .collect(toSet());
    reset(serializer);

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verify(serializer, times(notifications.size())).from(any(IssuesChangesNotification.class));
    verifyZeroInteractions(changeMock);
    verifyNoMoreInteractions(serializer);
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  @UseDataProvider("notFPorWontFixResolution")
  public void deliver_has_no_effect_if_no_issue_has_FP_or_wontfix_resolution(String newResolution) {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Change changeMock = mock(Change.class);
    Set<IssuesChangesNotification> notifications = IntStream.range(0, 2 + new Random().nextInt(5))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(randomIssues(t -> t.setNewResolution(newResolution)).collect(toSet()), changeMock))
      .map(serializer::serialize)
      .collect(toSet());
    reset(serializer);

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verify(serializer, times(notifications.size())).from(any(IssuesChangesNotification.class));
    verifyZeroInteractions(changeMock);
    verifyNoMoreInteractions(serializer);
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
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
    Project projectKey1 = newProject(randomAlphabetic(4));
    Project projectKey2 = newProject(randomAlphabetic(5));
    Project projectKey3 = newProject(randomAlphabetic(6));
    Project projectKey4 = newProject(randomAlphabetic(7));
    Change changeMock = mock(Change.class);
    // some notifications with some issues on project1
    Stream<IssuesChangesNotificationBuilder> project1Notifications = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(
        randomIssues(t -> t.setProject(projectKey1).setNewResolution(newResolution)).collect(toSet()),
        changeMock));
    // some notifications with some issues on project2
    Stream<IssuesChangesNotificationBuilder> project2Notifications = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(
        randomIssues(t -> t.setProject(projectKey2).setNewResolution(newResolution)).collect(toSet()),
        changeMock));
    // some notifications with some issues on project3 and project 4
    Stream<IssuesChangesNotificationBuilder> project3And4Notifications = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(
        Stream.concat(
          randomIssues(t -> t.setProject(projectKey3).setNewResolution(newResolution)),
          randomIssues(t -> t.setProject(projectKey4).setNewResolution(newResolution)))
          .collect(toSet()),
        changeMock));
    when(emailNotificationChannel.isActivated()).thenReturn(true);

    Set<IssuesChangesNotification> notifications = Stream.of(project1Notifications, project2Notifications, project3And4Notifications)
      .flatMap(t -> t)
      .map(serializer::serialize)
      .collect(toSet());
    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, projectKey1.getKey(), ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, projectKey2.getKey(), ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, projectKey3.getKey(), ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, projectKey4.getKey(), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
    verifyZeroInteractions(changeMock);
  }

  @Test
  @UseDataProvider("FPorWontFixResolution")
  public void deliver_does_not_send_email_request_for_notifications_a_subscriber_is_the_changeAuthor_of(String newResolution) {
    Project project = newProject(randomAlphabetic(5));
    User subscriber1 = newUser("subscriber1");
    User subscriber2 = newUser("subscriber2");
    User subscriber3 = newUser("subscriber3");
    User otherChangeAuthor = newUser("otherChangeAuthor");

    // subscriber1 is the changeAuthor of some notifications with issues assigned to subscriber1 only
    Set<IssuesChangesNotificationBuilder> subscriber1Notifications = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(
        randomIssues(t -> t.setProject(project).setNewResolution(newResolution).setAssignee(subscriber2)).collect(toSet()),
        newUserChange(subscriber1)))
      .collect(toSet());
    // subscriber1 is the changeAuthor of some notifications with issues assigned to subscriber1 and subscriber2
    Set<IssuesChangesNotificationBuilder> subscriber1and2Notifications = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(
        Stream.concat(
          randomIssues(t -> t.setProject(project).setNewResolution(newResolution).setAssignee(subscriber2)),
          randomIssues(t -> t.setProject(project).setNewResolution(newResolution).setAssignee(subscriber1)))
          .collect(toSet()),
        newUserChange(subscriber1)))
      .collect(toSet());
    // subscriber2 is the changeAuthor of some notifications with issues assigned to subscriber2 only
    Set<IssuesChangesNotificationBuilder> subscriber2Notifications = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(
        randomIssues(t -> t.setProject(project).setNewResolution(newResolution).setAssignee(subscriber2)).collect(toSet()),
        newUserChange(subscriber2)))
      .collect(toSet());
    // subscriber2 is the changeAuthor of some notifications with issues assigned to subscriber2 and subscriber 3
    Set<IssuesChangesNotificationBuilder> subscriber2And3Notifications = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(
        Stream.concat(
          randomIssues(t -> t.setProject(project).setNewResolution(newResolution).setAssignee(subscriber2)),
          randomIssues(t -> t.setProject(project).setNewResolution(newResolution).setAssignee(subscriber3)))
          .collect(toSet()),
        newUserChange(subscriber2)))
      .collect(toSet());
    // subscriber3 is the changeAuthor of no notification
    // otherChangeAuthor has some notifications
    Set<IssuesChangesNotificationBuilder> otherChangeAuthorNotifications = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(j -> new IssuesChangesNotificationBuilder(randomIssues(t -> t.setProject(project).setNewResolution(newResolution)).collect(toSet()),
        newUserChange(otherChangeAuthor)))
      .collect(toSet());
    when(emailNotificationChannel.isActivated()).thenReturn(true);

    Set<String> subscriberLogins = ImmutableSet.of(subscriber1.getLogin(), subscriber2.getLogin(), subscriber3.getLogin());
    when(notificationManager.findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, project.getKey(), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(subscriberLogins.stream().map(FPOrWontFixNotificationHandlerTest::emailRecipientOf).collect(toSet()));

    int deliveredCount = new Random().nextInt(200);
    when(emailNotificationChannel.deliverAll(anySet()))
      .thenReturn(deliveredCount)
      .thenThrow(new IllegalStateException("deliver should be called only once"));

    Set<IssuesChangesNotification> notifications = Stream.of(
      subscriber1Notifications.stream(),
      subscriber1and2Notifications.stream(),
      subscriber2Notifications.stream(),
      subscriber2And3Notifications.stream(),
      otherChangeAuthorNotifications.stream())
      .flatMap(t -> t)
      .map(serializer::serialize)
      .collect(toSet());
    reset(serializer);

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isEqualTo(deliveredCount);
    verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, project.getKey(), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    ArgumentCaptor<Set<EmailDeliveryRequest>> captor = ArgumentCaptor.forClass(requestSetType);
    verify(emailNotificationChannel).deliverAll(captor.capture());
    verifyNoMoreInteractions(emailNotificationChannel);
    ListMultimap<String, EmailDeliveryRequest> requestsByRecipientEmail = captor.getValue().stream()
      .collect(index(EmailDeliveryRequest::getRecipientEmail));
    assertThat(requestsByRecipientEmail.get(emailOf(subscriber1.getLogin())))
      .containsOnly(
        Stream.of(
          subscriber2Notifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber1, toFpOrWontFix(newResolution))),
          subscriber2And3Notifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber1, toFpOrWontFix(newResolution))),
          otherChangeAuthorNotifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber1, toFpOrWontFix(newResolution))))
          .flatMap(t -> t)
          .toArray(EmailDeliveryRequest[]::new));
    assertThat(requestsByRecipientEmail.get(emailOf(subscriber2.getLogin())))
      .containsOnly(
        Stream.of(
          subscriber1Notifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber2, toFpOrWontFix(newResolution))),
          subscriber1and2Notifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber2, toFpOrWontFix(newResolution))),
          otherChangeAuthorNotifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber2, toFpOrWontFix(newResolution))))
          .flatMap(t -> t)
          .toArray(EmailDeliveryRequest[]::new));
    assertThat(requestsByRecipientEmail.get(emailOf(subscriber3.getLogin())))
      .containsOnly(
        Stream.of(
          subscriber1Notifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber3, toFpOrWontFix(newResolution))),
          subscriber1and2Notifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber3, toFpOrWontFix(newResolution))),
          subscriber2Notifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber3, toFpOrWontFix(newResolution))),
          subscriber2And3Notifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber3, toFpOrWontFix(newResolution))),
          otherChangeAuthorNotifications.stream()
            .map(notif -> newEmailDeliveryRequest(notif, subscriber3, toFpOrWontFix(newResolution))))
          .flatMap(t -> t)
          .toArray(EmailDeliveryRequest[]::new));
    assertThat(requestsByRecipientEmail.get(emailOf(otherChangeAuthor.getLogin())))
      .isEmpty();
  }

  @Test
  @UseDataProvider("oneOrMoreProjectCounts")
  public void deliver_send_a_separated_email_request_for_FPs_and_Wont_Fix_issues(int projectCount) {
    Set<Project> projects = IntStream.range(0, projectCount).mapToObj(i -> newProject("prk_key_" + i)).collect(toSet());
    User subscriber1 = newUser("subscriber1");
    User changeAuthor = newUser("changeAuthor");

    Set<ChangedIssue> fpIssues = projects.stream()
      .flatMap(project -> randomIssues(t -> t.setProject(project).setNewResolution(RESOLUTION_FALSE_POSITIVE).setAssignee(subscriber1)))
      .collect(toSet());
    Set<ChangedIssue> wontFixIssues = projects.stream()
      .flatMap(project -> randomIssues(t -> t.setProject(project).setNewResolution(RESOLUTION_WONT_FIX).setAssignee(subscriber1)))
      .collect(toSet());
    UserChange userChange = newUserChange(changeAuthor);
    IssuesChangesNotificationBuilder fpAndWontFixNotifications = new IssuesChangesNotificationBuilder(
      Stream.concat(fpIssues.stream(), wontFixIssues.stream()).collect(toSet()),
      userChange);
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    projects.forEach(project -> when(notificationManager.findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, project.getKey(), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(singleton(emailRecipientOf(subscriber1.getLogin()))));

    int deliveredCount = new Random().nextInt(200);
    when(emailNotificationChannel.deliverAll(anySet()))
      .thenReturn(deliveredCount)
      .thenThrow(new IllegalStateException("deliver should be called only once"));
    Set<IssuesChangesNotification> notifications = singleton(serializer.serialize(fpAndWontFixNotifications));
    reset(serializer);

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isEqualTo(deliveredCount);
    projects
      .forEach(project -> verify(notificationManager).findSubscribedEmailRecipients(DO_NOT_FIX_ISSUE_CHANGE_DISPATCHER_KEY, project.getKey(), ALL_MUST_HAVE_ROLE_USER));
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    ArgumentCaptor<Set<EmailDeliveryRequest>> captor = ArgumentCaptor.forClass(requestSetType);
    verify(emailNotificationChannel).deliverAll(captor.capture());
    verifyNoMoreInteractions(emailNotificationChannel);
    ListMultimap<String, EmailDeliveryRequest> requestsByRecipientEmail = captor.getValue().stream()
      .collect(index(EmailDeliveryRequest::getRecipientEmail));
    assertThat(requestsByRecipientEmail.get(emailOf(subscriber1.getLogin())))
      .containsOnly(
        new EmailDeliveryRequest(emailOf(subscriber1.getLogin()), new FPOrWontFixNotification(
          userChange, wontFixIssues, FpOrWontFix.WONT_FIX)),
        new EmailDeliveryRequest(emailOf(subscriber1.getLogin()), new FPOrWontFixNotification(
          userChange, fpIssues, FpOrWontFix.FP)));
  }

  @DataProvider
  public static Object[][] oneOrMoreProjectCounts() {
    return new Object[][] {
      {1},
      {2 + new Random().nextInt(3)},
    };
  }

  private static EmailDeliveryRequest newEmailDeliveryRequest(IssuesChangesNotificationBuilder notif, User user, FpOrWontFix resolution) {
    return new EmailDeliveryRequest(
      emailOf(user.getLogin()),
      new FPOrWontFixNotification(notif.getChange(), notif.getIssues(), resolution));
  }

  private static FpOrWontFix toFpOrWontFix(String newResolution) {
    if (newResolution.equals(Issue.RESOLUTION_WONT_FIX)) {
      return FpOrWontFix.WONT_FIX;
    }
    if (newResolution.equals(RESOLUTION_FALSE_POSITIVE)) {
      return FpOrWontFix.FP;
    }
    throw new IllegalArgumentException("unsupported resolution " + newResolution);
  }

  private static long counter = 233_343;

  private static UserChange newUserChange(User subscriber1) {
    return new UserChange(counter += 100, subscriber1);
  }

  public User newUser(String subscriber1) {
    return new User(subscriber1, subscriber1 + "_login", subscriber1 + "_name");
  }

  @DataProvider
  public static Object[][] FPorWontFixResolution() {
    return new Object[][] {
      {RESOLUTION_FALSE_POSITIVE},
      {Issue.RESOLUTION_WONT_FIX}
    };
  }

  private static Stream<ChangedIssue> randomIssues(Consumer<ChangedIssue.Builder> consumer) {
    return IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> {
        ChangedIssue.Builder builder = new ChangedIssue.Builder("key_" + i)
          .setAssignee(new User(randomAlphabetic(3), randomAlphabetic(4), randomAlphabetic(5)))
          .setNewStatus(randomAlphabetic(12))
          .setNewResolution(randomAlphabetic(13))
          .setRule(new Rule(RuleKey.of(randomAlphabetic(6), randomAlphabetic(7)), randomAlphabetic(8)))
          .setProject(new Project.Builder(randomAlphabetic(9))
            .setKey(randomAlphabetic(10))
            .setProjectName(randomAlphabetic(11))
            .build());
        consumer.accept(builder);
        return builder.build();
      });
  }

  private static NotificationManager.EmailRecipient emailRecipientOf(String assignee1) {
    return new NotificationManager.EmailRecipient(assignee1, emailOf(assignee1));
  }

  private static String emailOf(String assignee1) {
    return assignee1 + "@baffe";
  }

}
