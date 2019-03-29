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
package org.sonar.server.qualityprofile;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationHandler;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static org.sonar.core.util.stream.MoreCollectors.toSet;

public class BuiltInQPChangeNotificationHandler implements NotificationHandler<BuiltInQPChangeNotification> {
  private final DbClient dbClient;
  private final EmailNotificationChannel emailNotificationChannel;

  public BuiltInQPChangeNotificationHandler(DbClient dbClient, EmailNotificationChannel emailNotificationChannel) {
    this.dbClient = dbClient;
    this.emailNotificationChannel = emailNotificationChannel;
  }

  @Override
  public Optional<NotificationDispatcherMetadata> getMetadata() {
    return Optional.empty();
  }

  @Override
  public Class<BuiltInQPChangeNotification> getNotificationClass() {
    return BuiltInQPChangeNotification.class;
  }

  @Override
  public int deliver(Collection<BuiltInQPChangeNotification> notifications) {
    if (notifications.isEmpty() || !emailNotificationChannel.isActivated()) {
      return 0;
    }

    try (DbSession session = dbClient.openSession(false)) {
      Set<EmailDeliveryRequest> deliveryRequests = dbClient.authorizationDao()
        .selectQualityProfileAdministratorLogins(session)
        .stream()
        .flatMap(t -> notifications.stream().map(notification -> new EmailDeliveryRequest(t.getEmail(), notification)))
        .collect(toSet());

      if (deliveryRequests.isEmpty()) {
        return 0;
      }

      return emailNotificationChannel.deliver(deliveryRequests);
    }
  }
}
