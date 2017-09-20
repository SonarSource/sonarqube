/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.Multimap;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.notification.NotificationQueueDao;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.Subscriber;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DefaultNotificationManagerTest {

  private DefaultNotificationManager underTest;

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

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

  @Mock
  private AuthorizationDao authorizationDao;

  @Mock
  private DbClient dbClient;

  @Captor
  private ArgumentCaptor<List<String>> captorLogins;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(dispatcher.getKey()).thenReturn("NewViolations");
    when(emailChannel.getKey()).thenReturn("Email");
    when(twitterChannel.getKey()).thenReturn("Twitter");
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(dbClient.notificationQueueDao()).thenReturn(notificationQueueDao);
    when(dbClient.authorizationDao()).thenReturn(authorizationDao);

    underTest = new DefaultNotificationManager(new NotificationChannel[] {emailChannel, twitterChannel}, dbClient);
  }

  @Test
  public void shouldProvideChannelList() {
    assertThat(underTest.getChannels()).containsOnly(emailChannel, twitterChannel);

    underTest = new DefaultNotificationManager(new NotificationChannel[] {}, db.getDbClient());
    assertThat(underTest.getChannels()).hasSize(0);
  }

  @Test
  public void shouldPersist() {
    Notification notification = new Notification("test");
    underTest.scheduleForSending(notification);

    verify(notificationQueueDao, only()).insert(any(List.class));
  }

  @Test
  public void shouldGetFromQueueAndDelete() {
    Notification notification = new Notification("test");
    NotificationQueueDto dto = NotificationQueueDto.toNotificationQueueDto(notification);
    List<NotificationQueueDto> dtos = Arrays.asList(dto);
    when(notificationQueueDao.selectOldest(1)).thenReturn(dtos);

    assertThat(underTest.getFromQueue()).isNotNull();

    InOrder inOrder = inOrder(notificationQueueDao);
    inOrder.verify(notificationQueueDao).selectOldest(1);
    inOrder.verify(notificationQueueDao).delete(dtos);
  }

  // SONAR-4739
  @Test
  public void shouldNotFailWhenUnableToDeserialize() throws Exception {
    NotificationQueueDto dto1 = mock(NotificationQueueDto.class);
    when(dto1.toNotification()).thenThrow(new InvalidClassException("Pouet"));
    List<NotificationQueueDto> dtos = Arrays.asList(dto1);
    when(notificationQueueDao.selectOldest(1)).thenReturn(dtos);

    underTest = spy(underTest);
    assertThat(underTest.getFromQueue()).isNull();
    assertThat(underTest.getFromQueue()).isNull();

    verify(underTest, times(1)).logDeserializationIssue();
  }

  @Test
  public void shouldFindNoRecipient() {
    assertThat(underTest.findSubscribedRecipientsForDispatcher(dispatcher, "uuid_45").asMap().entrySet()).hasSize(0);
  }

  @Test
  public void shouldFindSubscribedRecipientForGivenResource() {
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", "uuid_45"))
      .thenReturn(newHashSet(new Subscriber("user1", false), new Subscriber("user3", false), new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", "uuid_56"))
      .thenReturn(newHashSet(new Subscriber("user2", false)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", "uuid_45"))
      .thenReturn(newHashSet(new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewAlerts", "Twitter", "uuid_45"))
      .thenReturn(newHashSet(new Subscriber("user4", false)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(any(), eq(newHashSet("user1", "user3")), eq("uuid_45"), eq("user")))
      .thenReturn(newHashSet("user1", "user3"));
    when(authorizationDao.keepAuthorizedLoginsOnProject(any(), eq(newHashSet("user3")), eq("uuid_45"), eq("user")))
      .thenReturn(newHashSet("user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, "uuid_45");
    assertThat(multiMap.entries()).hasSize(3);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user1")).containsOnly(emailChannel);
    assertThat(map.get("user2")).isNull();
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);
    assertThat(map.get("user4")).isNull();
  }
}
