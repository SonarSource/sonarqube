/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.user.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.user.UserSession;

import static org.sonar.server.user.ws.DismissNoticeAction.SONARLINT_AD;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_DISMISS_SONARLINT_AD;

/**
 * @deprecated use DismissNoticeAction
 */
@Deprecated(since = "9.6", forRemoval = true)
public class DismissSonarlintAdAction implements UsersWsAction {
  private final UserSession userSession;
  private final DismissNoticeAction dismissNoticeAction;

  public DismissSonarlintAdAction(UserSession userSession, DismissNoticeAction dismissNoticeAction) {
    this.userSession = userSession;
    this.dismissNoticeAction = dismissNoticeAction;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction(ACTION_DISMISS_SONARLINT_AD)
      .setDescription("Dismiss SonarLint advertisement. Deprecated since 9.6, replaced api/users/dismiss_notice")
      .setSince("9.2")
      .setPost(true)
      .setDeprecatedSince("9.6")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    dismissNoticeAction.dismissNotice(response, userSession.getUuid(), SONARLINT_AD);
  }
}
