/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.dbcleaner.api;

import org.sonar.api.BatchExtension;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.persistence.resource.ResourceIndexerDao;

import java.util.List;

/**
 * @since 2.13
 */
public class DbCleanerCommands implements BatchExtension {

  private DatabaseSession session;
  private ResourceIndexerDao resourceIndexer;

  public DbCleanerCommands(DatabaseSession session, ResourceIndexerDao resourceIndexer) {
    this.session = session;
    this.resourceIndexer = resourceIndexer;
  }

  public DbCleanerCommands deleteSnapshots(List<Integer> snapshotIds, boolean includeDependents) {
    if (includeDependents) {
      PurgeUtils.deleteSnapshotsData(session, snapshotIds);
    } else {
      PurgeUtils.deleteSnapshots(session, snapshotIds);
    }
    return this;
  }

  public DbCleanerCommands deleteResources(List<Integer> resourceIds) {
    PurgeUtils.executeQuery(session, "", resourceIds, "DELETE FROM " + ResourceModel.class.getSimpleName() + " WHERE id in (:ids)");
    resourceIndexer.delete(resourceIds);
    return this;
  }
}
