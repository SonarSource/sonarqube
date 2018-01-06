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
package org.sonar.server.notification;

import com.google.common.collect.Multimap;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.notification.NotificationQueueDao;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.Subscriber;
import org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
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

  private PropertiesDao propertiesDao = mock(PropertiesDao.class);
  private NotificationDispatcher dispatcher = mock(NotificationDispatcher.class);
  private NotificationChannel emailChannel = mock(NotificationChannel.class);
  private NotificationChannel twitterChannel = mock(NotificationChannel.class);
  private NotificationQueueDao notificationQueueDao = mock(NotificationQueueDao.class);
  private AuthorizationDao authorizationDao = mock(AuthorizationDao.class);
  private DbClient dbClient = mock(DbClient.class);
  private DbSession dbSession = mock(DbSession.class);

  @Before
  public void setUp() {
    when(dispatcher.getKey()).thenReturn("NewViolations");
    when(emailChannel.getKey()).thenReturn("Email");
    when(twitterChannel.getKey()).thenReturn("Twitter");
    when(dbClient.openSession(anyBoolean())).thenReturn(dbSession);
    when(dbClient.propertiesDao()).thenReturn(propertiesDao);
    when(dbClient.notificationQueueDao()).thenReturn(notificationQueueDao);
    when(dbClient.authorizationDao()).thenReturn(authorizationDao);

    underTest = new DefaultNotificationManager(new NotificationChannel[] {emailChannel, twitterChannel}, dbClient);
  }

  @Test
  public void shouldProvideChannelList() {
    assertThat(underTest.getChannels()).containsOnly(emailChannel, twitterChannel);

    underTest = new DefaultNotificationManager(new NotificationChannel[] {}, dbClient);
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
    assertThat(underTest.findSubscribedRecipientsForDispatcher(dispatcher, "uuid_45", new SubscriberPermissionsOnProject(UserRole.USER)).asMap().entrySet())
      .hasSize(0);
  }

  @Test
  public void shouldFindSubscribedRecipientForGivenResource() {
    String projectUuid = "uuid_45";
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", projectUuid))
      .thenReturn(newHashSet(new Subscriber("user1", false), new Subscriber("user3", false), new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", "uuid_56"))
      .thenReturn(newHashSet(new Subscriber("user2", false)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", projectUuid))
      .thenReturn(newHashSet(new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewAlerts", "Twitter", projectUuid))
      .thenReturn(newHashSet(new Subscriber("user4", false)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user1", "user3"), projectUuid, "user"))
      .thenReturn(newHashSet("user1", "user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, projectUuid,
      SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER);
    assertThat(multiMap.entries()).hasSize(3);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user1")).containsOnly(emailChannel);
    assertThat(map.get("user2")).isNull();
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);
    assertThat(map.get("user4")).isNull();

    // code is optimized to perform only 1 SQL requests for all channels
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), anyString());
  }

  @Test
  public void should_apply_distinct_permission_filtering_global_or_project_subscribers() {
    String globalPermission = RandomStringUtils.randomAlphanumeric(4);
    String projectPermission = RandomStringUtils.randomAlphanumeric(5);
    String projectUuid = "uuid_45";
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", projectUuid))
      .thenReturn(newHashSet(new Subscriber("user1", false), new Subscriber("user3", false), new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", "uuid_56"))
      .thenReturn(newHashSet(new Subscriber("user2", false)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", projectUuid))
      .thenReturn(newHashSet(new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewAlerts", "Twitter", projectUuid))
      .thenReturn(newHashSet(new Subscriber("user4", false)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user3", "user4"), projectUuid, globalPermission))
      .thenReturn(newHashSet("user3"));
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user1", "user3"), projectUuid, projectPermission))
      .thenReturn(newHashSet("user1", "user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, projectUuid,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(multiMap.entries()).hasSize(3);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user1")).containsOnly(emailChannel);
    assertThat(map.get("user2")).isNull();
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);
    assertThat(map.get("user4")).isNull();

    // code is optimized to perform only 2 SQL requests for all channels
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void do_not_call_db_for_project_permission_filtering_if_there_is_no_project_subscriber() {
    String globalPermission = RandomStringUtils.randomAlphanumeric(4);
    String projectPermission = RandomStringUtils.randomAlphanumeric(5);
    String projectUuid = "uuid_45";
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", projectUuid))
        .thenReturn(newHashSet(new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", projectUuid))
        .thenReturn(newHashSet(new Subscriber("user3", true)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user3"), projectUuid, globalPermission))
        .thenReturn(newHashSet("user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, projectUuid,
        new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(multiMap.entries()).hasSize(2);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);

    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void do_not_call_db_for_project_permission_filtering_if_there_is_no_global_subscriber() {
    String globalPermission = RandomStringUtils.randomAlphanumeric(4);
    String projectPermission = RandomStringUtils.randomAlphanumeric(5);
    String projectUuid = "uuid_45";
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", projectUuid))
        .thenReturn(newHashSet(new Subscriber("user3", false)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", projectUuid))
        .thenReturn(newHashSet(new Subscriber("user3", false)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user3"), projectUuid, projectPermission))
        .thenReturn(newHashSet("user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, projectUuid,
        new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(multiMap.entries()).hasSize(2);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);

    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }
}
