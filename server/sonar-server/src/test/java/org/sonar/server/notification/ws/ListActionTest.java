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
import org.sonar.api.notifications.NotificationChannel;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.UserPermissionDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.notification.NotificationCenter;
import org.sonar.server.notification.NotificationDispatcherMetadata;
import org.sonar.server.notification.NotificationUpdater;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Notifications.ListResponse;
import org.sonarqube.ws.Notifications.Notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.server.notification.NotificationDispatcherMetadata.GLOBAL_NOTIFICATION;
import static org.sonar.server.notification.NotificationDispatcherMetadata.PER_PROJECT_NOTIFICATION;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.test.JsonAssert.assertJson;

public class ListActionTest {
  private static final String NOTIF_MY_NEW_ISSUES = "MyNewIssues";
  private static final String NOTIF_NEW_ISSUES = "NewIssues";
  private static final String NOTIF_NEW_QUALITY_GATE_STATUS = "NewQualityGateStatus";

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
  private UserDto user;

  private NotificationUpdater notificationUpdater;

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

    user = db.users().insertUser();
    userSession = UserSessionRule.standalone().logIn(user);

    NotificationCenter notificationCenter = new NotificationCenter(
      new NotificationDispatcherMetadata[] {metadata1, metadata2, metadata3},
      new NotificationChannel[] {emailChannel, twitterChannel});
    notificationUpdater = new NotificationUpdater(dbClient);
    ListAction underTest = new ListAction(notificationCenter, dbClient, userSession);
    ws = new WsActionTester(underTest);
  }

  @Test
  public void channels() {
    ListResponse result = call();

    assertThat(result.getChannelsList()).containsExactly(emailChannel.getKey(), twitterChannel.getKey());
  }

  @Test
  public void overall_dispatchers() {
    ListResponse result = call();

    assertThat(result.getGlobalTypesList()).containsExactly(NOTIF_MY_NEW_ISSUES, NOTIF_NEW_ISSUES, NOTIF_NEW_QUALITY_GATE_STATUS);
  }

  @Test
  public void per_project_dispatchers() {
    ListResponse result = call();

    assertThat(result.getPerProjectTypesList()).containsExactly(NOTIF_MY_NEW_ISSUES, NOTIF_NEW_QUALITY_GATE_STATUS);
  }

  @Test
  public void filter_unauthorized_projects() {
    ComponentDto project = addComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey("K1"));
    ComponentDto anotherProject = db.components().insertPrivateProject();
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, anotherProject);
    dbSession.commit();

    ListResponse result = call();

    assertThat(result.getNotificationsList()).extracting(Notification::getProject).containsOnly("K1");
  }

  @Test
  public void filter_channels() {
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, "Unknown Channel", NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    ListResponse result = call();

    assertThat(result.getNotificationsList()).extracting(Notification::getChannel).containsOnly(emailChannel.getKey());
  }

  @Test
  public void filter_overall_dispatchers() {
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, emailChannel.getKey(), "Unknown Notification", user, null);
    dbSession.commit();

    ListResponse result = call();

    assertThat(result.getNotificationsList()).extracting(Notification::getType).containsOnly(NOTIF_MY_NEW_ISSUES);
  }

  @Test
  public void filter_per_project_dispatchers() {
    ComponentDto project = addComponent(ComponentTesting.newPrivateProjectDto(db.organizations().insert()).setKey("K1"));
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    notificationUpdater.add(dbSession, emailChannel.getKey(), "Unknown Notification", user, project);
    dbSession.commit();

    ListResponse result = call();

    assertThat(result.getNotificationsList())
      .extracting(Notification::getType)
      .containsOnly(NOTIF_MY_NEW_ISSUES);
  }

  @Test
  public void order_with_global_then_by_channel_and_dispatcher() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project = addComponent(ComponentTesting.newPrivateProjectDto(organization).setKey("K1"));
    notificationUpdater.add(dbSession, twitterChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, twitterChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, user, project);
    dbSession.commit();

    ListResponse result = call();

    assertThat(result.getNotificationsList())
      .extracting(Notification::getChannel, Notification::getOrganization, Notification::getType, Notification::getProject)
      .containsExactly(
        tuple(emailChannel.getKey(), "", NOTIF_MY_NEW_ISSUES, ""),
        tuple(emailChannel.getKey(), "", NOTIF_NEW_ISSUES, ""),
        tuple(twitterChannel.getKey(), "", NOTIF_MY_NEW_ISSUES, ""),
        tuple(emailChannel.getKey(), organization.getKey(), NOTIF_MY_NEW_ISSUES, "K1"),
        tuple(emailChannel.getKey(), organization.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, "K1"),
        tuple(twitterChannel.getKey(), organization.getKey(), NOTIF_MY_NEW_ISSUES, "K1"));
  }

  @Test
  public void list_user_notifications_as_system_admin() {
    userSession.logIn().setSystemAdministrator();

    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_NEW_ISSUES, user, null);
    dbSession.commit();

    ListResponse result = call(user.getLogin());

    assertThat(result.getNotificationsList())
      .extracting(Notification::getType)
      .containsOnly(NOTIF_MY_NEW_ISSUES, NOTIF_NEW_ISSUES);
  }

  @Test
  public void fail_if_login_and_not_system_admin() {
    userSession.logIn().setNonSystemAdministrator();
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    dbSession.commit();

    expectedException.expect(ForbiddenException.class);

    call(user.getLogin());
  }

  @Test
  public void fail_if_login_is_provided_and_unknown() {
    userSession.logIn().setSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User 'LOGIN 404' not found");

    call("LOGIN 404");
  }

  @Test
  public void json_example() {
    OrganizationDto organization = db.organizations().insertForKey("my-org-1");
    ComponentDto project = addComponent(ComponentTesting.newPrivateProjectDto(organization).setKey(KEY_PROJECT_EXAMPLE_001).setName("My Project"));
    notificationUpdater.add(dbSession, twitterChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_NEW_ISSUES, user, null);
    notificationUpdater.add(dbSession, twitterChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_MY_NEW_ISSUES, user, project);
    notificationUpdater.add(dbSession, emailChannel.getKey(), NOTIF_NEW_QUALITY_GATE_STATUS, user, project);
    dbSession.commit();

    String result = ws.newRequest().execute().getInput();

    assertJson(ws.getDef().responseExampleAsString())
      .withStrictArrayOrder()
      .isSimilarTo(result);
  }

  @Test
  public void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("list");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.since()).isEqualTo("6.3");
    assertThat(definition.responseExampleAsString()).isNotEmpty();
    assertThat(definition.params()).hasSize(1);

    WebService.Param loginParam = definition.param("login");
    assertThat(loginParam.since()).isEqualTo("6.4");
    assertThat(loginParam.isRequired()).isFalse();
  }

  @Test
  public void fail_when_not_authenticated() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    call();
  }

  private ListResponse call() {
    return ws.newRequest().executeProtobuf(ListResponse.class);
  }

  private ListResponse call(String login) {
    return ws.newRequest().setParam("login", login).executeProtobuf(ListResponse.class);
  }

  private ComponentDto addComponent(ComponentDto component) {
    db.components().insertComponent(component);
    dbClient.userPermissionDao().insert(dbSession, new UserPermissionDto(component.getOrganizationUuid(), UserRole.USER, user.getId(), component.getId()));
    db.commit();

    return component;
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
    public void deliver(org.sonar.api.notifications.Notification notification, String username) {
      // do nothing
    }
  }
}
