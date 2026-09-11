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
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.notification.ws.AbstractGroupNotificationAction.CHANNEL_KEY;
import static org.sonar.server.notification.ws.AbstractGroupNotificationAction.PARAM_GROUP_UUID;
import static org.sonar.server.notification.ws.AbstractGroupNotificationAction.PARAM_TYPE;

class RemoveGroupActionIT {

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
    return new WsActionTester(new RemoveGroupAction(dbClient, userSession, dispatchers));
  }

  @Test
  void definition() {
    WebService.Action definition = ws.getDef();

    assertThat(definition.key()).isEqualTo("remove_group");
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
  void removes_subscription_when_subscribed() {
    userSession.logIn().setSystemAdministrator();
    GroupDto group = db.users().insertGroup("my-group");
    dbClient.notificationGroupSubscriptionsDao().insert(dbSession, group.getUuid(), TYPE, CHANNEL_KEY);
    dbSession.commit();

    ws.newRequest()
      .setParam(PARAM_GROUP_UUID, group.getUuid())
      .setParam(PARAM_TYPE, TYPE)
      .execute();

    assertThat(dbClient.notificationGroupSubscriptionsDao().selectAll(dbSession)).isEmpty();
  }

  @Test
  void is_noop_when_not_subscribed() {
    userSession.logIn().setSystemAdministrator();

    ws.newRequest()
      .setParam(PARAM_GROUP_UUID, "non-existent-uuid")
      .setParam(PARAM_TYPE, TYPE)
      .execute();

    assertThat(dbClient.notificationGroupSubscriptionsDao().selectAll(dbSession)).isEmpty();
  }

  /**
   * See {@code AddGroupActionIT#fails_when_no_dispatcher_supports_group_subscription}: the accepted
   * types must match the types advertised by api/notifications/list on this edition.
   */
  @Test
  void fails_when_no_dispatcher_supports_group_subscription() {
    userSession.logIn().setSystemAdministrator();
    WsActionTester wsWithoutDispatchers = newWs(List.of());

    var request = wsWithoutDispatchers.newRequest()
      .setParam(PARAM_GROUP_UUID, "some-uuid")
      .setParam(PARAM_TYPE, TYPE);

    assertThatThrownBy(request::execute)
      .isInstanceOf(BadRequestException.class)
      .hasMessage("Value of parameter 'type' (TestType) must be one of: []");
  }
}
