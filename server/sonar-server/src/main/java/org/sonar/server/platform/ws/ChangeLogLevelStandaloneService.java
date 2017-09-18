/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.ce.http.CeHttpClient;
import org.sonar.db.Database;
import org.sonar.server.app.WebServerProcessLogging;
import org.sonar.server.platform.ServerLogging;

public class ChangeLogLevelStandaloneService implements ChangeLogLevelService {

  private final ServerLogging logging;
  private final Database db;
  private final CeHttpClient ceHttpClient;
  private final WebServerProcessLogging webServerProcessLogging;

  public ChangeLogLevelStandaloneService(ServerLogging logging, Database db, CeHttpClient ceHttpClient, WebServerProcessLogging webServerProcessLogging) {
    this.logging = logging;
    this.db = db;
    this.ceHttpClient = ceHttpClient;
    this.webServerProcessLogging = webServerProcessLogging;
  }

  public void changeLogLevel(LoggerLevel level) {
    db.enableSqlLogging(level.equals(LoggerLevel.TRACE));
    logging.changeLevel(webServerProcessLogging, level);
    ceHttpClient.changeLogLevel(level);
  }
}
