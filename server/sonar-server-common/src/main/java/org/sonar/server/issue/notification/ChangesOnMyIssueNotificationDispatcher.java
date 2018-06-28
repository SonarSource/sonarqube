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
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;

import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

/**
 * This dispatcher means: "notify me when a change is done on an issue that is assigned to me or reported by me".
 *
 * @since 3.6, but the feature exists since 2.10 ("review-changed" notification)
 */
public class ChangesOnMyIssueNotificationDispatcher extends NotificationDispatcher {

  public static final String KEY = "ChangesOnMyIssue";
  private NotificationManager notificationManager;

  public ChangesOnMyIssueNotificationDispatcher(NotificationManager notificationManager) {
    super(IssueChangeNotification.TYPE);
    this.notificationManager = notificationManager;
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
    Multimap<String, NotificationChannel> subscribedRecipients = notificationManager
      .findSubscribedRecipientsForDispatcher(this, projectKey, ALL_MUST_HAVE_ROLE_USER);

    // See available fields in the class IssueNotifications.

    // All the following users can be null
    String changeAuthor = notification.getFieldValue("changeAuthor");
    String assignee = notification.getFieldValue("assignee");

    if (!Objects.equals(changeAuthor, assignee)) {
      addUserToContextIfSubscribed(context, assignee, subscribedRecipients);
    }
  }

  private static void addUserToContextIfSubscribed(Context context, @Nullable String user, Multimap<String, NotificationChannel> subscribedRecipients) {
    if (user != null) {
      Collection<NotificationChannel> channels = subscribedRecipients.get(user);
      for (NotificationChannel channel : channels) {
        context.addUser(user, channel);
      }
    }
  }
}
