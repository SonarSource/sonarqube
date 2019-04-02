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
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.server.notification.EmailNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.NotificationManager.EmailRecipient;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

public class ChangesOnMyIssueNotificationHandler extends EmailNotificationHandler<IssueChangeNotification> {

  private static final String KEY = "ChangesOnMyIssue";
  private static final NotificationDispatcherMetadata METADATA = NotificationDispatcherMetadata.create(KEY)
    .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
    .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));

  private final NotificationManager notificationManager;

  public ChangesOnMyIssueNotificationHandler(NotificationManager notificationManager, EmailNotificationChannel emailNotificationChannel) {
    super(emailNotificationChannel);
    this.notificationManager = notificationManager;
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return Optional.of(METADATA);
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return METADATA;
  }

  @Override
  public Class<IssueChangeNotification> getNotificationClass() {
    return IssueChangeNotification.class;
  }

  @Override
  public Set<EmailDeliveryRequest> toEmailDeliveryRequests(Collection<IssueChangeNotification> notifications) {
    Multimap<String, IssueChangeNotification> notificationsByProjectKey = notifications.stream()
      // ignore inconsistent data
      .filter(t -> t.getProjectKey() != null)
      // ignore notification on which we can't identify who should be notified
      .filter(t -> t.getAssignee() != null)
      // do not notify users of the changes they made themselves (changeAuthor is null when change comes from an analysis)
      .filter(t -> !Objects.equals(t.getAssignee(), t.getChangeAuthor()))
      .collect(index(IssueChangeNotification::getProjectKey));
    if (notificationsByProjectKey.isEmpty()) {
      return ImmutableSet.of();
    }

    return notificationsByProjectKey.asMap().entrySet()
      .stream()
      .flatMap(e -> toEmailDeliveryRequests(e.getKey(), e.getValue()))
      .collect(toSet(notifications.size()));
  }

  private Stream<? extends EmailDeliveryRequest> toEmailDeliveryRequests(String projectKey, Collection<IssueChangeNotification> notifications) {
    Map<String, EmailRecipient> recipientsByLogin = notificationManager
      .findSubscribedEmailRecipients(KEY, projectKey, ALL_MUST_HAVE_ROLE_USER)
      .stream()
      .collect(uniqueIndex(EmailRecipient::getLogin));
    return notifications.stream()
      .map(notification -> toEmailDeliveryRequest(recipientsByLogin, notification))
      .filter(Objects::nonNull);
  }

  @CheckForNull
  private static EmailNotificationChannel.EmailDeliveryRequest toEmailDeliveryRequest(Map<String, EmailRecipient> recipientsByLogin,
    IssueChangeNotification notification) {
    String assignee = notification.getAssignee();

    EmailRecipient emailRecipient = recipientsByLogin.get(assignee);
    if (emailRecipient != null) {
      return new EmailNotificationChannel.EmailDeliveryRequest(emailRecipient.getEmail(), notification);
    }
    return null;
  }
}
