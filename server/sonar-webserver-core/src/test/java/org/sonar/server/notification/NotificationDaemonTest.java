/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.notifications.Notification;
import org.sonar.core.platform.SpringComponentContainer;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class NotificationDaemonTest {
  private DefaultNotificationManager manager = mock(DefaultNotificationManager.class);
  private NotificationService notificationService = mock(NotificationService.class);
  private ScheduledExecutorService executorService = mock(ScheduledExecutorService.class);
  private MapSettings settings;
  private NotificationDaemon underTest;
  private InOrder inOrder;

  @Before
  public void setUp() {
    settings = new MapSettings()
      .setProperty(NotificationDaemon.PROPERTY_DELAY, 1L)
      .setProperty(NotificationDaemon.PROPERTY_DELAY_BEFORE_REPORTING_STATUS, 600L);

    underTest = new NotificationDaemon(settings.asConfig(), manager, notificationService, () -> executorService);
    inOrder = Mockito.inOrder(notificationService);
  }

  @Test
  public void stop_shuts_down_executor() throws InterruptedException {
    underTest.start();
    underTest.stop();

    verify(executorService).shutdown();
    verify(executorService).awaitTermination(5, TimeUnit.SECONDS);
  }

  @Test
  public void container_starts_and_stops_daemon_with_default_executor() {
    SpringComponentContainer container = new SpringComponentContainer();
    container.add(settings.asConfig(), manager, notificationService, NotificationDaemon.class);

    container.startComponents();
    assertThat(container.getComponentByType(NotificationDaemon.class)).isNotNull();
    container.stopComponents();
  }

  @Test
  public void no_effect_when_no_notification() {
    when(manager.getFromQueue()).thenReturn(null);

    startAndCaptureScheduledTask().run();

    verifyNoInteractions(notificationService);
  }

  @Test
  public void calls_both_api_and_deprecated_API() {
    Notification notification = mock(Notification.class);
    when(manager.getFromQueue()).thenReturn(notification).thenReturn(null);

    startAndCaptureScheduledTask().run();

    inOrder.verify(notificationService).deliverEmails(singleton(notification));
    inOrder.verify(notificationService).deliver(notification);
    inOrder.verifyNoMoreInteractions();
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

    startAndCaptureScheduledTask().run();

    inOrder.verify(notificationService).deliverEmails(singleton(notification1));
    inOrder.verify(notificationService).deliver(notification1);
    inOrder.verify(notificationService).deliverEmails(singleton(notification2));
    inOrder.verify(notificationService).deliver(notification2);
    inOrder.verify(notificationService).deliverEmails(singleton(notification3));
    inOrder.verify(notificationService).deliver(notification3);
    inOrder.verify(notificationService).deliverEmails(singleton(notification4));
    inOrder.verify(notificationService).deliver(notification4);
    inOrder.verifyNoMoreInteractions();
  }

  private Runnable startAndCaptureScheduledTask() {
    underTest.start();
    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    verify(executorService).scheduleWithFixedDelay(task.capture(), eq(0L), eq(1L), eq(TimeUnit.SECONDS));
    return task.getValue();
  }
}
