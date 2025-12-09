/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.alm.client.azure.AzureDevOpsHttpClient;
import org.sonar.alm.client.azure.GsonAzureRepo;
import org.sonar.alm.client.azure.GsonAzureRepoList;
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
import org.sonarqube.ws.AlmIntegrations.AzureRepo;
import org.sonarqube.ws.AlmIntegrations.SearchAzureReposWsResponse;

import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.Strings.CI;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchAzureReposAction implements AlmIntegrationsWsAction {

  private static final Logger LOG = LoggerFactory.getLogger(SearchAzureReposAction.class);

  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_PROJECT_NAME = "projectName";
  private static final String PARAM_SEARCH_QUERY = "searchQuery";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AzureDevOpsHttpClient azureDevOpsHttpClient;

  public SearchAzureReposAction(DbClient dbClient, UserSession userSession,
    AzureDevOpsHttpClient azureDevOpsHttpClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.azureDevOpsHttpClient = azureDevOpsHttpClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_azure_repos")
      .setDescription("Search the Azure repositories<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(false)
      .setSince("8.6")
      .setResponseExample(getClass().getResource("example-search_azure_repos.json"))
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");
    action.createParam(PARAM_PROJECT_NAME)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Project name filter");
    action.createParam(PARAM_SEARCH_QUERY)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Search query filter");
  }

  @Override
  public void handle(Request request, Response response) {

    SearchAzureReposWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);

  }

  private SearchAzureReposWsResponse doHandle(Request request) {

    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
      String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null");
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform Setting '%s' not found", almSettingKey)));
      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);

      String projectKey = request.param(PARAM_PROJECT_NAME);
      String searchQuery = request.param(PARAM_SEARCH_QUERY);
      String pat = almPatDto.map(AlmPatDto::getPersonalAccessToken).orElseThrow(() -> new IllegalArgumentException("No personal access token found"));
      String url = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");

      GsonAzureRepoList gsonAzureRepoList = azureDevOpsHttpClient.getRepos(url, pat, projectKey);

      Map<ProjectKeyName, ProjectDto> sqProjectsKeyByAzureKey = getSqProjectsKeyByCustomKey(dbSession, almSettingDto, gsonAzureRepoList);

      List<AzureRepo> repositories = gsonAzureRepoList.getValues()
        .stream()
        .filter(r -> isSearchOnlyByProjectName(searchQuery) || doesSearchCriteriaMatchProjectOrRepo(r, searchQuery))
        .map(repo -> toAzureRepo(repo, sqProjectsKeyByAzureKey))
        .sorted(comparing(AzureRepo::getName, String::compareToIgnoreCase))
        .toList();

      LOG.atDebug().log(repositories::toString);

      return SearchAzureReposWsResponse.newBuilder()
        .addAllRepositories(repositories)
        .build();
    }
  }

  private Map<ProjectKeyName, ProjectDto> getSqProjectsKeyByCustomKey(DbSession dbSession, AlmSettingDto almSettingDto,
    GsonAzureRepoList azureProjectList) {
    Set<String> projectNames = azureProjectList.getValues().stream().map(r -> r.getProject().getName()).collect(toSet());
    Set<ProjectKeyName> azureProjectsAndRepos = azureProjectList.getValues().stream().map(ProjectKeyName::from).collect(toSet());

    List<ProjectAlmSettingDto> projectAlmSettingDtos = dbClient.projectAlmSettingDao()
      .selectByAlmSettingAndSlugs(dbSession, almSettingDto, projectNames);

    Map<String, ProjectAlmSettingDto> filteredProjectsByUuid = projectAlmSettingDtos
      .stream()
      .filter(p -> azureProjectsAndRepos.contains(ProjectKeyName.from(p)))
      .collect(toMap(ProjectAlmSettingDto::getProjectUuid, Function.identity()));

    Set<String> projectUuids = filteredProjectsByUuid.values().stream().map(ProjectAlmSettingDto::getProjectUuid).collect(toSet());

    return dbClient.projectDao().selectByUuids(dbSession, projectUuids)
      .stream()
      .collect(Collectors.toMap(
        projectDto -> ProjectKeyName.from(filteredProjectsByUuid.get(projectDto.getUuid())),
        p -> p,
        resolveNameCollisionOperatorByNaturalOrder()));
  }

  private static boolean isSearchOnlyByProjectName(@Nullable String criteria) {
    return criteria == null || criteria.isEmpty();
  }

  private static boolean doesSearchCriteriaMatchProjectOrRepo(GsonAzureRepo repo, String criteria) {
    boolean matchProject = CI.contains(repo.getProject().getName(), criteria);
    boolean matchRepo = CI.contains(repo.getName(), criteria);
    return matchProject || matchRepo;
  }

  private static AzureRepo toAzureRepo(GsonAzureRepo azureRepo, Map<ProjectKeyName, ProjectDto> sqProjectsKeyByAzureKey) {
    AzureRepo.Builder builder = AzureRepo.newBuilder()
      .setName(azureRepo.getName())
      .setProjectName(azureRepo.getProject().getName());

    ProjectDto projectDto = sqProjectsKeyByAzureKey.get(ProjectKeyName.from(azureRepo));
    if (projectDto != null) {
      builder.setSqProjectName(projectDto.getName());
      builder.setSqProjectKey(projectDto.getKey());
    }

    return builder.build();
  }

  private static BinaryOperator<ProjectDto> resolveNameCollisionOperatorByNaturalOrder() {
    return (a, b) -> b.getKey().compareTo(a.getKey()) > 0 ? a : b;
  }

  static class ProjectKeyName {
    final String projectName;
    final String repoName;

    ProjectKeyName(String projectName, String repoName) {
      this.projectName = projectName;
      this.repoName = repoName;
    }

    public static ProjectKeyName from(ProjectAlmSettingDto project) {
      return new ProjectKeyName(project.getAlmSlug(), project.getAlmRepo());
    }

    public static ProjectKeyName from(GsonAzureRepo gsonAzureRepo) {
      return new ProjectKeyName(gsonAzureRepo.getProject().getName(), gsonAzureRepo.getName());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ProjectKeyName that = (ProjectKeyName) o;
      return Objects.equals(projectName, that.projectName) &&
        Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(projectName, repoName);
    }
  }

}
