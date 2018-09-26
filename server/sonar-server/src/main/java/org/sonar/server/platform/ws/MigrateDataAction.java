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

import org.sonar.api.config.Configuration;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.server.platform.db.migration.version.v74.PopulateTmpColumnsToCeActivity;
import org.sonar.server.platform.db.migration.version.v74.PopulateTmpColumnsToCeQueue;
import org.sonar.server.platform.db.migration.version.v74.PopulateTmpLastKeyColumnsToCeActivity;
import org.sonar.server.user.UserSession;

public class MigrateDataAction implements SystemWsAction {
  private static final Logger LOG = Loggers.get(MigrateDataAction.class);

  private final UserSession userSession;
  private final DbClient dbClient;

  public MigrateDataAction(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    controller.createAction("migrate_data")
      .setPost(true)
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.isSystemAdministrator();

    Configuration emptyConfiguration = new MapSettings().asConfig();
    new PopulateTmpColumnsToCeQueue(dbClient.getDatabase()).execute();
    new PopulateTmpColumnsToCeActivity(dbClient.getDatabase(), emptyConfiguration).execute();
    new PopulateTmpLastKeyColumnsToCeActivity(dbClient.getDatabase(), emptyConfiguration).execute();
    LOG.info("done");

    response.noContent();
  }
}
