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
package org.sonar.plugins.core.issue.notification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.api.notifications.*;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NewFalsePositiveNotificationDispatcherTest {
  @Mock
  NotificationManager notifications;

  @Mock
  NotificationDispatcher.Context context;

  @Mock
  NotificationChannel emailChannel;

  @Mock
  NotificationChannel twitterChannel;

  NewFalsePositiveNotificationDispatcher dispatcher;

  @Before
  public void setUp() {
    dispatcher = new NewFalsePositiveNotificationDispatcher(notifications);
  }

  @Test
  public void test_metadata() throws Exception {
    NotificationDispatcherMetadata metadata = NewFalsePositiveNotificationDispatcher.newMetadata();
    assertThat(metadata.getDispatcherKey()).isEqualTo(dispatcher.getKey());
    assertThat(metadata.getProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION)).isEqualTo("true");
    assertThat(metadata.getProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void should_not_dispatch_if_other_notification_type() throws Exception {
    Notification notification = new Notification("other");
    dispatcher.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void should_dispatch_to_subscribers() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("simon", emailChannel);
    recipients.put("freddy", twitterChannel);
    recipients.put("godin", twitterChannel);
    when(notifications.findNotificationSubscribers(dispatcher, "struts")).thenReturn(recipients);

    Notification notification = new Notification("issue-changes").setFieldValue("projectKey", "struts")
      .setFieldValue("changeAuthor", "godin")
      .setFieldValue("new.resolution", "FALSE-POSITIVE")
      .setFieldValue("assignee", "freddy");
    dispatcher.performDispatch(notification, context);

    verify(context).addUser("simon", emailChannel);
    verify(context).addUser("freddy", twitterChannel);
    // do not notify the person who flagged the issue as false-positive
    verify(context, never()).addUser("godin", twitterChannel);
    verifyNoMoreInteractions(context);
  }

}
