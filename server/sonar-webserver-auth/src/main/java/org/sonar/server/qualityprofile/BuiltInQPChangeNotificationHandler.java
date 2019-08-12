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
import org.sonar.server.notification.EmailNotificationHandler;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

import static org.sonar.core.util.stream.MoreCollectors.toSet;

public class BuiltInQPChangeNotificationHandler extends EmailNotificationHandler<BuiltInQPChangeNotification> {
  private final DbClient dbClient;

  public BuiltInQPChangeNotificationHandler(DbClient dbClient, EmailNotificationChannel emailNotificationChannel) {
    super(emailNotificationChannel);
    this.dbClient = dbClient;
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
  public Set<EmailDeliveryRequest> toEmailDeliveryRequests(Collection<BuiltInQPChangeNotification> notifications) {
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.authorizationDao()
        .selectQualityProfileAdministratorLogins(session)
        .stream()
        .flatMap(t -> notifications.stream().map(notification -> new EmailDeliveryRequest(t.getEmail(), notification)))
        .collect(toSet());
    }
  }
}
