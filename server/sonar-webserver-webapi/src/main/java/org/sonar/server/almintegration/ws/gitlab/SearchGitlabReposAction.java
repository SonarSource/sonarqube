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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.alm.client.gitlab.GitlabApplicationClient;
import org.sonar.alm.client.gitlab.Project;
import org.sonar.alm.client.gitlab.ProjectList;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.pat.AlmPatDto;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.server.almintegration.ws.AlmIntegrationsWsAction;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmIntegrations;
import org.sonarqube.ws.AlmIntegrations.GitlabRepository;
import org.sonarqube.ws.Common.Paging;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchGitlabReposAction implements AlmIntegrationsWsAction {

  private static final String PARAM_ALM_SETTING = "almSetting";
  private static final String PARAM_PROJECT_NAME = "projectName";
  private static final int DEFAULT_PAGE_SIZE = 20;
  private static final int MAX_PAGE_SIZE = 500;

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GitlabApplicationClient gitlabApplicationClient;

  public SearchGitlabReposAction(DbClient dbClient, UserSession userSession, GitlabApplicationClient gitlabApplicationClient) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.gitlabApplicationClient = gitlabApplicationClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_gitlab_repos")
      .setDescription("Search the GitLab projects.<br/>" +
        "Requires the 'Create Projects' permission")
      .setPost(false)
      .setSince("8.5")
      .setHandler(this);

    action.createParam(PARAM_ALM_SETTING)
      .setRequired(true)
      .setMaximumLength(200)
      .setDescription("DevOps Platform setting key");
    action.createParam(PARAM_PROJECT_NAME)
      .setRequired(false)
      .setMaximumLength(200)
      .setDescription("Project name filter");

    action.addPagingParams(DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);

    action.setResponseExample(getClass().getResource("search_gitlab_repos.json"));
  }

  @Override
  public void handle(Request request, Response response) {
    AlmIntegrations.SearchGitlabReposWsResponse wsResponse = doHandle(request);
    writeProtobuf(wsResponse, request, response);
  }

  private AlmIntegrations.SearchGitlabReposWsResponse doHandle(Request request) {
    String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
    String projectName = request.param(PARAM_PROJECT_NAME);

    int pageNumber = request.mandatoryParamAsInt("p");
    int pageSize = request.mandatoryParamAsInt("ps");

    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);

      String userUuid = requireNonNull(userSession.getUuid(), "User UUID cannot be null");
      AlmSettingDto almSettingDto = dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform Setting '%s' not found", almSettingKey)));
      Optional<AlmPatDto> almPatDto = dbClient.almPatDao().selectByUserAndAlmSetting(dbSession, userUuid, almSettingDto);

      String personalAccessToken = almPatDto.map(AlmPatDto::getPersonalAccessToken).orElseThrow(() -> new IllegalArgumentException("No personal access token found"));
      String gitlabUrl = requireNonNull(almSettingDto.getUrl(), "DevOps Platform url cannot be null");

      ProjectList gitlabProjectList = gitlabApplicationClient
        .searchProjects(gitlabUrl, personalAccessToken, projectName, pageNumber, pageSize);

      Map<String, ProjectKeyName> sqProjectsKeyByGitlabProjectId = getSqProjectsKeyByGitlabProjectId(dbSession, almSettingDto, gitlabProjectList);

      List<GitlabRepository> gitlabRepositories = gitlabProjectList.getProjects().stream()
        .map(project -> toGitlabRepository(project, sqProjectsKeyByGitlabProjectId))
        .toList();

      Paging.Builder pagingBuilder = Paging.newBuilder()
        .setPageIndex(gitlabProjectList.getPageNumber())
        .setPageSize(gitlabProjectList.getPageSize());
      Integer gitlabProjectListTotal = gitlabProjectList.getTotal();
      if (gitlabProjectListTotal != null) {
        pagingBuilder.setTotal(gitlabProjectListTotal);
      }
      return AlmIntegrations.SearchGitlabReposWsResponse.newBuilder()
        .addAllRepositories(gitlabRepositories)
        .setPaging(pagingBuilder.build())
        .build();
    }
  }

  private Map<String, ProjectKeyName> getSqProjectsKeyByGitlabProjectId(DbSession dbSession, AlmSettingDto almSettingDto,
    ProjectList gitlabProjectList) {
    Set<String> gitlabProjectIds = gitlabProjectList.getProjects().stream().map(Project::getId).map(String::valueOf)
      .collect(toSet());
    Map<String, ProjectAlmSettingDto> projectAlmSettingDtos = dbClient.projectAlmSettingDao()
      .selectByAlmSettingAndRepos(dbSession, almSettingDto, gitlabProjectIds)
      .stream().collect(Collectors.toMap(ProjectAlmSettingDto::getProjectUuid, Function.identity()));

    return dbClient.projectDao().selectByUuids(dbSession, projectAlmSettingDtos.keySet())
      .stream()
      .collect(Collectors.toMap(projectDto -> projectAlmSettingDtos.get(projectDto.getUuid()).getAlmRepo(),
        p -> new ProjectKeyName(p.getKey(), p.getName()), resolveNameCollisionOperatorByNaturalOrder()));
  }

  private static BinaryOperator<ProjectKeyName> resolveNameCollisionOperatorByNaturalOrder() {
    return (a, b) -> b.key.compareTo(a.key) > 0 ? a : b;
  }

  private static GitlabRepository toGitlabRepository(Project project, Map<String, ProjectKeyName> sqProjectsKeyByGitlabProjectId) {
    String name = project.getName();
    String pathName = removeLastOccurrenceOfString(project.getNameWithNamespace(), " / " + name);

    String slug = project.getPath();
    String pathSlug = removeLastOccurrenceOfString(project.getPathWithNamespace(), "/" + slug);

    GitlabRepository.Builder builder = GitlabRepository.newBuilder()
      .setId(project.getId())
      .setName(name)
      .setPathName(pathName)
      .setSlug(slug)
      .setPathSlug(pathSlug)
      .setUrl(project.getWebUrl());

    String projectIdAsString = String.valueOf(project.getId());
    Optional.ofNullable(sqProjectsKeyByGitlabProjectId.get(projectIdAsString))
      .ifPresent(p -> builder
        .setSqProjectKey(p.key)
        .setSqProjectName(p.name));

    return builder.build();
  }

  private static String removeLastOccurrenceOfString(String string, String stringToRemove) {
    StringBuilder resultString = new StringBuilder(string);
    int index = resultString.lastIndexOf(stringToRemove);
    if (index > -1) {
      resultString.delete(index, string.length() + index);
    }
    return resultString.toString();
  }

  static class ProjectKeyName {
    String key;
    String name;

    ProjectKeyName(String key, String name) {
      this.key = key;
      this.name = name;
    }
  }
}
