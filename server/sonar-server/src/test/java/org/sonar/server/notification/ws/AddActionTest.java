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
package org.sonar.server.notification.ws;

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
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

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_CHANNEL;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_LOGIN;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_PROJECT;
import static org.sonar.server.notification.ws.NotificationsWsParameters.PARAM_TYPE;

public class AddActionTest {
  private static final String NOTIF_MY_NEW_ISSUES = "Dispatcher1";
  private static final String NOTIF_NEW_ISSUES = "Dispatcher2";
  private static final String NOTIF_NEW_QUALITY_GATE_STATUS = "Dispatcher3";
  private static final String USER_LOGIN = "george.orwell";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession;
  @Rule
  public DbTester db = DbTester.create();

  private DbClient dbClient = db.getDbClient();

  private NotificationChannel emailChannel = new FakeNotificationChannel("EmailChannel");
  private NotificationChannel twitterChannel = new FakeNotificationChannel("TwitterChannel");
  // default channel, based on class simple name
  private NotificationChannel defaultChannel = new FakeNotificationChannel("EmailNotificationChannel");

  private UserDto user;

  private NotificationCenter notificationCenter;
  private AddAction underTest;
  private WsActionTester ws;

  @Before
  public void setUp() {
    NotificationDispatcherMetadata metadata1 = NotificationDispatcherMetadata.create(NOTIF_MY_NEW_ISSUES)
      .setProperty(GLOBAL_NOTIFICATION, "true")
      .setProperty(PER_PROJECT_NOTIFICATION, "true");
    NotificationDispatcherMetadata metadata2 = NotificationDispatcherMetadata.create(NOTIF_NEW_ISSUES)
      .setProperty(GLOBAL_NOTIFICATION, "true");
    NotificationDispatcherMetadata metadata3 = NotificationDispatcherMetadata.create(NOTIF_NEW_QUALITY_GATE_STATUS)
      .setProperty(GLOBAL_NOTIFICATION, "true")
      .setProperty(PER_PROJECT_NOTIFICATION, "true");

    user = db.users().insertUser(USER_LOGIN);
    userSession = UserSessionRule.standalone().logIn(user);

    notificationCenter = new NotificationCenter(
      new NotificationDispatcherMetadata[] {metadata1, metadata2, metadata3},
      new NotificationChannel[] {emailChannel, twitterChannel, defaultChannel});
    underTest = new AddAction(notificationCenter, new NotificationUpdater(dbClient), dbClient, TestComponentFinder.from(db), userSession);
    ws = new WsActionTester(underTest);
  }

