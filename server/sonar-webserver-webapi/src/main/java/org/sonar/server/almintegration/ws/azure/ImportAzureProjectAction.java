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
package org.sonar.server.almintegration.ws.azure;

import java.util.Optional;
import javax.inject.Inject;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.common.almintegration.ProjectKeyGenerator;
import org.sonar.server.common.component.ComponentCreationParameters;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.component.NewComponent;
import org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.CreateWsResponse;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.project.CreationMethod.getCreationMethod;
import static org.sonar.db.project.CreationMethod.Category.ALM_IMPORT;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.almintegration.ws.ImportHelper.toCreateResponse;
import static org.sonar.server.common.component.NewComponent.newComponentBuilder;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.checkNewCodeDefinitionParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportAzureProjectAction implements AlmIntegrationsWsAction {

  private static final String PARAM_REPOSITORY_NAME = "repositoryName";
  private static final String PARAM_PROJECT_NAME = "projectName";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AzureDevOpsHttpClient azureDevOpsHttpClient;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final ComponentUpdater componentUpdater;
  private final ImportHelper importHelper;
  private final ProjectKeyGenerator projectKeyGenerator;
  private final NewCodeDefinitionResolver newCodeDefinitionResolver;
  private final DefaultBranchNameResolver defaultBranchNameResolver;

  @Inject
  public ImportAzureProjectAction(DbClient dbClient, UserSession userSession, AzureDevOpsHttpClient azureDevOpsHttpClient,
    ProjectDefaultVisibility projectDefaultVisibility, ComponentUpdater componentUpdater,
    ImportHelper importHelper, ProjectKeyGenerator projectKeyGenerator, NewCodeDefinitionResolver newCodeDefinitionResolver,
    DefaultBranchNameResolver defaultBranchNameResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.azureDevOpsHttpClient = azureDevOpsHttpClient;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.componentUpdater = componentUpdater;
    this.importHelper = importHelper;
    this.projectKeyGenerator = projectKeyGenerator;
    this.newCodeDefinitionResolver = newCodeDefinitionResolver;
    this.defaultBranchNameResolver = defaultBranchNameResolver;
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

    try (DbSession dbSession = dbClient.openSession(false)) {

      String pat = getPat(dbSession, almSettingDto);

      String projectName = request.mandatoryParam(PARAM_PROJECT_NAME);
      String repositoryName = request.mandatoryParam(PARAM_REPOSITORY_NAME);

      String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
      GsonAzureRepo repo = azureDevOpsHttpClient.getRepo(url, pat, projectName, repositoryName);

      ComponentCreationData componentCreationData = createProject(dbSession, repo);
      ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();
      BranchDto mainBranchDto = Optional.ofNullable(componentCreationData.mainBranchDto()).orElseThrow();
      populatePRSetting(dbSession, repo, projectDto, almSettingDto);

      checkNewCodeDefinitionParam(newCodeDefinitionType, newCodeDefinitionValue);

      if (newCodeDefinitionType != null) {
        newCodeDefinitionResolver.createNewCodeDefinition(dbSession, projectDto.getUuid(), mainBranchDto.getUuid(),
          Optional.ofNullable(repo.getDefaultBranchName()).orElse(defaultBranchNameResolver.getEffectiveMainBranchName()),
          newCodeDefinitionType, newCodeDefinitionValue);
      }

      componentUpdater.commitAndIndex(dbSession, componentCreationData);

      return toCreateResponse(projectDto);
    }
  }

  private String getPat(DbSession dbSession, AlmSettingDto almSettingDto) {
    String userUuid = importHelper.getUserUuid();
    Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
    return almPatDto.map(AlmPatDto::getPersonalAccessToken)
      .orElseThrow(() -> new IllegalArgumentException(String.format("personal access token for '%s' is missing", almSettingDto.getKey())));
  }

  private ComponentCreationData createProject(DbSession dbSession, GsonAzureRepo repo) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    String uniqueProjectKey = projectKeyGenerator.generateUniqueProjectKey(repo.getProject().getName(), repo.getName());
    NewComponent newProject = newComponentBuilder()
      .setKey(uniqueProjectKey)
      .setName(repo.getName())
      .setPrivate(visibility)
      .setQualifier(PROJECT)
      .build();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(newProject)
      .userUuid(userSession.getUuid())
      .userLogin(userSession.getLogin())
      .mainBranchName(repo.getDefaultBranchName())
      .creationMethod(getCreationMethod(ALM_IMPORT, userSession.isAuthenticatedBrowserSession()))
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

  private void populatePRSetting(DbSession dbSession, GsonAzureRepo repo, ProjectDto projectDto, AlmSettingDto almSettingDto) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repo.getName())
      .setAlmSlug(repo.getProject().getName())
      .setProjectUuid(projectDto.getUuid())
      .setMonorepo(false);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(),
      projectDto.getName(), projectDto.getKey());
  }

}
