/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.api.notifications.*;

import java.util.Collection;
import java.util.Map;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;

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
    String projectIdString = notification.getFieldValue("projectId");
    if (projectIdString != null) {
      int projectId = Integer.parseInt(projectIdString);
      Multimap<String, NotificationChannel> subscribedRecipients = notifications.findSubscribedRecipientsForDispatcher(this, projectId);

      for (Map.Entry<String, Collection<NotificationChannel>> channelsByRecipients : subscribedRecipients.asMap().entrySet()) {
        String userLogin = channelsByRecipients.getKey();
        for (NotificationChannel channel : channelsByRecipients.getValue()) {
          context.addUser(userLogin, channel);
        }
      }
    }
  }
}
