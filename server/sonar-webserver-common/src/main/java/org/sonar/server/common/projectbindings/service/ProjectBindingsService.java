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
package org.sonar.server.common.projectbindings.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingQuery;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.SearchResults;

import static java.util.function.Function.identity;

public class ProjectBindingsService {

  private final DbClient dbClient;

  public ProjectBindingsService(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public Optional<ProjectAlmSettingDto> findProjectBindingByUuid(String uuid) {
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.projectAlmSettingDao().selectByUuid(session, uuid);
    }
  }

  public SearchResults<ProjectBindingInformation> findProjectBindingsByRequest(ProjectBindingsSearchRequest request) {
    // Check if repository URL is provided for Git URL search
    if (StringUtils.isNotBlank(request.repositoryUrl())) {
      return findProjectBindingsByGitUrl(request.repositoryUrl());
    }

    // Use traditional repository name search
    ProjectAlmSettingQuery query = buildProjectAlmSettingQuery(request);
    try (DbSession session = dbClient.openSession(false)) {
      int total = dbClient.projectAlmSettingDao().countProjectAlmSettings(session, query);
      if (request.pageSize() == 0) {
        return new SearchResults<>(List.of(), total);
      }
      List<ProjectBindingInformation> searchResults = performSearch(session, query, request.page(), request.pageSize());
      return new SearchResults<>(searchResults, total);
    }
  }

  private static ProjectAlmSettingQuery buildProjectAlmSettingQuery(ProjectBindingsSearchRequest request) {
    // Note: repositoryUrl is handled separately in findProjectBindingsByRequest
    // This method only handles traditional repository name search
    return new ProjectAlmSettingQuery(request.repository(), request.dopSettingId());
  }

  private List<ProjectBindingInformation> performSearch(DbSession dbSession, ProjectAlmSettingQuery query, int page, int pageSize) {
    List<ProjectAlmSettingDto> projectAlmSettings = dbClient.projectAlmSettingDao().selectProjectAlmSettings(dbSession, query, page, pageSize)
      .stream()
      .toList();
    Set<String> projectUuids = projectAlmSettings.stream().map(ProjectAlmSettingDto::getProjectUuid).collect(Collectors.toSet());

    List<ProjectDto> projectDtos = dbClient.projectDao().selectByUuids(dbSession, projectUuids);
    Map<String, ProjectDto> projectUuidsToProject = projectDtos.stream().collect(Collectors.toMap(ProjectDto::getUuid, identity()));

    return projectAlmSettings.stream().map(projectAlmSettingDtoToProjectBindingInformation(projectUuidsToProject)).toList();
  }

  private static Function<ProjectAlmSettingDto, ProjectBindingInformation> projectAlmSettingDtoToProjectBindingInformation(Map<String, ProjectDto> projectUuidToProject) {
    return projectAlmSettingDto -> {
      ProjectDto projectDto = projectUuidToProject.get(projectAlmSettingDto.getProjectUuid());
      return new ProjectBindingInformation(
        projectAlmSettingDto.getUuid(),
        projectAlmSettingDto.getAlmSettingUuid(),
        projectAlmSettingDto.getProjectUuid(),
        projectDto.getKey(),
        projectAlmSettingDto.getAlmRepo(),
        projectAlmSettingDto.getAlmSlug());
    };
  }

  public Optional<ProjectDto> findProjectFromBinding(ProjectAlmSettingDto projectAlmSettingDto) {
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.projectDao().selectByUuid(session, projectAlmSettingDto.getProjectUuid());
    }
  }

  /**
   * Searches for project bindings using a Git repository URL.
   * This method performs fuzzy matching by searching for repository names in database fields.
   * 
   * @param gitUrl The Git repository URL to search for
   * @return SearchResults containing matching project bindings
   */
  public SearchResults<ProjectBindingInformation> findProjectBindingsByGitUrl(@Nullable String gitUrl) {
    if (StringUtils.isBlank(gitUrl)) {
      return new SearchResults<>(List.of(), 0);
    }

    Optional<GitUrlParser.RepositoryInfo> repositoryInfoOpt = GitUrlParser.parseRepositoryUrl(gitUrl);
    if (repositoryInfoOpt.isEmpty()) {
      return new SearchResults<>(List.of(), 0);
    }

    GitUrlParser.RepositoryInfo repositoryInfo = repositoryInfoOpt.get();

    try (DbSession session = dbClient.openSession(false)) {
      Set<ProjectAlmSettingDto> projectAlmSettings = searchProjectBindings(session, repositoryInfo);

      Set<String> projectUuids = projectAlmSettings.stream()
        .map(ProjectAlmSettingDto::getProjectUuid)
        .collect(Collectors.toSet());

      List<ProjectDto> projectDtos = dbClient.projectDao().selectByUuids(session, projectUuids);
      Map<String, ProjectDto> projectUuidsToProject = projectDtos.stream()
        .collect(Collectors.toMap(ProjectDto::getUuid, identity()));

      List<ProjectBindingInformation> results = projectAlmSettings.stream()
        .map(projectAlmSettingDtoToProjectBindingInformation(projectUuidsToProject))
        .toList();

      return new SearchResults<>(results, results.size());
    }
  }

  private Set<ProjectAlmSettingDto> searchProjectBindings(DbSession session, GitUrlParser.RepositoryInfo repositoryInfo) {
    Set<ProjectAlmSettingDto> allResults = new HashSet<>();
    for (ALM alm : ALM.values()) {
      ProjectBindingSearchStrategy strategy = getSearchStrategy(alm);
      if (strategy != null) {
        allResults.addAll(strategy.search(dbClient, session, repositoryInfo));
      }
    }

    return allResults.stream().collect(Collectors.toSet());
  }

  private static ProjectBindingSearchStrategy getSearchStrategy(ALM alm) {
    return switch (alm) {
      case GITHUB -> ProjectBindingSearchStrategy.GITHUB;
      case AZURE_DEVOPS -> ProjectBindingSearchStrategy.AZURE_DEVOPS;
      case BITBUCKET, BITBUCKET_CLOUD -> ProjectBindingSearchStrategy.BITBUCKET;
      // Skip GitLab for now
      case GITLAB -> null;
    };
  }

}
