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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.io.InvalidClassException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.EmailSubscriberDto;
import org.sonar.db.notification.NotificationQueueDao;
import org.sonar.db.notification.NotificationQueueDto;
import org.sonar.db.permission.AuthorizationDao;
import org.sonar.db.property.PropertiesDao;
import org.sonar.db.property.Subscriber;
import org.sonar.server.notification.NotificationManager.EmailRecipient;
import org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject;

import static com.google.common.collect.Sets.newHashSet;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.sonar.server.notification.NotificationManager.SubscriberPermissionsOnProject.ALL_MUST_HAVE_ROLE_USER;

public class DefaultNotificationManagerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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

    assertThat(underTest.<Notification>getFromQueue()).isNotNull();

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
    assertThat(underTest.<Notification>getFromQueue()).isNull();
    assertThat(underTest.<Notification>getFromQueue()).isNull();

    verify(underTest, times(1)).logDeserializationIssue();
  }

  @Test
  public void shouldFindNoRecipient() {
    assertThat(underTest.findSubscribedRecipientsForDispatcher(dispatcher, "uuid_45", new SubscriberPermissionsOnProject(UserRole.USER)).asMap().entrySet())
      .hasSize(0);
  }

  @Test
  public void shouldFindSubscribedRecipientForGivenResource() {
    String projectKey = randomAlphabetic(6);
    String otherProjectKey = randomAlphabetic(7);
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", projectKey))
      .thenReturn(newHashSet(new Subscriber("user1", false), new Subscriber("user3", false), new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", otherProjectKey))
      .thenReturn(newHashSet(new Subscriber("user2", false)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", projectKey))
      .thenReturn(newHashSet(new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewAlerts", "Twitter", projectKey))
      .thenReturn(newHashSet(new Subscriber("user4", false)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user1", "user3"), projectKey, "user"))
      .thenReturn(newHashSet("user1", "user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, projectKey,
      ALL_MUST_HAVE_ROLE_USER);
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
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    String otherProjectKey = randomAlphabetic(7);
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", projectKey))
      .thenReturn(newHashSet(new Subscriber("user1", false), new Subscriber("user3", false), new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", otherProjectKey))
      .thenReturn(newHashSet(new Subscriber("user2", false)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", projectKey))
      .thenReturn(newHashSet(new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewAlerts", "Twitter", projectKey))
      .thenReturn(newHashSet(new Subscriber("user4", false)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user3", "user4"), projectKey, globalPermission))
      .thenReturn(newHashSet("user3"));
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user1", "user3"), projectKey, projectPermission))
      .thenReturn(newHashSet("user1", "user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, projectKey,
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
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", projectKey))
      .thenReturn(newHashSet(new Subscriber("user3", true)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", projectKey))
      .thenReturn(newHashSet(new Subscriber("user3", true)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user3"), projectKey, globalPermission))
      .thenReturn(newHashSet("user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, projectKey,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(multiMap.entries()).hasSize(2);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);

    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void do_not_call_db_for_project_permission_filtering_if_there_is_no_global_subscriber() {
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    when(propertiesDao.findUsersForNotification("NewViolations", "Email", projectKey))
      .thenReturn(newHashSet(new Subscriber("user3", false)));
    when(propertiesDao.findUsersForNotification("NewViolations", "Twitter", projectKey))
      .thenReturn(newHashSet(new Subscriber("user3", false)));

    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user3"), projectKey, projectPermission))
      .thenReturn(newHashSet("user3"));

    Multimap<String, NotificationChannel> multiMap = underTest.findSubscribedRecipientsForDispatcher(dispatcher, projectKey,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(multiMap.entries()).hasSize(2);

    Map<String, Collection<NotificationChannel>> map = multiMap.asMap();
    assertThat(map.get("user3")).containsOnly(emailChannel, twitterChannel);

    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void findSubscribedEmailRecipients_fails_with_NPE_if_projectKey_is_null() {
    String dispatcherKey = randomAlphabetic(12);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("projectKey is mandatory");

    underTest.findSubscribedEmailRecipients(dispatcherKey, null, ALL_MUST_HAVE_ROLE_USER);
  }

  @Test
  public void findSubscribedEmailRecipients_with_logins_fails_with_NPE_if_projectKey_is_null() {
    String dispatcherKey = randomAlphabetic(12);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("projectKey is mandatory");

    underTest.findSubscribedEmailRecipients(dispatcherKey, null, ImmutableSet.of(), ALL_MUST_HAVE_ROLE_USER);
  }

  @Test
  public void findSubscribedEmailRecipients_with_logins_fails_with_NPE_if_logins_is_null() {
    String dispatcherKey = randomAlphabetic(12);
    String projectKey = randomAlphabetic(6);

    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("logins can't be null");

    underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey, null, ALL_MUST_HAVE_ROLE_USER);
  }

  @Test
  public void findSubscribedEmailRecipients_with_logins_returns_empty_if_login_set_is_empty() {
    String dispatcherKey = randomAlphabetic(12);
    String projectKey = randomAlphabetic(6);

    Set<EmailRecipient> recipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey, ImmutableSet.of(), ALL_MUST_HAVE_ROLE_USER);

    assertThat(recipients).isEmpty();
  }

  @Test
  public void findSubscribedEmailRecipients_returns_empty_if_no_email_recipients_in_project_for_dispatcher_key() {
    String dispatcherKey = randomAlphabetic(12);
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    when(propertiesDao.findEmailSubscribersForNotification(dbSession, dispatcherKey, "EmailNotificationChannel", projectKey))
      .thenReturn(Collections.emptySet());

    Set<EmailRecipient> emailRecipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(emailRecipients).isEmpty();

    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(any(DbSession.class), anySet(), anyString(), anyString());
  }

  @Test
  public void findSubscribedEmailRecipients_with_logins_returns_empty_if_no_email_recipients_in_project_for_dispatcher_key() {
    String dispatcherKey = randomAlphabetic(12);
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    Set<String> logins = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> "login_" + i)
      .collect(Collectors.toSet());
    when(propertiesDao.findEmailSubscribersForNotification(dbSession, dispatcherKey, "EmailNotificationChannel", projectKey, logins))
      .thenReturn(Collections.emptySet());

    Set<EmailRecipient> emailRecipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey, logins,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(emailRecipients).isEmpty();

    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(any(DbSession.class), anySet(), anyString(), anyString());
  }

  @Test
  public void findSubscribedEmailRecipients_applies_distinct_permission_filtering_global_or_project_subscribers() {
    String dispatcherKey = randomAlphabetic(12);
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    when(propertiesDao.findEmailSubscribersForNotification(dbSession, dispatcherKey, "EmailNotificationChannel", projectKey))
      .thenReturn(
        newHashSet(EmailSubscriberDto.create("user1", false, "user1@foo"), EmailSubscriberDto.create("user3", false, "user3@foo"), EmailSubscriberDto.create("user3", true, "user3@foo")));
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user3", "user4"), projectKey, globalPermission))
      .thenReturn(newHashSet("user3"));
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user1", "user3"), projectKey, projectPermission))
      .thenReturn(newHashSet("user1", "user3"));

    Set<EmailRecipient> emailRecipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(emailRecipients)
      .isEqualTo(ImmutableSet.of(new EmailRecipient("user1", "user1@foo"), new EmailRecipient("user3", "user3@foo")));

    // code is optimized to perform only 2 SQL requests for all channels
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void findSubscribedEmailRecipients_with_logins_applies_distinct_permission_filtering_global_or_project_subscribers() {
    String dispatcherKey = randomAlphabetic(12);
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    Set<String> logins = ImmutableSet.of("user1", "user2", "user3");
    when(propertiesDao.findEmailSubscribersForNotification(dbSession, dispatcherKey, "EmailNotificationChannel", projectKey, logins))
      .thenReturn(
        newHashSet(EmailSubscriberDto.create("user1", false, "user1@foo"), EmailSubscriberDto.create("user3", false, "user3@foo"), EmailSubscriberDto.create("user3", true, "user3@foo")));
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user3", "user4"), projectKey, globalPermission))
      .thenReturn(newHashSet("user3"));
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, newHashSet("user1", "user3"), projectKey, projectPermission))
      .thenReturn(newHashSet("user1", "user3"));

    Set<EmailRecipient> emailRecipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey, logins,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    assertThat(emailRecipients)
      .isEqualTo(ImmutableSet.of(new EmailRecipient("user1", "user1@foo"), new EmailRecipient("user3", "user3@foo")));

    // code is optimized to perform only 2 SQL requests for all channels
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void findSubscribedEmailRecipients_does_not_call_db_for_project_permission_filtering_if_there_is_no_project_subscriber() {
    String dispatcherKey = randomAlphabetic(12);
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    Set<EmailSubscriberDto> subscribers = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> EmailSubscriberDto.create("user" + i, true, "user" + i + "@sonarsource.com"))
      .collect(Collectors.toSet());
    Set<String> logins = subscribers.stream().map(EmailSubscriberDto::getLogin).collect(Collectors.toSet());
    when(propertiesDao.findEmailSubscribersForNotification(dbSession, dispatcherKey, "EmailNotificationChannel", projectKey))
      .thenReturn(subscribers);
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, logins, projectKey, globalPermission))
      .thenReturn(logins);

    Set<EmailRecipient> emailRecipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    Set<EmailRecipient> expected = subscribers.stream().map(i -> new EmailRecipient(i.getLogin(), i.getEmail())).collect(Collectors.toSet());
    assertThat(emailRecipients)
      .isEqualTo(expected);

    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void findSubscribedEmailRecipients_with_logins_does_not_call_db_for_project_permission_filtering_if_there_is_no_project_subscriber() {
    String dispatcherKey = randomAlphabetic(12);
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    Set<EmailSubscriberDto> subscribers = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> EmailSubscriberDto.create("user" + i, true, "user" + i + "@sonarsource.com"))
      .collect(Collectors.toSet());
    Set<String> logins = subscribers.stream().map(EmailSubscriberDto::getLogin).collect(Collectors.toSet());
    when(propertiesDao.findEmailSubscribersForNotification(dbSession, dispatcherKey, "EmailNotificationChannel", projectKey, logins))
      .thenReturn(subscribers);
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, logins, projectKey, globalPermission))
      .thenReturn(logins);

    Set<EmailRecipient> emailRecipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey, logins,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    Set<EmailRecipient> expected = subscribers.stream().map(i -> new EmailRecipient(i.getLogin(), i.getEmail())).collect(Collectors.toSet());
    assertThat(emailRecipients)
      .isEqualTo(expected);

    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void findSubscribedEmailRecipients_does_not_call_DB_for_project_permission_filtering_if_there_is_no_global_subscriber() {
    String dispatcherKey = randomAlphabetic(12);
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    Set<EmailSubscriberDto> subscribers = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> EmailSubscriberDto.create("user" + i, false, "user" + i + "@sonarsource.com"))
      .collect(Collectors.toSet());
    Set<String> logins = subscribers.stream().map(EmailSubscriberDto::getLogin).collect(Collectors.toSet());
    when(propertiesDao.findEmailSubscribersForNotification(dbSession, dispatcherKey, "EmailNotificationChannel", projectKey))
      .thenReturn(subscribers);
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, logins, projectKey, projectPermission))
      .thenReturn(logins);

    Set<EmailRecipient> emailRecipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    Set<EmailRecipient> expected = subscribers.stream().map(i -> new EmailRecipient(i.getLogin(), i.getEmail())).collect(Collectors.toSet());
    assertThat(emailRecipients)
      .isEqualTo(expected);

    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }

  @Test
  public void findSubscribedEmailRecipients_with_logins_does_not_call_DB_for_project_permission_filtering_if_there_is_no_global_subscriber() {
    String dispatcherKey = randomAlphabetic(12);
    String globalPermission = randomAlphanumeric(4);
    String projectPermission = randomAlphanumeric(5);
    String projectKey = randomAlphabetic(6);
    Set<EmailSubscriberDto> subscribers = IntStream.range(0, 1 + new Random().nextInt(10))
      .mapToObj(i -> EmailSubscriberDto.create("user" + i, false, "user" + i + "@sonarsource.com"))
      .collect(Collectors.toSet());
    Set<String> logins = subscribers.stream().map(EmailSubscriberDto::getLogin).collect(Collectors.toSet());
    when(propertiesDao.findEmailSubscribersForNotification(dbSession, dispatcherKey, "EmailNotificationChannel", projectKey, logins))
      .thenReturn(subscribers);
    when(authorizationDao.keepAuthorizedLoginsOnProject(dbSession, logins, projectKey, projectPermission))
      .thenReturn(logins);

    Set<EmailRecipient> emailRecipients = underTest.findSubscribedEmailRecipients(dispatcherKey, projectKey, logins,
      new SubscriberPermissionsOnProject(globalPermission, projectPermission));
    Set<EmailRecipient> expected = subscribers.stream().map(i -> new EmailRecipient(i.getLogin(), i.getEmail())).collect(Collectors.toSet());
    assertThat(emailRecipients)
      .isEqualTo(expected);

    verify(authorizationDao, times(0)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(globalPermission));
    verify(authorizationDao, times(1)).keepAuthorizedLoginsOnProject(eq(dbSession), anySet(), anyString(), eq(projectPermission));
  }
}
