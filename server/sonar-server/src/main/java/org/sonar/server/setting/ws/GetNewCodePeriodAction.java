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
package org.sonar.server.setting.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.newcodeperiod.NewCodePeriodDao;
import org.sonar.server.user.UserSession;

public class GetNewCodePeriodAction implements SettingsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final NewCodePeriodDao newCodePeriodDao;

  public GetNewCodePeriodAction(DbClient dbClient, UserSession userSession, NewCodePeriodDao newCodePeriodDao) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.newCodePeriodDao = newCodePeriodDao;
  }

  @Override
  public void define(WebService.NewController context) {
    context.createAction("get_new_code_period")
      .setDescription("Updates the setting for the New Code Period.<br>" +
        "Requires one of the following permissions: " +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified component</li>" +
        "</ul>")
      .setSince("8.0")
      .setResponseExample(getClass().getResource("generate_secret_key-example.json"))
      .setHandler(this);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    try (DbSession dbSession = dbClient.openSession(false)) {
      // TODO should it fall back branch->project->global?
    }
  }

}
