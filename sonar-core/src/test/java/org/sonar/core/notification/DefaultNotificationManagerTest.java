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
package org.sonar.core.notification;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.core.notification.db.NotificationQueueDao;
import org.sonar.core.notification.db.NotificationQueueDto;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

  @Mock
  private NotificationQueueDao notificationQueueDao;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(dispatcher.getKey()).thenReturn("NewViolations");
    when(emailChannel.getKey()).thenReturn("Email");
    when(twitterChannel.getKey()).thenReturn("Twitter");

    manager = new DefaultNotificationManager(new NotificationChannel[] {emailChannel, twitterChannel}, notificationQueueDao, propertiesDao);
  }

  @Test
  public void shouldProvideChannelList() {
    assertThat(manager.getChannels()).containsOnly(emailChannel, twitterChannel);

    manager = new DefaultNotificationManager(notificationQueueDao, propertiesDao);
    assertThat(manager.getChannels()).hasSize(0);
  }

  @Test
  public void shouldPersist() {
    Notification notification = new Notification("test");
    manager.scheduleForSending(notification);

    verify(notificationQueueDao, only()).insert(any(List.class));
  }

  @Test
  public void shouldGetFromQueueAndDelete() {
    Notification notification = new Notification("test");
    NotificationQueueDto dto = NotificationQueueDto.toNotificationQueueDto(notification);
    List<NotificationQueueDto> dtos = Arrays.asList(dto);
    when(notificationQueueDao.findOldest(1)).thenReturn(dtos);

    assertThat(manager.getFromQueue()).isNotNull();

    InOrder inOrder = inOrder(notificationQueueDao);
    inOrder.verify(notificationQueueDao).findOldest(1);
    inOrder.verify(notificationQueueDao).delete(dtos);
  }

  // SONAR-4739
  @Test
  public void shouldNotFailWhenUnableToDeserialize() throws Exception {
    NotificationQueueDto dto1 = mock(NotificationQueueDto.class);
    when(dto1.toNotification()).thenThrow(new InvalidClassException("Pouet"));
    List<NotificationQueueDto> dtos = Arrays.asList(dto1);
    when(notificationQueueDao.findOldest(1)).thenReturn(dtos);

    manager = spy(manager);
    assertThat(manager.getFromQueue()).isNull();
    assertThat(manager.getFromQueue()).isNull();

    verify(manager, times(1)).logDeserializationIssue();
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

    Multimap<String, NotificationChannel> multiMap = manager.findSubscribedRecipientsForDispatcher(dispatcher, (Integer) null);
    assertThat(multiMap.entries()).hasSize(3);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user1")).containsOnly(emailChannel);
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);
    assertThat(map.get("user2")).isNull();
    assertThat(map.get("user4")).isNull();
  }

  @Test
  public void findNotificationSubscribers() {
    when(propertiesDao.findNotificationSubscribers("NewViolations", "Email", "struts")).thenReturn(Lists.newArrayList("user1", "user2"));
    when(propertiesDao.findNotificationSubscribers("NewViolations", "Twitter", "struts")).thenReturn(Lists.newArrayList("user2"));

    Multimap<String, NotificationChannel> multiMap = manager.findNotificationSubscribers(dispatcher, "struts");
    assertThat(multiMap.entries()).hasSize(3);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user1")).containsOnly(emailChannel);
    assertThat(map.get("user2")).containsOnly(emailChannel, twitterChannel);
    assertThat(map.get("other")).isNull();
  }
}
