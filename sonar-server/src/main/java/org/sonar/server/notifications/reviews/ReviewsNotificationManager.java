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

import org.sonar.api.ServerComponent;
import org.sonar.api.database.model.Review;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.jpa.session.DatabaseSessionFactory;

/**
 * @since 2.10
 */
public class ReviewsNotificationManager implements ServerComponent {

  private DatabaseSessionFactory sessionFactory;
  private NotificationManager notificationManager;

  public ReviewsNotificationManager(DatabaseSessionFactory sessionFactory, NotificationManager notificationManager) {
    this.sessionFactory = sessionFactory;
    this.notificationManager = notificationManager;
  }

  /**
   * Visibility has been relaxed for tests.
   */
  User getUserById(Integer id) {
    return sessionFactory.getSession().getEntity(User.class, id);
  }

  /**
   * Visibility has been relaxed for tests.
   */
  Review getReviewById(Long id) {
    return sessionFactory.getSession().getEntity(Review.class, id);
  }

  public void notifyCommentAdded(Long reviewId, Integer userId, String comment) {
    Review review = getReviewById(reviewId);
    User author = getUserById(userId);

    Notification notification = new Notification("review");
    notification // FIXME include info about review
        .setFieldValue("author", author.getLogin())
        .setFieldValue("comment", comment);

    notificationManager.scheduleForSending(notification);
  }

}
