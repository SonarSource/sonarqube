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
package org.sonar.server.common.projectbindings.service;

import java.util.List;
import java.util.function.Function;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingQuery;

/**
 * Enum-based strategy for searching project bindings based on Git repository information.
 * Each ALM platform (GitHub, Azure DevOps, Bitbucket) has different storage patterns for repository information in the database.
 */
public enum ProjectBindingSearchStrategy {

  /**
   * GitHub search strategy.
   * GitHub stores repository information in "org/repo" format in the alm_repo column.
   */
  GITHUB(info -> ProjectAlmSettingQuery.forAlmRepo(info.slug())),

  /**
   * Azure DevOps search strategy.
   * Azure DevOps stores project name in alm_slug and repository name in alm_repo.
   */
  AZURE_DEVOPS(info -> ProjectAlmSettingQuery.forAlmRepoAndSlug(info.repository(), info.projectName())),

  /**
   * Bitbucket search strategy.
   * Bitbucket stores repository name in the alm_repo column.
   */
  BITBUCKET(info -> ProjectAlmSettingQuery.forAlmRepo(info.repository()));

  private final Function<GitUrlParser.RepositoryInfo, ProjectAlmSettingQuery> queryBuilder;

  ProjectBindingSearchStrategy(Function<GitUrlParser.RepositoryInfo, ProjectAlmSettingQuery> queryBuilder) {
    this.queryBuilder = queryBuilder;
  }

  /**
   * Searches for project ALM settings based on the provided repository information.
   * 
   * @param dbClient the database client
   * @param session the database session
   * @param repositoryInfo parsed Git repository information
   * @return list of matching project ALM settings
   */
  public List<ProjectAlmSettingDto> search(DbClient dbClient, DbSession session, GitUrlParser.RepositoryInfo repositoryInfo) {
    ProjectAlmSettingQuery query = queryBuilder.apply(repositoryInfo);
    return dbClient.projectAlmSettingDao().selectProjectAlmSettings(session, query, 1, Integer.MAX_VALUE);
  }

}
