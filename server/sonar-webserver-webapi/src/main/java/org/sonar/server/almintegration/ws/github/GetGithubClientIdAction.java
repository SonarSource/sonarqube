/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.almintegration.ws.github;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmIntegrations;

import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GetGithubClientIdAction implements AlmIntegrationsWsAction {

  public static final String PARAM_ALM_SETTING = "almSetting";

  private final DbClient dbClient;
  private final UserSession userSession;

  public GetGithubClientIdAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("get_github_client_id")
      .setDescription("Get the client id of a Github Integration.")
      .setInternal(true)
      .setSince("8.4")
      .setResponseExample(getClass().getResource("example-get_github_client_id.json"))
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");
  }

  @Override
  public void handle(Request request, Response response) {
    AlmIntegrations.GithubClientIdWsResponse getResponse = doHandle(request);
    writeProtobuf(getResponse, request, response);
  }

  private AlmIntegrations.GithubClientIdWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
      AlmSettingDto almSetting = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("Github Setting '%s' not found", almSettingKey)));

      if (almSetting.getClientId() == null) {
        throw new NotFoundException(String.format("No client ID for setting with key '%s'", almSettingKey));
      }
      return AlmIntegrations.GithubClientIdWsResponse.newBuilder()
        .setClientId(almSetting.getClientId())
        .build();
    }
  }
}
