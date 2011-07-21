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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;

public class ReviewsNotificationManagerTest { // FIXME implement

  private Notification notification;
  private ReviewsNotificationManager manager;

  @Before
  public void setUp() {
    NotificationManager delegate = mock(NotificationManager.class);
    doAnswer(new Answer() {
      public Object answer(InvocationOnMock invocation) throws Throwable {
        notification = (Notification) invocation.getArguments()[0];
        return null;
      }
    }).when(delegate).scheduleForSending(any(Notification.class));
    manager = new ReviewsNotificationManager(delegate);
  }

}
