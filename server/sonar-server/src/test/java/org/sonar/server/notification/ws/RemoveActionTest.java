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
package org.sonar.server.notification.ws;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import org.sonar.server.notification.NotificationCenter;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.client.notification.RemoveRequest;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonarqube.ws.client.notification.NotificationsWsParameters.PARAM_CHANNEL;
import static org.sonarqube.ws.client.notification.NotificationsWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.notification.NotificationsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.notification.NotificationsWsParameters.PARAM_TYPE;

public class RemoveActionTest {
  private static final String NOTIF_MY_NEW_ISSUES = "Dispatcher1";
  private static final String NOTIF_NEW_ISSUES = "Dispatcher2";
  private static final String NOTIF_NEW_QUALITY_GATE_STATUS = "Dispatcher3";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession;
  @Rule
  public DbTester db = DbTester.create();
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private NotificationChannel emailChannel = new FakeNotificationChannel("EmailChannel");
  private NotificationChannel twitterChannel = new FakeNotificationChannel("TwitterChannel");
  // default channel, based on class simple name
  private NotificationChannel defaultChannel = new FakeNotificationChannel("EmailNotificationChannel");

  private NotificationCenter notificationCenter;
  private NotificationUpdater notificationUpdater;
  private RemoveAction underTest;

  private WsActionTester ws;
  private RemoveRequest.Builder request = RemoveRequest.builder().setType(NOTIF_MY_NEW_ISSUES);

  private UserDto user;

  @Before
  public void setUp() {
    user = db.users().insertUser();
    userSession = UserSessionRule.standalone().logIn(user);

    NotificationDispatcherMetadata metadata1 = NotificationDispatcherMetadata.create(NOTIF_MY_NEW_ISSUES)
      .setProperty(GLOBAL_NOTIFICATION, "true")
      .setProperty(PER_PROJECT_NOTIFICATION, "true");
    NotificationDispatcherMetadata metadata2 = NotificationDispatcherMetadata.create(NOTIF_NEW_ISSUES)
      .setProperty(GLOBAL_NOTIFICATION, "true");
    NotificationDispatcherMetadata metadata3 = NotificationDispatcherMetadata.create(NOTIF_NEW_QUALITY_GATE_STATUS)
      .setProperty(GLOBAL_NOTIFICATION, "true")
      .setProperty(PER_PROJECT_NOTIFICATION, "true");

    notificationCenter = new NotificationCenter(
      new NotificationDispatcherMetadata[] {metadata1, metadata2, metadata3},
      new NotificationChannel[] {emailChannel, twitterChannel, defaultChannel});
    notificationUpdater = new NotificationUpdater(dbClient);
    underTest = new RemoveAction(notificationCenter, notificationUpdater, dbClient, TestComponentFinder.from(db), userSession);
    ws = new WsActionTester(underTest);
  }

  @Test
  public void remove_to_email_channel_by_default() {
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    call(request);

    db.notifications().assertDoesNotExist(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getId(), null);
  }

  @Test
  public void remove_from_a_specific_channel() {
    notificationUpdater.add(dbSession, twitterChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, user, null);
    dbSession.commit();

    call(request.setType(NOTIF_NEW_QUALITY_GATE_STATUS).setChannel(twitterChannel.getKey()));

    db.notifications().assertDoesNotExist(twitterChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, user.getId(), null);
  }

  @Test
  public void remove_a_project_notification() {
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    dbSession.commit();

    call(request.setProject(project.getKey()));

    db.notifications().assertDoesNotExist(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getId(), project);
  }

  @Test
  public void fail_when_remove_a_global_notification_when_a_project_one_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Notification doesn't exist");

    call(request);
  }

  @Test
  public void fail_when_remove_a_project_notification_when_a_global_one_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Notification doesn't exist");

    call(request.setProject(project.getKey()));
  }

  @Test
  public void http_no_content() {
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    TestResponse result = call(request);

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void remove_a_notification_from_a_user_as_system_administrator() {
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getId(), null);
    userSession.logIn().setSystemAdministrator();
    dbSession.commit();

    call(request.setLogin(user.getLogin()));

    db.notifications().assertDoesNotExist(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getId(), null);
  }

  @Test
  public void fail_if_login_is_provided_and_unknown() {
    userSession.logIn().setSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'LOGIN 404' not found");

    call(request.setLogin("LOGIN 404"));
  }

  @Test
  public void fail_if_login_provided_and_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    expectedException.expect(ForbiddenException.class);

    call(request.setLogin(user.getLogin()));
  }

  @Test
  public void fail_when_notification_does_not_exist() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Notification doesn't exist");

    call(request);
  }

  @Test
  public void fail_when_unknown_channel() {
    expectedException.expect(IllegalArgumentException.class);

    call(request.setChannel("Channel42"));
  }

  @Test
  public void fail_when_unknown_global_dispatcher() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value of parameter 'type' (Dispatcher42) must be one of: [Dispatcher1, Dispatcher2, Dispatcher3]");

    call(request.setType("Dispatcher42"));
  }

  @Test
  public void fail_when_unknown_project_dispatcher() {
    ComponentDto project = db.components().insertPrivateProject();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value of parameter 'type' (Dispatcher42) must be one of: [Dispatcher1, Dispatcher3]");

    call(request.setType("Dispatcher42").setProject(project.key()));
  }

  @Test
  public void fail_when_no_dispatcher() {
    expectedException.expect(IllegalArgumentException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_when_project_is_unknown() {
    expectedException.expect(NotFoundException.class);

    call(request.setProject("Project-42"));
  }

  @Test
  public void fail_when_component_is_not_a_project() {
    db.components().insertViewAndSnapshot(newView(db.organizations().insert()).setKey("VIEW_1"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component 'VIEW_1' must be a project");

    call(request.setProject("VIEW_1"));
  }

  @Test
  public void fail_when_not_authenticated() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    call(request);
  }

  private TestResponse call(RemoveRequest.Builder wsRequestBuilder) {
    RemoveRequest wsRequest = wsRequestBuilder.build();

    TestRequest request = ws.newRequest();
    request.setParam(PARAM_TYPE, wsRequest.getType());
    setNullable(wsRequest.getChannel(), channel -> request.setParam(PARAM_CHANNEL, channel));
    setNullable(wsRequest.getProject(), project -> request.setParam(PARAM_PROJECT, project));
    setNullable(wsRequest.getLogin(), login -> request.setParam(PARAM_LOGIN, login));
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
    public void deliver(Notification notification, String username) {
      // do nothing
    }
  }
}
