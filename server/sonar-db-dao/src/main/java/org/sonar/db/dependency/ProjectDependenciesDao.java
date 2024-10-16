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
package org.sonar.db.dependency;

import java.util.List;
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;
import org.sonar.db.Pagination;

public class ProjectDependenciesDao implements Dao {

  private static ProjectDependenciesMapper mapper(DbSession session) {
    return session.getMapper(ProjectDependenciesMapper.class);
  }

  public void insert(DbSession session, ProjectDependencyDto projectDependencyDto) {
    mapper(session).insert(projectDependencyDto);
  }

  public void deleteByUuid(DbSession session, String uuid) {
    mapper(session).deleteByUuid(uuid);
  }

  public Optional<ProjectDependencyDto> selectByUuid(DbSession dbSession, String uuid) {
    return Optional.ofNullable(mapper(dbSession).selectByUuid(uuid));
  }

  /**
   * Retrieves all dependencies with a specific branch UUID, no other filtering is done by this method.
   */
  public List<ProjectDependencyDto> selectByBranchUuid(DbSession dbSession, String branchUuid) {
    return mapper(dbSession).selectByBranchUuid(branchUuid);
  }

  public List<ProjectDependencyDto> selectByQuery(DbSession session, ProjectDependenciesQuery projectDependenciesQuery, Pagination pagination) {
    return mapper(session).selectByQuery(projectDependenciesQuery, pagination);
  }

  public int countByQuery(DbSession session, ProjectDependenciesQuery projectDependenciesQuery) {
    return mapper(session).countByQuery(projectDependenciesQuery);
  }

  public void update(DbSession session, ProjectDependencyDto projectDependencyDto) {
    mapper(session).update(projectDependencyDto);
  }
}
