/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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
package org.sonar.server.event;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationManager;

import static org.mockito.Mockito.*;

public class NewAlertsTest {

  NotificationManager notificationManager = mock(NotificationManager.class);
  NotificationDispatcher.Context context = mock(NotificationDispatcher.Context.class);
  NotificationChannel emailChannel = mock(NotificationChannel.class);
  NotificationChannel twitterChannel = mock(NotificationChannel.class);
  NewAlerts dispatcher = new NewAlerts(notificationManager);

  @Test
  public void should_not_dispatch_if_not_alerts_notification() {
    Notification notification = new Notification("other-notif");
    dispatcher.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void should_dispatch_to_users_who_have_subscribed() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("user1", emailChannel);
    recipients.put("user2", twitterChannel);
    when(notificationManager.findSubscribedRecipientsForDispatcher(dispatcher, 34)).thenReturn(recipients);

    Notification notification = new Notification("alerts").setFieldValue("projectId", "34");
    dispatcher.performDispatch(notification, context);

    verify(context).addUser("user1", emailChannel);
    verify(context).addUser("user2", twitterChannel);
    verifyNoMoreInteractions(context);
  }

  @Test
  public void should_not_dispatch_if_missing_project_id() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("user1", emailChannel);
    recipients.put("user2", twitterChannel);
    when(notificationManager.findSubscribedRecipientsForDispatcher(dispatcher, 34)).thenReturn(recipients);

    Notification notification = new Notification("alerts");
    dispatcher.performDispatch(notification, context);

    verifyNoMoreInteractions(context);
  }

}
