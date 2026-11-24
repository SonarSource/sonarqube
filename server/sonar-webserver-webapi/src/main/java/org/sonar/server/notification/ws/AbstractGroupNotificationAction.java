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
import java.util.function.Consumer;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.notification.email.EmailNotificationChannel;
import org.sonar.server.user.UserSession;
import com.sonarsource.groupnotifications.api.GroupNotificationSubscriptionsService;
import com.sonarsource.groupnotifications.api.model.GroupNotificationSubscription;
import com.sonarsource.groupnotifications.api.model.GroupNotificationType;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;

public abstract class AbstractGroupNotificationAction implements NotificationsWsAction {

  static final String PARAM_GROUP_UUID = "groupUuid";
  static final String PARAM_TYPE = "type";
  static final String PARAM_CHANNEL = "channel";
  static final String CHANNEL_KEY = EmailNotificationChannel.class.getSimpleName();
  static final String BANNER_CHANNEL_KEY = "BannerChannel";

  protected final UserSession userSession;
  protected final GroupNotificationSubscriptionsService subscriptionsService;

  AbstractGroupNotificationAction(
    UserSession userSession,
    GroupNotificationSubscriptionsService subscriptionsService) {
    this.userSession = Objects.requireNonNull(userSession);
    this.subscriptionsService = Objects.requireNonNull(subscriptionsService);
  }

  protected void defineGroupAndTypeParams(WebService.NewAction action) {
    action.createParam(PARAM_GROUP_UUID)
      .setDescription("Group UUID")
      .setRequired(true)
      .setExampleValue("AU-TpxcA-iU5OvuD2FL7");

    action.createParam(PARAM_TYPE)
      .setDescription("Notification type")
      .setRequired(true)
      .setPossibleValues(availableTypeKeys());

    action.createParam(PARAM_CHANNEL)
      .setDescription("Delivery channel. Defaults to email for compatibility.")
      .setPossibleValues(availableChannelKeys());
  }

  protected void defineAction(WebService.NewController context, String actionKey, String description) {
    WebService.NewAction action = context.createAction(actionKey)
      .setDescription(description)
      .setSince("2026.5")
      .setInternal(true)
      .setPost(true)
      .setHandler(this);
    defineGroupAndTypeParams(action);
  }

  protected void handleSubscription(Request request, Response response, Consumer<GroupNotificationSubscription> operation) {
    GroupTypeChannel groupTypeChannel = validateAndExtract(request);
    operation.accept(new GroupNotificationSubscription(null, groupTypeChannel.groupUuid, groupTypeChannel.type, groupTypeChannel.channel));
    response.noContent();
  }

  protected GroupTypeChannel validateAndExtract(Request request) {
    userSession.checkIsSystemAdministrator();

    String groupUuid = request.mandatoryParam(PARAM_GROUP_UUID);
    String type = request.mandatoryParam(PARAM_TYPE);
    String channel = request.param(PARAM_CHANNEL);
    if (channel == null || channel.isBlank()) {
      channel = CHANNEL_KEY;
    }
    GroupNotificationType requestedType = new GroupNotificationType(type, channel);
    List<GroupNotificationType> supportedTypes = subscriptionsService.selectAvailableTypes();
    checkRequest(supportedTypes.contains(requestedType), "Unsupported notification type '%s' for channel '%s'", type, channel);

    return new GroupTypeChannel(groupUuid, type, channel);
  }

  private List<String> availableTypeKeys() {
    return subscriptionsService.selectAvailableTypes().stream()
      .map(GroupNotificationType::notificationType)
      .distinct()
      .toList();
  }

  private List<String> availableChannelKeys() {
    return subscriptionsService.selectAvailableTypes().stream()
      .map(GroupNotificationType::channelKey)
      .distinct()
      .toList();
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
