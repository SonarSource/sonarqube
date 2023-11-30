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
package org.sonar.server.almintegration.ws.gitlab;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.sonar.alm.client.gitlab.GitLabBranch;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.Project;
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
import org.sonar.server.almintegration.ws.ProjectKeyGenerator;
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.component.ComponentCreationParameters;
import org.sonar.server.component.ComponentUpdater;
import org.sonar.server.component.NewComponent;
import org.sonar.server.newcodeperiod.NewCodeDefinitionResolver;
import org.sonar.server.project.DefaultBranchNameResolver;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.CreateWsResponse;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.db.project.CreationMethod.Category.ALM_IMPORT;
import static org.sonar.db.project.CreationMethod.getCreationMethod;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.newcodeperiod.NewCodeDefinitionResolver.checkNewCodeDefinitionParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportGitLabProjectAction implements AlmIntegrationsWsAction {

  public static final String PARAM_GITLAB_PROJECT_ID = "gitlabProjectId";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final GitlabApplicationClient gitlabApplicationClient;
  private final ComponentUpdater componentUpdater;
  private final ImportHelper importHelper;
  private final ProjectKeyGenerator projectKeyGenerator;
  private final NewCodeDefinitionResolver newCodeDefinitionResolver;
  private final DefaultBranchNameResolver defaultBranchNameResolver;

  @Inject
  public ImportGitLabProjectAction(DbClient dbClient, UserSession userSession,
    ProjectDefaultVisibility projectDefaultVisibility, GitlabApplicationClient gitlabApplicationClient,
    ComponentUpdater componentUpdater, ImportHelper importHelper, ProjectKeyGenerator projectKeyGenerator, NewCodeDefinitionResolver newCodeDefinitionResolver,
    DefaultBranchNameResolver defaultBranchNameResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.gitlabApplicationClient = gitlabApplicationClient;
    this.componentUpdater = componentUpdater;
    this.importHelper = importHelper;
    this.projectKeyGenerator = projectKeyGenerator;
    this.newCodeDefinitionResolver = newCodeDefinitionResolver;
    this.defaultBranchNameResolver = defaultBranchNameResolver;
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
        new Change("10.3", String.format("Parameter %s becomes optional if you have only one configuration for GitLab", PARAM_ALM_SETTING)));

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

    String newCodeDefinitionType = request.param(PARAM_NEW_CODE_DEFINITION_TYPE);
    String newCodeDefinitionValue = request.param(PARAM_NEW_CODE_DEFINITION_VALUE);

    try (DbSession dbSession = dbClient.openSession(false)) {
      AlmSettingDto almSettingDto = importHelper.getAlmSettingDtoForAlm(request, ALM.GITLAB);
      String pat = getPat(dbSession, almSettingDto);

      long gitlabProjectId = request.mandatoryParamAsLong(PARAM_GITLAB_PROJECT_ID);

      String gitlabUrl = requireNonNull(almSettingDto.getUrl(), "DevOps Platform gitlabUrl cannot be null");
      Project gitlabProject = gitlabApplicationClient.getProject(gitlabUrl, pat, gitlabProjectId);

      Optional<String> almMainBranchName = getAlmDefaultBranch(pat, gitlabProjectId, gitlabUrl);

      ComponentCreationData componentCreationData = createProject(dbSession, gitlabProject, almMainBranchName.orElse(null));
      ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();
      BranchDto mainBranchDto = Optional.ofNullable(componentCreationData.mainBranchDto()).orElseThrow();
      populateMRSetting(dbSession, gitlabProjectId, projectDto, almSettingDto);

      checkNewCodeDefinitionParam(newCodeDefinitionType, newCodeDefinitionValue);

      if (newCodeDefinitionType != null) {
        newCodeDefinitionResolver.createNewCodeDefinition(dbSession, projectDto.getUuid(), mainBranchDto.getUuid(),
          almMainBranchName.orElse(defaultBranchNameResolver.getEffectiveMainBranchName()), newCodeDefinitionType, newCodeDefinitionValue);
      }

      componentUpdater.commitAndIndex(dbSession, componentCreationData);

      return ImportHelper.toCreateResponse(projectDto);
    }
  }

  private String getPat(DbSession dbSession, AlmSettingDto almSettingDto) {
    String userUuid = importHelper.getUserUuid();
    Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
    return almPatDto.map(AlmPatDto::getPersonalAccessToken)
      .orElseThrow(() -> new IllegalArgumentException(String.format("personal access token for '%s' is missing", almSettingDto.getKey())));
  }

  private Optional<String> getAlmDefaultBranch(String pat, long gitlabProjectId, String gitlabUrl) {
    Optional<GitLabBranch> almMainBranch = gitlabApplicationClient.getBranches(gitlabUrl, pat, gitlabProjectId).stream().filter(GitLabBranch::isDefault).findFirst();
    return almMainBranch.map(GitLabBranch::getName);
  }

  private void populateMRSetting(DbSession dbSession, Long gitlabProjectId, ProjectDto projectDto, AlmSettingDto almSettingDto) {
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, new ProjectAlmSettingDto()
        .setProjectUuid(projectDto.getUuid())
        .setAlmSettingUuid(almSettingDto.getUuid())
        .setAlmRepo(gitlabProjectId.toString())
        .setAlmSlug(null)
        .setMonorepo(false),
      almSettingDto.getKey(),
      projectDto.getName(), projectDto.getKey());
  }

  private ComponentCreationData createProject(DbSession dbSession, Project gitlabProject, @Nullable String mainBranchName) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    String uniqueProjectKey = projectKeyGenerator.generateUniqueProjectKey(gitlabProject.getPathWithNamespace());
    NewComponent newProject = newComponentBuilder()
      .setKey(uniqueProjectKey)
      .setName(gitlabProject.getName())
      .setPrivate(visibility)
      .setQualifier(PROJECT)
      .build();
    ComponentCreationParameters componentCreationParameters = ComponentCreationParameters.builder()
      .newComponent(newProject)
      .userUuid(userSession.getUuid())
      .userLogin(userSession.getLogin())
      .mainBranchName(mainBranchName)
      .creationMethod(getCreationMethod(ALM_IMPORT, userSession.isAuthenticatedBrowserSession()))
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

}
