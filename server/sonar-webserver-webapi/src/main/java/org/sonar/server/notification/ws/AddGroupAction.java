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

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.user.UserSession;

import static org.sonar.server.exceptions.NotFoundException.checkFound;

public class AddGroupAction extends AbstractGroupNotificationAction {

  @Autowired
  public AddGroupAction(DbClient dbClient, UserSession userSession) {
    this(dbClient, userSession, CHANNEL_BY_TYPE);
  }

  AddGroupAction(
    DbClient dbClient,
    UserSession userSession,
    Map<String, String> channelByType) {
    super(dbClient, userSession, channelByType);
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("add_group")
      .setDescription("Subscribe a group to a notification type. Requires system administration permission.")
      .setSince("2026.5")
      .setInternal(true)
      .setPost(true)
      .setHandler(this);

    defineGroupAndTypeParams(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    GroupTypeChannel groupTypeChannel = validateAndExtract(request);

    try (DbSession session = dbClient.openSession(false)) {
      checkFound(dbClient.groupDao().selectByUuid(session, groupTypeChannel.groupUuid), "Group '%s' not found", groupTypeChannel.groupUuid);
      dbClient.notificationGroupSubscriptionsDao().deleteByGroupAndTypeAndChannel(session, groupTypeChannel.groupUuid, groupTypeChannel.type, groupTypeChannel.channel);
      dbClient.notificationGroupSubscriptionsDao().insert(session, groupTypeChannel.groupUuid, groupTypeChannel.type, groupTypeChannel.channel);
      session.commit();
    }

    response.noContent();
  }
}
