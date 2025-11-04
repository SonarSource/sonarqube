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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(ProjectFinder.class);

  public ProjectFinder(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public SearchResult search(DbSession dbSession, @Nullable String searchQuery) {
    long startTime = System.currentTimeMillis();

    Set<ProjectDto> candidateProjects;
    if (userSession.isRoot()) {
      long loadAllStart = System.currentTimeMillis();
      candidateProjects = new HashSet<>(dbClient.projectDao().selectProjects(dbSession));
      LOGGER.info("Loaded {} projects for root user, took {} ms", candidateProjects.size(),
          System.currentTimeMillis() - loadAllStart);
    } else {
      long directStart = System.currentTimeMillis();
      List<ProjectDto> projectsWithDirectPermission = searchProjectsWithDirectScanPermission(dbSession);
      LOGGER.info("Projects with direct scan permission {} projects, {} ms", projectsWithDirectPermission.size(),
          System.currentTimeMillis() - directStart);

      long orgLevelStart = System.currentTimeMillis();
      List<ProjectDto> projectsWithOrgLevelPermissions = searchProjectsWithOrgLevelPermissions(dbSession);
      LOGGER.info("Projects with org level scan permission {} projects, {} ms", projectsWithOrgLevelPermissions.size(),
          System.currentTimeMillis() - orgLevelStart);

      candidateProjects = new HashSet<>(projectsWithDirectPermission);
      candidateProjects.addAll(projectsWithOrgLevelPermissions);
    }

    long filterStart = System.currentTimeMillis();
    List<ProjectDto> filteredProjects = filterByQuery(searchQuery, new ArrayList<>(candidateProjects));
    LOGGER.info("Apply query filter {} total ms, {} step ms", System.currentTimeMillis() - startTime,
        System.currentTimeMillis() - filterStart);

    long authorizationStart = System.currentTimeMillis();
    List<ProjectDto> authorizedProjects = userSession.keepAuthorizedEntities(UserRole.SCAN, filteredProjects);
    LOGGER.info("Keep authorized entities {} total ms, {} step ms", System.currentTimeMillis() - startTime,
        System.currentTimeMillis() - authorizationStart);

    long startResultTime = System.currentTimeMillis();
    List<Project> resultProjects = authorizedProjects.stream()
        .sorted(comparing(ProjectDto::getName, nullsFirst(String.CASE_INSENSITIVE_ORDER)))
        .map(p -> new Project(p.getKey(), p.getName()))
        .toList();
    LOGGER.info("Results project {} total ms, {} step ms", System.currentTimeMillis() - startTime,
        System.currentTimeMillis() - startResultTime);

    return new SearchResult(resultProjects);
  }

  private List<ProjectDto> searchProjectsWithDirectScanPermission(DbSession dbSession) {
    String userUuid = userSession.getUuid();
    if (userUuid == null) {
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

  private List<ProjectDto> searchProjectsWithOrgLevelPermissions(DbSession dbSession) {
    List<OrganizationDto> orgs = dbClient.organizationDao().selectOrgsForUserAndRole(dbSession, userSession.getUuid(),
        OrganizationPermission.SCAN.toString());
    if (orgs.isEmpty()) {
      return List.of();
    }
    List<String> orgUuids = orgs.stream().map(OrganizationDto::getUuid).toList();
    return dbClient.projectDao().selectProjectsByOrganizationUuids(dbSession, orgUuids);
  }

  private List<ProjectDto> filterByQuery(@Nullable String searchQuery, List<ProjectDto> projects) {
    if (searchQuery == null) {
      return projects;
    }
    return projects.stream()
        .filter(project -> !isFilteredByQuery(searchQuery, project.getName()))
        .toList();
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
