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
package org.sonar.server.platform.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.app.RestartFlagHolder;
import org.sonar.server.platform.WebServer;
import org.sonar.server.user.UserSession;

/**
 * Implementation of the {@code restart} action for the System WebService.
 */
public class RestartAction implements SystemWsAction {

  private static final Logger LOGGER = Loggers.get(RestartAction.class);

  private final UserSession userSession;
  private final ProcessCommandWrapper processCommandWrapper;
  private final RestartFlagHolder restartFlagHolder;
  private final WebServer webServer;

  public RestartAction(UserSession userSession, ProcessCommandWrapper processCommandWrapper, RestartFlagHolder restartFlagHolder,
    WebServer webServer) {
    this.userSession = userSession;
    this.processCommandWrapper = processCommandWrapper;
    this.restartFlagHolder = restartFlagHolder;
    this.webServer = webServer;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("restart")
      .setDescription("Restart server. Require 'Administer System' permission. Perform a full restart of the Web, Search and Compute Engine Servers processes.")
      .setSince("4.3")
      .setPost(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    if (!webServer.isStandalone()) {
      throw new IllegalArgumentException("Restart not allowed for cluster nodes");
    }

    userSession.checkIsSystemAdministrator();

    LOGGER.info("SonarQube restart requested by {}", userSession.getLogin());
    restartFlagHolder.set();
    processCommandWrapper.requestSQRestart();
  }

}
