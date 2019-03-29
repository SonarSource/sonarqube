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
package org.sonar.ce.task.projectanalysis.notification;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.sonar.api.web.UserRole;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationHandler;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.core.util.stream.MoreCollectors.toSet;

public class ReportAnalysisFailureNotificationHandler implements NotificationHandler<ReportAnalysisFailureNotification> {
  private static final String KEY = "CeReportTaskFailure";
  private static final NotificationDispatcherMetadata METADATA = NotificationDispatcherMetadata.create(KEY)
    .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
    .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));
  private static final SubscriberPermissionsOnProject REQUIRED_SUBSCRIBER_PERMISSIONS = new SubscriberPermissionsOnProject(UserRole.ADMIN, UserRole.USER);

  private final NotificationManager notificationManager;
  private final EmailNotificationChannel emailNotificationChannel;

  public ReportAnalysisFailureNotificationHandler(NotificationManager notificationManager, EmailNotificationChannel emailNotificationChannel) {
    this.notificationManager = notificationManager;
    this.emailNotificationChannel = emailNotificationChannel;
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return Optional.of(METADATA);
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return METADATA;
  }

  @Override
  public Class<ReportAnalysisFailureNotification> getNotificationClass() {
    return ReportAnalysisFailureNotification.class;
  }

  @Override
  public int deliver(Collection<ReportAnalysisFailureNotification> notifications) {
    if (notifications.isEmpty() || !emailNotificationChannel.isActivated()) {
      return 0;
    }

    Multimap<String, ReportAnalysisFailureNotification> notificationsByProjectKey = notifications.stream()
      .filter(t -> t.getProjectKey() != null)
      .collect(index(ReportAnalysisFailureNotification::getProjectKey));
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

  private Stream<? extends EmailDeliveryRequest> toEmailDeliveryRequests(String projectKey, Collection<ReportAnalysisFailureNotification> notifications) {
    return notificationManager.findSubscribedEmailRecipients(KEY, projectKey, REQUIRED_SUBSCRIBER_PERMISSIONS)
      .stream()
      .flatMap(emailRecipient -> notifications.stream()
        .map(notification -> new EmailDeliveryRequest(emailRecipient.getEmail(), notification)));
  }
}
