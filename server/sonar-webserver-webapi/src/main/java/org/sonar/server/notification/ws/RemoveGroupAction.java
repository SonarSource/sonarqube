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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.user.UserSession;
import com.sonarsource.groupnotifications.api.GroupNotificationSubscriptionsService;

public class RemoveGroupAction extends AbstractGroupNotificationAction {

  public RemoveGroupAction(UserSession userSession, GroupNotificationSubscriptionsService subscriptionsService) {
    super(userSession, subscriptionsService);
  }

  @Override
  public void define(WebService.NewController context) {
    defineAction(context, "remove_group", "Remove a group notification subscription. No-op if not found. Requires system administration permission.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    handleSubscription(request, response, subscriptionsService::remove);
  }
}
