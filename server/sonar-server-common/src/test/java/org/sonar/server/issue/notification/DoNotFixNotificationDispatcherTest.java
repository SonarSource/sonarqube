/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.web.UserRole;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class DoNotFixNotificationDispatcherTest {
  NotificationManager notifications = mock(NotificationManager.class);
  NotificationDispatcher.Context context = mock(NotificationDispatcher.Context.class);
  NotificationChannel emailChannel = mock(NotificationChannel.class);
  NotificationChannel twitterChannel = mock(NotificationChannel.class);
  DoNotFixNotificationDispatcher underTest = new DoNotFixNotificationDispatcher(notifications);

  @Test
  public void test_metadata() {
    NotificationDispatcherMetadata metadata = DoNotFixNotificationDispatcher.newMetadata();
    assertThat(metadata.getDispatcherKey()).isEqualTo(underTest.getKey());
    assertThat(metadata.getProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION)).isEqualTo("true");
    assertThat(metadata.getProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void should_not_dispatch_if_other_notification_type() {
    Notification notification = new Notification("other");
    underTest.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void should_dispatch_to_subscribers() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("simon", emailChannel);
    recipients.put("freddy", twitterChannel);
    recipients.put("godin", twitterChannel);
    when(notifications.findSubscribedRecipientsForDispatcher(underTest, "struts", new NotificationManager.SubscriberPermissionsOnProject(UserRole.USER))).thenReturn(recipients);

    Notification fpNotif = new IssueChangeNotification()
      .setFieldValue("projectKey", "struts")
      .setFieldValue("changeAuthor", "godin")
      .setFieldValue("new.resolution", Issue.RESOLUTION_FALSE_POSITIVE)
      .setFieldValue("assignee", "freddy");
    underTest.performDispatch(fpNotif, context);

    verify(context).addUser("simon", emailChannel);
    verify(context).addUser("freddy", twitterChannel);
    // do not notify the person who flagged the issue as false-positive
    verify(context, never()).addUser("godin", twitterChannel);
    verifyNoMoreInteractions(context);
  }

  /**
   * Only false positive and won't fix resolutions
   */
  @Test
  public void ignore_other_resolutions() {
    Notification fixedNotif = new IssueChangeNotification().setFieldValue("projectKey", "struts")
      .setFieldValue("changeAuthor", "godin")
      .setFieldValue("new.resolution", Issue.RESOLUTION_FIXED)
      .setFieldValue("assignee", "freddy");
    underTest.performDispatch(fixedNotif, context);

    verifyZeroInteractions(context);
  }
}
