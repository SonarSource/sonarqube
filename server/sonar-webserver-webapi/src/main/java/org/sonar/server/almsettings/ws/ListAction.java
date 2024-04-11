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
package org.sonar.server.almsettings.ws;

import java.util.Comparator;
import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings.AlmSetting;
import org.sonarqube.ws.AlmSettings.ListWsResponse;

import static java.util.Optional.ofNullable;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.common.AlmSettingMapper.toResponseAlm;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListAction implements AlmSettingsWsAction {

  private static final String PARAM_PROJECT = "project";
  private static final String BITBUCKETCLOUD_ROOT_URL = "https://bitbucket.org/";

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
      .setDescription("List DevOps Platform setting available for a given project, sorted by DevOps Platform key<br/>" +
        "Requires the 'Administer project' permission if the '" + PARAM_PROJECT + "' parameter is provided, requires the 'Create Projects' permission otherwise.")
      .setSince("8.1")
      .setResponseExample(getClass().getResource("example-list.json"))
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(false);

    action.setChangelog(
      new Change("8.3", "Permission needed changed to 'Administer project' or 'Create Projects'"),
      new Change("8.2", "Permission needed changed from 'Administer project' to 'Create Projects'"),
      new Change("8.6", "Field 'URL' added for Azure definitions"));
  }

  @Override
  public void handle(Request request, Response response) {
    ListWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private ListWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Request.StringParam projectKey = request.getParam(PARAM_PROJECT);
      if (projectKey.isPresent()) {
        ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey.getValue());
        userSession.checkEntityPermission(ADMIN, project);
      } else {
        userSession.checkPermission(PROVISION_PROJECTS);
      }

      List<AlmSettingDto> settings = dbClient.almSettingDao().selectAll(dbSession);
      List<AlmSetting> wsAlmSettings = settings
        .stream()
        .sorted(Comparator.comparing(AlmSettingDto::getKey))
        .map(almSetting -> {
          AlmSetting.Builder almSettingBuilder = AlmSetting.newBuilder()
            .setKey(almSetting.getKey())
            .setAlm(toResponseAlm(almSetting.getAlm()));

          if (almSetting.getAlm() == ALM.BITBUCKET_CLOUD) {
            almSettingBuilder.setUrl(BITBUCKETCLOUD_ROOT_URL + almSetting.getAppId() + "/");
          } else {
            ofNullable(almSetting.getUrl()).ifPresent(almSettingBuilder::setUrl);
          }

          return almSettingBuilder.build();
        })
        .toList();
      return ListWsResponse.newBuilder()
        .addAllAlmSettings(wsAlmSettings).build();
    }
  }
}
