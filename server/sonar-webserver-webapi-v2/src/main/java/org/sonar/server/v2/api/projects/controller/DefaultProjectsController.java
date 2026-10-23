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
package org.sonar.server.v2.api.projects.controller;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.projects.request.ProjectsSearchRestRequest;
import org.sonar.server.v2.api.projects.response.ProjectsSearchRestResponse;
import org.sonar.server.v2.api.projects.response.ProjectsSearchRestResponse.ProjectLinkRestResponse;
import org.sonar.server.v2.api.projects.response.ProjectsSearchRestResponse.ProjectRestResponse;
import org.sonar.server.v2.api.response.PageRestResponse;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.groupingBy;
import static org.sonar.db.permission.ProjectPermission.USER;

public class DefaultProjectsController implements ProjectsController {
  private final DbClient dbClient;
  private final UserSession userSession;

  public DefaultProjectsController(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public ProjectsSearchRestResponse getProjects(ProjectsSearchRestRequest request) {
    checkArgument(!isEmpty(request.ids()) || !isEmpty(request.keys()), "At least one of ids or keys is required");
    try (DbSession session = dbClient.openSession(false)) {
      List<ProjectDto> candidates = isEmpty(request.ids())
        ? dbClient.projectDao().selectProjectsByKeys(session, request.keys())
        : dbClient.projectDao().selectByUuids(session, request.ids());
      List<ProjectDto> projects = userSession.keepAuthorizedEntities(USER, candidates).stream()
        .filter(p -> "TRK".equals(p.getQualifier()))
        .filter(p -> isEmpty(request.keys()) || request.keys().contains(p.getKey()))
        .filter(p -> isEmpty(request.tags()) || p.getTags().stream().anyMatch(request.tags()::contains))
        .sorted(Comparator.comparingLong(ProjectDto::getCreatedAt).thenComparing(ProjectDto::getUuid))
        .toList();
      List<ProjectDto> page = projects.stream()
        .skip((long) (request.pageIndex() - 1) * request.pageSize())
        .limit(request.pageSize())
        .toList();
      Map<String, List<ProjectLinkDto>> links = page.isEmpty() ? Map.of()
        : dbClient.projectLinkDao().selectByProjectUuids(session, page.stream().map(ProjectDto::getUuid).toList()).stream()
          .collect(groupingBy(ProjectLinkDto::getProjectUuid));
      return new ProjectsSearchRestResponse(page.stream().map(p -> toResponse(p, links.getOrDefault(p.getUuid(), List.of()))).toList(),
        new PageRestResponse(request.pageIndex(), request.pageSize(), projects.size()));
    }
  }

  private static boolean isEmpty(@Nullable Collection<?> values) {
    return values == null || values.isEmpty();
  }

  private static ProjectRestResponse toResponse(ProjectDto project, List<ProjectLinkDto> links) {
    return new ProjectRestResponse(project.getUuid(), project.getKey(), project.getName(), project.isPrivate() ? "private" : "public",
      project.getDescription(), project.getTags(),
      links.stream().map(link -> new ProjectLinkRestResponse(link.getName(), link.getHref(), link.getType())).toList(),
      Instant.ofEpochMilli(project.getCreatedAt()), Instant.ofEpochMilli(project.getUpdatedAt()));
  }
}
