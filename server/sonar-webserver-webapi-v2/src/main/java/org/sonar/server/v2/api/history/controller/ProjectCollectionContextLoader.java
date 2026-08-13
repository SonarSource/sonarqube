/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.history.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTreeQuery;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarsource.history.api.model.ProjectCollectionHistoryEntityType;
import org.sonarsource.history.model.ProjectBranch;

import static org.sonar.db.component.ComponentQualifiers.APP;
import static org.sonar.db.component.ComponentQualifiers.PROJECT;
import static org.sonar.db.component.ComponentTreeQuery.Strategy.LEAVES;
import static org.sonar.db.permission.ProjectPermission.USER;

public class ProjectCollectionContextLoader {

  private final UserSession userSession;
  private final DbClient dbClient;

  public ProjectCollectionContextLoader(UserSession userSession, DbClient dbClient) {
    this.userSession = userSession;
    this.dbClient = dbClient;
  }

  public ProjectCollectionContext load(DbSession session, String portfolioId) {
    return loadPortfolioContext(session, portfolioId);
  }

  public ProjectCollectionContext load(DbSession session, ProjectCollectionHistoryEntityType entityType, String entityId) {
    return switch (entityType) {
      case PORTFOLIO -> loadPortfolioContext(session, entityId);
      case APPLICATION -> loadApplicationContext(session, entityId);
    };
  }

  private ProjectCollectionContext loadPortfolioContext(DbSession session, String portfolioId) {
    PortfolioDto portfolio = dbClient.portfolioDao().selectByUuid(session, portfolioId)
      .orElseThrow(() -> contextNotFound(portfolioId));
    userSession.checkEntityPermission(USER, portfolio);

    ComponentTreeQuery projectLeavesQuery = ComponentTreeQuery.builder()
      .setBaseUuid(portfolio.getUuid())
      .setQualifiers(Set.of(PROJECT))
      .setStrategy(LEAVES)
      .build();
    List<String> branchIds = dbClient.componentDao().selectDescendants(session, projectLeavesQuery).stream()
      .map(ComponentDto::getCopyComponentUuid)
      .filter(Objects::nonNull)
      .toList();
    return createContext(session, dbClient.branchDao().selectByUuids(session, branchIds));
  }

  private ProjectCollectionContext loadApplicationContext(DbSession session, String applicationBranchId) {
    BranchDto applicationBranch = dbClient.branchDao().selectByUuid(session, applicationBranchId)
      .orElseThrow(() -> contextNotFound(applicationBranchId));
    ProjectDto application = dbClient.projectDao().selectByUuid(session, applicationBranch.getProjectUuid())
      .filter(project -> APP.equals(project.getQualifier()))
      .orElseThrow(() -> contextNotFound(applicationBranchId));
    userSession.checkEntityPermission(USER, application);
    userSession.checkChildProjectsPermission(USER, application);
    List<BranchDto> branches = applicationBranch.isMain()
      ? dbClient.applicationProjectsDao().selectProjectsMainBranchesOfApplication(session, application.getUuid())
      : List.copyOf(dbClient.applicationProjectsDao().selectProjectBranchesFromAppBranchUuid(session, applicationBranchId));
    return createContext(session, branches);
  }

  private ProjectCollectionContext createContext(DbSession session, List<BranchDto> branchDtos) {
    Set<String> projectUuids = branchDtos.stream().map(BranchDto::getProjectUuid).collect(Collectors.toSet());
    List<ProjectDto> projects = dbClient.projectDao().selectByUuids(session, projectUuids);
    Map<String, ProjectDto> projectsByUuid = projects.stream()
      .collect(Collectors.toMap(ProjectDto::getUuid, Function.identity()));
    List<ProjectBranch> branches = branchDtos.stream()
      .map(branch -> toProjectBranch(branch, projectsByUuid))
      .toList();
    Set<String> visibleProjectUuids = userSession.keepAuthorizedEntities(USER, projects).stream()
      .map(ProjectDto::getUuid)
      .collect(Collectors.toSet());
    Set<String> visibleBranchIds = branchDtos.stream()
      .filter(branch -> visibleProjectUuids.contains(branch.getProjectUuid()))
      .map(BranchDto::getUuid)
      .collect(Collectors.toSet());
    return new ProjectCollectionContext(branches, visibleBranchIds);
  }

  private static ProjectBranch toProjectBranch(
    BranchDto branch,
    Map<String, ProjectDto> projectsByUuid) {
    ProjectDto project = projectsByUuid.get(branch.getProjectUuid());
    if (project == null) {
      throw new IllegalStateException("Project '%s' for branch '%s' not found"
        .formatted(branch.getProjectUuid(), branch.getUuid()));
    }
    return new ProjectBranch(
      branch.getUuid(), branch.getKey(), project.getKey(), project.getName());
  }

  private static NotFoundException contextNotFound(String contextId) {
    return new NotFoundException("Portfolio or application branch '%s' not found".formatted(contextId));
  }
}
