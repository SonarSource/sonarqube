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
package org.sonar.server.ce.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.queue.CeQueue;
import org.sonar.server.user.UserSession;

public class PauseAction implements CeWsAction {

  private static final String PASSCODE_REMOVAL_CHANGE = "System passcode is no longer supported, the system administration permission is now required.";

  private final UserSession userSession;
  private final CeQueue ceQueue;

  public PauseAction(UserSession userSession, CeQueue ceQueue) {
    this.userSession = userSession;
    this.ceQueue = ceQueue;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("pause")
      .setDescription("Requests pause of Compute Engine workers. Requires the system administration permission.")
      .setSince("7.2")
      .setChangelog(
        new Change("2026.6", PASSCODE_REMOVAL_CHANGE),
        new Change("26.10", PASSCODE_REMOVAL_CHANGE))
      .setInternal(true)
      .setHandler(this)
      .setPost(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();

    ceQueue.pauseWorkers();
  }
}
