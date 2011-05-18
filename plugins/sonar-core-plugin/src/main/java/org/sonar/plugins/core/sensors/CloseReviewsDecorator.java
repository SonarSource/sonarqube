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
package org.sonar.plugins.core.sensors;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.index.ResourcePersister;

/**
 * Decorator that currently only closes a review when its corresponding violation has been fixed.
 */
@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
public class CloseReviewsDecorator implements Decorator {

  private static final Logger LOG = LoggerFactory.getLogger(CloseReviewsDecorator.class);

  private ResourcePersister resourcePersister;
  private DatabaseSession databaseSession;

  public CloseReviewsDecorator(ResourcePersister resourcePersister, DatabaseSession databaseSession) {
    this.resourcePersister = resourcePersister;
    this.databaseSession = databaseSession;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Snapshot currentSnapshot = resourcePersister.getSnapshot(resource);
    if (currentSnapshot != null) {
      int resourceId = currentSnapshot.getResourceId();
      int snapshotId = currentSnapshot.getId();
      Query query = databaseSession.createNativeQuery(generateUpdateOnResourceSqlRequest(resourceId, snapshotId));
      int rowUpdated = query.executeUpdate();
      LOG.debug("- {} reviews set to 'closed' on resource #{}", rowUpdated, resourceId);

      if (ResourceUtils.isRootProject(resource)) {
        query = databaseSession.createNativeQuery(generateUpdateOnProjectSqlRequest(resourceId, currentSnapshot.getId()));
        query.setParameter(1, Boolean.TRUE);
        rowUpdated = query.executeUpdate();
        LOG.debug("- {} reviews set to 'closed' on project #{}", rowUpdated, resourceId);
      }

      databaseSession.commit();
    }
  }

  protected String generateUpdateOnResourceSqlRequest(int resourceId, int snapshotId) {
    return "UPDATE reviews SET status='CLOSED', updated_at=CURRENT_TIMESTAMP WHERE resource_id = " + resourceId
        + " AND rule_failure_permanent_id NOT IN " + "(SELECT permanent_id FROM rule_failures WHERE snapshot_id = " + snapshotId
        + " AND permanent_id IS NOT NULL)";
  }

  protected String generateUpdateOnProjectSqlRequest(int projectId, int projectSnapshotId) {
    return "UPDATE reviews SET status='CLOSED', updated_at=CURRENT_TIMESTAMP WHERE status='OPEN' AND project_id=" + projectId
        + " AND resource_id IN ( SELECT prev.project_id FROM snapshots prev  WHERE prev.root_project_id=" + projectId
        + " AND prev.islast=? AND NOT EXISTS ( SELECT cur.id FROM snapshots cur WHERE cur.root_snapshot_id=" + projectSnapshotId
        + " AND cur.created_at > prev.created_at AND cur.root_project_id=" + projectId + " AND cur.project_id=prev.project_id ) )";
  }

}
