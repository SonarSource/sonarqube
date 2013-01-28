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
package org.sonar.plugins.emailnotifications.newviolations;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.core.properties.PropertiesDao;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NewViolationsOnMyFavouriteProjectTest {

  @Test
  public void shouldNotDispatchIfNotNewViolationsNotification() throws Exception {
    NotificationDispatcher.Context context = mock(NotificationDispatcher.Context.class);
    NewViolationsOnMyFavouriteProject dispatcher = new NewViolationsOnMyFavouriteProject(null);
    Notification notification = new Notification("other-notif");
    dispatcher.performDispatch(notification, context);

    verify(context, never()).addUser(any(String.class));
  }

  @Test
  public void shouldDispatchToUsersWhoHaveFlaggedProjectAsFavourite() {
    NotificationDispatcher.Context context = mock(NotificationDispatcher.Context.class);
    PropertiesDao propertiesDao = mock(PropertiesDao.class);
    when(propertiesDao.findUserIdsForFavouriteResource(34L)).thenReturn(Lists.newArrayList("user1", "user2"));
    NewViolationsOnMyFavouriteProject dispatcher = new NewViolationsOnMyFavouriteProject(propertiesDao);

    Notification notification = new Notification("new-violations").setFieldValue("projectId", "34");
    dispatcher.performDispatch(notification, context);

    verify(context).addUser("user1");
    verify(context).addUser("user2");
    verifyNoMoreInteractions(context);
  }

}
