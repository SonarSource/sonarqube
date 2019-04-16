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
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.AnalysisChange;
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

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.unorderedIndex;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

@RunWith(DataProviderRunner.class)
public class ChangesOnMyIssueNotificationHandlerTest {
  private static final String CHANGE_ON_MY_ISSUES_DISPATCHER_KEY = "ChangesOnMyIssue";
  private static final String NO_CHANGE_AUTHOR = null;

  private NotificationManager notificationManager = mock(NotificationManager.class);
  private EmailNotificationChannel emailNotificationChannel = mock(EmailNotificationChannel.class);
  private IssuesChangesNotificationSerializer serializer = new IssuesChangesNotificationSerializer();
  private ChangesOnMyIssueNotificationHandler underTest = new ChangesOnMyIssueNotificationHandler(
    notificationManager, emailNotificationChannel, serializer);

  private Class<Set<EmailDeliveryRequest>> emailDeliveryRequestSetType = (Class<Set<EmailDeliveryRequest>>) (Object) Set.class;
  private ArgumentCaptor<Set<EmailDeliveryRequest>> emailDeliveryRequestSetCaptor = ArgumentCaptor.forClass(emailDeliveryRequestSetType);

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
    assertThat(underTest.getNotificationClass()).isEqualTo(IssuesChangesNotification.class);
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
  public void deliver_has_no_effect_if_no_notification_has_assignee() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<ChangedIssue> issues = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(i -> new ChangedIssue.Builder("issue_key_" + i)
        .setNewStatus("foo")
        .setAssignee(null)
        .setRule(newRule())
        .setProject(newProject(i + ""))
        .build())
      .collect(toSet());
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(issues, new UserChange(new Random().nextLong(), new User("user_uuid", "user_login", null)));

    int deliver = underTest.deliver(ImmutableSet.of(serializer.serialize(builder)));

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_has_no_effect_if_all_issues_are_assigned_to_the_changeAuthor() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<UserChange> userChanges = IntStream.range(0, 1 + new Random().nextInt(3))
      .mapToObj(i -> new UserChange(new Random().nextLong(), new User("user_uuid_" + i, "user_login_" + i, null)))
      .collect(toSet());
    Set<IssuesChangesNotificationBuilder> notificationBuilders = userChanges.stream()
      .map(userChange -> {
        Set<ChangedIssue> issues = IntStream.range(0, 1 + new Random().nextInt(2))
          .mapToObj(i -> new ChangedIssue.Builder("issue_key_" + i + userChange.getUser().getUuid())
            .setNewStatus("foo")
            .setAssignee(userChange.getUser())
            .setRule(newRule())
            .setProject(newProject(i + ""))
            .build())
          .collect(toSet());
        return new IssuesChangesNotificationBuilder(issues, userChange);
      })
      .collect(toSet());
    Set<IssuesChangesNotification> notifications = notificationBuilders.stream()
      .map(t -> serializer.serialize(t))
      .collect(toSet());

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isZero();
    verifyZeroInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_checks_by_projectKey_if_notifications_have_subscribed_assignee_to_ChangesOnMyIssues_notifications() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Project project = newProject();
    Set<ChangedIssue> issues = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(i -> new ChangedIssue.Builder("issue_key_" + i)
        .setNewStatus("foo")
        .setAssignee(newUser("assignee_" + i))
        .setRule(newRule())
        .setProject(project)
        .build())
      .collect(toSet());
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(issues, new UserChange(new Random().nextLong(), new User("user_uuid", "user_login", null)));

    int deliver = underTest.deliver(ImmutableSet.of(serializer.serialize(builder)));

