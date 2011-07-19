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
package org.sonar.plugins.email.review;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.plugins.email.reviews.CommentOnReviewAssignedToMe;

public class CommentOnReviewAssignedToMeTest { // FIXME implement me

  private NotificationDispatcher.Context context;
  private CommentOnReviewAssignedToMe dispatcher;

  @Before
  public void setUp() {
    context = mock(NotificationDispatcher.Context.class);
    dispatcher = new CommentOnReviewAssignedToMe();
  }

  @Test
  public void shouldDispatchToAssignee() {
    // CommentOnReviewNotification notification = new CommentOnReviewNotification(new Review().setAssigneeId(1), new User(), "comment");
    // dispatcher.dispatch(notification, context);
    // verify(context).addUser(1);
    //
    // notification = new CommentOnReviewNotification(new Review().setAssigneeId(2), new User(), "comment");
    // dispatcher.dispatch(notification, context);
    // verify(context).addUser(2);
  }

  @Test
  public void shouldNotDispatchWhenNotAssigned() {
    // CommentOnReviewNotification notification = new CommentOnReviewNotification(new Review(), new User(), "comment");
    // dispatcher.dispatch(notification, context);
    // verifyNoMoreInteractions(context);
  }

}
