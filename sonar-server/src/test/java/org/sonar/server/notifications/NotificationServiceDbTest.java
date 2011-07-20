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
package org.sonar.server.notifications;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

public class NotificationServiceDbTest extends AbstractDbUnitTestCase {

  private NotificationService notificationService;

  @Before
  public void setUp() {
    setupData("fixture");
    notificationService = new NotificationService(getSessionFactory(), null, null);
  }

  @Test
  public void should() {
    NotificationChannel email = mock(NotificationChannel.class);
    when(email.getKey()).thenReturn("EmailNotificationChannel");
    NotificationDispatcher commentOnReviewAssignedToMe = mock(NotificationDispatcher.class);
    when(commentOnReviewAssignedToMe.getKey()).thenReturn("CommentOnReviewAssignedToMe");

    assertThat(notificationService.isEnabled("simon", email, commentOnReviewAssignedToMe), is(true));
    assertThat(notificationService.isEnabled("godin", email, commentOnReviewAssignedToMe), is(false));
  }

}
