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
package org.sonar.server.notification;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.db.DbClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationMediumTest {
  private static String CREATOR_SIMON = "simon";
  private static String CREATOR_EVGENY = "evgeny";
  private static String ASSIGNEE_SIMON = "simon";

  private DefaultNotificationManager manager = mock(DefaultNotificationManager.class);
  private Notification notification = mock(Notification.class);
  private NotificationChannel emailChannel = mock(NotificationChannel.class);
  private NotificationChannel gtalkChannel = mock(NotificationChannel.class);
  private NotificationDispatcher commentOnIssueAssignedToMe = mock(NotificationDispatcher.class);
  private NotificationDispatcher commentOnIssueCreatedByMe = mock(NotificationDispatcher.class);
  private NotificationDispatcher qualityGateChange = mock(NotificationDispatcher.class);
  private DbClient dbClient = mock(DbClient.class);
  private NotificationService service = new NotificationService(dbClient, new NotificationDispatcher[] {commentOnIssueAssignedToMe, commentOnIssueCreatedByMe, qualityGateChange});
  private NotificationDaemon underTest = null;

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

    MapSettings settings = new MapSettings(new PropertyDefinitions(NotificationDaemon.class)).setProperty("sonar.notifications.delay", 1L);

    underTest = new NotificationDaemon(settings.asConfig(), manager, service);
  }

  /**
   * Given:
   * Simon wants to receive notifications by email on comments for reviews assigned to him or created by him.
   * <p/>
   * When:
   * Freddy adds comment to review created by Simon and assigned to Simon.
   * <p/>
   * Then:
   * Only one notification should be delivered to Simon by Email.
   */
  @Test
  public void scenario1() {
    setUpMocks();
    doAnswer(addUser(ASSIGNEE_SIMON, emailChannel)).when(commentOnIssueAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));
    doAnswer(addUser(CREATOR_SIMON, emailChannel)).when(commentOnIssueCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    underTest.start();
    verify(emailChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    underTest.stop();

    verify(gtalkChannel, never()).deliver(notification, ASSIGNEE_SIMON);
  }

  /**
   * Given:
   * Evgeny wants to receive notification by GTalk on comments for reviews created by him.
   * Simon wants to receive notification by Email on comments for reviews assigned to him.
   * <p/>
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * <p/>
   * Then:
   * Two notifications should be delivered - one to Simon by Email and another to Evgeny by GTalk.
   */
  @Test
  public void scenario2() {
    setUpMocks();
    doAnswer(addUser(ASSIGNEE_SIMON, emailChannel)).when(commentOnIssueAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));
    doAnswer(addUser(CREATOR_EVGENY, gtalkChannel)).when(commentOnIssueCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    underTest.start();
    verify(emailChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    verify(gtalkChannel, timeout(2000)).deliver(notification, CREATOR_EVGENY);
    underTest.stop();

    verify(emailChannel, never()).deliver(notification, CREATOR_EVGENY);
    verify(gtalkChannel, never()).deliver(notification, ASSIGNEE_SIMON);
  }

  /**
   * Given:
   * Simon wants to receive notifications by Email and GTLak on comments for reviews assigned to him.
   * <p/>
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * <p/>
   * Then:
   * Two notifications should be delivered to Simon - one by Email and another by GTalk.
   */
  @Test
  public void scenario3() {
    setUpMocks();
    doAnswer(addUser(ASSIGNEE_SIMON, new NotificationChannel[] {emailChannel, gtalkChannel}))
      .when(commentOnIssueAssignedToMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    underTest.start();
    verify(emailChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    verify(gtalkChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    underTest.stop();

    verify(emailChannel, never()).deliver(notification, CREATOR_EVGENY);
    verify(gtalkChannel, never()).deliver(notification, CREATOR_EVGENY);
  }

  /**
   * Given:
   * Nobody wants to receive notifications.
   * <p/>
   * When:
   * Freddy adds comment to review created by Evgeny and assigned to Simon.
   * <p/>
   * Then:
   * No notifications.
   */
  @Test
  public void scenario4() {
    setUpMocks();

    underTest.start();
    underTest.stop();

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

    underTest.start();
    verify(emailChannel, timeout(2000)).deliver(notification, ASSIGNEE_SIMON);
    underTest.stop();

    verify(gtalkChannel, never()).deliver(notification, ASSIGNEE_SIMON);
  }

  @Test
  public void shouldNotAddNullAsUser() {
    setUpMocks();
    doAnswer(addUser(null, gtalkChannel)).when(commentOnIssueCreatedByMe).dispatch(same(notification), any(NotificationDispatcher.Context.class));

    underTest.start();
    underTest.stop();

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
    Settings settings = new MapSettings().setProperty("sonar.notifications.delay", 1L);

    service = new NotificationService(dbClient);
    assertThat(service.getDispatchers()).hasSize(0);
  }

  @Test
  public void shouldLogEvery10Minutes() {
    setUpMocks();
    // Emulate 2 notifications in DB
    when(manager.getFromQueue()).thenReturn(notification).thenReturn(notification).thenReturn(null);
    when(manager.count()).thenReturn(1L).thenReturn(0L);
    underTest = spy(underTest);
    // Emulate processing of each notification take 10 min to have a log each time
    when(underTest.now()).thenReturn(0L).thenReturn(10 * 60 * 1000 + 1L).thenReturn(20 * 60 * 1000 + 2L);
    underTest.start();
    verify(underTest, timeout(200)).log(0, 1, 10);
    verify(underTest, timeout(200)).log(0, 0, 20);
    underTest.stop();
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
