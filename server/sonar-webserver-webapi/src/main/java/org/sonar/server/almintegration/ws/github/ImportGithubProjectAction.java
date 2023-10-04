/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;
import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubApplicationClient.Repository;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.alm.client.github.security.UserAccessToken;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.auth.github.GitHubSettings;
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
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.component.ComponentCreationParameters;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.management.ManagedProjectService;
import org.sonar.server.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.project.CreationMethod.Category.ALM_IMPORT;
import static org.sonar.db.project.CreationMethod.getCreationMethod;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.almintegration.ws.ImportHelper.toCreateResponse;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.newcodeperiod.NewCodeDefinitionResolver.checkNewCodeDefinitionParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportGithubProjectAction implements AlmIntegrationsWsAction {

  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_REPOSITORY_KEY = "repositoryKey";

  private final DbClient dbClient;

  private final ManagedProjectService managedProjectService;
  private final UserSession userSession;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final GithubApplicationClient githubApplicationClient;
  private final ComponentUpdater componentUpdater;
  private final ImportHelper importHelper;
  private final ProjectKeyGenerator projectKeyGenerator;

  private final NewCodeDefinitionResolver newCodeDefinitionResolver;

  private final DefaultBranchNameResolver defaultBranchNameResolver;

  private final GitHubSettings gitHubSettings;

  @Inject
  public ImportGithubProjectAction(DbClient dbClient, ManagedProjectService managedProjectService, UserSession userSession, ProjectDefaultVisibility projectDefaultVisibility,
    GithubApplicationClientImpl githubApplicationClient, ComponentUpdater componentUpdater, ImportHelper importHelper,
    ProjectKeyGenerator projectKeyGenerator, NewCodeDefinitionResolver newCodeDefinitionResolver,
    DefaultBranchNameResolver defaultBranchNameResolver, GitHubSettings gitHubSettings) {
    this.dbClient = dbClient;
    this.managedProjectService = managedProjectService;
    this.userSession = userSession;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.githubApplicationClient = githubApplicationClient;
    this.componentUpdater = componentUpdater;
    this.importHelper = importHelper;
    this.projectKeyGenerator = projectKeyGenerator;
    this.newCodeDefinitionResolver = newCodeDefinitionResolver;
    this.defaultBranchNameResolver = defaultBranchNameResolver;
    this.gitHubSettings = gitHubSettings;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("import_github_project")
      .setDescription("Create a SonarQube project with the information from the provided GitHub repository.<br/>" +
        "Autoconfigure pull request decoration mechanism. If Automatic Provisioning is enable for GitHub, it will also synchronize permissions from the repository.<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setSince("8.4")
      .setHandler(this)
      .setChangelog(
        new Change("10.3", String.format("Parameter %s becomes optional if you have only one configuration for GitHub", PARAM_ALM_SETTING)),
        new Change("10.3", "Endpoint visibility change from internal to public"));

    action.createParam(PARAM_ALM_SETTING)
      .setMaximumLength(200)
      .setDescription("DevOps Platform configuration key. This parameter is optional if you have only one GitHub integration.");

    action.createParam(PARAM_ORGANIZATION)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("GitHub organization");

    action.createParam(PARAM_REPOSITORY_KEY)
      .setRequired(true)
      .setMaximumLength(256)
      .setDescription("GitHub repository key");

    action.createParam(PARAM_NEW_CODE_DEFINITION_TYPE)
      .setDescription(NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION)
      .setSince("10.1");

    action.createParam(PARAM_NEW_CODE_DEFINITION_VALUE)
      .setDescription(NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION)
      .setSince("10.1");
  }

  @Override
  public void handle(Request request, Response response) {
    Projects.CreateWsResponse createResponse = doHandle(request);
    writeProtobuf(createResponse, request, response);
  }

  private Projects.CreateWsResponse doHandle(Request request) {
    importHelper.checkProvisionProjectPermission();
    AlmSettingDto almSettingDto = importHelper.getAlmSettingDtoForAlm(request, ALM.GITHUB);

    String newCodeDefinitionType = request.param(PARAM_NEW_CODE_DEFINITION_TYPE);
    String newCodeDefinitionValue = request.param(PARAM_NEW_CODE_DEFINITION_VALUE);
    try (DbSession dbSession = dbClient.openSession(false)) {

      AccessToken accessToken = getAccessToken(dbSession, almSettingDto);

      String githubOrganization = request.mandatoryParam(PARAM_ORGANIZATION);
      String repositoryKey = request.mandatoryParam(PARAM_REPOSITORY_KEY);

      String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
      Repository repository = githubApplicationClient.getRepository(url, accessToken, githubOrganization, repositoryKey)
        .orElseThrow(() -> new NotFoundException(String.format("GitHub repository '%s' not found", repositoryKey)));

      ComponentCreationData componentCreationData = createProject(dbSession, repository, repository.getDefaultBranch());
      ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();
      BranchDto mainBranchDto = Optional.ofNullable(componentCreationData.mainBranchDto()).orElseThrow();

      populatePRSetting(dbSession, repository, projectDto, almSettingDto);

      checkNewCodeDefinitionParam(newCodeDefinitionType, newCodeDefinitionValue);

      if (newCodeDefinitionType != null) {
        newCodeDefinitionResolver.createNewCodeDefinition(dbSession, projectDto.getUuid(), mainBranchDto.getUuid(),
          Optional.ofNullable(repository.getDefaultBranch()).orElse(defaultBranchNameResolver.getEffectiveMainBranchName()),
          newCodeDefinitionType, newCodeDefinitionValue);
      }

      componentUpdater.commitAndIndex(dbSession, componentCreationData);

      String userUuid = Objects.requireNonNull(userSession.getUuid());
      managedProjectService.queuePermissionSyncTask(userUuid, mainBranchDto.getUuid(), projectDto.getUuid());

      return toCreateResponse(projectDto);
    }
  }

  private AccessToken getAccessToken(DbSession dbSession, AlmSettingDto almSettingDto) {
    String userUuid = importHelper.getUserUuid();
    return dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto)
      .map(AlmPatDto::getPersonalAccessToken)
      .map(UserAccessToken::new)
      .orElseThrow(() -> new IllegalArgumentException("No personal access token found"));
  }

  private ComponentCreationData createProject(DbSession dbSession, Repository repo, String mainBranchName) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    String uniqueProjectKey = projectKeyGenerator.generateUniqueProjectKey(repo.getFullName());
    NewComponent projectComponent = newComponentBuilder()
      .setKey(uniqueProjectKey)
      .setName(repo.getName())
      .setPrivate(visibility)
      .setQualifier(PROJECT)
      .build();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(projectComponent)
      .userLogin(userSession.getLogin())
      .userUuid(userSession.getUuid())
      .mainBranchName(mainBranchName)
      .isManaged(gitHubSettings.isProvisioningEnabled())
      .creationMethod(getCreationMethod(ALM_IMPORT, userSession.isAuthenticatedBrowserSession()))
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

  private void populatePRSetting(DbSession dbSession, Repository repo, ProjectDto projectDto, AlmSettingDto almSettingDto) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repo.getFullName())
      .setAlmSlug(null)
      .setProjectUuid(projectDto.getUuid())
      .setSummaryCommentEnabled(true)
      .setMonorepo(false);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(),
      projectDto.getName(), projectDto.getKey());
  }
}
