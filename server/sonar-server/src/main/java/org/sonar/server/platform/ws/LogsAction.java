/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.platform.ws;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.server.platform.ServerLogging;
import org.sonarqube.ws.MediaTypes;
import org.sonar.server.user.UserSession;

public class LogsAction implements SystemWsAction {

  private final UserSession userSession;
  private final ServerLogging serverLogging;

  public LogsAction(UserSession userSession, ServerLogging serverLogging) {
    this.userSession = userSession;
    this.serverLogging = serverLogging;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("logs")
      .setDescription("System logs in plain-text format. Requires system administration permission.")
      .setResponseExample(getClass().getResource("logs-example.log"))
      .setSince("5.2")
      .setHandler(this);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkGlobalPermission(UserRole.ADMIN);
    wsResponse.stream().setMediaType(MediaTypes.TXT);
    File file = serverLogging.getCurrentLogFile();
    if (file.exists()) {
      FileUtils.copyFile(file, wsResponse.stream().output());
    }
  }
}
