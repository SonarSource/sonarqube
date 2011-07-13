/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.server.notifications.reviews;

import org.sonar.jpa.entity.Review;
import org.sonar.server.notifications.Notification;
import org.sonar.server.notifications.NotificationDispatcher;

/**
 * This dispatcher means: "notify me when when someone comments on review assigned to me". 
 * 
 * @since 2.10
 */
public class CommentOnReviewAssignedToMe extends NotificationDispatcher {

  @Override
  public void dispatch(Notification notification, Context context) {
    if (notification instanceof CommentOnReviewNotification) {
      Review review = ((CommentOnReviewNotification) notification).getReview();
      if (review.getAssigneeId() != null) {
        context.addUser(review.getAssigneeId());
      }
    }
  }

}
