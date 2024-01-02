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

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.alm.client.gitlab.GitLabBranch;
import org.sonar.alm.client.gitlab.GitlabHttpClient;
import org.sonar.alm.client.gitlab.Project;
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
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ImportGitLabProjectAction implements AlmIntegrationsWsAction {

  public static final String PARAM_GITLAB_PROJECT_ID = "gitlabProjectId";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final GitlabHttpClient gitlabHttpClient;
  private final ComponentUpdater componentUpdater;
  private final ImportHelper importHelper;
  private final ProjectKeyGenerator projectKeyGenerator;

  public ImportGitLabProjectAction(DbClient dbClient, UserSession userSession,
    ProjectDefaultVisibility projectDefaultVisibility, GitlabHttpClient gitlabHttpClient,
    ComponentUpdater componentUpdater, ImportHelper importHelper, ProjectKeyGenerator projectKeyGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.gitlabHttpClient = gitlabHttpClient;
    this.componentUpdater = componentUpdater;
    this.importHelper = importHelper;
    this.projectKeyGenerator = projectKeyGenerator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("import_gitlab_project")
      .setDescription("Import a GitLab project to SonarQube, creating a new project and configuring MR decoration<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setSince("8.5")
      .setHandler(this);

    action.createParam(ImportHelper.PARAM_ALM_SETTING)
      .setRequired(true)
      .setDescription("DevOps Platform setting key");
    action.createParam(PARAM_GITLAB_PROJECT_ID)
      .setRequired(true)
      .setDescription("GitLab project ID");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
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

      long gitlabProjectId = request.mandatoryParamAsLong(PARAM_GITLAB_PROJECT_ID);

      String gitlabUrl = requireNonNull(almSettingDto.getUrl(), "DevOps Platform gitlabUrl cannot be null");
      Project gitlabProject = gitlabHttpClient.getProject(gitlabUrl, pat, gitlabProjectId);

      Optional<String> almMainBranchName = getAlmDefaultBranch(pat, gitlabProjectId, gitlabUrl);
      ComponentDto componentDto = createProject(dbSession, gitlabProject, almMainBranchName.orElse(null));
      populateMRSetting(dbSession, gitlabProjectId, componentDto, almSettingDto);
      componentUpdater.commitAndIndex(dbSession, componentDto);

      return ImportHelper.toCreateResponse(componentDto);
    }
  }

  private Optional<String> getAlmDefaultBranch(String pat, long gitlabProjectId, String gitlabUrl) {
    Optional<GitLabBranch> almMainBranch = gitlabHttpClient.getBranches(gitlabUrl, pat, gitlabProjectId).stream().filter(GitLabBranch::isDefault).findFirst();
    return almMainBranch.map(GitLabBranch::getName);
  }

  private void populateMRSetting(DbSession dbSession, Long gitlabProjectId, ComponentDto componentDto, AlmSettingDto almSettingDto) {
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, new ProjectAlmSettingDto()
        .setProjectUuid(componentDto.branchUuid())
        .setAlmSettingUuid(almSettingDto.getUuid())
        .setAlmRepo(gitlabProjectId.toString())
        .setAlmSlug(null)
        .setMonorepo(false),
      almSettingDto.getKey(),
      componentDto.name(), componentDto.getKey());
  }

  private ComponentDto createProject(DbSession dbSession, Project gitlabProject, @Nullable String mainBranchName) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    String uniqueProjectKey = projectKeyGenerator.generateUniqueProjectKey(gitlabProject.getPathWithNamespace());

    return componentUpdater.createWithoutCommit(dbSession, newComponentBuilder()
        .setKey(uniqueProjectKey)
        .setName(gitlabProject.getName())
        .setPrivate(visibility)
        .setQualifier(PROJECT)
        .build(),
      userSession.getUuid(), userSession.getLogin(), mainBranchName, s -> {
      });
  }

}
