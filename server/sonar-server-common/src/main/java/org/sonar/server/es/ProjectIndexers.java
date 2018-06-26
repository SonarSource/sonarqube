/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.sonar.db.component.ComponentDto;

public interface ProjectIndexers {

  /**
   * Commits the DB transaction and indexes the specified projects, if needed (according to
   * "cause" parameter).
   * IMPORTANT - UUIDs must relate to projects only. Modules, directories and files are forbidden
   * and will lead to lack of indexing.
   */
  void commitAndIndexByProjectUuids(DbSession dbSession, Collection<String> projectUuids, ProjectIndexer.Cause cause);

  default void commitAndIndex(DbSession dbSession, Collection<ComponentDto> projectOrModules, ProjectIndexer.Cause cause) {
    Collection<String> projectUuids = projectOrModules.stream()
      .map(ComponentDto::projectUuid)
      .collect(MoreCollectors.toSet(projectOrModules.size()));
    commitAndIndexByProjectUuids(dbSession, projectUuids, cause);
  }
}
