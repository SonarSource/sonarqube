/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.index;

import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;

public interface ResourcePersister {
  
  Snapshot saveProject(Project project, Project parent);

  /**
   * Persist a resource in database. Returns null if the resource must not be persisted (scope lower than file)
   */
  Snapshot saveResource(Project project, Resource resource, Resource parent);

  /**
   * Persist a resource in database. Returns null if the resource must not be persisted (scope lower than file)
   */
  Snapshot saveResource(Project project, Resource resource);

  Snapshot getSnapshot(Resource resource);

  /**
   * @throws ResourceNotPersistedException if the resource is not persisted.
   */
  Snapshot getSnapshotOrFail(Resource resource);
 

  /**
   * The current snapshot which is flagged as "last", different then the current analysis.
   * @param onlyOlder true if the result must be anterior to the snapshot parameter
   */
  Snapshot getLastSnapshot(Snapshot snapshot, boolean onlyOlder);

  void clear();
}
