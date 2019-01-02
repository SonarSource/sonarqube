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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NotificationDispatcherTest {

  @Mock
  private NotificationChannel channel;

  @Mock
  private Notification notification;

  @Mock
  private NotificationDispatcher.Context context;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    when(notification.getType()).thenReturn("event1");
  }

  @Test
  public void defaultMethods() {
    NotificationDispatcher dispatcher = new FakeGenericNotificationDispatcher();
    assertThat(dispatcher.getKey(), is("FakeGenericNotificationDispatcher"));
    assertThat(dispatcher.toString(), is("FakeGenericNotificationDispatcher"));
  }

  @Test
  public void shouldAlwaysRunDispatchForGenericDispatcher() {
    NotificationDispatcher dispatcher = new FakeGenericNotificationDispatcher();
    dispatcher.performDispatch(notification, context);

    verify(context, times(1)).addUser("user1", channel);
  }

  @Test
  public void shouldNotAlwaysRunDispatchForSpecificDispatcher() {
    NotificationDispatcher dispatcher = new FakeSpecificNotificationDispatcher();

    // a "event1" notif is sent
    dispatcher.performDispatch(notification, context);
    verify(context, never()).addUser("user1", channel);

    // now, a "specific-event" notif is sent
    when(notification.getType()).thenReturn("specific-event");
    dispatcher.performDispatch(notification, context);
    verify(context, times(1)).addUser("user1", channel);
  }

  class FakeGenericNotificationDispatcher extends NotificationDispatcher {
    @Override
    public void dispatch(Notification notification, Context context) {
      context.addUser("user1", channel);
    }
  }

  class FakeSpecificNotificationDispatcher extends NotificationDispatcher {

    public FakeSpecificNotificationDispatcher() {
      super("specific-event");
    }

    @Override
    public void dispatch(Notification notification, Context context) {
      context.addUser("user1", channel);
    }
  }

}
