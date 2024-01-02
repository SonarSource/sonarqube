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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import org.sonar.alm.client.bitbucketserver.BitbucketServerRestClient;
import org.sonar.alm.client.bitbucketserver.Repository;
import org.sonar.alm.client.bitbucketserver.RepositoryList;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmIntegrations.BBSRepo;
import org.sonarqube.ws.AlmIntegrations.SearchBitbucketserverReposWsResponse;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchBitbucketServerReposAction implements AlmIntegrationsWsAction {
  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_REPO_NAME = "repositoryName";
  private static final String PARAM_PROJECT_NAME = "projectName";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final BitbucketServerRestClient bitbucketServerRestClient;
  private final ProjectAlmSettingDao projectAlmSettingDao;
  private final ProjectDao projectDao;

  public SearchBitbucketServerReposAction(DbClient dbClient, UserSession userSession,
    BitbucketServerRestClient bitbucketServerRestClient, ProjectAlmSettingDao projectAlmSettingDao, ProjectDao projectDao) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.bitbucketServerRestClient = bitbucketServerRestClient;
    this.projectAlmSettingDao = projectAlmSettingDao;
    this.projectDao = projectDao;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_bitbucketserver_repos")
      .setDescription("Search the Bitbucket Server repositories with REPO_ADMIN access<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(false)
      .setSince("8.2")
      .setResponseExample(getClass().getResource("example-search_bitbucketserver_repos.json"))
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");
    action.createParam(PARAM_PROJECT_NAME)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Project name filter");
    action.createParam(PARAM_REPO_NAME)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Repository name filter");
  }

  @Override
  public void handle(Request request, Response response) {
    SearchBitbucketserverReposWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private SearchBitbucketserverReposWsResponse doHandle(Request request) {

    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
      String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null");
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform Setting '%s' not found", almSettingKey)));
      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);

      String projectKey = request.param(PARAM_PROJECT_NAME);
      String repoName = request.param(PARAM_REPO_NAME);
      String pat = almPatDto.map(AlmPatDto::getPersonalAccessToken).orElseThrow(() -> new IllegalArgumentException("No personal access token found"));
      String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");
      RepositoryList gsonBBSRepoList = bitbucketServerRestClient.getRepos(url, pat, projectKey, repoName);

      Map<String, String> sqProjectsKeyByBBSKey = getSqProjectsKeyByBBSKey(dbSession, almSettingDto, gsonBBSRepoList);
      List<BBSRepo> bbsRepos = gsonBBSRepoList.getValues().stream().map(gsonBBSRepo -> toBBSRepo(gsonBBSRepo, sqProjectsKeyByBBSKey))
        .toList();

      SearchBitbucketserverReposWsResponse.Builder builder = SearchBitbucketserverReposWsResponse.newBuilder()
        .setIsLastPage(gsonBBSRepoList.isLastPage())
        .addAllRepositories(bbsRepos);
      return builder.build();
    }
  }

  private Map<String, String> getSqProjectsKeyByBBSKey(DbSession dbSession, AlmSettingDto almSettingDto, RepositoryList gsonBBSRepoList) {
    Set<String> slugs = gsonBBSRepoList.getValues().stream().map(Repository::getSlug).collect(toSet());

    List<ProjectAlmSettingDto> projectAlmSettingDtos = projectAlmSettingDao.selectByAlmSettingAndSlugs(dbSession, almSettingDto, slugs);
    // As the previous request return bbs only filtered by slug, we need to do an additional filtering on bitbucketServer projectKey + slug
    Set<String> bbsProjectsAndRepos = gsonBBSRepoList.getValues().stream().map(SearchBitbucketServerReposAction::customKey).collect(toSet());
    Map<String, ProjectAlmSettingDto> filteredProjectsByUuid = projectAlmSettingDtos.stream()
      .filter(p -> bbsProjectsAndRepos.contains(customKey(p)))
      .collect(toMap(ProjectAlmSettingDto::getProjectUuid, Function.identity()));

    Set<String> projectUuids = filteredProjectsByUuid.values().stream().map(ProjectAlmSettingDto::getProjectUuid).collect(toSet());
    return projectDao.selectByUuids(dbSession, projectUuids).stream()
      .collect(toMap(p -> customKey(filteredProjectsByUuid.get(p.getUuid())), ProjectDto::getKey, resolveNameCollisionOperatorByNaturalOrder()));
  }

  private static BBSRepo toBBSRepo(Repository gsonBBSRepo, Map<String, String> sqProjectsKeyByBBSKey) {
    BBSRepo.Builder builder = BBSRepo.newBuilder()
      .setSlug(gsonBBSRepo.getSlug())
      .setId(gsonBBSRepo.getId())
      .setName(gsonBBSRepo.getName())
      .setProjectKey(gsonBBSRepo.getProject().getKey());

    String sqProjectKey = sqProjectsKeyByBBSKey.get(customKey(gsonBBSRepo));
    if (sqProjectKey != null) {
      builder.setSqProjectKey(sqProjectKey);
    }

    return builder.build();
  }

  private static String customKey(ProjectAlmSettingDto project) {
    return project.getAlmRepo() + "/" + project.getAlmSlug();
  }

  private static String customKey(Repository gsonBBSRepo) {
    return gsonBBSRepo.getProject().getKey() + "/" + gsonBBSRepo.getSlug();
  }

  private static BinaryOperator<String> resolveNameCollisionOperatorByNaturalOrder() {
    return (a, b) -> new TreeSet<>(Arrays.asList(a, b)).first();
  }

}
