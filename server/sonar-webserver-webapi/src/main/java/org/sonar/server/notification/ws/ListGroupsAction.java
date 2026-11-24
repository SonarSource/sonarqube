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
import org.sonar.db.user.GroupDto;
import org.sonar.server.user.UserSession;
import com.sonarsource.groupnotifications.api.GroupNotificationSubscriptionsService;
import com.sonarsource.groupnotifications.api.model.GroupNotificationSubscription;
import org.sonarqube.ws.Notifications;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListGroupsAction implements NotificationsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupNotificationSubscriptionsService subscriptionsService;

  public ListGroupsAction(DbClient dbClient, UserSession userSession, GroupNotificationSubscriptionsService subscriptionsService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.subscriptionsService = subscriptionsService;
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

    List<GroupNotificationSubscription> subscriptions = subscriptionsService.selectByOrganization(null);
    try (DbSession session = dbClient.openSession(false)) {
      List<String> groupUuids = subscriptions.stream().map(GroupNotificationSubscription::groupUuid).distinct().toList();
      Map<String, String> namesByUuid = groupUuids.isEmpty()
        ? Map.of()
        : dbClient.groupDao().selectByUuids(session, groupUuids).stream()
          .collect(Collectors.toMap(GroupDto::getUuid, GroupDto::getName));

      Notifications.ListGroupsResponse.Builder responseBuilder = Notifications.ListGroupsResponse.newBuilder();
      subscriptionsService.selectAvailableTypes().forEach(type -> responseBuilder.addAvailableTypes(
        Notifications.GroupNotificationType.newBuilder()
          .setNotificationType(type.notificationType())
          .setChannelKey(type.channelKey())));
      for (GroupNotificationSubscription sub : subscriptions) {
        GroupNotificationSubscriptionView view = GroupNotificationSubscriptionView.from(sub, namesByUuid);
        responseBuilder.addSubscriptions(Notifications.GroupSubscription.newBuilder()
          .setGroupUuid(view.groupUuid())
          .setGroupName(view.groupName())
          .setNotificationType(view.notificationType())
          .setChannelKey(view.channelKey()));
      }
      writeProtobuf(responseBuilder.build(), request, response);
    }
  }
}
