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

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.RuleFailureModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.NotDryRun;
import org.sonar.jpa.entity.Review;
import org.sonar.plugins.core.timemachine.ViolationTrackingDecorator;

import com.google.common.collect.Maps;

/**
 * Decorator that updates reviews that are linked to violations for which the message and the line number have changed. In this case, the
 * message of the review and its corresponding line number must change.
 */
@NotDryRun
@DependsUpon(CloseReviewsDecorator.REVIEW_LIFECYCLE_BARRIER)
public class UpdateReviewsDecorator implements Decorator {

  private ResourcePersister resourcePersister;
  private DatabaseSession databaseSession;
  private ViolationTrackingDecorator violationTrackingDecorator;
  private Query updateReviewQuery;
  private Map<Integer, Violation> violationsPerPermanentId;

  public UpdateReviewsDecorator(ResourcePersister resourcePersister, DatabaseSession databaseSession,
      ViolationTrackingDecorator violationTrackingDecorator) {
    this.resourcePersister = resourcePersister;
    this.databaseSession = databaseSession;
    this.violationTrackingDecorator = violationTrackingDecorator;
    violationsPerPermanentId = Maps.newHashMap();
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  @SuppressWarnings({ "rawtypes" })
  public void decorate(Resource resource, DecoratorContext context) {
    Snapshot currentSnapshot = resourcePersister.getSnapshot(resource);
    if (currentSnapshot != null) {
      Date currentDate = new Date();
      // prepare the map of rule failures by permanent_id
      for (Violation violation : context.getViolations()) {
        RuleFailureModel ruleFailure = violationTrackingDecorator.getReferenceViolation(violation);
        if (ruleFailure != null) {
          violationsPerPermanentId.put(ruleFailure.getPermanentId(), violation);
        }
      }
      // and run the update
      updateReviews(currentSnapshot.getResourceId(), currentDate);

      databaseSession.commit();
    }
  }

  @SuppressWarnings({ "unchecked" })
  protected void updateReviews(int resourceId, Date currentDate) {
    // prepare the DB native queries
    updateReviewQuery = databaseSession
        .createNativeQuery("UPDATE reviews SET title=?, resource_line=?, updated_at=CURRENT_TIMESTAMP WHERE id=?");
    Query searchReviewsQuery = databaseSession.getEntityManager().createNativeQuery(
        "SELECT * FROM reviews WHERE status!='CLOSED' AND resource_id=?", Review.class);
    // and iterate over the reviews that we find
    List<Review> reviews = searchReviewsQuery.setParameter(1, resourceId).getResultList();
    for (Review review : reviews) {
      checkReviewForUpdate(review, currentDate);
    }
  }

  protected void checkReviewForUpdate(Review review, Date currentDate) {
    Violation violation = violationsPerPermanentId.get(review.getRuleFailurePermamentId());
    if (violation != null) {
      String message = violation.getMessage();
      Integer line = violation.getLineId();
      if ( !review.getTitle().equals(message) || !review.getResourceLine().equals(line)) {
        updateReviewQuery.setParameter(1, message).setParameter(2, line).setParameter(3, review.getId()).executeUpdate();
      }
    }
  }

}
