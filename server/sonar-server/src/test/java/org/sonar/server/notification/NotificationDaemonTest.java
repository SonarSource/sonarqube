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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.verification.Timeout;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.Notification;

import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

public class NotificationDaemonTest {
  private DefaultNotificationManager manager = mock(DefaultNotificationManager.class);
  private NotificationService notificationService = mock(NotificationService.class);
  private NotificationDaemon underTest;
  private InOrder inOrder;

  @Before
  public void setUp() throws Exception {
    MapSettings settings = new MapSettings(new PropertyDefinitions(NotificationDaemon.class)).setProperty("sonar.notifications.delay", 1L);

    underTest = new NotificationDaemon(settings.asConfig(), manager, notificationService);
    inOrder = Mockito.inOrder(notificationService);
  }

  @After
  public void tearDown() {
    underTest.stop();
  }

  @Test
  public void no_effect_when_no_notification() {
    when(manager.getFromQueue()).thenReturn(null);

    underTest.start();
    inOrder.verify(notificationService, new Timeout(2000, Mockito.times(0))).deliverEmails(anyCollection());
    inOrder.verifyNoMoreInteractions();
    underTest.stop();
  }

  @Test
  public void calls_both_api_and_deprecated_API() {
    Notification notification = mock(Notification.class);
    when(manager.getFromQueue()).thenReturn(notification).thenReturn(null);

    underTest.start();
    inOrder.verify(notificationService, timeout(2000)).deliverEmails(singleton(notification));
    inOrder.verify(notificationService).deliver(notification);
    inOrder.verifyNoMoreInteractions();
    underTest.stop();
  }

  @Test
  public void notifications_are_processed_one_by_one_even_with_new_API() {
    Notification notification1 = mock(Notification.class);
    Notification notification2 = mock(Notification.class);
    Notification notification3 = mock(Notification.class);
    Notification notification4 = mock(Notification.class);
    when(manager.getFromQueue())
      .thenReturn(notification1)
      .thenReturn(notification2)
      .thenReturn(notification3)
      .thenReturn(notification4)
      .thenReturn(null);

    underTest.start();
    inOrder.verify(notificationService, timeout(2000)).deliverEmails(singleton(notification1));
    inOrder.verify(notificationService).deliver(notification1);
    inOrder.verify(notificationService, timeout(2000)).deliverEmails(singleton(notification2));
    inOrder.verify(notificationService).deliver(notification2);
    inOrder.verify(notificationService, timeout(2000)).deliverEmails(singleton(notification3));
    inOrder.verify(notificationService).deliver(notification3);
    inOrder.verify(notificationService, timeout(2000)).deliverEmails(singleton(notification4));
    inOrder.verify(notificationService).deliver(notification4);
    inOrder.verifyNoMoreInteractions();
    underTest.stop();

  }
}