    assertThat(deliver).isZero();
    Set<String> assigneeLogins = issues.stream().map(i -> i.getAssignee().get().getLogin()).collect(toSet());
    verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, project.getKey(), assigneeLogins, ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  public void deliver_checks_by_projectKeys_if_notifications_have_subscribed_assignee_to_ChangesOnMyIssues_notifications() {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Set<ChangedIssue> issues = IntStream.range(0, 1 + new Random().nextInt(2))
      .mapToObj(i -> new ChangedIssue.Builder("issue_key_" + i)
        .setNewStatus("foo")
        .setAssignee(newUser("" + i))
        .setRule(newRule())
        .setProject(newProject(i + ""))
        .build())
      .collect(toSet());
    IssuesChangesNotificationBuilder builder = new IssuesChangesNotificationBuilder(issues, new UserChange(new Random().nextLong(), new User("user_uuid", "user_login", null)));

    int deliver = underTest.deliver(ImmutableSet.of(serializer.serialize(builder)));

    assertThat(deliver).isZero();
    issues.stream()
      .collect(MoreCollectors.index(ChangedIssue::getProject))
      .asMap()
      .forEach((key, value) -> {
        String projectKey = key.getKey();
        Set<String> assigneeLogins = value.stream().map(i -> i.getAssignee().get().getLogin()).collect(toSet());
        verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, projectKey, assigneeLogins, ALL_MUST_HAVE_ROLE_USER);
      });
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verifyNoMoreInteractions(emailNotificationChannel);
  }

  @Test
  @UseDataProvider("userOrAnalysisChange")
  public void deliver_creates_a_notification_per_assignee_with_only_his_issues_on_the_single_project(Change userOrAnalysisChange) {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Project project = newProject();
    User assignee1 = newUser("assignee_1");
    User assignee2 = newUser("assignee_2");
    Set<ChangedIssue> assignee1Issues = IntStream.range(0, 10)
      .mapToObj(i -> newChangedIssue("1_issue_key_" + i, assignee1, project))
      .collect(toSet());
    Set<ChangedIssue> assignee2Issues = IntStream.range(0, 10)
      .mapToObj(i -> newChangedIssue("2_issue_key_" + i, assignee2, project))
      .collect(toSet());
    Set<IssuesChangesNotification> notifications = Stream.of(
      // notification with only assignee1 5 notifications
      new IssuesChangesNotificationBuilder(assignee1Issues.stream().limit(5).collect(toSet()), userOrAnalysisChange),
      // notification with only assignee2 6 notifications
      new IssuesChangesNotificationBuilder(assignee2Issues.stream().limit(6).collect(toSet()), userOrAnalysisChange),
      // notification with 4 assignee1 and 3 assignee2 notifications
      new IssuesChangesNotificationBuilder(
        Stream.concat(assignee1Issues.stream().skip(6), assignee2Issues.stream().skip(7)).collect(toSet()),
        userOrAnalysisChange))
      .map(t -> serializer.serialize(t))
      .collect(toSet());
    when(notificationManager.findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, project.getKey(), ImmutableSet.of(assignee1.getLogin(), assignee2.getLogin()),
      ALL_MUST_HAVE_ROLE_USER))
        .thenReturn(ImmutableSet.of(emailRecipientOf(assignee1.getLogin()), emailRecipientOf(assignee2.getLogin())));
    int deliveredCount = new Random().nextInt(100);
    when(emailNotificationChannel.deliverAll(anySet())).thenReturn(deliveredCount);

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isEqualTo(deliveredCount);
    verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY,
      project.getKey(), ImmutableSet.of(assignee1.getLogin(), assignee2.getLogin()), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(emailDeliveryRequestSetCaptor.capture());
    verifyNoMoreInteractions(emailNotificationChannel);

    Set<EmailDeliveryRequest> emailDeliveryRequests = emailDeliveryRequestSetCaptor.getValue();
    assertThat(emailDeliveryRequests).hasSize(4);
    ListMultimap<String, EmailDeliveryRequest> emailDeliveryRequestByEmail = emailDeliveryRequests.stream()
      .collect(index(EmailDeliveryRequest::getRecipientEmail));
    List<EmailDeliveryRequest> assignee1Requests = emailDeliveryRequestByEmail.get(emailOf(assignee1.getLogin()));
    assertThat(assignee1Requests)
      .hasSize(2)
      .extracting(t -> (ChangesOnMyIssuesNotification) t.getNotification())
      .extracting(ChangesOnMyIssuesNotification::getChange)
      .containsOnly(userOrAnalysisChange);
    assertThat(assignee1Requests)
      .extracting(t -> (ChangesOnMyIssuesNotification) t.getNotification())
      .extracting(ChangesOnMyIssuesNotification::getChangedIssues)
      .containsOnly(
        assignee1Issues.stream().limit(5).collect(unorderedIndex(t -> project, t -> t)),
        assignee1Issues.stream().skip(6).collect(unorderedIndex(t -> project, t -> t)));

    List<EmailDeliveryRequest> assignee2Requests = emailDeliveryRequestByEmail.get(emailOf(assignee2.getLogin()));
    assertThat(assignee2Requests)
      .hasSize(2)
      .extracting(t -> (ChangesOnMyIssuesNotification) t.getNotification())
      .extracting(ChangesOnMyIssuesNotification::getChange)
      .containsOnly(userOrAnalysisChange);
    assertThat(assignee2Requests)
      .extracting(t -> (ChangesOnMyIssuesNotification) t.getNotification())
      .extracting(ChangesOnMyIssuesNotification::getChangedIssues)
      .containsOnly(
        assignee2Issues.stream().limit(6).collect(unorderedIndex(t -> project, t -> t)),
        assignee2Issues.stream().skip(7).collect(unorderedIndex(t -> project, t -> t)));
  }

  @Test
  @UseDataProvider("userOrAnalysisChange")
  public void deliver_ignores_issues_which_assignee_is_the_changeAuthor(Change userOrAnalysisChange) {
    when(emailNotificationChannel.isActivated()).thenReturn(true);
    Project project1 = newProject();
    Project project2 = newProject();
    User assignee1 = newUser("assignee_1");
    User assignee2 = newUser("assignee_2");
    Set<ChangedIssue> assignee1Issues = IntStream.range(0, 10)
      .mapToObj(i -> newChangedIssue("1_issue_key_" + i, assignee1, project1))
      .collect(toSet());
    Set<ChangedIssue> assignee2Issues = IntStream.range(0, 10)
      .mapToObj(i -> newChangedIssue("2_issue_key_" + i, assignee2, project2))
      .collect(toSet());

    UserChange assignee2Change1 = new UserChange(new Random().nextLong(), assignee2);
    Set<IssuesChangesNotification> notifications = Stream.of(
      // notification from assignee1 with issues from assignee1 only
      new IssuesChangesNotificationBuilder(
        assignee1Issues.stream().limit(4).collect(toSet()),
        new UserChange(new Random().nextLong(), assignee1)),
      // notification from assignee2 with issues from assignee1 and assignee2
      new IssuesChangesNotificationBuilder(
        Stream.concat(
          assignee1Issues.stream().skip(4).limit(2),
          assignee2Issues.stream().limit(4))
          .collect(toSet()),
        assignee2Change1),
      // notification from assignee2 with issues from assignee2 only
      new IssuesChangesNotificationBuilder(
        assignee2Issues.stream().skip(4).limit(3).collect(toSet()),
        new UserChange(new Random().nextLong(), assignee2)),
      // notification from other change with issues from assignee1 and assignee2)
      new IssuesChangesNotificationBuilder(
        Stream.concat(
          assignee1Issues.stream().skip(6),
          assignee2Issues.stream().skip(7))
          .collect(toSet()),
        userOrAnalysisChange))
      .map(t -> serializer.serialize(t))
      .collect(toSet());
    when(notificationManager.findSubscribedEmailRecipients(
      CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, project1.getKey(), ImmutableSet.of(assignee1.getLogin()), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(ImmutableSet.of(emailRecipientOf(assignee1.getLogin())));
    when(notificationManager.findSubscribedEmailRecipients(
      CHANGE_ON_MY_ISSUES_DISPATCHER_KEY, project2.getKey(), ImmutableSet.of(assignee2.getLogin()), ALL_MUST_HAVE_ROLE_USER))
      .thenReturn(ImmutableSet.of(emailRecipientOf(assignee2.getLogin())));
    int deliveredCount = new Random().nextInt(100);
    when(emailNotificationChannel.deliverAll(anySet())).thenReturn(deliveredCount);

    int deliver = underTest.deliver(notifications);

    assertThat(deliver).isEqualTo(deliveredCount);
    verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY,
      project1.getKey(), ImmutableSet.of(assignee1.getLogin()), ALL_MUST_HAVE_ROLE_USER);
    verify(notificationManager).findSubscribedEmailRecipients(CHANGE_ON_MY_ISSUES_DISPATCHER_KEY,
      project2.getKey(), ImmutableSet.of(assignee2.getLogin()), ALL_MUST_HAVE_ROLE_USER);
    verifyNoMoreInteractions(notificationManager);
    verify(emailNotificationChannel).isActivated();
    verify(emailNotificationChannel).deliverAll(emailDeliveryRequestSetCaptor.capture());
    verifyNoMoreInteractions(emailNotificationChannel);

    Set<EmailDeliveryRequest> emailDeliveryRequests = emailDeliveryRequestSetCaptor.getValue();
    assertThat(emailDeliveryRequests).hasSize(3);
    ListMultimap<String, EmailDeliveryRequest> emailDeliveryRequestByEmail = emailDeliveryRequests.stream()
      .collect(index(EmailDeliveryRequest::getRecipientEmail));
    List<EmailDeliveryRequest> assignee1Requests = emailDeliveryRequestByEmail.get(emailOf(assignee1.getLogin()));
    assertThat(assignee1Requests)
      .hasSize(2)
      .extracting(t -> (ChangesOnMyIssuesNotification) t.getNotification())
      .extracting(ChangesOnMyIssuesNotification::getChange)
      .containsOnly(userOrAnalysisChange, assignee2Change1);
    assertThat(assignee1Requests)
      .extracting(t -> (ChangesOnMyIssuesNotification) t.getNotification())
      .extracting(ChangesOnMyIssuesNotification::getChangedIssues)
      .containsOnly(
        assignee1Issues.stream().skip(4).limit(2).collect(unorderedIndex(t -> project1, t -> t)),
        assignee1Issues.stream().skip(6).collect(unorderedIndex(t -> project1, t -> t)));

    List<EmailDeliveryRequest> assignee2Requests = emailDeliveryRequestByEmail.get(emailOf(assignee2.getLogin()));
    assertThat(assignee2Requests)
      .hasSize(1)
      .extracting(t -> (ChangesOnMyIssuesNotification) t.getNotification())
      .extracting(ChangesOnMyIssuesNotification::getChange)
      .containsOnly(userOrAnalysisChange);
    assertThat(assignee2Requests)
      .extracting(t -> (ChangesOnMyIssuesNotification) t.getNotification())
      .extracting(ChangesOnMyIssuesNotification::getChangedIssues)
      .containsOnly(assignee2Issues.stream().skip(7).collect(unorderedIndex(t -> project2, t -> t)));
  }

  @DataProvider
  public static Object[][] userOrAnalysisChange() {
    User changeAuthor = new User(randomAlphabetic(12), randomAlphabetic(10), randomAlphabetic(11));
    return new Object[][] {
      {new AnalysisChange(new Random().nextLong())},
      {new UserChange(new Random().nextLong(), changeAuthor)},
    };
  }

  private static Project newProject() {
    String base = randomAlphabetic(6);
    return newProject(base);
  }

  private static Project newProject(String base) {
    return new Project.Builder("prj_uuid_" + base)
      .setKey("prj_key_" + base)
      .setProjectName("prj_name_" + base)
      .build();
  }

  private static User newUser(String name) {
    return new User(name + "_uuid", name + "login", name);
  }

  private static ChangedIssue newChangedIssue(String key, User assignee1, Project project) {
    return new ChangedIssue.Builder(key)
      .setNewStatus("foo")
      .setAssignee(assignee1)
      .setRule(newRule())
      .setProject(project)
      .build();
  }

  private static Rule newRule() {
    return new Rule(RuleKey.of(randomAlphabetic(3), randomAlphabetic(4)), randomAlphabetic(5));
  }

  private static Set<IssuesChangesNotification> randomSetOfNotifications(@Nullable String projectKey, @Nullable String assignee, @Nullable String changeAuthor) {
    return IntStream.range(0, 1 + new Random().nextInt(5))
      .mapToObj(i -> newNotification(projectKey, assignee, changeAuthor))
      .collect(Collectors.toSet());
  }

  private static IssuesChangesNotification newNotification(@Nullable String projectKey, @Nullable String assignee, @Nullable String changeAuthor) {
    IssuesChangesNotification notification = mock(IssuesChangesNotification.class);
    return notification;
  }

  private static NotificationManager.EmailRecipient emailRecipientOf(String login) {
    return new NotificationManager.EmailRecipient(login, emailOf(login));
  }

  private static String emailOf(String login) {
    return login + "@plouf";
  }

}
