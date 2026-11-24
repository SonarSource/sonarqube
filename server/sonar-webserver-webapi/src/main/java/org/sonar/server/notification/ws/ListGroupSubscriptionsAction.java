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
import java.util.Objects;
import java.util.Comparator;
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
import static java.util.stream.Collectors.toMap;

public class ListGroupSubscriptionsAction implements NotificationsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupNotificationSubscriptionsService subscriptionsService;

  public ListGroupSubscriptionsAction(DbClient dbClient, UserSession userSession, GroupNotificationSubscriptionsService subscriptionsService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.subscriptionsService = subscriptionsService;
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

    String userUuid = Objects.requireNonNull(userSession.getUuid(), "User must be authenticated");
    List<GroupNotificationSubscription> subscriptions = subscriptionsService.selectForUser(userUuid, null);
    List<String> groupUuids = subscriptions.stream().map(GroupNotificationSubscription::groupUuid).distinct().toList();

    Map<String, String> namesByUuid = groupUuids.isEmpty() ? Map.of() : selectGroupNames(groupUuids);

    Notifications.ListGroupSubscriptionsResponse.Builder responseBuilder = Notifications.ListGroupSubscriptionsResponse.newBuilder();
    List<GroupNotificationSubscriptionView> sortedSubscriptions = subscriptions.stream()
      .map(sub -> GroupNotificationSubscriptionView.from(sub, namesByUuid))
      .sorted(Comparator.comparing(GroupNotificationSubscriptionView::groupName)
        .thenComparing(GroupNotificationSubscriptionView::notificationType))
      .toList();
    for (GroupNotificationSubscriptionView view : sortedSubscriptions) {
      responseBuilder.addGroupSubscriptions(Notifications.UserGroupSubscription.newBuilder()
        .setGroupName(view.groupName())
        .setNotificationType(view.notificationType())
        .setChannelKey(view.channelKey())
        .setGroupUuid(view.groupUuid()));
    }

    writeProtobuf(responseBuilder.build(), request, response);
  }

  private Map<String, String> selectGroupNames(List<String> groupUuids) {
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.groupDao().selectByUuids(session, groupUuids).stream()
        .collect(toMap(GroupDto::getUuid, GroupDto::getName));
    }
  }
}
