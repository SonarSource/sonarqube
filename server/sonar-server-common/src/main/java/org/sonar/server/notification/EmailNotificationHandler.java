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
package org.sonar.server.notification;

import java.util.Collection;
import java.util.Set;
import org.sonar.api.notifications.Notification;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.notification.email.EmailNotificationChannel.EmailDeliveryRequest;

public abstract class EmailNotificationHandler<T extends Notification> implements NotificationHandler<T> {
  private final EmailNotificationChannel emailChannel;

  protected EmailNotificationHandler(EmailNotificationChannel emailChannel) {
    this.emailChannel = emailChannel;
  }

  @Override
  public int deliver(Collection<T> notifications) {
    if (notifications.isEmpty() || !emailChannel.isActivated()) {
      return 0;
    }

    Set<EmailDeliveryRequest> requests = toEmailDeliveryRequests(notifications);
    if (requests.isEmpty()) {
      return 0;
    }
    return emailChannel.deliverAll(requests);
  }

  protected abstract Set<EmailDeliveryRequest> toEmailDeliveryRequests(Collection<T> notifications);
}
