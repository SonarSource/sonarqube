/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.issue;

import com.google.common.collect.Multimap;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;

import java.util.Collection;
import java.util.Map;

/**
 * This dispatcher means: "notify me when new issues are introduced during project scan".
 * 
 * @since 2.14
 */
public class NewIssuesNotificationDispatcher extends NotificationDispatcher {

  private final NotificationManager manager;

  public NewIssuesNotificationDispatcher(NotificationManager manager) {
    super("new-issues");
    this.manager = manager;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    int projectId = Integer.parseInt(notification.getFieldValue("projectId"));
    Multimap<String, NotificationChannel> subscribedRecipients = manager.findSubscribedRecipientsForDispatcher(this, projectId);

    for (Map.Entry<String, Collection<NotificationChannel>> channelsByRecipients : subscribedRecipients.asMap().entrySet()) {
      String userLogin = channelsByRecipients.getKey();
      for (NotificationChannel channel : channelsByRecipients.getValue()) {
        context.addUser(userLogin, channel);
      }
    }
  }

}
