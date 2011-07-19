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
package org.sonar.plugins.email.reviews;

import org.sonar.api.notifications.Notification;
import org.sonar.plugins.email.api.EmailMessage;
import org.sonar.plugins.email.api.EmailTemplate;

public class CommentOnReviewEmailTemplate extends EmailTemplate {

  @Override
  public EmailMessage format(Notification notification) {
    if ("review".equals(notification.getType())) {
      String reviewId = notification.getFieldValue("reviewId");
      String author = notification.getFieldValue("author");
      String comment = notification.getFieldValue("comment");
      EmailMessage email = new EmailMessage()
          .setFrom(author)
          .setMessageId("review/" + reviewId)
          .setSubject("Review #" + reviewId)
          .setMessage(comment);
      return email;
    }
    return null;
  }

}
