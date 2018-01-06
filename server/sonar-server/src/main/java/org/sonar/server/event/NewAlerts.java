/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.event;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;

import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

/**
 * This dispatcher means: "notify me each new alert event".
 *
 * @since 3.5
 */
public class NewAlerts extends NotificationDispatcher {

  public static final String KEY = "NewAlerts";
  private final NotificationManager notifications;

  public NewAlerts(NotificationManager notifications) {
    super("alerts");
    this.notifications = notifications;
  }

  @Override
  public String getKey() {
    return KEY;
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return NotificationDispatcherMetadata.create(KEY)
      .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
      .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    String projectKey = notification.getFieldValue("projectKey");
    if (projectKey != null) {
      Multimap<String, NotificationChannel> subscribedRecipients = notifications
          .findSubscribedRecipientsForDispatcher(this, projectKey, ALL_MUST_HAVE_ROLE_USER);

      for (Map.Entry<String, Collection<NotificationChannel>> channelsByRecipients : subscribedRecipients.asMap().entrySet()) {
        String userLogin = channelsByRecipients.getKey();
        for (NotificationChannel channel : channelsByRecipients.getValue()) {
          context.addUser(userLogin, channel);
        }
      }
    }
  }
}
