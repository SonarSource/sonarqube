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
package org.sonar.server.common.projectbindings.service;

import java.util.List;
import java.util.Optional;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingQuery;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.SearchResults;

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

  public SearchResults<ProjectAlmSettingDto> findProjectBindingsByRequest(ProjectBindingsSearchRequest request) {
    ProjectAlmSettingQuery query = buildProjectAlmSettingQuery(request);
    try (DbSession session = dbClient.openSession(false)) {
      int total = dbClient.projectAlmSettingDao().countProjectAlmSettings(session, query);
      if (request.pageSize() == 0) {
        return new SearchResults<>(List.of(), total);
      }
      List<ProjectAlmSettingDto> searchResults = performSearch(session, query, request.page(), request.pageSize());
      return new SearchResults<>(searchResults, total);
    }
  }

  private static ProjectAlmSettingQuery buildProjectAlmSettingQuery(ProjectBindingsSearchRequest request) {
    return new ProjectAlmSettingQuery(request.repository(), request.dopSettingId());
  }

  private List<ProjectAlmSettingDto> performSearch(DbSession dbSession, ProjectAlmSettingQuery query, int page, int pageSize) {
    return dbClient.projectAlmSettingDao().selectProjectAlmSettings(dbSession, query, page, pageSize)
      .stream()
      .toList();
  }

  public Optional<ProjectDto> findProjectFromBinding(ProjectAlmSettingDto projectAlmSettingDto) {
    try (DbSession session = dbClient.openSession(false)) {
      return dbClient.projectDao().selectByUuid(session, projectAlmSettingDto.getProjectUuid());
    }
  }

}
