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
package org.sonar.server.es;

import java.util.Collection;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;

public interface ProjectIndexers {

  /**
   * Commits the DB transaction and indexes the specified projects, if needed (according to
   * "cause" parameter).
   * IMPORTANT - UUIDs must relate to applications and projects only. Modules, directories and files are forbidden
   * and will lead to lack of indexing.
   */
  void commitAndIndexByProjectUuids(DbSession dbSession, Collection<String> projectUuids, ProjectIndexer.Cause cause);

  default void commitAndIndexProjects(DbSession dbSession, Collection<ProjectDto> projects, ProjectIndexer.Cause cause) {
    Collection<String> projectUuids = projects.stream()
      .map(ProjectDto::getUuid)
      .collect(MoreCollectors.toSet(projects.size()));
    commitAndIndexByProjectUuids(dbSession, projectUuids, cause);
  }

  default void commitAndIndexComponents(DbSession dbSession, Collection<ComponentDto> projects, ProjectIndexer.Cause cause) {
    Collection<String> projectUuids = projects.stream()
      .map(ComponentDto::branchUuid)
      .collect(MoreCollectors.toSet(projects.size()));
    commitAndIndexByProjectUuids(dbSession, projectUuids, cause);
  }

  default void commitAndIndexBranches(DbSession dbSession, Collection<BranchDto> branches, ProjectIndexer.Cause cause) {
    Collection<String> branchUuids = branches.stream()
      .map(BranchDto::getUuid)
      .toList();
    commitAndIndexByProjectUuids(dbSession, branchUuids, cause);
  }
}
