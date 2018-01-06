/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.edition.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.user.UserSession;

public class ClearErrorMessageAction implements EditionsWsAction {
  private final UserSession userSession;
  private final MutableEditionManagementState editionManagementState;

  public ClearErrorMessageAction(UserSession userSession, MutableEditionManagementState editionManagementState) {
    this.userSession = userSession;
    this.editionManagementState = editionManagementState;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("clear_error_message")
      .setSince("6.7")
      .setPost(true)
      .setDescription("Clear error message of last install of an edition (if any). Require 'Administer System' permission.")
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession
      .checkLoggedIn()
      .checkIsSystemAdministrator();

    editionManagementState.clearInstallErrorMessage();

    response.noContent();
  }
}
