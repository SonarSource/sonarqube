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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.api.issue.Issue;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationHandler;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.NotificationManager.EmailRecipient;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static java.util.Optional.of;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

public class DoNotFixNotificationHandler implements NotificationHandler<IssueChangeNotification> {

  public static final String KEY = "NewFalsePositiveIssue";
  private static final NotificationDispatcherMetadata METADATA = NotificationDispatcherMetadata.create(KEY)
    .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
    .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));

  private static final Set<String> SUPPORTED_NEW_RESOLUTIONS = ImmutableSet.of(Issue.RESOLUTION_FALSE_POSITIVE, Issue.RESOLUTION_WONT_FIX);

  private final NotificationManager notificationManager;
  private final EmailNotificationChannel emailNotificationChannel;

  public DoNotFixNotificationHandler(NotificationManager notificationManager, EmailNotificationChannel emailNotificationChannel) {
    this.notificationManager = notificationManager;
    this.emailNotificationChannel = emailNotificationChannel;
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return of(METADATA);
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return METADATA;
  }

  @Override
  public Class<IssueChangeNotification> getNotificationClass() {
    return IssueChangeNotification.class;
  }

  @Override
  public int deliver(Collection<IssueChangeNotification> notifications) {
    if (notifications.isEmpty() || !emailNotificationChannel.isActivated()) {
      return 0;
    }

    Multimap<String, IssueChangeNotification> notificationsByProjectKey = notifications.stream()
      // ignore inconsistent data
      .filter(t -> t.getProjectKey() != null)
      // ignore notification on which we can't identify who should not be notified
      // (and anyway, it should not be null as an analysis can not resolve an issue as FP or Won't fix)
      .filter(t -> t.getChangeAuthor() != null)
      // ignore changes which did not lead to a FP or Won't Fix resolution
      .filter(t -> SUPPORTED_NEW_RESOLUTIONS.contains(t.getNewResolution()))
      .collect(index(IssueChangeNotification::getProjectKey));
    if (notificationsByProjectKey.isEmpty()) {
      return 0;
    }

    Set<EmailDeliveryRequest> deliveryRequests = notificationsByProjectKey.asMap().entrySet()
      .stream()
      .flatMap(e -> toEmailDeliveryRequests(e.getKey(), e.getValue()))
      .collect(toSet(notifications.size()));
    if (deliveryRequests.isEmpty()) {
      return 0;
    }
    return emailNotificationChannel.deliver(deliveryRequests);
  }

  private Stream<? extends EmailDeliveryRequest> toEmailDeliveryRequests(String projectKey, Collection<IssueChangeNotification> notifications) {
    Set<EmailRecipient> recipients = notificationManager
      .findSubscribedEmailRecipients(KEY, projectKey, ALL_MUST_HAVE_ROLE_USER);
    return notifications.stream()
      .flatMap(notification -> recipients.stream()
        // do not notify author of the change
        .filter(t -> !Objects.equals(t.getLogin(), notification.getChangeAuthor()))
        .map(t -> new EmailDeliveryRequest(t.getEmail(), notification)));
  }

}
