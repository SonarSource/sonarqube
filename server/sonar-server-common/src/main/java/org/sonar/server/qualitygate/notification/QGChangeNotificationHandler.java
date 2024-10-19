/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.qualitygate.notification;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.server.notification.EmailNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static java.util.Collections.emptySet;
import static org.sonar.core.util.stream.MoreCollectors.index;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

public class QGChangeNotificationHandler extends EmailNotificationHandler<QGChangeNotification> {

  public static final String KEY = "NewAlerts";
  private static final NotificationDispatcherMetadata METADATA = NotificationDispatcherMetadata.create(KEY)
    .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
    .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));

  private final NotificationManager notificationManager;

  public QGChangeNotificationHandler(NotificationManager notificationManager, EmailNotificationChannel emailNotificationChannel) {
    super(emailNotificationChannel);
    this.notificationManager = notificationManager;
  }

  @Override
  public Class<QGChangeNotification> getNotificationClass() {
    return QGChangeNotification.class;
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return Optional.of(METADATA);
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return METADATA;
  }

  @Override
  public Set<EmailDeliveryRequest> toEmailDeliveryRequests(Collection<QGChangeNotification> notifications) {
    Multimap<String, QGChangeNotification> notificationsByProjectKey = notifications.stream()
      .filter(t -> t.getProjectKey() != null)
      .collect(index(QGChangeNotification::getProjectKey));
    if (notificationsByProjectKey.isEmpty()) {
      return emptySet();
    }

    return notificationsByProjectKey.asMap().entrySet()
      .stream()
      .flatMap(e -> toEmailDeliveryRequests(e.getKey(), e.getValue()))
      .collect(Collectors.toSet());
  }

  private Stream<? extends EmailDeliveryRequest> toEmailDeliveryRequests(String projectKey, Collection<QGChangeNotification> notifications) {
    return notificationManager.findSubscribedEmailRecipients(KEY, projectKey, ALL_MUST_HAVE_ROLE_USER)
      .stream()
      .flatMap(emailRecipient -> notifications.stream()
        .map(notification -> new EmailDeliveryRequest(emailRecipient.email(), notification)));
  }
}
