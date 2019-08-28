/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Controller;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationsWsTest {
  private NotificationsWsAction action = new FakeNotificationAction();
  private NotificationsWsAction[] actions = {action};
  private NotificationsWs underTest = new NotificationsWs(actions);

  @Test
  public void definition() {
    WebService.Context context = new WebService.Context();

    underTest.define(context);

    Controller controller = context.controller("api/notifications");
    assertThat(controller.path()).isEqualTo("api/notifications");
  }

  private static class FakeNotificationAction implements NotificationsWsAction {
    @Override
    public void define(WebService.NewController context) {
      context.createAction("fake")
        .setHandler(this);
    }

    @Override
    public void handle(Request request, Response response) {
      // do nothing
    }
  }
}
