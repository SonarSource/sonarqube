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
import java.util.Objects;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.notification.NotificationGroupSubscriptionDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Notifications;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListGroupSubscriptionsAction implements NotificationsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;

  public ListGroupSubscriptionsAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("list_group_subscriptions")
      .setDescription("List notification group subscriptions for the current user's groups. Requires authentication.")
      .setSince("2026.5")
      .setInternal(true)
      .setResponseExample(getClass().getResource("list_group_subscriptions-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    try (DbSession session = dbClient.openSession(false)) {
      String userUuid = Objects.requireNonNull(userSession.getUuid(), "User must be authenticated");
      List<NotificationGroupSubscriptionDto> subscriptions =
        dbClient.notificationGroupSubscriptionsDao().selectByUserUuidWithGroupName(session, userUuid);

      Notifications.ListGroupSubscriptionsResponse.Builder responseBuilder = Notifications.ListGroupSubscriptionsResponse.newBuilder();
      for (NotificationGroupSubscriptionDto sub : subscriptions) {
        responseBuilder.addGroupSubscriptions(Notifications.UserGroupSubscription.newBuilder()
          .setGroupName(sub.getGroupName())
          .setNotificationType(sub.getNotificationType()));
      }

      writeProtobuf(responseBuilder.build(), request, response);
    }
  }
}
