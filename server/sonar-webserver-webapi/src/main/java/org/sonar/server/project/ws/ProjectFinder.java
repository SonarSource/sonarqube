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
package org.sonar.server.project.ws;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
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

  public ProjectFinder(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public SearchResult search(DbSession dbSession, @Nullable String searchQuery) {
    List<ProjectDto> allProjects = dbClient.projectDao().selectProjects(dbSession);

    Set<String> projectsUserHasAccessTo = userSession.keepAuthorizedEntities(ProjectPermission.SCAN, allProjects)
      .stream()
      .map(ProjectDto::getKey)
      .collect(toSet());

    applyQueryAndPermissionFilter(searchQuery, allProjects, projectsUserHasAccessTo);

    List<Project> resultProjects = allProjects.stream()
      .sorted(comparing(ProjectDto::getName, nullsFirst(String.CASE_INSENSITIVE_ORDER)))
      .map(p -> new Project(p.getKey(), p.getName()))
      .toList();
    return new SearchResult(resultProjects);
  }

  private void applyQueryAndPermissionFilter(@Nullable String searchQuery, final List<ProjectDto> projects,
    Set<String> projectsUserHasAccessTo) {
    Iterator<ProjectDto> projectIterator = projects.iterator();
    while (projectIterator.hasNext()) {
      ProjectDto project = projectIterator.next();
      if (isFilteredByQuery(searchQuery, project.getName()) || !hasPermission(projectsUserHasAccessTo, project.getKey())) {
        projectIterator.remove();
      }
    }
  }

  private static boolean isFilteredByQuery(@Nullable String query, String projectName) {
    return query != null && !projectName.toLowerCase(ENGLISH).contains(query.toLowerCase(ENGLISH));
  }

  private boolean hasPermission(Set<String> projectsUserHasAccessTo, String projectKey) {
    return userSession.hasPermission(GlobalPermission.SCAN) || projectsUserHasAccessTo.contains(projectKey);
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
