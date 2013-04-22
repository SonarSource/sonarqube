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

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class ReviewsNotificationManagerTest {

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

  @Test
  public void shouldScheduleNotification() {
    Map<String, String> oldValues = Maps.newHashMap();
    Map<String, String> newValues = Maps.newHashMap();
    newValues.put("project", "Sonar");
    newValues.put("projectId", "42");
    newValues.put("resource", "org.sonar.server.ui.DefaultPages");
    newValues.put("title", "Utility classes should not have a public or default constructor.");
    newValues.put("creator", "olivier");
    newValues.put("assignee", "godin");
    oldValues.put("assignee", "simon");
    manager.notifyChanged(1L, "freddy", oldValues, newValues);
    assertThat(notification, notNullValue());
    assertThat(notification.getType(), is("review-changed"));
    assertThat(notification.getDefaultMessage(), is("Review #1 has changed."));
    assertThat(notification.getFieldValue("reviewId"), is("1"));
    assertThat(notification.getFieldValue("author"), is("freddy"));
    assertThat(notification.getFieldValue("project"), is("Sonar"));
    assertThat(notification.getFieldValue("projectId"), is("42"));
    assertThat(notification.getFieldValue("resource"), is("org.sonar.server.ui.DefaultPages"));
    assertThat(notification.getFieldValue("title"), is("Utility classes should not have a public or default constructor."));
    assertThat(notification.getFieldValue("creator"), is("olivier"));
    assertThat(notification.getFieldValue("assignee"), is("godin"));
    assertThat(notification.getFieldValue("old.assignee"), is("simon"));
    assertThat(notification.getFieldValue("new.assignee"), is("godin"));
  }

}
