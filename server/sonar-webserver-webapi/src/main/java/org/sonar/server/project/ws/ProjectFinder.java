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
package org.sonar.server.project.ws;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.user.UserSession;

@ServerSide
public class ProjectFinder {

  private final DbClient dbClient;
  private final UserSession userSession;

  public ProjectFinder(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public SearchResult search(DbSession dbSession, @Nullable String searchQuery) {
    Set<ProjectDto> candidateProjects;

    if (userSession.isRoot()) {
      candidateProjects = new HashSet<>(dbClient.projectDao().selectProjects(dbSession));
    } else {
      List<ProjectDto> projectsWithDirectScanPermission = searchProjectsWithDirectScanPermission(dbSession);
      List<ProjectDto> projectsWithOrgLevelScanPermissions = searchProjectsWithOrgLevelScanPermissions(dbSession);

      candidateProjects = new HashSet<>();
      candidateProjects.addAll(projectsWithDirectScanPermission);
      candidateProjects.addAll(projectsWithOrgLevelScanPermissions);
    }

    Set<ProjectDto> filteredProjects = filterByQuery(searchQuery, candidateProjects);

    List<Project> resultProjects = filteredProjects.stream()
        .sorted(comparing(ProjectDto::getName, nullsFirst(String.CASE_INSENSITIVE_ORDER)))
        .map(p -> new Project(p.getKey(), p.getName()))
        .toList();

    return new SearchResult(resultProjects);
  }

  private List<ProjectDto> searchProjectsWithDirectScanPermission(DbSession dbSession) {
    String userUuid = userSession.getUuid();
    if (StringUtils.isEmpty(userUuid)) {
      return List.of();
    }

    List<String> projectUuids = dbClient.roleDao()
        .selectEntityUuidsByPermissionAndUserUuidAndQualifier(dbSession, UserRole.SCAN, userUuid,
            Set.of(ComponentQualifiers.PROJECT));
    if (projectUuids.isEmpty()) {
      return List.of();
    }

    return dbClient.projectDao().selectByUuids(dbSession, Set.copyOf(projectUuids));
  }

  private List<ProjectDto> searchProjectsWithOrgLevelScanPermissions(DbSession dbSession) {
    List<OrganizationDto> orgs = dbClient.organizationDao().selectOrgsForUserAndRole(dbSession, userSession.getUuid(),
        OrganizationPermission.SCAN.toString());
    if (orgs.isEmpty()) {
      return List.of();
    }
    List<String> orgUuids = orgs.stream().map(OrganizationDto::getUuid).toList();
    return dbClient.projectDao().selectProjectsByOrganizationUuids(dbSession, orgUuids);
  }

  private Set<ProjectDto> filterByQuery(@Nullable String searchQuery, Set<ProjectDto> projects) {
    if (StringUtils.isEmpty(searchQuery)) {
      return projects;
    }
    return projects.stream()
        .filter(project -> !isFilteredByQuery(searchQuery, project.getName()))
        .collect(Collectors.toSet());
  }

  private static boolean isFilteredByQuery(@Nullable String query, String projectName) {
    return query != null && !projectName.toLowerCase(ENGLISH).contains(query.toLowerCase(ENGLISH));
  }

  public static class SearchResult {

    private final List<Project> projects;

    public SearchResult(List<Project> projects) {
      this.projects = projects;
    }

    public List<Project> getProjects() {
      return projects;
    }
  }

  public static class Project {
    private final String key;
    private final String name;

    public Project(String key, @Nullable String name) {
      this.key = requireNonNull(key, "Key cant be null");
      this.name = name;
    }

    public String getKey() {
      return key;
    }

    @CheckForNull
    public String getName() {
      return name;
    }

  }
}
