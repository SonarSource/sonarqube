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
package org.sonar.server.notifications;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.Settings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.notification.NotificationQueueElement;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationServiceTest {
  private static String CREATOR_SIMON = "simon";
  private static String CREATOR_EVGENY = "evgeny";
  private static String ASSIGNEE_SIMON = "simon";

  private final DefaultNotificationManager manager = mock(DefaultNotificationManager.class);
  private final NotificationQueueElement queueElement = mock(NotificationQueueElement.class);
  private final Notification notification = mock(Notification.class);
  private final NotificationChannel emailChannel = mock(NotificationChannel.class);
  private final NotificationChannel gtalkChannel = mock(NotificationChannel.class);
  private final NotificationDispatcher commentOnReviewAssignedToMe = mock(NotificationDispatcher.class);
  private final NotificationDispatcher commentOnReviewCreatedByMe = mock(NotificationDispatcher.class);

  private NotificationService service;

  private void setUpMocks(String creator, String assignee) {
    when(emailChannel.getKey()).thenReturn("email");
    when(gtalkChannel.getKey()).thenReturn("gtalk");
    when(commentOnReviewAssignedToMe.getKey()).thenReturn("comment on review assigned to me");
    when(commentOnReviewCreatedByMe.getKey()).thenReturn("comment on review created by me");
    when(queueElement.getNotification()).thenReturn(notification);
    when(manager.getFromQueue()).thenReturn(queueElement).thenReturn(null);
    when(manager.getChannels()).thenReturn(Lists.newArrayList(emailChannel, gtalkChannel));

    Settings settings = new Settings().setProperty("sonar.notifications.delay", 1L);

    service = new NotificationService(settings, manager,
        new NotificationDispatcher[] {commentOnReviewAssignedToMe, commentOnReviewCreatedByMe});
  }

  /**
   * Given:
   * Simon wants to receive notifications by email on comments for reviews assigned to him or created by him.
   *
   * When:
   * Freddy adds comment to review created by Simon and assigned to Simon.
   * 
   * Then:
   * Only one notification should be delivered to Simon by Email.
   */
  @Test
  public void scenario1() {
    setUpMocks(CREATOR_SIMON, ASSIGNEE_SIMON);
    doAnswer(addUser(ASSIGNEE_SIMON, emailChannel)).when(commentOnReviewAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));
    doAnswer(addUser(CREATOR_SIMON, emailChannel)).when(commentOnReviewCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    service.start();
    verify(emailChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    service.stop();

    verify(gtalkChannel, never()).deliver(notification, ASSIGNEE_SIMON);
  }

  /**
   * Given:
   * Evgeny wants to receive notification by GTalk on comments for reviews created by him.
   * Simon wants to receive notification by Email on comments for reviews assigned to him.
   * 
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * 
   * Then:
   * Two notifications should be delivered - one to Simon by Email and another to Evgeny by GTalk.
   */
  @Test
  public void scenario2() {
    setUpMocks(CREATOR_EVGENY, ASSIGNEE_SIMON);
    doAnswer(addUser(ASSIGNEE_SIMON, emailChannel)).when(commentOnReviewAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));
    doAnswer(addUser(CREATOR_EVGENY, gtalkChannel)).when(commentOnReviewCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    service.start();
    verify(emailChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    verify(gtalkChannel, timeout(2000)).deliver(notification, CREATOR_EVGENY);
    service.stop();

    verify(emailChannel, never()).deliver(notification, CREATOR_EVGENY);
    verify(gtalkChannel, never()).deliver(notification, ASSIGNEE_SIMON);
  }

  /**
   * Given:
   * Simon wants to receive notifications by Email and GTLak on comments for reviews assigned to him.
   * 
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * 
   * Then:
   * Two notifications should be delivered to Simon - one by Email and another by GTalk.
   */
  @Test
  public void scenario3() {
    setUpMocks(CREATOR_EVGENY, ASSIGNEE_SIMON);
    doAnswer(addUser(ASSIGNEE_SIMON, new NotificationChannel[] {emailChannel, gtalkChannel}))
        .when(commentOnReviewAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    service.start();
    verify(emailChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    verify(gtalkChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    service.stop();

    verify(emailChannel, never()).deliver(notification, CREATOR_EVGENY);
    verify(gtalkChannel, never()).deliver(notification, CREATOR_EVGENY);
  }

  /**
   * Given:
   * Nobody wants to receive notifications.
   * 
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * 
   * Then:
   * No notifications.
   */
  @Test
  public void scenario4() {
    setUpMocks(CREATOR_EVGENY, ASSIGNEE_SIMON);

    service.start();
    service.stop();

    verify(emailChannel, never()).deliver(any(Notification.class), anyString());
    verify(gtalkChannel, never()).deliver(any(Notification.class), anyString());
  }

  @Test
  public void shouldNotAddNullAsUser() {
    setUpMocks(CREATOR_EVGENY, ASSIGNEE_SIMON);
    doAnswer(addUser(null, gtalkChannel)).when(commentOnReviewCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    service.start();
    service.stop();

    verify(emailChannel, never()).deliver(any(Notification.class), anyString());
    verify(gtalkChannel, never()).deliver(any(Notification.class), anyString());
  }

  @Test
  public void shouldReturnDispatcherAndChannelListsUsedInWebapp() {
    setUpMocks(CREATOR_SIMON, ASSIGNEE_SIMON);

    assertThat(service.getChannels()).containsOnly(emailChannel, gtalkChannel);
    assertThat(service.getDispatchers()).containsOnly(commentOnReviewAssignedToMe, commentOnReviewCreatedByMe);
  }

  @Test
  public void shouldReturnNoDispatcher() {
    Settings settings = new Settings().setProperty("sonar.notifications.delay", 1L);

    service = new NotificationService(settings, manager);
    assertThat(service.getDispatchers()).hasSize(0);
  }

  private static Answer<Object> addUser(final String user, final NotificationChannel channel) {
    return addUser(user, new NotificationChannel[] {channel});
  }

  private static Answer<Object> addUser(final String user, final NotificationChannel[] channels) {
    return new Answer<Object>() {
      public Object answer(InvocationOnMock invocation) {
        for (NotificationChannel channel : channels) {
          ((NotificationDispatcher.Context) invocation.getArguments()[1]).addUser(user, channel);
        }
        return null;
      }
    };
  }
}
