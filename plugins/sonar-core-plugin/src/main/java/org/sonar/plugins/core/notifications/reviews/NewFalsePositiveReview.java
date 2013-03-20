/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.core.notifications.reviews;

import com.google.common.collect.Multimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.core.review.ReviewDto;

import java.util.Collection;
import java.util.Map;

/**
 * This dispatcher means: "notify me when someone resolve a review as false positive".
 * 
 * @since 3.6
 */
public class NewFalsePositiveReview extends NotificationDispatcher {

  private NotificationManager notificationManager;

  public NewFalsePositiveReview(NotificationManager notificationManager) {
    super("review-changed");
    this.notificationManager = notificationManager;
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    String newResolution = notification.getFieldValue("new.resolution");
    if (StringUtils.equals(newResolution, ReviewDto.RESOLUTION_FALSE_POSITIVE)) {
      String author = notification.getFieldValue("author");
      int projectId = Integer.parseInt(notification.getFieldValue("projectId"));
      Multimap<String, NotificationChannel> subscribedRecipients = notificationManager.findSubscribedRecipientsForDispatcher(this, projectId);
      notify(author, context, subscribedRecipients);
    }
  }

  private void notify(String author, Context context, Multimap<String, NotificationChannel> subscribedRecipients) {
    for (Map.Entry<String, Collection<NotificationChannel>> channelsByRecipients : subscribedRecipients.asMap().entrySet()) {
      String userLogin = channelsByRecipients.getKey();
      if (!StringUtils.equals(author, userLogin)) {
        for (NotificationChannel channel : channelsByRecipients.getValue()) {
          context.addUser(userLogin, channel);
        }
      }
    }
  }

}
