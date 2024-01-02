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

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings.GetBindingWsResponse;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.almsettings.ws.AlmSettingsSupport.toAlmWs;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GetBindingAction implements AlmSettingsWsAction {

  private static final String PARAM_PROJECT = "project";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public GetBindingAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("get_binding")
      .setDescription("Get DevOps Platform binding of a given project.<br/>" +
        "Requires the 'Browse' permission on the project")
      .setSince("8.1")
      .setResponseExample(getClass().getResource("example-get_binding.json"))
      .setChangelog(
        new Change("8.6", "Azure binding now contains the project and repository names"),
        new Change("8.7", "Azure binding now contains a monorepo flag for monorepo feature in Enterprise Edition and above"),
        new Change("10.1", "Permission needed changed from 'Administer' to 'Browse'"))
      .setHandler(this);

    action
      .createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) {
    GetBindingWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private GetBindingWsResponse doHandle(Request request) {
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
      userSession.checkEntityPermission(USER, project);
      ProjectAlmSettingDto projectAlmSetting = dbClient.projectAlmSettingDao().selectByProject(dbSession, project)
        .orElseThrow(() -> new NotFoundException(format("Project '%s' is not bound to any DevOps Platform", project.getKey())));
      AlmSettingDto almSetting = dbClient.almSettingDao().selectByUuid(dbSession, projectAlmSetting.getAlmSettingUuid())
        .orElseThrow(() -> new IllegalStateException(format("DevOps Platform setting with uuid '%s' cannot be found", projectAlmSetting.getAlmSettingUuid())));

      GetBindingWsResponse.Builder builder = GetBindingWsResponse.newBuilder()
        .setAlm(toAlmWs(almSetting.getAlm()))
        .setKey(almSetting.getKey());
      ofNullable(projectAlmSetting.getAlmRepo()).ifPresent(builder::setRepository);
      ofNullable(almSetting.getUrl()).ifPresent(builder::setUrl);
      ofNullable(projectAlmSetting.getAlmSlug()).ifPresent(builder::setSlug);
      ofNullable(projectAlmSetting.getSummaryCommentEnabled()).ifPresent(builder::setSummaryCommentEnabled);
      ofNullable(projectAlmSetting.getMonorepo()).ifPresent(builder::setMonorepo);
      return builder.build();
    }
  }
}
