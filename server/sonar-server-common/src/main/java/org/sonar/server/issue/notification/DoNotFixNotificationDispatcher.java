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
package org.sonar.server.issue.notification;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import org.sonar.api.issue.Issue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;

import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

/**
 * This dispatcher means: "notify me when an issue is resolved as false positive or won't fix".
 */
public class DoNotFixNotificationDispatcher extends NotificationDispatcher {

  public static final String KEY = "NewFalsePositiveIssue";

  private final NotificationManager notifications;

  public DoNotFixNotificationDispatcher(NotificationManager notifications) {
    super(IssueChangeNotification.TYPE);
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
    String newResolution = notification.getFieldValue("new.resolution");
    if (Objects.equals(newResolution, Issue.RESOLUTION_FALSE_POSITIVE) || Objects.equals(newResolution, Issue.RESOLUTION_WONT_FIX)) {
      String author = notification.getFieldValue("changeAuthor");
      String projectKey = notification.getFieldValue("projectKey");
      Multimap<String, NotificationChannel> subscribedRecipients = notifications
        .findSubscribedRecipientsForDispatcher(this, projectKey, ALL_MUST_HAVE_ROLE_USER);
      notify(author, context, subscribedRecipients);
    }
  }

  private static void notify(String author, Context context, Multimap<String, NotificationChannel> subscribedRecipients) {
    for (Map.Entry<String, Collection<NotificationChannel>> channelsByRecipients : subscribedRecipients.asMap().entrySet()) {
      String login = channelsByRecipients.getKey();
      // Do not notify the person that resolved the issue
      if (!Objects.equals(author, login)) {
        for (NotificationChannel channel : channelsByRecipients.getValue()) {
          context.addUser(login, channel);
        }
      }
    }
  }

}
