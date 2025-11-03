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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.es.EsClientProvider;
import org.sonar.server.user.UserSession;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsFirst;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

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
    long startProjectTime = startTime;
    List<ProjectDto> allProjects = dbClient.projectDao().selectProjects(dbSession);
    LOGGER.info("Found {} projects, took {} ms", allProjects.size(), System.currentTimeMillis() - startProjectTime);

    long startEntityTime = System.currentTimeMillis();
    Set<String> projectsUserHasAccessTo = userSession.keepAuthorizedEntities(UserRole.SCAN, allProjects)
      .stream()
      .map(ProjectDto::getKey)
      .collect(toSet());
    LOGGER.info("Keep authorized entities {} total ms, {} step ms", System.currentTimeMillis() - startTime, System.currentTimeMillis() - startEntityTime);

    long startQueryPermTime = System.currentTimeMillis();
    applyQueryAndPermissionFilter(searchQuery, allProjects, projectsUserHasAccessTo);
    LOGGER.info("Apply query perm filter {} total ms, {} step ms", System.currentTimeMillis() - startTime, System.currentTimeMillis() - startQueryPermTime);

    long searchOrgLevelTime = System.currentTimeMillis();
    List<ProjectDto> projectsWithOrgLevelPermissions = searchProjectsWithOrgLevelPermissions(dbSession);
    LOGGER.info("Search Proj with org level perm {} total ms, {} step ms", System.currentTimeMillis() - startTime, System.currentTimeMillis() - searchOrgLevelTime);

    if (!projectsWithOrgLevelPermissions.isEmpty()) {
      LOGGER.info("Project with org level not empty check");
      long startProjWithOrgLevelFilterTime = System.currentTimeMillis();
      List<ProjectDto> uniqueProjects = projectsWithOrgLevelPermissions
              .stream()
              .filter(p -> !allProjects.contains(p))
              .toList();
      LOGGER.info("Filter unique projects {} total ms, {} step ms", System.currentTimeMillis() - startTime, System.currentTimeMillis() - startProjWithOrgLevelFilterTime);

      if (!uniqueProjects.isEmpty()) {
        allProjects.addAll(uniqueProjects);
      }
    }

    long startResultTime = System.currentTimeMillis();
    List<Project> resultProjects = allProjects.stream()
            .sorted(comparing(ProjectDto::getName, nullsFirst(String.CASE_INSENSITIVE_ORDER)))
            .map(p -> new Project(p.getKey(), p.getName())).collect(Collectors.toList());
    LOGGER.info("Results project {} total ms, {} step ms", System.currentTimeMillis() - startTime, System.currentTimeMillis() - startResultTime);

    return new SearchResult(resultProjects);
  }

  private List<ProjectDto> searchProjectsWithOrgLevelPermissions(DbSession dbSession) {
    List<OrganizationDto> orgs = dbClient.organizationDao().selectOrgsForUserAndRole(dbSession, userSession.getUuid(),
            OrganizationPermission.SCAN.toString());
    if (orgs.isEmpty()) {
      return List.of();
    }
    List<String> orgUuids = orgs.stream().map(o -> o.getUuid())
            .collect(Collectors.toList());
    return dbClient.projectDao().selectProjectsByOrganizationUuids(dbSession, orgUuids);
  }

  private void applyQueryAndPermissionFilter(@Nullable String searchQuery, final List<ProjectDto> projects,
          Set<String> projectsUserHasAccessTo) {
    Iterator<ProjectDto> projectIterator = projects.iterator();
    while (projectIterator.hasNext()) {
      ProjectDto project = projectIterator.next();
      if (isFilteredByQuery(searchQuery, project.getName()) || !hasPermission(projectsUserHasAccessTo,
              project.getKey())) {
        projectIterator.remove();
      }
    }
  }

  private static boolean isFilteredByQuery(@Nullable String query, String projectName) {
    return query != null && !projectName.toLowerCase(ENGLISH).contains(query.toLowerCase(ENGLISH));
  }

  private boolean hasPermission(Set<String> projectsUserHasAccessTo, String projectKey) {
    return userSession.hasPermission(OrganizationPermission.SCAN, "" /* TODO */) || projectsUserHasAccessTo.contains(projectKey);
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
