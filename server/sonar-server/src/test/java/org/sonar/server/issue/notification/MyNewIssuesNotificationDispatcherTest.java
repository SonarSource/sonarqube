/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.notification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.web.UserRole;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationManager;

import static org.mockito.Mockito.*;

public class MyNewIssuesNotificationDispatcherTest {

  private MyNewIssuesNotificationDispatcher underTest;

  private NotificationManager notificationManager = mock(NotificationManager.class);
  private NotificationDispatcher.Context context = mock(NotificationDispatcher.Context.class);
  private NotificationChannel emailChannel = mock(NotificationChannel.class);
  private NotificationChannel twitterChannel = mock(NotificationChannel.class);


  @Before
  public void setUp() {
    underTest = new MyNewIssuesNotificationDispatcher(notificationManager);
  }

  @Test
  public void do_not_dispatch_if_no_new_notification() {
    Notification notification = new Notification("other-notif");
    underTest.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void dispatch_to_users_who_have_subscribed_to_notification_and_project() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("user1", emailChannel);
    recipients.put("user2", twitterChannel);
    when(notificationManager.findSubscribedRecipientsForDispatcher(underTest, "struts", new NotificationManager.SubscriberPermissionsOnProject(UserRole.USER))).thenReturn(recipients);

    Notification notification = new Notification(MyNewIssuesNotification.MY_NEW_ISSUES_NOTIF_TYPE)
      .setFieldValue("projectKey", "struts")
      .setFieldValue("assignee", "user1");
    underTest.performDispatch(notification, context);

    verify(context).addUser("user1", emailChannel);
    verifyNoMoreInteractions(context);
  }
}
