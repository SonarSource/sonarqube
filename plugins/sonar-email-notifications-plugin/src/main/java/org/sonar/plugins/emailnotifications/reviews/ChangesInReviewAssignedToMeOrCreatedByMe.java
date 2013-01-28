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
package org.sonar.plugins.emailnotifications.reviews;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationDispatcher;

/**
 * This dispatcher means: "notify me when someone changes review assigned to me or created by me".
 * 
 * @since 2.10
 */
public class ChangesInReviewAssignedToMeOrCreatedByMe extends NotificationDispatcher {

  public ChangesInReviewAssignedToMeOrCreatedByMe() {
    super("review-changed");
  }

  @Override
  public void dispatch(Notification notification, Context context) {
    String author = notification.getFieldValue("author");
    String creator = notification.getFieldValue("creator");
    String oldAssignee = notification.getFieldValue("old.assignee");
    String assignee = notification.getFieldValue("assignee");
    if (creator != null && !StringUtils.equals(author, creator)) {
      context.addUser(creator);
    }
    if (oldAssignee != null && !StringUtils.equals(author, oldAssignee)) {
      context.addUser(oldAssignee);
    }
    if (assignee != null && !StringUtils.equals(author, assignee)) {
      context.addUser(assignee);
    }
  }

}
