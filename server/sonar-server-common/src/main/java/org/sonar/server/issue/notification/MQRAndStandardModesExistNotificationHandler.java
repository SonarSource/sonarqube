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
package org.sonar.server.issue.notification;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.notification.EmailNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.email.EmailNotificationChannel;

import static java.util.stream.Collectors.toSet;

public class MQRAndStandardModesExistNotificationHandler extends EmailNotificationHandler<MQRAndStandardModesExistNotification> {

  private final DbClient dbClient;

  protected MQRAndStandardModesExistNotificationHandler(DbClient dbClient, EmailNotificationChannel emailChannel) {
    super(emailChannel);
    this.dbClient = dbClient;
  }

  @Override
  protected Set<EmailNotificationChannel.EmailDeliveryRequest> toEmailDeliveryRequests(Collection<MQRAndStandardModesExistNotification> notifications) {
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.authorizationDao().selectGlobalAdministerEmailSubscribers(session)
        .stream()
        .flatMap(t -> notifications.stream().map(notification -> new EmailNotificationChannel.EmailDeliveryRequest(t.getEmail(), notification)))
        .collect(toSet());
    }
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return Optional.empty();
  }

  @Override
  public Class<MQRAndStandardModesExistNotification> getNotificationClass() {
    return MQRAndStandardModesExistNotification.class;
  }
}
