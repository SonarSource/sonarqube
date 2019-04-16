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
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.Change;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.User;
import org.sonar.server.notification.EmailNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.NotificationManager.EmailRecipient;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.unorderedFlattenIndex;
import static org.sonar.core.util.stream.MoreCollectors.unorderedIndex;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

public class ChangesOnMyIssueNotificationHandler extends EmailNotificationHandler<IssuesChangesNotification> {

  private static final String KEY = "ChangesOnMyIssue";
  private static final NotificationDispatcherMetadata METADATA = NotificationDispatcherMetadata.create(KEY)
    .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
    .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));

  private final NotificationManager notificationManager;
  private final IssuesChangesNotificationSerializer serializer;

  public ChangesOnMyIssueNotificationHandler(NotificationManager notificationManager,
    EmailNotificationChannel emailNotificationChannel, IssuesChangesNotificationSerializer serializer) {
    super(emailNotificationChannel);
    this.notificationManager = notificationManager;
    this.serializer = serializer;
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return Optional.of(METADATA);
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return METADATA;
  }

  @Override
  public Class<IssuesChangesNotification> getNotificationClass() {
    return IssuesChangesNotification.class;
  }

  @Override
  public Set<EmailDeliveryRequest> toEmailDeliveryRequests(Collection<IssuesChangesNotification> notifications) {
    Set<NotificationWithProjectKeys> notificationsWithPeerChangedIssues = notifications.stream()
      .map(serializer::from)
      // ignore notification of which the changeAuthor is the assignee of all changed issues
      .filter(t -> t.getIssues().stream().anyMatch(issue -> issue.getAssignee().isPresent() && isPeerChanged(t.getChange(), issue)))
      .map(NotificationWithProjectKeys::new)
      .collect(Collectors.toSet());
    if (notificationsWithPeerChangedIssues.isEmpty()) {
      return ImmutableSet.of();
    }

    Set<String> projectKeys = notificationsWithPeerChangedIssues.stream()
      .flatMap(t -> t.getProjectKeys().stream())
      .collect(Collectors.toSet());

    // shortcut to save from building unnecessary data structures when all changed issues in notifications belong to
    // the same project
    if (projectKeys.size() == 1) {
      Set<User> assigneesOfPeerChangedIssues = notificationsWithPeerChangedIssues.stream()
        .flatMap(t -> t.getIssues().stream().filter(issue -> isPeerChanged(t.getChange(), issue)))
        .map(ChangedIssue::getAssignee)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
      Set<EmailRecipient> subscribedAssignees = notificationManager.findSubscribedEmailRecipients(
        KEY,
        projectKeys.iterator().next(),
        assigneesOfPeerChangedIssues.stream().map(User::getLogin).collect(Collectors.toSet()),
        ALL_MUST_HAVE_ROLE_USER);

      return subscribedAssignees.stream()
        .flatMap(recipient -> notificationsWithPeerChangedIssues.stream()
          // do not notify users of the changes they made themselves
          .filter(notification -> !notification.getChange().isAuthorLogin(recipient.getLogin()))
          .map(notification -> toEmailDeliveryRequest(notification, recipient, projectKeys)))
        .filter(Objects::nonNull)
        .collect(toSet(notificationsWithPeerChangedIssues.size()));
    }

    SetMultimap<String, String> assigneeLoginsOfPeerChangedIssuesByProjectKey = notificationsWithPeerChangedIssues.stream()
      .flatMap(notification -> notification.getIssues().stream()
        .filter(issue -> issue.getAssignee().isPresent())
        .filter(issue -> isPeerChanged(notification.getChange(), issue)))
      .collect(unorderedIndex(t -> t.getProject().getKey(), t -> t.getAssignee().get().getLogin()));

    SetMultimap<String, EmailRecipient> authorizedAssigneeLoginsByProjectKey = assigneeLoginsOfPeerChangedIssuesByProjectKey.asMap().entrySet()
      .stream()
      .collect(unorderedFlattenIndex(
        Map.Entry::getKey,
        entry -> {
          String projectKey = entry.getKey();
          Set<String> assigneeLogins = (Set<String>) entry.getValue();
          return notificationManager.findSubscribedEmailRecipients(KEY, projectKey, assigneeLogins, ALL_MUST_HAVE_ROLE_USER).stream();
        }));

    SetMultimap<EmailRecipient, String> projectKeyByRecipient = authorizedAssigneeLoginsByProjectKey.entries().stream()
      .collect(unorderedIndex(Map.Entry::getValue, Map.Entry::getKey));

    return projectKeyByRecipient.asMap().entrySet()
      .stream()
      .flatMap(entry -> {
        EmailRecipient recipient = entry.getKey();
        Set<String> subscribedProjectKeys = (Set<String>) entry.getValue();
        return notificationsWithPeerChangedIssues.stream()
          // do not notify users of the changes they made themselves
          .filter(notification -> !notification.getChange().isAuthorLogin(recipient.getLogin()))
          .map(notification -> toEmailDeliveryRequest(notification, recipient, subscribedProjectKeys))
          .filter(Objects::nonNull);
      })
      .collect(toSet(notificationsWithPeerChangedIssues.size()));
  }

  /**
   * Creates the {@link EmailDeliveryRequest} for the specified {@code recipient} with issues from the
   * specified {@code notification} it is the assignee of.
   *
   * @return {@code null} when the recipient is the assignee of no issue in {@code notification}.
   */
  @CheckForNull
  private static EmailDeliveryRequest toEmailDeliveryRequest(NotificationWithProjectKeys notification, EmailRecipient recipient, Set<String> subscribedProjectKeys) {
    Set<ChangedIssue> recipientIssuesByProject = notification.getIssues().stream()
      .filter(issue -> issue.getAssignee().filter(assignee -> recipient.getLogin().equals(assignee.getLogin())).isPresent())
      .filter(issue -> subscribedProjectKeys.contains(issue.getProject().getKey()))
      .collect(toSet(notification.getIssues().size()));
    if (recipientIssuesByProject.isEmpty()) {
      return null;
    }
    return new EmailDeliveryRequest(
      recipient.getEmail(),
      new ChangesOnMyIssuesNotification(notification.getChange(), recipientIssuesByProject));
  }

  /**
   * Is the author of the change the assignee of the specified issue?
   * If not, it means the issue has been changed by a peer of the author of the change.
   */
  private static boolean isPeerChanged(Change change, ChangedIssue issue) {
    Optional<User> assignee = issue.getAssignee();
    return !assignee.isPresent() || !change.isAuthorLogin(assignee.get().getLogin());
  }

}
