/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.notification.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.component.TestComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.ws.RemoveAction.RemoveRequest;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newPortfolio;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_CHANNEL;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_LOGIN;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_PROJECT;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_TYPE;

public class RemoveActionTest {
  private static final String NOTIF_MY_NEW_ISSUES = "Dispatcher1";
  private static final String NOTIF_NEW_ISSUES = "Dispatcher2";
  private static final String NOTIF_NEW_QUALITY_GATE_STATUS = "Dispatcher3";
  @Rule
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public final DbTester db = DbTester.create();
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final NotificationChannel emailChannel = new FakeNotificationChannel("EmailChannel");
  private final NotificationChannel twitterChannel = new FakeNotificationChannel("TwitterChannel");
  // default channel, based on class simple name
  private final NotificationChannel defaultChannel = new FakeNotificationChannel("EmailNotificationChannel");

  private final NotificationUpdater notificationUpdater = new NotificationUpdater(dbClient);
  private final Dispatchers dispatchers = mock(Dispatchers.class);

  private final RemoveRequest request = new RemoveRequest().setType(NOTIF_MY_NEW_ISSUES);

  private final WsActionTester ws = new WsActionTester(new RemoveAction(new NotificationCenter(
    new NotificationDispatcherMetadata[] {},
    new NotificationChannel[] {emailChannel, twitterChannel, defaultChannel}), notificationUpdater, dispatchers, dbClient, TestComponentFinder.from(db), userSession));

  @Test
  public void remove_to_email_channel_by_default() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    call(request);

    db.notifications().assertDoesNotExist(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getUuid(), null);
  }

  @Test
  public void remove_from_a_specific_channel() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_NEW_QUALITY_GATE_STATUS));
    notificationUpdater.add(dbSession, twitterChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, user, null);
    dbSession.commit();

    call(request.setType(NOTIF_NEW_QUALITY_GATE_STATUS).setChannel(twitterChannel.getKey()));

    db.notifications().assertDoesNotExist(twitterChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, user.getUuid(), null);
  }

  @Test
  public void remove_a_project_notification() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    when(dispatchers.getProjectDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    dbSession.commit();

    call(request.setProject(project.getKey()));

    db.notifications().assertDoesNotExist(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getUuid(), project);
  }

  @Test
  public void fail_when_remove_a_global_notification_when_a_project_one_exists() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    when(dispatchers.getProjectDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    dbSession.commit();

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Notification doesn't exist");
  }

  @Test
  public void fail_when_remove_a_project_notification_when_a_global_one_exists() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    when(dispatchers.getProjectDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    RemoveRequest request = this.request.setProject(project.getKey());
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Notification doesn't exist");
  }

  @Test
  public void http_no_content() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    when(dispatchers.getProjectDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    TestResponse result = call(request);

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void remove_a_notification_from_a_user_as_system_administrator() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getUuid(), null);
    userSession.logIn().setSystemAdministrator();
    dbSession.commit();

    call(request.setLogin(user.getLogin()));

    db.notifications().assertDoesNotExist(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getUuid(), null);
  }

  @Test
  public void fail_if_login_is_provided_and_unknown() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setSystemAdministrator();
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));

    RemoveRequest request = this.request.setLogin("LOGIN 404");
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("User 'LOGIN 404' not found");
  }

  @Test
  public void fail_if_login_provided_and_not_system_administrator() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user).setNonSystemAdministrator();
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    RemoveRequest request = this.request.setLogin(user.getLogin());
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void fail_when_notification_does_not_exist() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Notification doesn't exist");
  }

  @Test
  public void fail_when_unknown_channel() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));

    RemoveRequest request = this.request.setChannel("Channel42");
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void fail_when_unknown_global_dispatcher() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(asList(NOTIF_MY_NEW_ISSUES, NOTIF_NEW_ISSUES, NOTIF_NEW_QUALITY_GATE_STATUS));

    RemoveRequest request = this.request.setType("Dispatcher42");
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Value of parameter 'type' (Dispatcher42) must be one of: [Dispatcher1, Dispatcher2, Dispatcher3]");
  }

  @Test
  public void fail_when_unknown_project_dispatcher() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    when(dispatchers.getProjectDispatchers()).thenReturn(asList(NOTIF_MY_NEW_ISSUES, NOTIF_NEW_QUALITY_GATE_STATUS));
    ComponentDto project = db.components().insertPrivateProject();

    RemoveRequest request = this.request.setType("Dispatcher42").setProject(project.getKey());
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Value of parameter 'type' (Dispatcher42) must be one of: [Dispatcher1, Dispatcher3]");
  }

  @Test
  public void fail_when_no_type_parameter() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));

    TestRequest request = ws.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The 'type' parameter is missing");
  }

  @Test
  public void fail_when_project_is_unknown() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    when(dispatchers.getProjectDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));

    RemoveRequest request = this.request.setProject("Project-42");
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(NotFoundException.class);
  }

  @Test
  public void fail_when_component_is_not_a_project() {
    UserDto user = db.users().insertUser();
    userSession.logIn(user);
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    when(dispatchers.getProjectDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));
    db.components().insertPortfolioAndSnapshot(newPortfolio().setKey("VIEW_1"));

    RemoveRequest request = this.request.setProject("VIEW_1");
    assertThatThrownBy(() -> call(request))
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Component 'VIEW_1' must be a project");
  }

  @Test
  public void fail_when_not_authenticated() {
    userSession.anonymous();
    when(dispatchers.getGlobalDispatchers()).thenReturn(singletonList(NOTIF_MY_NEW_ISSUES));

    assertThatThrownBy(() -> call(request))
      .isInstanceOf(UnauthorizedException.class);
  }

  private TestResponse call(RemoveRequest remove) {
    TestRequest request = ws.newRequest();
    request.setParam(PARAM_TYPE, remove.getType());
    ofNullable(remove.getChannel()).ifPresent(channel -> request.setParam(PARAM_CHANNEL, channel));
    ofNullable(remove.getProject()).ifPresent(project -> request.setParam(PARAM_PROJECT, project));
    ofNullable(remove.getLogin()).ifPresent(login -> request.setParam(PARAM_LOGIN, login));
    return request.execute();
  }

  private static class FakeNotificationChannel extends NotificationChannel {
    private final String key;

    private FakeNotificationChannel(String key) {
      this.key = key;
    }

    @Override
    public String getKey() {
      return this.key;
    }

    @Override
    public boolean deliver(Notification notification, String username) {
      // do nothing
      return true;
    }
  }
}
