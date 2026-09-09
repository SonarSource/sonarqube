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
import java.util.Objects;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.user.UserSession;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public abstract class AbstractGroupNotificationAction implements NotificationsWsAction {

  static final String PARAM_GROUP_UUID = "groupUuid";
  static final String PARAM_TYPE = "type";
  // Maps notification type name to channel key. Populated by each feature that uses group subscriptions;
  // this map controls which types are accepted by add_group / remove_group and how they are stored.
  static final Map<String, String> CHANNEL_BY_TYPE = Map.of();

  protected final DbClient dbClient;
  protected final UserSession userSession;
  protected final Map<String, String> channelByType;

  AbstractGroupNotificationAction(
    DbClient dbClient,
    UserSession userSession,
    Map<String, String> channelByType) {
    this.dbClient = Objects.requireNonNull(dbClient);
    this.userSession = Objects.requireNonNull(userSession);
    this.channelByType = Objects.requireNonNull(channelByType);
  }

  protected void defineGroupAndTypeParams(WebService.NewAction action) {
    action.createParam(PARAM_GROUP_UUID)
      .setDescription("Group UUID")
      .setRequired(true)
      .setExampleValue("AU-TpxcA-iU5OvuD2FL7");

    action.createParam(PARAM_TYPE)
      .setDescription("Notification type")
      .setRequired(true)
      .setPossibleValues(channelByType.keySet());
  }

  protected GroupTypeChannel validateAndExtract(Request request) {
    userSession.checkIsSystemAdministrator();

    String groupUuid = request.mandatoryParam(PARAM_GROUP_UUID);
    String type = request.mandatoryParam(PARAM_TYPE);
    String channel = channelByType.get(type);
    checkRequest(channel != null, "Unknown notification type: '%s'", type);

    return new GroupTypeChannel(groupUuid, type, channel);
  }

  public static class GroupTypeChannel {
    public final String groupUuid;
    public final String type;
    public final String channel;

    public GroupTypeChannel(
      String groupUuid,
      String type,
      String channel) {
      this.groupUuid = Objects.requireNonNull(groupUuid);
      this.type = Objects.requireNonNull(type);
      this.channel = Objects.requireNonNull(channel);
    }
  }
}
