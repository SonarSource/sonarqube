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
package org.sonar.core.notification;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class DefaultNotificationManagerTest extends AbstractDbUnitTestCase {

  private DefaultNotificationManager manager;

  @Before
  public void setUp() {
    manager = new DefaultNotificationManager(getSessionFactory());
  }

  @Test
  public void shouldPersist() throws Exception {
    Notification notification = new Notification("test");
    manager.scheduleForSending(notification);

    NotificationQueueElement queueElement = manager.getFromQueue();
    assertThat(queueElement.getNotification(), is(notification));

    assertThat(manager.getFromQueue(), nullValue());
  }

  @Test
  public void shouldCheckEnablement() {
    setupData("fixture");
    assertThat(manager.isEnabled("simon", "email", "CommentOnReviewAssignedToMe"), is(true));
    assertThat(manager.isEnabled("godin", "email", "CommentOnReviewAssignedToMe"), is(false));
  }

}
