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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.Notifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ListGroupSubscriptionsActionIT {

  @RegisterExtension
  public final UserSessionRule userSession = UserSessionRule.standalone();
  @RegisterExtension
  public final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final WsActionTester ws = new WsActionTester(new ListGroupSubscriptionsAction(dbClient, userSession));

  @Test
  void fails_when_anonymous() {
    userSession.anonymous();

    var request = ws.newRequest();
    assertThatThrownBy(request::execute).isInstanceOf(UnauthorizedException.class);
  }

  @Nested
  class WhenLoggedIn {

    private UserDto user;

    @BeforeEach
    void setUp() {
      user = db.users().insertUser();
      userSession.logIn(user);
    }

    @Test
    void returns_empty_list_when_user_has_no_group_memberships() {
      Notifications.ListGroupSubscriptionsResponse response = ws.newRequest()
        .executeProtobuf(Notifications.ListGroupSubscriptionsResponse.class);

      assertThat(response.getGroupSubscriptionsList()).isEmpty();
    }

    @Nested
    class WithGroupMembership {

      private GroupDto group;

      @BeforeEach
      void setUp() {
        group = db.users().insertGroup("my-group");
        db.users().insertMember(group, user);
      }

      @Test
      void returns_empty_list_when_user_groups_have_no_subscriptions() {
        Notifications.ListGroupSubscriptionsResponse response = ws.newRequest()
          .executeProtobuf(Notifications.ListGroupSubscriptionsResponse.class);

        assertThat(response.getGroupSubscriptionsList()).isEmpty();
      }

      @Test
      void returns_subscriptions_for_user_groups_only() {
        GroupDto otherGroup = db.users().insertGroup("other-group");
        dbClient.notificationGroupSubscriptionsDao().insert(dbSession, group.getUuid(), "security-alert-raised", "email");
        dbClient.notificationGroupSubscriptionsDao().insert(dbSession, otherGroup.getUuid(), "security-alert-raised", "email");
        dbSession.commit();

        Notifications.ListGroupSubscriptionsResponse response = ws.newRequest()
          .executeProtobuf(Notifications.ListGroupSubscriptionsResponse.class);

        assertThat(response.getGroupSubscriptionsList())
          .extracting(
            Notifications.UserGroupSubscription::getGroupName,
            Notifications.UserGroupSubscription::getNotificationType)
          .containsExactly(tuple("my-group", "security-alert-raised"));
      }

      @Test
      void does_not_require_admin_permission() {
        dbClient.notificationGroupSubscriptionsDao().insert(dbSession, group.getUuid(), "security-alert-raised", "email");
        dbSession.commit();
        userSession.logIn(user).setNonSystemAdministrator();

        Notifications.ListGroupSubscriptionsResponse response = ws.newRequest()
          .executeProtobuf(Notifications.ListGroupSubscriptionsResponse.class);

        assertThat(response.getGroupSubscriptionsList()).hasSize(1);
      }
    }
  }
}
