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
package org.sonar.server.almintegration.ws.gitlab;

import javax.annotation.Nullable;
import jakarta.inject.Inject;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.common.project.ImportProjectRequest;
import org.sonar.server.common.project.ImportProjectService;
import org.sonar.server.common.project.ImportedProject;
import org.sonarqube.ws.Projects.CreateWsResponse;

import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportGitLabProjectAction implements AlmIntegrationsWsAction {

  public static final String PARAM_GITLAB_PROJECT_ID = "gitlabProjectId";

  private final ImportProjectService importProjectService;
  private final ImportHelper importHelper;

  @Inject
  public ImportGitLabProjectAction(ImportProjectService importProjectService, ImportHelper importHelper) {
    this.importProjectService = importProjectService;
    this.importHelper = importHelper;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("import_gitlab_project")
      .setDescription("Import a GitLab project to SonarQube, creating a new project and configuring MR decoration<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setSince("8.5")
      .setHandler(this)
      .setChangelog(
        new Change("10.5", "This endpoint is deprecated, please use its API v2 version /api/v2/dop-translation/bound-projects"),
        new Change("10.3", String.format("Parameter %s becomes optional if you have only one configuration for GitLab", PARAM_ALM_SETTING)))
      .setDeprecatedSince("10.5");

    action.createParam(ImportHelper.PARAM_ALM_SETTING)
      .setDescription("DevOps Platform configuration key. This parameter is optional if you have only one GitLab integration.");

    action.createParam(PARAM_GITLAB_PROJECT_ID)
      .setRequired(true)
      .setDescription("GitLab project ID");

    action.createParam(PARAM_NEW_CODE_DEFINITION_TYPE)
      .setDescription(NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION)
      .setSince("10.1");

    action.createParam(PARAM_NEW_CODE_DEFINITION_VALUE)
      .setDescription(NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION)
      .setSince("10.1");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    CreateWsResponse createResponse = doHandle(request);
    writeProtobuf(createResponse, request, response);
  }

  private CreateWsResponse doHandle(Request request) {
    importHelper.checkProvisionProjectPermission();
    AlmSettingDto almSettingDto = importHelper.getAlmSettingDtoForAlm(request, ALM.GITLAB);
    String gitlabProjectId = request.mandatoryParam(PARAM_GITLAB_PROJECT_ID);
    String newCodeDefinitionType = request.param(PARAM_NEW_CODE_DEFINITION_TYPE);
    String newCodeDefinitionValue = request.param(PARAM_NEW_CODE_DEFINITION_VALUE);
    ImportedProject importedProject = importProjectService.importProject(toServiceRequest(almSettingDto, gitlabProjectId, newCodeDefinitionType, newCodeDefinitionValue));

    return ImportHelper.toCreateResponse(importedProject.projectDto());
  }

  private static ImportProjectRequest toServiceRequest(AlmSettingDto almSettingDto, String gitlabProjectId, @Nullable String newCodeDefinitionType,
    @Nullable String newCodeDefinitionValue) {
    return new ImportProjectRequest(null, null, almSettingDto.getUuid(), gitlabProjectId, null, newCodeDefinitionType, newCodeDefinitionValue, false);
  }
}
