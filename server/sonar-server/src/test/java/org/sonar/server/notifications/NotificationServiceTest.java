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
package org.sonar.server.notifications;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.Settings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.core.notification.NotificationDispatcher;
import org.sonar.core.notification.DefaultNotificationManager;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.server.db.DbClient;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class NotificationServiceTest {
  private static String CREATOR_SIMON = "simon";
  private static String CREATOR_EVGENY = "evgeny";
  private static String ASSIGNEE_SIMON = "simon";

  DefaultNotificationManager manager = mock(DefaultNotificationManager.class);
  Notification notification = mock(Notification.class);
  NotificationChannel emailChannel = mock(NotificationChannel.class);
  NotificationChannel gtalkChannel = mock(NotificationChannel.class);
  NotificationDispatcher commentOnIssueAssignedToMe = mock(NotificationDispatcher.class);
  NotificationDispatcher commentOnIssueCreatedByMe = mock(NotificationDispatcher.class);
  NotificationDispatcher qualityGateChange = mock(NotificationDispatcher.class);
  DbClient dbClient = mock(DbClient.class);

  private NotificationService service;

  private void setUpMocks() {
    when(emailChannel.getKey()).thenReturn("email");
    when(gtalkChannel.getKey()).thenReturn("gtalk");
    when(commentOnIssueAssignedToMe.getKey()).thenReturn("CommentOnIssueAssignedToMe");
    when(commentOnIssueAssignedToMe.getType()).thenReturn("issue-changes");
    when(commentOnIssueCreatedByMe.getKey()).thenReturn("CommentOnIssueCreatedByMe");
    when(commentOnIssueCreatedByMe.getType()).thenReturn("issue-changes");
    when(qualityGateChange.getKey()).thenReturn("QGateChange");
    when(qualityGateChange.getType()).thenReturn("qgate-changes");
    when(manager.getFromQueue()).thenReturn(notification).thenReturn(null);

    Settings settings = new Settings().setProperty("sonar.notifications.delay", 1L);

    service = new NotificationService(settings, manager,
      dbClient, mock(DatabaseSessionFactory.class),
      new NotificationDispatcher[] {commentOnIssueAssignedToMe, commentOnIssueCreatedByMe, qualityGateChange});
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
    setUpMocks();
    doAnswer(addUser(ASSIGNEE_SIMON, emailChannel)).when(commentOnIssueAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));
    doAnswer(addUser(CREATOR_SIMON, emailChannel)).when(commentOnIssueCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

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
    setUpMocks();
    doAnswer(addUser(ASSIGNEE_SIMON, emailChannel)).when(commentOnIssueAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));
    doAnswer(addUser(CREATOR_EVGENY, gtalkChannel)).when(commentOnIssueCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

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
    setUpMocks();
    doAnswer(addUser(ASSIGNEE_SIMON, new NotificationChannel[] {emailChannel, gtalkChannel}))
      .when(commentOnIssueAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

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
    setUpMocks();

    service.start();
    service.stop();

    verify(emailChannel, never()).deliver(any(Notification.class), anyString());
    verify(gtalkChannel, never()).deliver(any(Notification.class), anyString());
  }

  // SONAR-4548
  @Test
  public void shouldNotStopWhenException() {
    setUpMocks();
    when(manager.getFromQueue()).thenThrow(new RuntimeException("Unexpected exception")).thenReturn(notification).thenReturn(null);
    doAnswer(addUser(ASSIGNEE_SIMON, emailChannel)).when(commentOnIssueAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));
    doAnswer(addUser(CREATOR_SIMON, emailChannel)).when(commentOnIssueCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    service.start();
    verify(emailChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    service.stop();

    verify(gtalkChannel, never()).deliver(notification, ASSIGNEE_SIMON);
  }

  @Test
  public void shouldNotAddNullAsUser() {
    setUpMocks();
    doAnswer(addUser(null, gtalkChannel)).when(commentOnIssueCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    service.start();
    service.stop();

    verify(emailChannel, never()).deliver(any(Notification.class), anyString());
    verify(gtalkChannel, never()).deliver(any(Notification.class), anyString());
  }

  @Test
  public void getDispatchers() {
    setUpMocks();

    assertThat(service.getDispatchers()).containsOnly(commentOnIssueAssignedToMe, commentOnIssueCreatedByMe, qualityGateChange);
  }

  @Test
  public void getDispatchers_empty() {
    Settings settings = new Settings().setProperty("sonar.notifications.delay", 1L);

    service = new NotificationService(settings, manager, dbClient, mock(DatabaseSessionFactory.class));
    assertThat(service.getDispatchers()).hasSize(0);
  }

  @Test
  public void shouldLogEvery10Minutes() {
    setUpMocks();
    // Emulate 2 notifications in DB
    when(manager.getFromQueue()).thenReturn(notification).thenReturn(notification).thenReturn(null);
    when(manager.count()).thenReturn(1L).thenReturn(0L);
    service = spy(service);
    // Emulate processing of each notification take 10 min to have a log each time
    when(service.now()).thenReturn(0L).thenReturn(10 * 60 * 1000 + 1L).thenReturn(20 * 60 * 1000 + 2L);
    service.start();
    verify(service, timeout(200)).log(1, 1, 10);
    verify(service, timeout(200)).log(2, 0, 20);
    service.stop();
  }

  @Test
  public void hasProjectSubscribersForType() {
    setUpMocks();

    PropertiesDao dao = mock(PropertiesDao.class);
    when(dbClient.propertiesDao()).thenReturn(dao);

    // no subscribers
    when(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_UUID", Arrays.asList("CommentOnIssueAssignedToMe", "CommentOnIssueCreatedByMe"))).thenReturn(false);
    assertThat(service.hasProjectSubscribersForTypes("PROJECT_UUID", Sets.newHashSet("issue-changes"))).isFalse();

    // has subscribers on one dispatcher (among the two)
    when(dao.hasProjectNotificationSubscribersForDispatchers("PROJECT_UUID", Arrays.asList("CommentOnIssueAssignedToMe", "CommentOnIssueCreatedByMe"))).thenReturn(true);
    assertThat(service.hasProjectSubscribersForTypes("PROJECT_UUID", Sets.newHashSet("issue-changes"))).isTrue();
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
