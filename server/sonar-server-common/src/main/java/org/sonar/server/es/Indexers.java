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
package org.sonar.server.es;

import java.util.Collection;
import java.util.stream.Collectors;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.entity.EntityDto;

public interface Indexers {
  enum EntityEvent {
    CREATION,
    DELETION,
    PROJECT_KEY_UPDATE,
    PROJECT_TAGS_UPDATE,
    PERMISSION_CHANGE
  }

  enum BranchEvent {
    // Note that when a project/app is deleted, no events are sent for each branch removed as part of that process
    DELETION,
    MEASURE_CHANGE,
    SWITCH_OF_MAIN_BRANCH
  }

  /**
   * Re-index data based on the event. It commits the DB session once any indexing request was written in the same session,
   * ensuring consistency between the DB and the indexes. Therefore, DB data changes that cause the indexing event should
   * be done using the same DB session and the session should be uncommitted.
   */
  void commitAndIndexOnEntityEvent(DbSession dbSession, Collection<String> entityUuids, EntityEvent cause);

  /**
   * Re-index data based on the event. It commits the DB session once any indexing request was written in the same session,
   * ensuring consistency between the DB and the indexes. Therefore, DB data changes that cause the indexing event should
   * be done using the same DB session and the session should be uncommitted.
   */
  void commitAndIndexOnBranchEvent(DbSession dbSession, Collection<String> branchUuids, BranchEvent cause);

  /**
   * Re-index data based on the event. It commits the DB session once any indexing request was written in the same session,
   * ensuring consistency between the DB and the indexes. Therefore, DB data changes that cause the indexing event should
   * be done using the same DB session and the session should be uncommitted.
   */
  default void commitAndIndexEntities(DbSession dbSession, Collection<? extends EntityDto> entities, EntityEvent cause) {
    Collection<String> entityUuids = entities.stream()
      .map(EntityDto::getUuid)
      .collect(Collectors.toSet());
    commitAndIndexOnEntityEvent(dbSession, entityUuids, cause);
  }

  /**
   * Re-index data based on the event. It commits the DB session once any indexing request was written in the same session,
   * ensuring consistency between the DB and the indexes. Therefore, DB data changes that cause the indexing event should
   * be done using the same DB session and the session should be uncommitted.
   */
  default void commitAndIndexBranches(DbSession dbSession, Collection<BranchDto> branches, BranchEvent cause) {
    Collection<String> branchUuids = branches.stream()
      .map(BranchDto::getUuid)
      .collect(Collectors.toSet());
    commitAndIndexOnBranchEvent(dbSession, branchUuids, cause);
  }
}
