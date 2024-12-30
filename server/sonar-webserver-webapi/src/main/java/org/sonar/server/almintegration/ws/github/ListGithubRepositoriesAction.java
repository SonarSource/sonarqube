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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.auth.github.GithubApplicationClient;
import org.sonar.auth.github.GithubApplicationClient.Repository;
import org.sonar.alm.client.github.GithubApplicationClientImpl;
import org.sonar.auth.github.security.AccessToken;
import org.sonar.auth.github.security.UserAccessToken;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDao;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmIntegrations;
import org.sonarqube.ws.Common;

import static java.util.Objects.requireNonNull;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ListGithubRepositoriesAction implements AlmIntegrationsWsAction {

  public static final String PARAM_ALM_SETTING = "almSetting";
  public static final String PARAM_ORGANIZATION = "organization";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GithubApplicationClient githubApplicationClient;
  private final ProjectAlmSettingDao projectAlmSettingDao;

  public ListGithubRepositoriesAction(DbClient dbClient, UserSession userSession, GithubApplicationClientImpl githubApplicationClient, ProjectAlmSettingDao projectAlmSettingDao) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.githubApplicationClient = githubApplicationClient;
    this.projectAlmSettingDao = projectAlmSettingDao;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("list_github_repositories")
      .setDescription("List the GitHub repositories for an organization<br/>" +
        "Requires the 'Create Projects' permission")
      .setInternal(true)
      .setSince("8.4")
      .setResponseExample(getClass().getResource("example-list_github_repositories.json"))
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");

    action.createParam(PARAM_ORGANIZATION)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("Github organization");

    action.createParam(TEXT_QUERY)
      .setDescription("Limit search to repositories that contain the supplied string")
      .setExampleValue("Apache");

    action.createParam(PAGE)
      .setDescription("Index of the page to display")
      .setDefaultValue(1);
    action.createParam(PAGE_SIZE)
      .setDescription("Size for the paging to apply")
      .setDefaultValue(100);
  }

  @Override
  public void handle(Request request, Response response) {
    AlmIntegrations.ListGithubRepositoriesWsResponse getResponse = doHandle(request);
    writeProtobuf(getResponse, request, response);
  }

  private AlmIntegrations.ListGithubRepositoriesWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("GitHub Setting '%s' not found", almSettingKey)));

      String userUuid = requireNonNull(userSession.getUuid(), "User UUID is not null");
      String url = requireNonNull(almSettingDto.getUrl(), String.format("No URL set for GitHub '%s'", almSettingKey));

      AccessToken accessToken = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto)
        .map(AlmPatDto::getPersonalAccessToken)
        .map(UserAccessToken::new)
        .orElseThrow(() -> new IllegalArgumentException("No personal access token found"));

      int pageIndex = request.hasParam(PAGE) ? request.mandatoryParamAsInt(PAGE) : 1;
      int pageSize = request.hasParam(PAGE_SIZE) ? request.mandatoryParamAsInt(PAGE_SIZE) : 100;

      GithubApplicationClient.Repositories repositories = githubApplicationClient
        .listRepositories(url, accessToken, request.mandatoryParam(PARAM_ORGANIZATION), request.param(TEXT_QUERY), pageIndex, pageSize);

      AlmIntegrations.ListGithubRepositoriesWsResponse.Builder response = AlmIntegrations.ListGithubRepositoriesWsResponse.newBuilder()
        .setPaging(Common.Paging.newBuilder()
          .setPageIndex(pageIndex)
          .setPageSize(pageSize)
          .setTotal(repositories.getTotal())
          .build());

      List<Repository> repositoryList = repositories.getRepositories();
      if (repositoryList != null) {

        Set<String> repo = repositoryList.stream().map(Repository::getFullName).collect(Collectors.toSet());
        List<ProjectAlmSettingDto> projectAlmSettingDtos = projectAlmSettingDao.selectByAlmSettingAndRepos(dbSession, almSettingDto, repo);

        Map<String, ProjectDto> projectsDtoByAlmRepo = getProjectDtoByAlmRepo(dbSession, projectAlmSettingDtos);

        for (Repository repository : repositoryList) {
          AlmIntegrations.GithubRepository.Builder builder = AlmIntegrations.GithubRepository.newBuilder()
            .setId(repository.getId())
            .setKey(repository.getFullName())
            .setName(repository.getName())
            .setUrl(repository.getUrl());

          if (projectsDtoByAlmRepo.containsKey(repository.getFullName())) {
            Optional.ofNullable(projectsDtoByAlmRepo.get(repository.getFullName()))
              .ifPresent(p -> builder.setSqProjectKey(p.getKey()));
          }

          response.addRepositories(builder.build());
        }
      }

      return response.build();
    }
  }

  private Map<String, ProjectDto> getProjectDtoByAlmRepo(DbSession dbSession, List<ProjectAlmSettingDto> projectAlmSettingDtos) {
    Map<String, ProjectAlmSettingDto> projectAlmSettingDtoByProjectUuid = projectAlmSettingDtos.stream()
      .collect(Collectors.toMap(ProjectAlmSettingDto::getProjectUuid, Function.identity()));

    Set<String> projectUuids = projectAlmSettingDtos.stream().map(ProjectAlmSettingDto::getProjectUuid).collect(Collectors.toSet());

    return dbClient.projectDao().selectByUuids(dbSession, projectUuids)
      .stream()
      .collect(Collectors.toMap(projectDto -> projectAlmSettingDtoByProjectUuid.get(projectDto.getUuid()).getAlmRepo(),
        Function.identity(), resolveNameCollisionOperatorByNaturalOrder()));
  }

  private static BinaryOperator<ProjectDto> resolveNameCollisionOperatorByNaturalOrder() {
    Comparator<ProjectDto> comparator = Comparator.comparing(ProjectDto::getKey);
    return (a, b) -> comparator.compare(a, b) > 0 ? b : a;
  }
}
