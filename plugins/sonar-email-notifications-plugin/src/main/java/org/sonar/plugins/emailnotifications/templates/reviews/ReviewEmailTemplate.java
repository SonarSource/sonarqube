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
package org.sonar.plugins.emailnotifications.templates.reviews;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.config.EmailSettings;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.Notification;
import org.sonar.api.security.UserFinder;
import org.sonar.plugins.emailnotifications.api.EmailMessage;
import org.sonar.plugins.emailnotifications.api.EmailTemplate;

/**
 * Creates email message for notification "review-changed".
 * 
 * @since 2.10
 */
public class ReviewEmailTemplate extends EmailTemplate {

  private EmailSettings configuration;
  private UserFinder userFinder;

  public ReviewEmailTemplate(EmailSettings configuration, UserFinder userFinder) {
    this.configuration = configuration;
    this.userFinder = userFinder;
  }

  @Override
  public EmailMessage format(Notification notification) {
    if (!"review-changed".equals(notification.getType())) {
      return null;
    }
    String reviewId = notification.getFieldValue("reviewId");
    String author = notification.getFieldValue("author");
    StringBuilder sb = new StringBuilder();

    append(sb, "Project", null, notification.getFieldValue("project"));
    append(sb, "Resource", null, notification.getFieldValue("resource"));
    sb.append('\n');
    append(sb, null, null, notification.getFieldValue("title"));
    sb.append('\n');
    append(sb, "Status", notification.getFieldValue("old.status"), notification.getFieldValue("new.status"));
    append(sb, "Resolution", notification.getFieldValue("old.resolution"), notification.getFieldValue("new.resolution"));
    append(sb, "Assignee", getUserFullName(notification.getFieldValue("old.assignee")), getUserFullName(notification.getFieldValue("new.assignee")));
    appendComment(sb, notification);
    appendFooter(sb, notification);

    EmailMessage message = new EmailMessage()
        .setMessageId("review/" + reviewId)
        .setSubject("Review #" + reviewId + ("FALSE-POSITIVE".equals(notification.getFieldValue("new.resolution")) ? " - False Positive" : ""))
        .setMessage(sb.toString());
    if (author != null) {
      message.setFrom(getUserFullName(author));
    }
    return message;
  }

  private void append(StringBuilder sb, String name, String oldValue, String newValue) {
    if (oldValue != null || newValue != null) {
      if (name != null) {
        sb.append(name).append(": ");
      }
      if (newValue != null) {
        sb.append(newValue);
      }
      if (oldValue != null) {
        sb.append(" (was ").append(oldValue).append(")");
      }
      sb.append('\n');
    }
  }

  private void appendComment(StringBuilder sb, Notification notification) {
    String newComment = notification.getFieldValue("new.comment");
    String oldComment = notification.getFieldValue("old.comment");

    if (newComment != null) {
      // comment was added or modified
      sb.append("Comment:\n  ").append(newComment).append('\n');
      if (oldComment != null) {
        // comment was modified
        sb.append("Was:\n  ").append(oldComment).append('\n');
      }
    } else if (oldComment != null) {
      // comment was deleted
      sb.append("Comment deleted, was:\n  ").append(oldComment).append('\n');
    }
  }

  private void appendFooter(StringBuilder sb, Notification notification) {
    String reviewId = notification.getFieldValue("reviewId");
    sb.append("\n")
        .append("See it in Sonar: ").append(configuration.getServerBaseURL()).append("/reviews/view/").append(reviewId).append('\n');
  }

  /**
   * Visibility has been relaxed for tests.
   */
  String getUserFullName(String login) {
    if (login == null) {
      return null;
    }
    User user = userFinder.findByLogin(login);
    if (user == null) {
      // most probably user was deleted
      return login;
    }
    return StringUtils.defaultIfBlank(user.getName(), login);
  }

}
