/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.notification.NotificationGroupSubscriptionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.notification.ws.AbstractGroupNotificationAction.CHANNEL_KEY;
import static org.sonar.server.notification.ws.AbstractGroupNotificationAction.PARAM_GROUP_UUID;
import static org.sonar.server.notification.ws.AbstractGroupNotificationAction.PARAM_TYPE;

class AddGroupActionIT {

  private static final String TYPE = "TestType";

  @RegisterExtension
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final WsActionTester ws = newWs(List.of(TYPE));

  private WsActionTester newWs(List<String> groupSubscriptionDispatchers) {
    Dispatchers dispatchers = mock(Dispatchers.class);
    when(dispatchers.getGroupSubscriptionDispatchers()).thenReturn(groupSubscriptionDispatchers);
    return new WsActionTester(new AddGroupAction(dbClient, userSession, dispatchers));
  }

  @Test
  void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("add_group");
    assertThat(definition.isPost()).isTrue();
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.params()).extracting(WebService.Param::key)
      .containsExactlyInAnyOrder(PARAM_GROUP_UUID, PARAM_TYPE);
  }

  @Test
  void fails_when_not_system_admin() {
    userSession.logIn().setNonSystemAdministrator();

    var request = ws.newRequest().setParam(PARAM_GROUP_UUID, "some-uuid");
    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void fails_when_anonymous() {
    userSession.anonymous();

    var request = ws.newRequest().setParam(PARAM_GROUP_UUID, "some-uuid");
    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void subscribes_group_successfully() {
    userSession.logIn().setSystemAdministrator();
    GroupDto group = db.users().insertGroup("my-group");

    ws.newRequest()
      .setParam(PARAM_GROUP_UUID, group.getUuid())
      .setParam(PARAM_TYPE, TYPE)
      .execute();

    assertThat(dbClient.notificationGroupSubscriptionsDao().selectAll(dbSession))
      .extracting(NotificationGroupSubscriptionDto::getNotificationType, NotificationGroupSubscriptionDto::getChannelKey)
      .containsExactly(tuple(TYPE, CHANNEL_KEY));
  }

  /**
   * On an edition where no dispatcher declares itself group-subscribable (e.g. Community Build,
   * which does not ship core-extension-security-alerts), no type may be subscribed: a subscription
   * to an unregistered dispatcher could never be delivered.
   */
  @Test
  void fails_when_no_dispatcher_supports_group_subscription() {
    userSession.logIn().setSystemAdministrator();
    GroupDto group = db.users().insertGroup("my-group");
    WsActionTester wsWithoutDispatchers = newWs(List.of());

    var request = wsWithoutDispatchers.newRequest()
      .setParam(PARAM_GROUP_UUID, group.getUuid())
      .setParam(PARAM_TYPE, TYPE);

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Value of parameter 'type' (TestType) must be one of: []");
    assertThat(dbClient.notificationGroupSubscriptionsDao().selectAll(dbSession)).isEmpty();
  }

  @Test
  void is_idempotent_when_called_twice() {
    userSession.logIn().setSystemAdministrator();
    GroupDto group = db.users().insertGroup("my-group");

    ws.newRequest()
      .setParam(PARAM_GROUP_UUID, group.getUuid())
      .setParam(PARAM_TYPE, TYPE)
      .execute();
    ws.newRequest()
      .setParam(PARAM_GROUP_UUID, group.getUuid())
      .setParam(PARAM_TYPE, TYPE)
      .execute();

    assertThat(dbClient.notificationGroupSubscriptionsDao().selectAll(dbSession)).hasSize(1);
  }
}
