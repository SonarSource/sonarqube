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
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.user.UserSession;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public abstract class AbstractGroupNotificationAction implements NotificationsWsAction {

  static final String PARAM_GROUP_UUID = "groupUuid";
  static final String PARAM_TYPE = "type";
  // Group subscriptions are delivered by email only, so the channel is not a request parameter.
  static final String CHANNEL_KEY = EmailNotificationChannel.class.getSimpleName();

  protected final DbClient dbClient;
  protected final UserSession userSession;
  private final Dispatchers dispatchers;

  AbstractGroupNotificationAction(
    DbClient dbClient,
    UserSession userSession,
    Dispatchers dispatchers) {
    this.dbClient = Objects.requireNonNull(dbClient);
    this.userSession = Objects.requireNonNull(userSession);
    this.dispatchers = Objects.requireNonNull(dispatchers);
  }

  protected void defineGroupAndTypeParams(WebService.NewAction action) {
    action.createParam(PARAM_GROUP_UUID)
      .setDescription("Group UUID")
      .setRequired(true)
      .setExampleValue("AU-TpxcA-iU5OvuD2FL7");

    action.createParam(PARAM_TYPE)
      .setDescription("Notification type")
      .setRequired(true)
      .setPossibleValues(dispatchers.getGroupSubscriptionDispatchers());
  }

  protected GroupTypeChannel validateAndExtract(Request request) {
    userSession.checkIsSystemAdministrator();

    String groupUuid = request.mandatoryParam(PARAM_GROUP_UUID);
    String type = request.mandatoryParam(PARAM_TYPE);
    // Only types whose dispatcher is actually registered on this instance are accepted: a type
    // advertised by neither api/notifications/list nor any handler could never be delivered.
    List<String> supportedTypes = dispatchers.getGroupSubscriptionDispatchers();
    checkRequest(supportedTypes.contains(type), "Value of parameter '%s' (%s) must be one of: %s", PARAM_TYPE, type, supportedTypes);

    return new GroupTypeChannel(groupUuid, type, CHANNEL_KEY);
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
