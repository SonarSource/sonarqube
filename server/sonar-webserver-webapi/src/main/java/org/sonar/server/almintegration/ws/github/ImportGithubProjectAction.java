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
package org.sonar.server.almintegration.ws.github;

import org.sonar.alm.client.github.GithubApplicationClient;
import org.sonar.alm.client.github.GithubApplicationClient.Repository;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.alm.client.github.security.AccessToken;
import org.sonar.alm.client.github.security.UserAccessToken;
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
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.almintegration.ws.ImportHelper.toCreateResponse;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ImportGithubProjectAction implements AlmIntegrationsWsAction {

  public static final String PARAM_ORGANIZATION = "organization";
  public static final String PARAM_REPOSITORY_KEY = "repositoryKey";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final GithubApplicationClient githubApplicationClient;
  private final ComponentUpdater componentUpdater;
  private final ImportHelper importHelper;
  private final ProjectKeyGenerator projectKeyGenerator;

  public ImportGithubProjectAction(DbClient dbClient, UserSession userSession, ProjectDefaultVisibility projectDefaultVisibility,
    GithubApplicationClientImpl githubApplicationClient, ComponentUpdater componentUpdater, ImportHelper importHelper, ProjectKeyGenerator projectKeyGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.githubApplicationClient = githubApplicationClient;
    this.componentUpdater = componentUpdater;
    this.importHelper = importHelper;
    this.projectKeyGenerator = projectKeyGenerator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("import_github_project")
      .setDescription("Create a SonarQube project with the information from the provided GitHub repository.<br/>" +
        "Autoconfigure pull request decoration mechanism.<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setInternal(true)
      .setSince("8.4")
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");

    action.createParam(PARAM_ORGANIZATION)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("GitHub organization");

    action.createParam(PARAM_REPOSITORY_KEY)
      .setRequired(true)
      .setMaximumLength(256)
      .setDescription("GitHub repository key");
  }

  @Override
  public void handle(Request request, Response response) {
    Projects.CreateWsResponse createResponse = doHandle(request);
    writeProtobuf(createResponse, request, response);
  }

  private Projects.CreateWsResponse doHandle(Request request) {
    importHelper.checkProvisionProjectPermission();
    AlmSettingDto almSettingDto = importHelper.getAlmSetting(request);
    String userUuid = importHelper.getUserUuid();
    try (DbSession dbSession = dbClient.openSession(false)) {

      AccessToken accessToken = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto)
        .map(AlmPatDto::getPersonalAccessToken)
        .map(UserAccessToken::new)
        .orElseThrow(() -> new IllegalArgumentException("No personal access token found"));

      String githubOrganization = request.mandatoryParam(PARAM_ORGANIZATION);
      String repositoryKey = request.mandatoryParam(PARAM_REPOSITORY_KEY);

      String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
      Repository repository = githubApplicationClient.getRepository(url, accessToken, githubOrganization, repositoryKey)
        .orElseThrow(() -> new NotFoundException(String.format("GitHub repository '%s' not found", repositoryKey)));

      ComponentDto componentDto = createProject(dbSession, repository, repository.getDefaultBranch());
      populatePRSetting(dbSession, repository, componentDto, almSettingDto);
      componentUpdater.commitAndIndex(dbSession, componentDto);

      return toCreateResponse(componentDto);
    }
  }

  private ComponentDto createProject(DbSession dbSession, Repository repo, String mainBranchName) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    String uniqueProjectKey = projectKeyGenerator.generateUniqueProjectKey(repo.getFullName());
    return componentUpdater.createWithoutCommit(dbSession, newComponentBuilder()
        .setKey(uniqueProjectKey)
        .setName(repo.getName())
        .setPrivate(visibility)
        .setQualifier(PROJECT)
        .build(),
      userSession.getUuid(), userSession.getLogin(), mainBranchName, s -> {});
  }

  private void populatePRSetting(DbSession dbSession, Repository repo, ComponentDto componentDto, AlmSettingDto almSettingDto) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      .setAlmRepo(repo.getFullName())
      .setAlmSlug(null)
      .setProjectUuid(componentDto.uuid())
      .setSummaryCommentEnabled(true)
      .setMonorepo(false);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(),
      componentDto.name(), componentDto.getKey());
  }
}
