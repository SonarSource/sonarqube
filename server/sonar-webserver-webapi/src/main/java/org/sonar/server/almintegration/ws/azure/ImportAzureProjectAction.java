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
package org.sonar.server.almintegration.ws.azure;

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
import static org.sonar.server.almintegration.ws.ImportHelper.toCreateResponse;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportAzureProjectAction implements AlmIntegrationsWsAction {

  private static final String PARAM_REPOSITORY_NAME = "repositoryName";
  private static final String PARAM_PROJECT_NAME = "projectName";

  private final ImportHelper importHelper;

  private final ImportProjectService importProjectService;

  @Inject
  public ImportAzureProjectAction(ImportHelper importHelper, ImportProjectService importProjectService) {
    this.importHelper = importHelper;
    this.importProjectService = importProjectService;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("import_azure_project")
      .setDescription("Create a SonarQube project with the information from the provided Azure DevOps project.<br/>" +
        "Autoconfigure pull request decoration mechanism.<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setSince("8.6")
      .setHandler(this)
      .setChangelog(
        new Change("10.5", "This endpoint is deprecated, please use its API v2 version /api/v2/dop-translation/bound-projects"),
        new Change("10.3", String.format("Parameter %s becomes optional if you have only one configuration for Azure", PARAM_ALM_SETTING)),
        new Change("10.3", "Endpoint visibility change from internal to public"))
      .setDeprecatedSince("10.5");

    action.createParam(PARAM_ALM_SETTING)
      .setMaximumLength(200)
      .setDescription("DevOps Platform configuration key. This parameter is optional if you have only one Azure integration.");

    action.createParam(PARAM_PROJECT_NAME)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Azure project name");

    action.createParam(PARAM_REPOSITORY_NAME)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Azure repository name");

    action.createParam(PARAM_NEW_CODE_DEFINITION_TYPE)
      .setDescription(NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION)
      .setSince("10.1");

    action.createParam(PARAM_NEW_CODE_DEFINITION_VALUE)
      .setDescription(NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION)
      .setSince("10.1");
  }

  @Override
  public void handle(Request request, Response response) {
    CreateWsResponse createResponse = doHandle(request);
    writeProtobuf(createResponse, request, response);
  }

  private CreateWsResponse doHandle(Request request) {
    importHelper.checkProvisionProjectPermission();
    AlmSettingDto almSettingDto = importHelper.getAlmSettingDtoForAlm(request, ALM.AZURE_DEVOPS);
    String newCodeDefinitionType = request.param(PARAM_NEW_CODE_DEFINITION_TYPE);
    String newCodeDefinitionValue = request.param(PARAM_NEW_CODE_DEFINITION_VALUE);
    String projectName = request.mandatoryParam(PARAM_PROJECT_NAME);
    String repositoryName = request.mandatoryParam(PARAM_REPOSITORY_NAME);

    ImportedProject importedProject = importProjectService
      .importProject(toServiceRequest(almSettingDto, projectName, repositoryName, newCodeDefinitionType, newCodeDefinitionValue));

    return toCreateResponse(importedProject.projectDto());
  }

  private static ImportProjectRequest toServiceRequest(AlmSettingDto almSettingDto, String projectIdentifier, String repositoryIdentifier, @Nullable String newCodeDefinitionType,
    @Nullable String newCodeDefinitionValue) {
    return new ImportProjectRequest(null, null, almSettingDto.getUuid(), repositoryIdentifier, projectIdentifier, newCodeDefinitionType, newCodeDefinitionValue, false, false);
  }

}
