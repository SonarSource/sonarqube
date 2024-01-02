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
package org.sonar.server.almintegration.ws.bitbucketcloud;

import javax.annotation.Nullable;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Repository;
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
import org.sonar.server.component.NewComponent;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects;

import static java.util.Optional.ofNullable;
import static org.sonar.api.resources.Qualifiers.PROJECT;
import static org.sonar.server.almintegration.ws.ImportHelper.PARAM_ALM_SETTING;
import static org.sonar.server.almintegration.ws.ImportHelper.toCreateResponse;
import static org.sonar.server.component.NewComponent.newComponentBuilder;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ImportBitbucketCloudRepoAction implements AlmIntegrationsWsAction {

  private static final String PARAM_REPO_SLUG = "repositorySlug";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final BitbucketCloudRestClient bitbucketCloudRestClient;
  private final ProjectDefaultVisibility projectDefaultVisibility;
  private final ComponentUpdater componentUpdater;
  private final ImportHelper importHelper;
  private final ProjectKeyGenerator projectKeyGenerator;

  public ImportBitbucketCloudRepoAction(DbClient dbClient, UserSession userSession, BitbucketCloudRestClient bitbucketCloudRestClient,
    ProjectDefaultVisibility projectDefaultVisibility, ComponentUpdater componentUpdater, ImportHelper importHelper,
    ProjectKeyGenerator projectKeyGenerator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
    this.projectDefaultVisibility = projectDefaultVisibility;
    this.componentUpdater = componentUpdater;
    this.importHelper = importHelper;
    this.projectKeyGenerator = projectKeyGenerator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("import_bitbucketcloud_repo")
      .setDescription("Create a SonarQube project with the information from the provided Bitbucket Cloud repository.<br/>" +
        "Autoconfigure pull request decoration mechanism.<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(true)
      .setInternal(true)
      .setSince("9.0")
      .setHandler(this);

    action.createParam(PARAM_REPO_SLUG)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Bitbucket Cloud repository slug");

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");

  }

  @Override
  public void handle(Request request, Response response) {
    Projects.CreateWsResponse createResponse = doHandle(request);
    writeProtobuf(createResponse, request, response);
  }

  private Projects.CreateWsResponse doHandle(Request request) {
    importHelper.checkProvisionProjectPermission();

    String userUuid = importHelper.getUserUuid();
    String repoSlug = request.mandatoryParam(PARAM_REPO_SLUG);
    AlmSettingDto almSettingDto = importHelper.getAlmSetting(request);
    String workspace = ofNullable(almSettingDto.getAppId())
      .orElseThrow(() -> new IllegalArgumentException(String.format("workspace for alm setting %s is missing", almSettingDto.getKey())));

    try (DbSession dbSession = dbClient.openSession(false)) {
      String pat = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto)
        .map(AlmPatDto::getPersonalAccessToken)
        .orElseThrow(() -> new IllegalArgumentException(String.format("Username and App Password for '%s' is missing", almSettingDto.getKey())));

      Repository repo = bitbucketCloudRestClient.getRepo(pat, workspace, repoSlug);

      ComponentDto componentDto = createProject(dbSession, workspace, repo, repo.getMainBranch().getName());

      populatePRSetting(dbSession, repo, componentDto, almSettingDto);

      componentUpdater.commitAndIndex(dbSession, componentDto);

      return toCreateResponse(componentDto);
    }
  }

  private ComponentDto createProject(DbSession dbSession, String workspace, Repository repo, @Nullable String defaultBranchName) {
    boolean visibility = projectDefaultVisibility.get(dbSession).isPrivate();
    String uniqueProjectKey = projectKeyGenerator.generateUniqueProjectKey(workspace, repo.getSlug());
    NewComponent newProject = newComponentBuilder()
      .setKey(uniqueProjectKey)
      .setName(repo.getName())
      .setPrivate(visibility)
      .setQualifier(PROJECT)
      .build();
    String userUuid = userSession.isLoggedIn() ? userSession.getUuid() : null;
    String userLogin = userSession.isLoggedIn() ? userSession.getLogin() : null;

    return componentUpdater.createWithoutCommit(dbSession, newProject, userUuid, userLogin, defaultBranchName, p -> {
    });
  }

  private void populatePRSetting(DbSession dbSession, Repository repo, ComponentDto componentDto, AlmSettingDto almSettingDto) {
    ProjectAlmSettingDto projectAlmSettingDto = new ProjectAlmSettingDto()
      .setAlmSettingUuid(almSettingDto.getUuid())
      // Bitbucket Cloud PR decoration reads almRepo
      .setAlmRepo(repo.getSlug())
      .setProjectUuid(componentDto.uuid())
      .setMonorepo(false);
    dbClient.projectAlmSettingDao().insertOrUpdate(dbSession, projectAlmSettingDto, almSettingDto.getKey(),
      componentDto.name(), componentDto.getKey());
  }

}
