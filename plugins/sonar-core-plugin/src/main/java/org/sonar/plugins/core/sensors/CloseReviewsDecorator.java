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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.*;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.database.model.User;
import org.sonar.api.notifications.Notification;
import org.sonar.api.notifications.NotificationManager;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.security.UserFinder;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.NotDryRun;
import org.sonar.jpa.entity.Review;

import java.util.List;

/**
 * Decorator that handles the life cycle of a review (for instance, closes a review when its corresponding violation has been fixed).
 */
@NotDryRun
@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
@DependedUpon(CloseReviewsDecorator.REVIEW_LIFECYCLE_BARRIER)
public class CloseReviewsDecorator implements Decorator {

  private static final Logger LOG = LoggerFactory.getLogger(CloseReviewsDecorator.class);
  public static final String REVIEW_LIFECYCLE_BARRIER = "REVIEW_LIFECYCLE_BARRIER";

  private Project project;
  private ResourcePersister resourcePersister;
  private DatabaseSession databaseSession;
  private NotificationManager notificationManager;
  private UserFinder userFinder;

  public CloseReviewsDecorator(Project project, ResourcePersister resourcePersister, DatabaseSession databaseSession,
                               NotificationManager notificationManager, UserFinder userFinder) {
    this.project = project;
    this.resourcePersister = resourcePersister;
    this.databaseSession = databaseSession;
    this.notificationManager = notificationManager;
    this.userFinder = userFinder;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Snapshot currentSnapshot = resourcePersister.getSnapshot(resource);
    if (currentSnapshot != null) {
      int resourceId = currentSnapshot.getResourceId();
      int snapshotId = currentSnapshot.getId();

      closeReviewsOnResolvedViolations(resource, resourceId, snapshotId);
      reopenReviewsOnUnresolvedViolations(resource, resourceId);

      if (ResourceUtils.isRootProject(resource)) {
        closeReviewsForDeletedResources(resourceId, currentSnapshot.getId());
      }

      databaseSession.commit();
    }
  }

  /**
   * Close reviews for which violations have been fixed.
   */
  protected int closeReviewsOnResolvedViolations(Resource resource, int resourceId, int snapshotId) {
    String conditions = " WHERE resource_id=" + resourceId + " AND "
      + "( "
      + "  (manual_violation=:automaticViolation AND status!='CLOSED' AND rule_failure_permanent_id NOT IN " + "(SELECT permanent_id FROM rule_failures WHERE snapshot_id=" + snapshotId + " AND permanent_id IS NOT NULL))"
      + "  OR "
      + "  (manual_violation=:manualViolation AND status='RESOLVED')"
      + ")";
    List<Review> reviews = databaseSession.getEntityManager().createNativeQuery("SELECT * FROM reviews " + conditions, Review.class)
      .setParameter("automaticViolation", false)
      .setParameter("manualViolation", true)
      .getResultList();

    for (Review review : reviews) {
      notifyClosed(resource, review);
    }

    int rowUpdated = databaseSession.createNativeQuery("UPDATE reviews SET status='CLOSED', updated_at=CURRENT_TIMESTAMP" + conditions)
      .setParameter("automaticViolation", false)
      .setParameter("manualViolation", true)
      .executeUpdate();
    if (rowUpdated > 0) {
      LOG.debug("- {} reviews closed on #{}", rowUpdated, resourceId);
    }
    return rowUpdated;
  }

  /**
   * Reopen reviews that had been set to resolved but for which the violation is still here.
   * Manual violations are ignored.
   */
  protected int reopenReviewsOnUnresolvedViolations(Resource resource, int resourceId) {
    String conditions = " WHERE status='RESOLVED' AND resolution<>'FALSE-POSITIVE' AND manual_violation=:manualViolation AND resource_id=" + resourceId;
    List<Review> reviews = databaseSession.getEntityManager().createNativeQuery("SELECT * FROM reviews " + conditions, Review.class)
      .setParameter("manualViolation", false)
      .getResultList();
    for (Review review : reviews) {
      notifyReopened(resource, review);
    }

    int rowUpdated = databaseSession.createNativeQuery("UPDATE reviews SET status='REOPENED', resolution=NULL, updated_at=CURRENT_TIMESTAMP" + conditions)
      .setParameter("manualViolation", false)
      .executeUpdate();
    if (rowUpdated > 0) {
      LOG.debug("- {} reviews reopened on #{}", rowUpdated, resourceId);
    }
    return rowUpdated;
  }

  /**
   * Close reviews that relate to resources that have been deleted or renamed.
   */
  protected int closeReviewsForDeletedResources(int projectId, int projectSnapshotId) {
    String conditions = " WHERE status!='CLOSED' AND project_id=" + projectId
      + " AND resource_id IN ( SELECT prev.project_id FROM snapshots prev  WHERE prev.root_project_id=" + projectId
      + " AND prev.islast=? AND NOT EXISTS ( SELECT cur.id FROM snapshots cur WHERE cur.root_snapshot_id=" + projectSnapshotId
      + " AND cur.created_at > prev.created_at AND cur.root_project_id=" + projectId + " AND cur.project_id=prev.project_id ) )";
    List<Review> reviews = databaseSession.getEntityManager().createNativeQuery("SELECT * FROM reviews " + conditions, Review.class)
      .setParameter(1, Boolean.TRUE)
      .getResultList();
    for (Review review : reviews) {
      notifyClosed(null, review);
    }
    int rowUpdated = databaseSession.createNativeQuery("UPDATE reviews SET status='CLOSED', updated_at=CURRENT_TIMESTAMP" + conditions)
      .setParameter(1, Boolean.TRUE)
      .executeUpdate();
    LOG.debug("- {} reviews set to 'closed' on project #{}", rowUpdated, projectSnapshotId);
    return rowUpdated;
  }

  private String getCreator(Review review) {
    if (review.getUserId() == null) { // no creator and in fact this should never happen in real-life, however happens during unit tests
      return null;
    }
    User user = userFinder.findById(review.getUserId());
    return user != null ? user.getLogin() : null;
  }

  private String getAssignee(Review review) {
    if (review.getAssigneeId() == null) { // not assigned
      return null;
    }
    User user = userFinder.findById(review.getAssigneeId());
    return user != null ? user.getLogin() : null;
  }

  void notifyReopened(Resource resource, Review review) {
    Notification notification = createReviewNotification(resource, review)
      .setFieldValue("old.status", review.getStatus())
      .setFieldValue("new.status", "REOPENED")
      .setFieldValue("old.resolution", review.getResolution())
      .setFieldValue("new.resolution", null);
    notificationManager.scheduleForSending(notification);
  }

  void notifyClosed(Resource resource, Review review) {
    Notification notification = createReviewNotification(resource, review)
      .setFieldValue("old.status", review.getStatus())
      .setFieldValue("new.status", "CLOSED");
    notificationManager.scheduleForSending(notification);
  }

  private Notification createReviewNotification(Resource resource, Review review) {
    return new Notification("review-changed")
      .setFieldValue("reviewId", String.valueOf(review.getId()))
      .setFieldValue("project", project.getRoot().getLongName())
      .setFieldValue("resource", resource != null ? resource.getLongName() : null)
      .setFieldValue("title", review.getTitle())
      .setFieldValue("creator", getCreator(review))
      .setFieldValue("assignee", getAssignee(review));
  }

}