  @Test
  public void add_to_email_channel_by_default() {
    call(NOTIF_MY_NEW_ISSUES, null, null, null);

    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), null);
  }

  @Test
  public void add_to_a_specific_channel() {
    call(NOTIF_NEW_QUALITY_GATE_STATUS, twitterChannel.getKey(), null, null);

    db.notifications().assertExists(twitterChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, userSession.getUserId(), null);
  }

  @Test
  public void add_notification_on_private_with_USER_permission() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    call(NOTIF_MY_NEW_ISSUES, null, project.getDbKey(), null);

    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), project);
  }

  @Test
  public void add_notification_on_public_project() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    call(NOTIF_MY_NEW_ISSUES, null, project.getDbKey(), null);

    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), project);
  }

  @Test
  public void add_a_global_notification_when_a_project_one_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);
    call(NOTIF_MY_NEW_ISSUES, null, project.getDbKey(), null);

    call(NOTIF_MY_NEW_ISSUES, null, null, null);

    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), project);
    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), null);
  }

  @Test
  public void add_a_notification_on_private_project_when_a_global_one_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    call(NOTIF_MY_NEW_ISSUES, null, null, null);

    userSession.addProjectPermission(UserRole.USER, project);
    call(NOTIF_MY_NEW_ISSUES, null, project.getDbKey(), null);

    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), project);
    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), null);
  }


  @Test
  public void add_a_notification_on_public_project_when_a_global_one_exists() {
    ComponentDto project = db.components().insertPublicProject();
    userSession.registerComponents(project);
    call(NOTIF_MY_NEW_ISSUES, null, null, null);

    call(NOTIF_MY_NEW_ISSUES, null, project.getDbKey(), null);

    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), project);
    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), null);
  }

  @Test
  public void http_no_content() {
    TestResponse result = call(NOTIF_MY_NEW_ISSUES, null, null, null);

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
  }

  @Test
  public void add_a_notification_to_a_user_as_system_administrator() {
    userSession.logIn().setSystemAdministrator();

    call(NOTIF_MY_NEW_ISSUES, null, null, user.getLogin());

    db.notifications().assertExists(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, user.getId(), null);
  }

  @Test
  public void fail_if_login_is_provided_and_unknown() {
    userSession.logIn().setSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'LOGIN 404' not found");

    call(NOTIF_MY_NEW_ISSUES, null, null, "LOGIN 404");
  }

  @Test
  public void fail_if_login_provided_and_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    call(NOTIF_MY_NEW_ISSUES, null, null, user.getLogin());
  }

  @Test
  public void fail_when_notification_already_exists() {
    call(NOTIF_MY_NEW_ISSUES, null, null, null);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Notification already added");

    call(NOTIF_MY_NEW_ISSUES, null, null, null);
  }

  @Test
  public void fail_when_unknown_channel() {
    expectedException.expect(IllegalArgumentException.class);

    call(NOTIF_MY_NEW_ISSUES, "Channel42", null, null);
  }

  @Test
  public void fail_when_unknown_global_dispatcher() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value of parameter 'type' (Dispatcher42) must be one of: [Dispatcher1, Dispatcher2, Dispatcher3]");

    call("Dispatcher42", null, null, null);
  }

  @Test
  public void fail_when_unknown_project_dispatcher_on_private_project() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.addProjectPermission(UserRole.USER, project);

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value of parameter 'type' (Dispatcher42) must be one of: [Dispatcher1, Dispatcher3]");

    call("Dispatcher42", null, project.getDbKey(), null);
  }

  @Test
  public void fail_when_unknown_project_dispatcher_on_public_project() {
    ComponentDto project = db.components().insertPublicProject();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Value of parameter 'type' (Dispatcher42) must be one of: [Dispatcher1, Dispatcher3]");

    call("Dispatcher42", null, project.getDbKey(), null);
  }

  @Test
  public void fail_when_no_dispatcher() {
    expectedException.expect(IllegalArgumentException.class);

    ws.newRequest().execute();
  }

  @Test
  public void fail_when_project_is_unknown() {
    expectedException.expect(NotFoundException.class);

    call(NOTIF_MY_NEW_ISSUES, null, "Project-42", null);
  }

  @Test
  public void fail_when_component_is_not_a_project() {
    db.components().insertViewAndSnapshot(newView(db.organizations().insert()).setDbKey("VIEW_1"));

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("Component 'VIEW_1' must be a project");

    call(NOTIF_MY_NEW_ISSUES, null, "VIEW_1", null);
  }

  @Test
  public void fail_when_not_authenticated() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    call(NOTIF_MY_NEW_ISSUES, null, null, null);
  }

  @Test
  public void fail_when_using_branch_db_key() throws Exception {
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage(format("Component key '%s' not found", branch.getDbKey()));

    call(NOTIF_MY_NEW_ISSUES, null, branch.getDbKey(), null);
  }

  @Test
  public void fail_when_user_does_not_have_USER_permission_on_private_project() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().setNonRoot().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    call(NOTIF_MY_NEW_ISSUES, null, project.getDbKey(), userSession.getLogin());
  }

  private TestResponse call(String type, @Nullable String channel, @Nullable String project, @Nullable String login) {
    TestRequest request = ws.newRequest();
    request.setParam(PARAM_TYPE, type);
    setNullable(channel, channel1 -> request.setParam(PARAM_CHANNEL, channel1));
    setNullable(project, project1 -> request.setParam(PARAM_PROJECT, project1));
    setNullable(login, login1 -> request.setParam(PARAM_LOGIN, login1));
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
