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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationDispatcher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class ChangesInReviewAssignedToMeOrCreatedByMeTest {

  private NotificationDispatcher.Context context;
  private ChangesInReviewAssignedToMeOrCreatedByMe dispatcher;

  @Before
  public void setUp() {
    context = mock(NotificationDispatcher.Context.class);
    dispatcher = new ChangesInReviewAssignedToMeOrCreatedByMe();
  }

  @Test
  public void dispatchToCreatorAndAssignee() {
    Notification notification = new Notification("review-changed")
        .setFieldValue("author", "olivier")
        .setFieldValue("creator", "simon")
        .setFieldValue("old.assignee", "godin")
        .setFieldValue("assignee", "freddy");
    dispatcher.performDispatch(notification, context);

    verify(context).addUser("simon");
    verify(context).addUser("godin");
    verify(context).addUser("freddy");
    verifyNoMoreInteractions(context);
  }

  @Test
  public void doNotDispatchToAuthorOfChanges() {
    dispatcher.performDispatch(new Notification("review-changed").setFieldValue("author", "simon").setFieldValue("creator", "simon"), context);
    dispatcher.performDispatch(new Notification("review-changed").setFieldValue("author", "simon").setFieldValue("assignee", "simon"), context);
    dispatcher.performDispatch(new Notification("review-changed").setFieldValue("author", "simon").setFieldValue("old.assignee", "simon"), context);

    verifyNoMoreInteractions(context);
  }

  @Test
  public void shouldNotDispatch() {
    Notification notification = new Notification("other");
    dispatcher.performDispatch(notification, context);

    verifyNoMoreInteractions(context);
  }

}
