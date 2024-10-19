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
package org.sonar.server.platform.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.app.RestartFlagHolder;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.user.UserSession;

/**
 * Implementation of the {@code restart} action for the System WebService.
 */
public class RestartAction implements SystemWsAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestartAction.class);

  private final UserSession userSession;
  private final ProcessCommandWrapper processCommandWrapper;
  private final RestartFlagHolder restartFlagHolder;
  private final NodeInformation nodeInformation;

  public RestartAction(UserSession userSession, ProcessCommandWrapper processCommandWrapper, RestartFlagHolder restartFlagHolder,
    NodeInformation nodeInformation) {
    this.userSession = userSession;
    this.processCommandWrapper = processCommandWrapper;
    this.restartFlagHolder = restartFlagHolder;
    this.nodeInformation = nodeInformation;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("restart")
      .setDescription("Restarts server. Requires 'Administer System' permission. Performs a full restart of the Web, Search and Compute Engine Servers processes."
         + " Does not reload sonar.properties.")
      .setSince("4.3")
      .setPost(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) {
    if (!nodeInformation.isStandalone()) {
      throw new IllegalArgumentException("Restart not allowed for cluster nodes");
    }

    userSession.checkIsSystemAdministrator();

    LOGGER.info("SonarQube restart requested by {}", userSession.getLogin());
    restartFlagHolder.set();
    processCommandWrapper.requestSQRestart();
  }

}
