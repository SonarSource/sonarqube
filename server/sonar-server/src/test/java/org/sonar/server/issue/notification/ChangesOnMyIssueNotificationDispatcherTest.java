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
package org.sonar.server.issue.notification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.notifications.*;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ChangesOnMyIssueNotificationDispatcherTest {

  @Mock
  NotificationManager notifications;

  @Mock
  NotificationDispatcher.Context context;

  @Mock
  NotificationChannel emailChannel;

  @Mock
  NotificationChannel twitterChannel;

  ChangesOnMyIssueNotificationDispatcher dispatcher;

  @Before
  public void setUp() {
    dispatcher = new ChangesOnMyIssueNotificationDispatcher(notifications);
  }

  @Test
  public void test_metadata() throws Exception {
    NotificationDispatcherMetadata metadata = ChangesOnMyIssueNotificationDispatcher.newMetadata();
    assertThat(metadata.getDispatcherKey()).isEqualTo(dispatcher.getKey());
    assertThat(metadata.getProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION)).isEqualTo("true");
    assertThat(metadata.getProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void should_not_dispatch_if_other_notification_type() {
    Notification notification = new Notification("other-notif");
    dispatcher.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void should_dispatch_to_reporter_and_assignee() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("simon", emailChannel);
    recipients.put("freddy", twitterChannel);
    recipients.put("godin", twitterChannel);
    when(notifications.findNotificationSubscribers(dispatcher, "struts")).thenReturn(recipients);

    Notification notification = new IssueChangeNotification().setFieldValue("projectKey", "struts")
      .setFieldValue("changeAuthor", "olivier")
      .setFieldValue("reporter", "simon")
      .setFieldValue("assignee", "freddy");
    dispatcher.performDispatch(notification, context);

    verify(context).addUser("simon", emailChannel);
    verify(context).addUser("freddy", twitterChannel);
    verify(context, never()).addUser("godin", twitterChannel);
    verifyNoMoreInteractions(context);
  }

  @Test
  public void should_not_dispatch_to_author_of_changes() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("simon", emailChannel);
    recipients.put("freddy", twitterChannel);
    recipients.put("godin", twitterChannel);
    when(notifications.findNotificationSubscribers(dispatcher, "struts")).thenReturn(recipients);

    // change author is the reporter
    dispatcher.performDispatch(new IssueChangeNotification().setFieldValue("projectKey", "struts")
      .setFieldValue("changeAuthor", "simon").setFieldValue("reporter", "simon"), context);

    // change author is the assignee
    dispatcher.performDispatch(new IssueChangeNotification().setFieldValue("projectKey", "struts")
      .setFieldValue("changeAuthor", "simon").setFieldValue("assignee", "simon"), context);

    // no change author
    dispatcher.performDispatch(new IssueChangeNotification().setFieldValue("projectKey", "struts")
      .setFieldValue("new.resolution", "FIXED"), context);

    verifyNoMoreInteractions(context);
  }
}
