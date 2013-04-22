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
package org.sonar.plugins.core.notifications.reviews;

import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;

import java.util.Collection;

/**
 * This dispatcher means: "notify me when someone changes review assigned to me or created by me".
 * 
 * @since 2.10
 */
public class ChangesInReviewAssignedToMeOrCreatedByMe extends NotificationDispatcher {

  private NotificationManager notificationManager;

  public ChangesInReviewAssignedToMeOrCreatedByMe(NotificationManager notificationManager) {
    super("review-changed");
    this.notificationManager = notificationManager;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    int projectId = Integer.parseInt(notification.getFieldValue("projectId"));
    Multimap<String, NotificationChannel> subscribedRecipients = notificationManager.findSubscribedRecipientsForDispatcher(this, projectId);

    String author = notification.getFieldValue("author");
    String creator = notification.getFieldValue("creator");
    String oldAssignee = notification.getFieldValue("old.assignee");
    String assignee = notification.getFieldValue("assignee");
    if (creator != null && !StringUtils.equals(author, creator)) {
      addUserToContextIfSubscribed(context, creator, subscribedRecipients);
    }
    if (oldAssignee != null && !StringUtils.equals(author, oldAssignee)) {
      addUserToContextIfSubscribed(context, oldAssignee, subscribedRecipients);
    }
    if (assignee != null && !StringUtils.equals(author, assignee)) {
      addUserToContextIfSubscribed(context, assignee, subscribedRecipients);
    }
  }

  private void addUserToContextIfSubscribed(Context context, String user, Multimap<String, NotificationChannel> subscribedRecipients) {
    Collection<NotificationChannel> channels = subscribedRecipients.get(user);
    for (NotificationChannel channel : channels) {
      context.addUser(user, channel);
    }
  }

}
