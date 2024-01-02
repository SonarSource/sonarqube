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
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.almintegration.ws.ImportHelper;
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.CreateWsResponse;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.almintegration.ws.ImportHelper.toCreateResponse;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

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

  public ImportAzureProjectAction(DbClient dbClient, UserSession userSession, AzureDevOpsHttpClient azureDevOpsHttpClient,
    ProjectDefaultVisibility projectDefaultVisibility, ComponentUpdater componentUpdater,
    ImportHelper importHelper, ProjectKeyGenerator projectKeyGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.azureDevOpsHttpClient = azureDevOpsHttpClient;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.componentUpdater = componentUpdater;
    this.importHelper = importHelper;
    this.projectKeyGenerator = projectKeyGenerator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("import_azure_project")
      .setDescription("Create a SonarQube project with the information from the provided Azure DevOps project.<br/>" +
        "Autoconfigure pull request decoration mechanism.<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setInternal(true)
      .setSince("8.6")
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");

    action.createParam(PARAM_PROJECT_NAME)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Azure project name");

    action.createParam(PARAM_REPOSITORY_NAME)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Azure repository name");
  }

  @Override
  public void handle(Request request, Response response) {
    CreateWsResponse createResponse = doHandle(request);
    writeProtobuf(createResponse, request, response);
  }

  private CreateWsResponse doHandle(Request request) {
    importHelper.checkProvisionProjectPermission();
    AlmSettingDto almSettingDto = importHelper.getAlmSetting(request);
    String userUuid = importHelper.getUserUuid();
    try (DbSession dbSession = dbClient.openSession(false)) {

      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
      String pat = almPatDto.map(AlmPatDto::getPersonalAccessToken)
        .orElseThrow(() -> new IllegalArgumentException(String.format("personal access token for '%s' is missing", almSettingDto.getKey())));

      String projectName = request.mandatoryParam(PARAM_PROJECT_NAME);
      String repositoryName = request.mandatoryParam(PARAM_REPOSITORY_NAME);

      String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
      GsonAzureRepo repo = azureDevOpsHttpClient.getRepo(url, pat, projectName, repositoryName);

      ComponentDto componentDto = createProject(dbSession, repo);
      populatePRSetting(dbSession, repo, componentDto, almSettingDto);
      componentUpdater.commitAndIndex(dbSession, componentDto);

      return toCreateResponse(componentDto);
    }
  }

  private ComponentDto createProject(DbSession dbSession, GsonAzureRepo repo) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    String uniqueProjectKey = projectKeyGenerator.generateUniqueProjectKey(repo.getProject().getName(), repo.getName());
    return componentUpdater.createWithoutCommit(dbSession, newComponentBuilder()
        .setKey(uniqueProjectKey)
        .setName(repo.getName())
        .setPrivate(visibility)
        .setQualifier(PROJECT)
        .build(),
      userSession.isLoggedIn() ? userSession.getUuid() : null,
      userSession.isLoggedIn() ? userSession.getLogin() : null,
      repo.getDefaultBranchName(),
      s -> {
      });
  }

  private void populatePRSetting(DbSession dbSession, GsonAzureRepo repo, ComponentDto componentDto, AlmSettingDto almSettingDto) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repo.getName())
      .setAlmSlug(repo.getProject().getName())
      .setProjectUuid(componentDto.uuid())
      .setMonorepo(false);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(),
      componentDto.name(), componentDto.getKey());
  }

}
