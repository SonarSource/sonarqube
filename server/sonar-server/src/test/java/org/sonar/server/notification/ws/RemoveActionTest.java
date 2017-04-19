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
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.notification.NotificationCenter;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.client.notification.AddRequest;

import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.ComponentTesting.newView;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonarqube.ws.client.notification.NotificationsWsParameters.PARAM_CHANNEL;
import static org.sonarqube.ws.client.notification.NotificationsWsParameters.PARAM_PROJECT;
import static org.sonarqube.ws.client.notification.NotificationsWsParameters.PARAM_TYPE;

public class RemoveActionTest {
  private static final String NOTIF_MY_NEW_ISSUES = "Dispatcher1";
  private static final String NOTIF_NEW_ISSUES = "Dispatcher2";
  private static final String NOTIF_NEW_QUALITY_GATE_STATUS = "Dispatcher3";
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone().logIn().setUserId(123);
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
  private AddRequest.Builder request = AddRequest.builder()
    .setType(NOTIF_MY_NEW_ISSUES);

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

    notificationCenter = new NotificationCenter(
      new NotificationDispatcherMetadata[] {metadata1, metadata2, metadata3},
      new NotificationChannel[] {emailChannel, twitterChannel, defaultChannel});
    notificationUpdater = new NotificationUpdater(userSession, dbClient);
    underTest = new RemoveAction(notificationCenter, notificationUpdater, dbClient, new ComponentFinder(dbClient), userSession);
    ws = new WsActionTester(underTest);
  }

  @Test
  public void remove_to_email_channel_by_default() {
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, null);
    dbSession.commit();

    call(request);

    db.notifications().assertDoesNotExist(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), null);
  }

  @Test
  public void remove_from_a_specific_channel() {
    notificationUpdater.add(dbSession, twitterChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, null);
    dbSession.commit();

    call(request.setType(NOTIF_NEW_QUALITY_GATE_STATUS).setChannel(twitterChannel.getKey()));

    db.notifications().assertDoesNotExist(twitterChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, userSession.getUserId(), null);
  }

  @Test
  public void remove_a_project_notification() {
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, project);
    dbSession.commit();

    call(request.setProject(project.getKey()));

    db.notifications().assertDoesNotExist(defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, userSession.getUserId(), project);
  }

  @Test
  public void fail_when_remove_a_global_notification_when_a_project_one_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, project);
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Notification doesn't exist");

    call(request);
  }

  @Test
  public void fail_when_remove_a_project_notification_when_a_global_one_exists() {
    ComponentDto project = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, null);
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Notification doesn't exist");

    call(request.setProject(project.getKey()));
  }

  @Test
  public void http_no_content() {
    notificationUpdater.add(dbSession, defaultChannel.getKey(), NOTIF_MY_NEW_ISSUES, null);
    dbSession.commit();

    TestResponse result = call(request);

    assertThat(result.getStatus()).isEqualTo(HTTP_NO_CONTENT);
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

  private TestResponse call(AddRequest.Builder wsRequestBuilder) {
    AddRequest wsRequest = wsRequestBuilder.build();
    TestRequest request = ws.newRequest();
    request.setParam(PARAM_TYPE, wsRequest.getType());
    setNullable(wsRequest.getChannel(), channel -> request.setParam(PARAM_CHANNEL, channel));
    setNullable(wsRequest.getProject(), project -> request.setParam(PARAM_PROJECT, project));
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
