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
package org.sonar.plugins.core.sensors;

import org.sonar.api.BatchExtension;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.security.UserFinder;
import org.sonar.core.review.ReviewDto;

import javax.annotation.Nullable;

public class ReviewNotifications implements BatchExtension {
  private NotificationManager notificationManager;
  private UserFinder userFinder;

  public ReviewNotifications(NotificationManager notificationManager, UserFinder userFinder) {
    this.notificationManager = notificationManager;
    this.userFinder = userFinder;
  }

  void notifyReopened(ReviewDto review, Project project, Resource resource) {
    Notification notification = createReviewNotification(review, project, resource)
        .setFieldValue("old.status", review.getStatus())
        .setFieldValue("new.status", ReviewDto.STATUS_REOPENED)
        .setFieldValue("old.resolution", review.getResolution())
        .setFieldValue("new.resolution", null);
    notificationManager.scheduleForSending(notification);
  }

  void notifyClosed(ReviewDto review, Project project, @Nullable Resource resource) {
    Notification notification = createReviewNotification(review, project, resource)
        .setFieldValue("old.status", review.getStatus())
        .setFieldValue("new.status", ReviewDto.STATUS_CLOSED);
    notificationManager.scheduleForSending(notification);
  }

  private Notification createReviewNotification(ReviewDto review, Project project, @Nullable Resource resource) {
    return new Notification("review-changed")
        .setFieldValue("reviewId", String.valueOf(review.getId()))
        .setFieldValue("project", project.getRoot().getLongName())
        .setFieldValue("projectId", String.valueOf(project.getId()))
        .setFieldValue("resource", resource != null ? resource.getLongName() : null)
        .setFieldValue("title", review.getTitle())
        .setFieldValue("creator", getCreator(review))
        .setFieldValue("assignee", getAssignee(review));
  }

  private String getCreator(ReviewDto review) {
    if (review.getUserId() == null) { // no creator and in fact this should never happen in real-life, however happens during unit tests
      return null;
    }
    User user = userFinder.findById(review.getUserId());
    return user != null ? user.getLogin() : null;
  }

  private String getAssignee(ReviewDto review) {
    if (review.getAssigneeId() == null) { // not assigned
      return null;
    }
    User user = userFinder.findById(review.getAssigneeId().intValue());
    return user != null ? user.getLogin() : null;
  }
}
