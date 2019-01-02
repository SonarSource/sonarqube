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
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.Ce;

public class InfoAction implements CeWsAction {

  private final UserSession userSession;
  private final SystemPasscode systemPasscode;
  private final CeQueue ceQueue;

  public InfoAction(UserSession userSession, SystemPasscode systemPasscode, CeQueue ceQueue) {
    this.userSession = userSession;
    this.systemPasscode = systemPasscode;
    this.ceQueue = ceQueue;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("info")
      .setDescription("Gets information about Compute Engine. Requires the system administration permission or " +
        "system passcode (see " + SystemPasscodeImpl.PASSCODE_CONF_PROPERTY + " in sonar.properties).")
      .setSince("7.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(getClass().getResource("info-example.json"));
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    if (!systemPasscode.isValid(request) && !userSession.isSystemAdministrator()) {
      throw AbstractUserSession.insufficientPrivilegesException();
    }

    Ce.InfoWsResponse.Builder builder = Ce.InfoWsResponse.newBuilder();
    CeQueue.WorkersPauseStatus status = ceQueue.getWorkersPauseStatus();
    builder.setWorkersPauseStatus(convert(status));
    WsUtils.writeProtobuf(builder.build(), request, response);
  }

  private Ce.WorkersPauseStatus convert(CeQueue.WorkersPauseStatus status) {
    switch (status) {
      case PAUSING:
        return Ce.WorkersPauseStatus.PAUSING;
      case PAUSED:
        return Ce.WorkersPauseStatus.PAUSED;
      case RESUMED:
        return Ce.WorkersPauseStatus.RESUMED;
      default:
        throw new IllegalStateException("Unsupported WorkersPauseStatus: " + status);
    }
  }
}
