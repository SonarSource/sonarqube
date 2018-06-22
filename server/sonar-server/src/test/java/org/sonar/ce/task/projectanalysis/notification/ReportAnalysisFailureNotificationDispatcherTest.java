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
package org.sonar.ce.notification;

import com.google.common.collect.HashMultimap;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.web.UserRole;
import org.sonar.server.notification.NotificationDispatcher;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ReportAnalysisFailureNotificationDispatcherTest {
  private NotificationManager notificationManager = mock(NotificationManager.class);
  private Notification notificationMock = mock(Notification.class);
  private NotificationDispatcher.Context contextMock = mock(NotificationDispatcher.Context.class);
  private ReportAnalysisFailureNotificationDispatcher underTest = new ReportAnalysisFailureNotificationDispatcher(notificationManager);

  @Test
  public void dispatcher_defines_key() {
    assertThat(underTest.getKey()).isNotEmpty();
  }

  @Test
  public void newMetadata_indicates_enabled_global_and_project_level_notifications() {
    NotificationDispatcherMetadata metadata = ReportAnalysisFailureNotificationDispatcher.newMetadata();

    assertThat(metadata.getProperty(NotificationDispatcherMetadata.GLOBAL_NOTIFICATION)).isEqualTo("true");
    assertThat(metadata.getProperty(NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION)).isEqualTo("true");
  }

  @Test
  public void performDispatch_has_no_effect_if_type_is_empty() {
    when(notificationMock.getType()).thenReturn("");

    underTest.performDispatch(notificationMock, contextMock);

    verify(notificationMock).getType();
    verifyNoMoreInteractions(notificationMock, contextMock);
  }

  @Test
  public void performDispatch_has_no_effect_if_type_is_not_ReportAnalysisFailureNotification_TYPE() {
    when(notificationMock.getType()).thenReturn(randomAlphanumeric(6));

    underTest.performDispatch(notificationMock, contextMock);

    verify(notificationMock).getType();
    verifyNoMoreInteractions(notificationMock, contextMock);
  }

  @Test
  public void performDispatch_adds_user_for_each_recipient_and_channel_for_the_component_uuid_in_the_notification() {
    when(notificationMock.getType()).thenReturn(ReportAnalysisFailureNotification.TYPE);
    String projectKey = randomAlphanumeric(9);
    when(notificationMock.getFieldValue("project.key")).thenReturn(projectKey);
    HashMultimap<String, NotificationChannel> multimap = HashMultimap.create();
    String login1 = randomAlphanumeric(3);
    String login2 = randomAlphanumeric(3);
    NotificationChannel channel1 = mock(NotificationChannel.class);
    NotificationChannel channel2 = mock(NotificationChannel.class);
    NotificationChannel channel3 = mock(NotificationChannel.class);
    multimap.put(login1, channel1);
    multimap.put(login1, channel2);
    multimap.put(login2, channel2);
    multimap.put(login2, channel3);
    when(notificationManager.findSubscribedRecipientsForDispatcher(underTest, projectKey, new SubscriberPermissionsOnProject(UserRole.ADMIN, UserRole.USER)))
      .thenReturn(multimap);

    underTest.performDispatch(notificationMock, contextMock);

    verify(contextMock).addUser(login1, channel1);
    verify(contextMock).addUser(login1, channel2);
    verify(contextMock).addUser(login2, channel2);
    verify(contextMock).addUser(login2, channel3);
    verifyNoMoreInteractions(contextMock);
  }

  @Test
  public void performDispatch_adds_no_user_if_notification_manager_returns_none() {
    when(notificationMock.getType()).thenReturn(ReportAnalysisFailureNotification.TYPE);
    String projectKey = randomAlphanumeric(9);
    when(notificationMock.getFieldValue("project.key")).thenReturn(projectKey);
    HashMultimap<String, NotificationChannel> multimap = HashMultimap.create();
    when(notificationManager.findSubscribedRecipientsForDispatcher(underTest, projectKey, new SubscriberPermissionsOnProject(UserRole.ADMIN, UserRole.USER)))
      .thenReturn(multimap);

    underTest.performDispatch(notificationMock, contextMock);

    verifyNoMoreInteractions(contextMock);
  }
}
