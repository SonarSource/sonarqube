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
package org.sonar.server.almsettings;

import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings.AlmSetting;
import org.sonarqube.ws.AlmSettings.ListWsResponse;

import static java.util.Optional.ofNullable;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements AlmSettingsWsAction {

  private static final String PARAM_PROJECT = "project";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public ListAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list")
      .setDescription("List ALM setting available for a given project.<br/>" +
        "Requires the 'Administer' permission on the project")
      .setSince("8.1")
      .setResponseExample(getClass().getResource("list-example.json"))
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ListWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private ListWsResponse doHandle(Request request) {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
      userSession.checkComponentPermission(ADMIN, project);
      List<AlmSettingDto> settings = dbClient.almSettingDao().selectAll(dbSession);
      List<AlmSetting> wsAlmSettings = settings.stream()
        .map(almSetting -> {
          AlmSetting.Builder almSettingBuilder = AlmSetting.newBuilder()
            .setKey(almSetting.getKey())
            .setAlm(AlmSettingsSupport.toAlmWs(almSetting.getAlm()));
          ofNullable(almSetting.getUrl()).ifPresent(almSettingBuilder::setUrl);
          return almSettingBuilder.build();
        })
        .collect(Collectors.toList());
      return ListWsResponse.newBuilder()
        .addAllAlmSettings(wsAlmSettings).build();
    }
  }

}
