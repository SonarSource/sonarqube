/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.almsettings.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings.CountBindingWsResponse;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class CountBindingAction implements AlmSettingsWsAction {

  private static final String PARAM_ALM_SETTING = "almSetting";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AlmSettingsSupport almSettingsSupport;

  public CountBindingAction(DbClient dbClient, UserSession userSession, AlmSettingsSupport almSettingsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.almSettingsSupport = almSettingsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("count_binding")
      .setDescription("Count number of project bound to an DevOps Platform setting.<br/>" +
        "Requires the 'Administer System' permission")
      .setSince("8.1")
      .setResponseExample(getClass().getResource("example-count_binding.json"))
      .setHandler(this);

    action
      .createParam(PARAM_ALM_SETTING)
      .setDescription("DevOps Platform setting key")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkIsSystemAdministrator();
    CountBindingWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private CountBindingWsResponse doHandle(Request request) {
    String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
    try (DbSession dbSession = dbClient.openSession(false)) {
      AlmSettingDto almSetting = almSettingsSupport.getAlmSetting(dbSession, almSettingKey);
      int projectsBound = dbClient.projectAlmSettingDao().countByAlmSetting(dbSession, almSetting);
      return CountBindingWsResponse.newBuilder()
        .setKey(almSetting.getKey())
        .setProjects(projectsBound)
        .build();
    }
  }
}
