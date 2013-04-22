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
package org.sonar.plugins.core.notifications.reviews;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.api.notifications.NotificationManager;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ChangesInReviewAssignedToMeOrCreatedByMeTest {

  @Mock
  private NotificationManager notificationManager;

  @Mock
  private NotificationDispatcher.Context context;

  @Mock
  private NotificationChannel emailChannel;

  @Mock
  private NotificationChannel twitterChannel;

  private ChangesInReviewAssignedToMeOrCreatedByMe dispatcher;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);

    dispatcher = new ChangesInReviewAssignedToMeOrCreatedByMe(notificationManager);
  }

  @Test
  public void should_not_dispatch_if_not_new_violations_notification() throws Exception {
    Notification notification = new Notification("other-notif");
    dispatcher.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void should_dispatch_to_creator_and_assignee() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("simon", emailChannel);
    recipients.put("freddy", twitterChannel);
    recipients.put("godin", twitterChannel);
    when(notificationManager.findSubscribedRecipientsForDispatcher(dispatcher, 42)).thenReturn(recipients);

    Notification notification = new Notification("review-changed")
        .setFieldValue("projectId", "42")
        .setFieldValue("author", "olivier")
        .setFieldValue("creator", "simon")
        .setFieldValue("old.assignee", "godin")
        .setFieldValue("assignee", "freddy");
    dispatcher.performDispatch(notification, context);

    verify(context).addUser("simon", emailChannel);
    verify(context).addUser("freddy", twitterChannel);
    verify(context).addUser("godin", twitterChannel);
    verifyNoMoreInteractions(context);
  }

  @Test
  public void should_not_dispatch_to_author_of_changes() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("simon", emailChannel);
    recipients.put("freddy", twitterChannel);
    recipients.put("godin", twitterChannel);
    when(notificationManager.findSubscribedRecipientsForDispatcher(dispatcher, 42)).thenReturn(recipients);

    dispatcher.performDispatch(new Notification("review-changed").setFieldValue("projectId", "42")
        .setFieldValue("author", "simon").setFieldValue("creator", "simon"), context);
    dispatcher.performDispatch(new Notification("review-changed").setFieldValue("projectId", "42")
        .setFieldValue("author", "simon").setFieldValue("assignee", "simon"), context);
    dispatcher.performDispatch(new Notification("review-changed").setFieldValue("projectId", "42")
        .setFieldValue("author", "simon").setFieldValue("old.assignee", "simon"), context);

    verifyNoMoreInteractions(context);
  }

  @Test
  public void should_not_dispatch_when_other_notification_type() {
    Notification notification = new Notification("other");
    dispatcher.performDispatch(notification, context);

    verifyNoMoreInteractions(context);
  }

}
