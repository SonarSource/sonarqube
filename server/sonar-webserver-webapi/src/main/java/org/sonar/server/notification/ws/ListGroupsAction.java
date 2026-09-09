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
import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.notification.NotificationGroupSubscriptionDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Notifications;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListGroupsAction implements NotificationsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;

  public ListGroupsAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("list_groups")
      .setDescription("List group notification subscriptions. Requires system administration permission.")
      .setSince("2026.5")
      .setInternal(true)
      .setResponseExample(getClass().getResource("list_groups-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    try (DbSession session = dbClient.openSession(false)) {
      List<NotificationGroupSubscriptionDto> subscriptions = dbClient.notificationGroupSubscriptionsDao().selectAll(session);
      List<String> groupUuids = subscriptions.stream().map(NotificationGroupSubscriptionDto::getGroupUuid).distinct().toList();
      Map<String, String> namesByUuid = dbClient.groupDao().selectByUuids(session, groupUuids)
        .stream()
        .collect(Collectors.toMap(GroupDto::getUuid, GroupDto::getName));

      Notifications.ListGroupsResponse.Builder responseBuilder = Notifications.ListGroupsResponse.newBuilder();
      for (NotificationGroupSubscriptionDto sub : subscriptions) {
        responseBuilder.addSubscriptions(Notifications.GroupSubscription.newBuilder()
          .setGroupUuid(sub.getGroupUuid())
          .setGroupName(namesByUuid.getOrDefault(sub.getGroupUuid(), sub.getGroupUuid()))
          .setNotificationType(sub.getNotificationType())
          .setChannelKey(sub.getChannelKey()));
      }
      writeProtobuf(responseBuilder.build(), request, response);
    }
  }
}
