/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.notification;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.notifications.NotificationDispatcher;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Collection;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class DefaultNotificationManagerTest extends AbstractDbUnitTestCase {

  private DefaultNotificationManager manager;

  @Mock
  private PropertiesDao propertiesDao;

  @Mock
  private NotificationDispatcher dispatcher;

  @Mock
  private NotificationChannel emailChannel;

  @Mock
  private NotificationChannel twitterChannel;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(dispatcher.getKey()).thenReturn("NewViolations");
    when(emailChannel.getKey()).thenReturn("Email");
    when(twitterChannel.getKey()).thenReturn("Twitter");

    manager = new DefaultNotificationManager(new NotificationChannel[] {emailChannel, twitterChannel}, getSessionFactory(), propertiesDao);
  }

  @Test
  public void shouldProvideChannelList() {
    assertThat(manager.getChannels()).containsOnly(emailChannel, twitterChannel);

    manager = new DefaultNotificationManager(getSessionFactory(), propertiesDao);
    assertThat(manager.getChannels()).hasSize(0);
  }

  @Test
  public void shouldPersist() throws Exception {
    Notification notification = new Notification("test");
    manager.scheduleForSending(notification);

    NotificationQueueElement queueElement = manager.getFromQueue();
    assertThat(queueElement.getNotification(), is(notification));

    assertThat(manager.getFromQueue(), nullValue());
  }

  @Test
  public void shouldFindNoRecipient() {
    assertThat(manager.findSubscribedRecipientsForDispatcher(dispatcher, 45).asMap().entrySet()).hasSize(0);
  }

  @Test
  public void shouldFindSubscribedRecipientForGivenResource() {
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", 45L)).thenReturn(Lists.newArrayList("user1", "user2"));
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", null)).thenReturn(Lists.newArrayList("user1", "user3"));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", 56L)).thenReturn(Lists.newArrayList("user2"));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", null)).thenReturn(Lists.newArrayList("user3"));
    when(propertiesDao.findUsersForNotification("NewAlerts", "Twitter", null)).thenReturn(Lists.newArrayList("user4"));

    Multimap<String, NotificationChannel> multiMap = manager.findSubscribedRecipientsForDispatcher(dispatcher, 45);
    assertThat(multiMap.entries()).hasSize(4);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user1")).containsOnly(emailChannel);
    assertThat(map.get("user2")).containsOnly(emailChannel);
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);
    assertThat(map.get("user4")).isNull();
  }

  @Test
  public void shouldFindSubscribedRecipientForNoResource() {
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", 45L)).thenReturn(Lists.newArrayList("user1", "user2"));
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", null)).thenReturn(Lists.newArrayList("user1", "user3"));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", 56L)).thenReturn(Lists.newArrayList("user2"));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", null)).thenReturn(Lists.newArrayList("user3"));
    when(propertiesDao.findUsersForNotification("NewAlerts", "Twitter", null)).thenReturn(Lists.newArrayList("user4"));

    Multimap<String, NotificationChannel> multiMap = manager.findSubscribedRecipientsForDispatcher(dispatcher, null);
    assertThat(multiMap.entries()).hasSize(3);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user1")).containsOnly(emailChannel);
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);
    assertThat(map.get("user2")).isNull();
    assertThat(map.get("user4")).isNull();
  }

}
