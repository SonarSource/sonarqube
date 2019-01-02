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
package org.sonar.server.issue.notification;

import com.google.common.collect.Multimap;
import java.util.Collection;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;

import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

/**
 * This dispatcher means: "notify me when new issues are introduced during project analysis"
 */
public class MyNewIssuesNotificationDispatcher extends NotificationDispatcher {

  public static final String KEY = "SQ-MyNewIssues";
  private final NotificationManager manager;

  public MyNewIssuesNotificationDispatcher(NotificationManager manager) {
    super(MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE);
    this.manager = manager;
  }

  public static NotificationDispatcherMetadata newMetadata() {
    return NotificationDispatcherMetadata.create(KEY)
      .setProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION, String.valueOf(true))
      .setProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION, String.valueOf(true));
  }

  @Override
  public String getKey() {
    return KEY;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    String projectKey = notification.getFieldValue("projectKey");
    String assignee = notification.getFieldValue("assignee");
    Multimap<String, NotificationChannel> subscribedRecipients = manager
        .findSubscribedRecipientsForDispatcher(this, projectKey, ALL_MUST_HAVE_ROLE_USER);

    Collection<NotificationChannel> channels = subscribedRecipients.get(assignee);
    for (NotificationChannel channel : channels) {
      context.addUser(assignee, channel);
    }
  }

}
