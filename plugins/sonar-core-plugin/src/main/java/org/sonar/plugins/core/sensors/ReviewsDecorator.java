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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.index.ResourcePersister;

/**
 * Decorator that currently only closes a review when its corresponding violation has been fixed.
 */
@DependsUpon("ViolationPersisterDecorator")
public class ReviewsDecorator implements Decorator {

  private static final Logger LOG = LoggerFactory.getLogger(ReviewsDecorator.class);

  private ResourcePersister resourcePersister;
  private DatabaseSession databaseSession;

  public ReviewsDecorator(ResourcePersister resourcePersister, DatabaseSession databaseSession) {
    this.resourcePersister = resourcePersister;
    this.databaseSession = databaseSession;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Snapshot currentSnapshot = resourcePersister.getSnapshot(resource);
    if (currentSnapshot != null) {
      int resourceId = currentSnapshot.getResourceId();
      int snapshotId = currentSnapshot.getId();
      Query query = databaseSession.createNativeQuery("UPDATE reviews SET status='closed' " + "WHERE resource_id = " + resourceId
          + " AND rule_failure_permanent_id NOT IN " + "(SELECT permanent_id FROM rule_failures WHERE snapshot_id = " + snapshotId + ")");
      int rowUpdated = query.executeUpdate();
      LOG.debug("- {} reviews set to 'closed' on resource #{}", rowUpdated, resourceId);
      databaseSession.commit();
    }
  }

}
