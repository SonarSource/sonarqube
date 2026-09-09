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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ListGroupsActionIT {

  @RegisterExtension
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final WsActionTester ws = new WsActionTester(new ListGroupsAction(dbClient, userSession));

  @Test
  void fails_when_not_system_admin() {
    userSession.logIn().setNonSystemAdministrator();

    var request = ws.newRequest();
    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void fails_when_anonymous() {
    userSession.anonymous();

    var request = ws.newRequest();
    assertThatThrownBy(request::execute).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void returns_subscriptions_with_group_names() {
    userSession.logIn().setSystemAdministrator();
    GroupDto group = db.users().insertGroup("my-group");
    dbClient.notificationGroupSubscriptionsDao().insert(dbSession, group.getUuid(), "TypeA", "ChannelX");
    dbSession.commit();

    Notifications.ListGroupsResponse response = ws.newRequest().executeProtobuf(Notifications.ListGroupsResponse.class);

    assertThat(response.getSubscriptionsList())
      .extracting(
        Notifications.GroupSubscription::getGroupUuid,
        Notifications.GroupSubscription::getGroupName,
        Notifications.GroupSubscription::getNotificationType,
        Notifications.GroupSubscription::getChannelKey)
      .containsExactly(tuple(group.getUuid(), "my-group", "TypeA", "ChannelX"));
  }

  @Test
  void falls_back_to_uuid_when_group_not_found() {
    userSession.logIn().setSystemAdministrator();
    dbClient.notificationGroupSubscriptionsDao().insert(dbSession, "orphan-uuid", "TypeA", "ChannelX");
    dbSession.commit();

    Notifications.ListGroupsResponse response = ws.newRequest().executeProtobuf(Notifications.ListGroupsResponse.class);

    assertThat(response.getSubscriptionsList())
      .extracting(
        Notifications.GroupSubscription::getGroupUuid,
        Notifications.GroupSubscription::getGroupName)
      .containsExactly(tuple("orphan-uuid", "orphan-uuid"));
  }
}
