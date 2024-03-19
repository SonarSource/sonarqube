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
package org.sonar.server.almintegration.ws.bitbucketserver;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Branch;
import org.sonar.alm.client.bitbucketserver.BranchesList;
import org.sonar.alm.client.bitbucketserver.Repository;
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
import org.sonar.server.component.ComponentCreationData;
import org.sonar.server.common.component.ComponentCreationParameters;
import org.sonar.server.common.component.ComponentUpdater;
import org.sonar.server.common.component.NewComponent;
import org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver;
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
import static org.sonar.server.common.component.NewComponent.newComponentBuilder;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_TYPE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.NEW_CODE_PERIOD_VALUE_DESCRIPTION_PROJECT_CREATION;
import static org.sonar.server.common.newcodeperiod.NewCodeDefinitionResolver.checkNewCodeDefinitionParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_TYPE;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.PARAM_NEW_CODE_DEFINITION_VALUE;

public class ImportBitbucketServerProjectAction implements AlmIntegrationsWsAction {

  private static final String PARAM_PROJECT_KEY = "projectKey";
  private static final String PARAM_REPO_SLUG = "repositorySlug";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final BitbucketServerRestClient bitbucketServerRestClient;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final ComponentUpdater componentUpdater;
  private final ImportHelper importHelper;
  private final ProjectKeyGenerator projectKeyGenerator;

  private final NewCodeDefinitionResolver newCodeDefinitionResolver;

  private final DefaultBranchNameResolver defaultBranchNameResolver;

  @Inject
  public ImportBitbucketServerProjectAction(DbClient dbClient, UserSession userSession, BitbucketServerRestClient bitbucketServerRestClient,
    ProjectDefaultVisibility projectDefaultVisibility, ComponentUpdater componentUpdater,
    ImportHelper importHelper, ProjectKeyGenerator projectKeyGenerator, NewCodeDefinitionResolver newCodeDefinitionResolver,
    DefaultBranchNameResolver defaultBranchNameResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.bitbucketServerRestClient = bitbucketServerRestClient;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.componentUpdater = componentUpdater;
    this.importHelper = importHelper;
    this.projectKeyGenerator = projectKeyGenerator;
    this.newCodeDefinitionResolver = newCodeDefinitionResolver;
    this.defaultBranchNameResolver = defaultBranchNameResolver;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("import_bitbucketserver_project")
      .setDescription("Create a SonarQube project with the information from the provided BitbucketServer project.<br/>" +
                      "Autoconfigure pull request decoration mechanism.<br/>" +
                      "Requires the 'Create Projects' permission")
      .setPost(true)
      .setSince("8.2")
      .setHandler(this)
      .setChangelog(
        new Change("10.3", String.format("Parameter %s becomes optional if you have only one configuration for BitBucket Server", PARAM_ALM_SETTING)),
        new Change("10.3", "Endpoint visibility change from internal to public"));

    action.createParam(PARAM_ALM_SETTING)
      .setMaximumLength(200)
      .setDescription("DevOps Platform configuration key. This parameter is optional if you have only one BitBucket Server integration.");

    action.createParam(PARAM_PROJECT_KEY)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("BitbucketServer project key");

    action.createParam(PARAM_REPO_SLUG)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("BitbucketServer repository slug");


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
    AlmSettingDto almSettingDto = importHelper.getAlmSettingDtoForAlm(request, ALM.BITBUCKET);

    String newCodeDefinitionType = request.param(PARAM_NEW_CODE_DEFINITION_TYPE);
    String newCodeDefinitionValue = request.param(PARAM_NEW_CODE_DEFINITION_VALUE);
    try (DbSession dbSession = dbClient.openSession(false)) {

      String pat = getPat(dbSession, almSettingDto);

      String projectKey = request.mandatoryParam(PARAM_PROJECT_KEY);
      String repoSlug = request.mandatoryParam(PARAM_REPO_SLUG);

      String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
      Repository repo = bitbucketServerRestClient.getRepo(url, pat, projectKey, repoSlug);

      String defaultBranchName = getDefaultBranchName(pat, projectKey, repoSlug, url);

      ComponentCreationData componentCreationData = createProject(dbSession, repo, defaultBranchName);
      ProjectDto projectDto = Optional.ofNullable(componentCreationData.projectDto()).orElseThrow();
      BranchDto mainBranchDto = Optional.ofNullable(componentCreationData.mainBranchDto()).orElseThrow();

      populatePRSetting(dbSession, repo, projectDto, almSettingDto);

      checkNewCodeDefinitionParam(newCodeDefinitionType, newCodeDefinitionValue);

      if (newCodeDefinitionType != null) {
        newCodeDefinitionResolver.createNewCodeDefinition(dbSession, projectDto.getUuid(), mainBranchDto.getUuid(),
          Optional.ofNullable(defaultBranchName).orElse(defaultBranchNameResolver.getEffectiveMainBranchName()),
          newCodeDefinitionType, newCodeDefinitionValue);
      }

      componentUpdater.commitAndIndex(dbSession, componentCreationData);

      return toCreateResponse(projectDto);
    }
  }

  private String getPat(DbSession dbSession, AlmSettingDto almSettingDto) {
    String userUuid = importHelper.getUserUuid();

    Optional<AlmPatDto> almPatDot = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);
    return almPatDot.map(AlmPatDto::getPersonalAccessToken)
      .orElseThrow(() -> new IllegalArgumentException(String.format("personal access token for '%s' is missing",
        almSettingDto.getKey())));
  }

  private String getDefaultBranchName(String pat, String projectKey, String repoSlug, String url) {
    BranchesList branches = bitbucketServerRestClient.getBranches(url, pat, projectKey, repoSlug);
    Optional<Branch> defaultBranch = branches.findDefaultBranch();
    return defaultBranch.map(Branch::getName).orElse(null);
  }

  private ComponentCreationData createProject(DbSession dbSession, Repository repo, @Nullable String defaultBranchName) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    String uniqueProjectKey = projectKeyGenerator.generateUniqueProjectKey(repo.getProject().getKey(), repo.getSlug());
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
      .mainBranchName(defaultBranchName)
      .creationMethod(getCreationMethod(ALM_IMPORT, userSession.isAuthenticatedBrowserSession()))
      .build();
    return componentUpdater.createWithoutCommit(dbSession, componentCreationParameters);
  }

  private void populatePRSetting(DbSession dbSession, Repository repo, ProjectDto componentDto, AlmSettingDto almSettingDto) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repo.getProject().getKey())
      .setAlmSlug(repo.getSlug())
      .setProjectUuid(componentDto.getUuid())
      .setMonorepo(false);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(),
      componentDto.getName(), componentDto.getKey());
  }

}
