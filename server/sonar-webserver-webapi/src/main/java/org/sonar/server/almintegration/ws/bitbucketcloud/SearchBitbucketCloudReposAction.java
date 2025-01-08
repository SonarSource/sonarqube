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
package org.sonar.server.almintegration.ws.bitbucketcloud;

import java.util.List;
import java.util.Map;
import org.sonar.alm.client.bitbucket.bitbucketcloud.BitbucketCloudRestClient;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import org.sonar.alm.client.bitbucket.bitbucketcloud.Repository;
import org.sonar.alm.client.bitbucket.bitbucketcloud.RepositoryList;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmIntegrations.BBCRepo;
import org.sonarqube.ws.AlmIntegrations.SearchBitbucketcloudReposWsResponse;
import org.sonarqube.ws.Common.Paging;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchBitbucketCloudReposAction implements AlmIntegrationsWsAction {

  private static final BinaryOperator<String> resolveCollisionByNaturalOrder = (a, b) -> a.compareTo(b) < 0 ? a : b;
  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_REPO_NAME = "repositoryName";
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MAX_PAGE_SIZE = 100;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final BitbucketCloudRestClient bitbucketCloudRestClient;

  public SearchBitbucketCloudReposAction(DbClient dbClient, UserSession userSession,
    BitbucketCloudRestClient bitbucketCloudRestClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.bitbucketCloudRestClient = bitbucketCloudRestClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_bitbucketcloud_repos")
      .setDescription("Search the Bitbucket Cloud repositories<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(false)
      .setSince("9.0")
      .setResponseExample(getClass().getResource("example-search_bitbucketcloud_repos.json"))
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");

    action.createParam(PARAM_REPO_NAME)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Repository name filter");

    action.addPagingParams(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
  }

  @Override
  public void handle(Request request, Response response) {
    SearchBitbucketcloudReposWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private SearchBitbucketcloudReposWsResponse doHandle(Request request) {
    userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);
    String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
    String repoName = request.param(PARAM_REPO_NAME);
    int page = request.mandatoryParamAsInt(PAGE);
    int pageSize = request.mandatoryParamAsInt(PAGE_SIZE);

    try (DbSession dbSession = dbClient.openSession(false)) {
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform Setting '%s' not found", almSettingKey)));

      String workspace = ofNullable(almSettingDto.getAppId())
        .orElseThrow(() -> new IllegalArgumentException(String.format("workspace for alm setting %s is missing", almSettingDto.getKey())));
      String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null");
      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);

      String pat = almPatDto.map(AlmPatDto::getPersonalAccessToken).orElseThrow(() -> new IllegalArgumentException("No personal access token found"));

      RepositoryList repositoryList = bitbucketCloudRestClient.searchRepos(pat, workspace, repoName, page, pageSize);

      Map<String, String> sqProjectKeyByRepoSlug = getSqProjectKeyByRepoSlug(dbSession, almSettingDto, repositoryList.getValues());

      List<BBCRepo> bbcRepos = repositoryList.getValues().stream()
        .map(repository -> toBBCRepo(repository, workspace, sqProjectKeyByRepoSlug))
        .toList();

      SearchBitbucketcloudReposWsResponse.Builder builder = SearchBitbucketcloudReposWsResponse.newBuilder()
        .setIsLastPage(repositoryList.getNext() == null)
        .setPaging(Paging.newBuilder().setPageIndex(page).setPageSize(pageSize).build())
        .addAllRepositories(bbcRepos);
      return builder.build();
    }
  }

  private Map<String, String> getSqProjectKeyByRepoSlug(DbSession dbSession, AlmSettingDto almSettingDto, List<Repository> repositories) {
    Set<String> repoSlugs = repositories.stream().map(Repository::getSlug).collect(toSet());

    List<ProjectAlmSettingDto> projectAlmSettingDtos = dbClient.projectAlmSettingDao().selectByAlmSettingAndRepos(dbSession, almSettingDto, repoSlugs);

    Map<String, String> repoSlugByProjectUuid = projectAlmSettingDtos.stream()
      .collect(toMap(ProjectAlmSettingDto::getProjectUuid, ProjectAlmSettingDto::getAlmRepo));

    return dbClient.projectDao().selectByUuids(dbSession, repoSlugByProjectUuid.keySet())
      .stream()
      .collect(toMap(p -> repoSlugByProjectUuid.get(p.getUuid()), ProjectDto::getKey, resolveCollisionByNaturalOrder));
  }

  private static BBCRepo toBBCRepo(Repository gsonBBCRepo, String workspace, Map<String, String> sqProjectKeyByRepoSlug) {
    BBCRepo.Builder builder = BBCRepo.newBuilder()
      .setSlug(gsonBBCRepo.getSlug())
      .setUuid(gsonBBCRepo.getUuid())
      .setName(gsonBBCRepo.getName())
      .setWorkspace(workspace)
      .setProjectKey(gsonBBCRepo.getProject().getKey());

    String sqProjectKey = sqProjectKeyByRepoSlug.get(gsonBBCRepo.getSlug());
    ofNullable(sqProjectKey).ifPresent(builder::setSqProjectKey);

    return builder.build();
  }

}
