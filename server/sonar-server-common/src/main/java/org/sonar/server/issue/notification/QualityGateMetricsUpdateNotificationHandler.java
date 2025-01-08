/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.server.notification.EmailNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static java.util.stream.Collectors.toSet;

public class QualityGateMetricsUpdateNotificationHandler extends EmailNotificationHandler<QualityGateMetricsUpdateNotification> {
  static final String KEY = "QualityGateConditionsMismatch";
  private static final NotificationDispatcherMetadata METADATA = NotificationDispatcherMetadata.create(KEY)
    .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
    .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(false))
    .setProperty(NotificationDispatcherMetadata.ENABLED_BY_DEFAULT_NOTIFICATION, String.valueOf(true))
    .setProperty(NotificationDispatcherMetadata.PERMISSION_RESTRICTION, GlobalPermission.ADMINISTER_QUALITY_GATES.getKey());

  private final DbClient dbClient;

  protected QualityGateMetricsUpdateNotificationHandler(DbClient dbClient, EmailNotificationChannel emailChannel) {
    super(emailChannel);
    this.dbClient = dbClient;
  }

  @Override
  protected Set<EmailNotificationChannel.EmailDeliveryRequest> toEmailDeliveryRequests(Collection<QualityGateMetricsUpdateNotification> notifications) {
    try (DbSession session = dbClient.openSession(false)) {

      Set<EmailSubscriberDto> subscriptions = dbClient.authorizationDao()
        .selectQualityGateAdministratorLogins(session);

      if (subscriptions.isEmpty()) {
        return Set.of();
      }

      Set<String> disabledLogins = dbClient.propertiesDao().findDisabledEmailSubscribersForNotification(
        session, KEY, EmailNotificationChannel.class.getSimpleName(), null,
        subscriptions.stream().map(EmailSubscriberDto::getLogin).collect(toSet()))
        .stream()
        .map(EmailSubscriberDto::getLogin)
        .collect(toSet());

      return subscriptions
        .stream()
        .filter(s -> !disabledLogins.contains(s.getLogin()))
        .flatMap(s -> notifications.stream().map(notification -> new EmailNotificationChannel.EmailDeliveryRequest(s.getEmail(), notification)))
        .collect(toSet());
    }
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return Optional.of(METADATA);
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return METADATA;
  }

  @Override
  public Class<QualityGateMetricsUpdateNotification> getNotificationClass() {
    return QualityGateMetricsUpdateNotification.class;
  }

}
