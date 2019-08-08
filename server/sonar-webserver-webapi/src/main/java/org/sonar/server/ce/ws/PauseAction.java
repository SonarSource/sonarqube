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
package org.sonar.server.ce.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.queue.CeQueue;
import org.sonar.server.user.AbstractUserSession;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.user.SystemPasscodeImpl;
import org.sonar.server.user.UserSession;

public class PauseAction implements CeWsAction {

  private final UserSession userSession;
  private final SystemPasscode systemPasscode;
  private final CeQueue ceQueue;

  public PauseAction(UserSession userSession, SystemPasscode systemPasscode, CeQueue ceQueue) {
    this.userSession = userSession;
    this.systemPasscode = systemPasscode;
    this.ceQueue = ceQueue;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("pause")
      .setDescription("Requests pause of Compute Engine workers. Requires the system administration permission or " +
        "system passcode (see " + SystemPasscodeImpl.PASSCODE_CONF_PROPERTY + " in sonar.properties).")
      .setSince("7.2")
      .setInternal(true)
      .setHandler(this)
      .setPost(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (!systemPasscode.isValid(request) && !userSession.isSystemAdministrator()) {
      throw AbstractUserSession.insufficientPrivilegesException();
    }

    ceQueue.pauseWorkers();
  }
}
