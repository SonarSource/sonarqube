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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.web.UserRole;
import org.sonar.db.Database;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.user.UserSession;

public class ChangeLogLevelAction implements SystemWsAction {

  private static final String PARAM_LEVEL = "level";

  private final UserSession userSession;
  private final ServerLogging logging;
  private final Database db;

  public ChangeLogLevelAction(UserSession userSession, ServerLogging logging, Database db) {
    this.userSession = userSession;
    this.logging = logging;
    this.db = db;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction newAction = controller.createAction("change_log_level")
      .setDescription("Temporarily changes level of logs. New level is not persistent and is lost " +
        "when restarting server. Requires system administration permission.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    newAction.createParam(PARAM_LEVEL)
      .setDescription("The new level. Be cautious: DEBUG, and even more TRACE, may have performance impacts.")
      .setPossibleValues(ServerLogging.ALLOWED_ROOT_LOG_LEVELS)
      .setRequired(true);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) {
    userSession.checkGlobalPermission(UserRole.ADMIN);
    LoggerLevel level = LoggerLevel.valueOf(wsRequest.mandatoryParam(PARAM_LEVEL));
    db.enableSqlLogging(level.equals(LoggerLevel.TRACE));
    logging.changeLevel(level);
    wsResponse.noContent();
  }
}
