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
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.api.issue.Issue;
import org.sonar.server.issue.notification.IssuesChangesNotificationBuilder.ChangedIssue;
import org.sonar.server.notification.EmailNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.NotificationManager.EmailRecipient;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static com.google.common.collect.Sets.intersection;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.unorderedFlattenIndex;
import static org.sonar.core.util.stream.MoreCollectors.unorderedIndex;
import static org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix.FP;
import static org.sonar.server.issue.notification.FPOrWontFixNotification.FpOrWontFix.WONT_FIX;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

public class FPOrWontFixNotificationHandler extends EmailNotificationHandler<IssuesChangesNotification> {

  public static final String KEY = "NewFalsePositiveIssue";
  private static final NotificationDispatcherMetadata METADATA = NotificationDispatcherMetadata.create(KEY)
    .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(false))
    .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));

  private static final Set<String> FP_OR_WONTFIX_RESOLUTIONS = ImmutableSet.of(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_WONT_FIX);

  private final NotificationManager notificationManager;
  private final IssuesChangesNotificationSerializer serializer;

  public FPOrWontFixNotificationHandler(NotificationManager notificationManager,
    EmailNotificationChannel emailNotificationChannel, IssuesChangesNotificationSerializer serializer) {
    super(emailNotificationChannel);
    this.notificationManager = notificationManager;
    this.serializer = serializer;
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return of(METADATA);
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
    Set<NotificationWithProjectKeys> changeNotificationsWithFpOrWontFix = notifications.stream()
      .map(serializer::from)
      // ignore notifications which contain no issue changed to a FP or Won't Fix resolution
      .filter(t -> t.getIssues().stream()
        .filter(issue -> issue.getNewResolution().isPresent())
        .anyMatch(issue -> FP_OR_WONTFIX_RESOLUTIONS.contains(issue.getNewResolution().get())))
      .map(NotificationWithProjectKeys::new)
      .collect(Collectors.toSet());
    if (changeNotificationsWithFpOrWontFix.isEmpty()) {
      return emptySet();
    }
    Set<String> projectKeys = changeNotificationsWithFpOrWontFix.stream()
      .flatMap(t -> t.getProjectKeys().stream())
      .collect(Collectors.toSet());

    // shortcut to save from building unnecessary data structures when all changed issues in notifications belong to
    // the same project
    if (projectKeys.size() == 1) {
      Set<EmailRecipient> recipients = notificationManager.findSubscribedEmailRecipients(KEY, projectKeys.iterator().next(), ALL_MUST_HAVE_ROLE_USER);
      return changeNotificationsWithFpOrWontFix.stream()
        .flatMap(notification -> toRequests(notification, projectKeys, recipients))
        .collect(toSet(changeNotificationsWithFpOrWontFix.size()));
    }

    Set<EmailRecipientAndProject> recipientsByProjectKey = projectKeys.stream()
      .flatMap(projectKey -> notificationManager.findSubscribedEmailRecipients(KEY, projectKey, ALL_MUST_HAVE_ROLE_USER).stream()
        .map(emailRecipient -> new EmailRecipientAndProject(emailRecipient, projectKey)))
      .collect(Collectors.toSet());

    // builds sets of projectKeys for which a given recipient has subscribed to
    SetMultimap<EmailRecipient, String> projectKeysByRecipient = recipientsByProjectKey.stream()
      .collect(unorderedIndex(t -> t.recipient, t -> t.projectKey));
    // builds sets of recipients who subscribed to the same subset of projects
    Multimap<Set<String>, EmailRecipient> recipientsBySubscribedProjects = projectKeysByRecipient.asMap()
      .entrySet().stream()
      .collect(unorderedIndex(t -> (Set<String>) t.getValue(), Map.Entry::getKey));

    return changeNotificationsWithFpOrWontFix.stream()
      .flatMap(notification -> {
        // builds sets of recipients for each sub group of the notification's projectKeys necessary
        SetMultimap<Set<String>, EmailRecipient> recipientsByProjectKeys = recipientsBySubscribedProjects.asMap().entrySet()
          .stream()
          .collect(unorderedFlattenIndex(t -> intersection(t.getKey(), notification.getProjectKeys()).immutableCopy(), t -> t.getValue().stream()));
        return recipientsByProjectKeys.asMap().entrySet().stream()
          .flatMap(entry -> toRequests(notification, entry.getKey(), entry.getValue()));
      })
      .collect(toSet(changeNotificationsWithFpOrWontFix.size()));
  }

  private static Stream<EmailDeliveryRequest> toRequests(NotificationWithProjectKeys notification, Set<String> projectKeys, Collection<EmailRecipient> recipients) {
    return recipients.stream()
      // do not notify author of the change
      .filter(recipient -> !notification.getChange().isAuthorLogin(recipient.getLogin()))
      .flatMap(recipient -> {
        SetMultimap<String, ChangedIssue> issuesByNewResolution = notification.getIssues().stream()
          // ignore issues not changed to a FP or Won't Fix resolution
          .filter(issue -> issue.getNewResolution().filter(FP_OR_WONTFIX_RESOLUTIONS::contains).isPresent())
          // ignore issues belonging to projects the recipients have not subscribed to
          .filter(issue -> projectKeys.contains(issue.getProject().getKey()))
          .collect(unorderedIndex(t -> t.getNewResolution().get(), issue -> issue));

        return Stream.of(
          ofNullable(issuesByNewResolution.get(Issue.RESOLUTION_FALSE_POSITIVE))
            .filter(t -> !t.isEmpty())
            .map(fpIssues -> new FPOrWontFixNotification(notification.getChange(), fpIssues, FP))
            .orElse(null),
          ofNullable(issuesByNewResolution.get(Issue.RESOLUTION_WONT_FIX))
            .filter(t -> !t.isEmpty())
            .map(wontFixIssues -> new FPOrWontFixNotification(notification.getChange(), wontFixIssues, WONT_FIX))
            .orElse(null))
          .filter(Objects::nonNull)
          .map(fpOrWontFixNotification -> new EmailDeliveryRequest(recipient.getEmail(), fpOrWontFixNotification));
      });
  }

  private static final class EmailRecipientAndProject {
    private final EmailRecipient recipient;
    private final String projectKey;

    private EmailRecipientAndProject(EmailRecipient recipient, String projectKey) {
      this.recipient = recipient;
      this.projectKey = projectKey;
    }
  }

}
