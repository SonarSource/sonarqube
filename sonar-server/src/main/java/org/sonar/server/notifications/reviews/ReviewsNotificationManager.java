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
package org.sonar.server.notifications.reviews;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;

import java.util.Map;
import java.util.Set;

/**
 * @since 2.10
 */
public class ReviewsNotificationManager implements ServerComponent {

  private NotificationManager notificationManager;

  public ReviewsNotificationManager(NotificationManager notificationManager) {
    this.notificationManager = notificationManager;
  }

  /**
   * @param reviewId reviewId id of review, which was modified
   * @param author author of change (username)
   * @param oldValues map of old values
   * @param newValues map of new values
   */
  public void notifyChanged(Long reviewId, String author, Map<String, String> oldValues, Map<String, String> newValues) {
    Notification notification = new Notification("review-changed")
        .setDefaultMessage("Review #" + reviewId + " has changed.")
        .setFieldValue("reviewId", String.valueOf(reviewId))
        .setFieldValue("project", newValues.get("project"))
        .setFieldValue("projectId", newValues.get("projectId"))
        .setFieldValue("resource", newValues.get("resource"))
        .setFieldValue("title", newValues.get("title"))
        .setFieldValue("author", author)
        .setFieldValue("creator", newValues.get("creator"))
        .setFieldValue("severity", newValues.get("severity"))
        .setFieldValue("assignee", newValues.get("assignee"));
    Set<String> fields = Sets.newHashSet();
    fields.addAll(oldValues.keySet());
    fields.addAll(newValues.keySet());
    for (String field : fields) {
      String oldValue = oldValues.get(field);
      String newValue = newValues.get(field);
      if (!StringUtils.equals(oldValue, newValue)) {
        notification.setFieldValue("new." + field, newValue);
        notification.setFieldValue("old." + field, oldValue);
      }
    }
    notificationManager.scheduleForSending(notification);
  }

}
