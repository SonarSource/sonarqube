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

public class NewFalsePositiveReviewTest {

  @Mock
  private NotificationManager notificationManager;

  @Mock
  private NotificationDispatcher.Context context;

  @Mock
  private NotificationChannel emailChannel;

  @Mock
  private NotificationChannel twitterChannel;

  private NotificationDispatcher dispatcher;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);

    dispatcher = new NewFalsePositiveReview(notificationManager);
  }

  @Test
  public void should_not_dispatch_if_not_reviews_notification() throws Exception {
    Notification notification = new Notification("other-notif");
    dispatcher.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }

  @Test
  public void should_dispatch_false_positive_resolution_to_every_subscribers() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("user1", emailChannel);
    recipients.put("user2", twitterChannel);
    when(notificationManager.findSubscribedRecipientsForDispatcher(dispatcher, 42)).thenReturn(recipients);

    dispatcher.performDispatch(new Notification("review-changed")
        .setFieldValue("projectId", "42")
        .setFieldValue("author", "user3")
        .setFieldValue("new.resolution", "FALSE-POSITIVE"),
        context);

    verify(context).addUser("user1", emailChannel);
    verify(context).addUser("user2", twitterChannel);
    verifyNoMoreInteractions(context);
  }

  @Test
  public void should_not_dispatch_to_author_of_changes() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("user1", emailChannel);
    recipients.put("user2", twitterChannel);
    when(notificationManager.findSubscribedRecipientsForDispatcher(dispatcher, 42)).thenReturn(recipients);

    dispatcher.performDispatch(new Notification("review-changed")
        .setFieldValue("projectId", "42")
        .setFieldValue("author", "user1")
        .setFieldValue("new.resolution", "FALSE-POSITIVE"),
        context);

    verify(context).addUser("user2", twitterChannel);
    verify(context, never()).addUser("user1", emailChannel);
    verifyNoMoreInteractions(context);
  }

  @Test
  public void should_not_dispatch_other_than_false_positive_resolution() {
    Multimap<String, NotificationChannel> recipients = HashMultimap.create();
    recipients.put("user", emailChannel);
    when(notificationManager.findSubscribedRecipientsForDispatcher(dispatcher, 42)).thenReturn(recipients);

    dispatcher.performDispatch(new Notification("review-changed")
        .setFieldValue("projectId", "42")
        .setFieldValue("author", "user2")
        .setFieldValue("new.assignee", "user"), context);

    verify(context, never()).addUser(any(String.class), any(NotificationChannel.class));
  }
}
