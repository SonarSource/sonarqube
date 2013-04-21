/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.core.sensors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.*;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Violation;
import org.sonar.api.violations.ViolationQuery;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.DryRunIncompatible;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;

import javax.annotation.Nullable;
import java.util.*;

@DryRunIncompatible
@DependsUpon(DecoratorBarriers.END_OF_VIOLATION_TRACKING)
@DependedUpon(ReviewWorkflowDecorator.END_OF_REVIEWS_UPDATES)
public class ReviewWorkflowDecorator implements Decorator {

  public static final String END_OF_REVIEWS_UPDATES = "END_OF_REVIEWS_UPDATES";

  private ReviewNotifications notifications;
  private ReviewDao reviewDao;
  private ResourcePersister resourcePersister;

  public ReviewWorkflowDecorator(ReviewNotifications notifications, ReviewDao reviewDao, ResourcePersister resourcePersister) {
    this.notifications = notifications;
    this.reviewDao = reviewDao;
    this.resourcePersister = resourcePersister;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  public void decorate(Resource resource, DecoratorContext context) {
    Snapshot snapshot = resourcePersister.getSnapshot(resource);
    if (snapshot != null) {
      Collection<ReviewDto> openReviews = reviewDao.selectOpenByResourceId(snapshot.getResourceId());
      Set<ReviewDto> updated = Sets.newHashSet();
      if (!openReviews.isEmpty()) {
        List<Violation> violations = context.getViolations(ViolationQuery.create().forResource(resource).setSwitchMode(ViolationQuery.SwitchMode.BOTH));
        closeResolvedStandardViolations(openReviews, violations, context.getProject(), resource, updated);
        closeResolvedManualViolations(openReviews, context.getProject(), resource, updated);
        reopenUnresolvedViolations(openReviews, context.getProject(), resource, updated);
        updateReviewInformation(openReviews, violations, updated);
      }
      if (ResourceUtils.isRootProject(resource)) {
        closeReviewsOnDeletedResources((Project) resource, snapshot.getResourceId(), snapshot.getId(), updated);
      }
      persistUpdates(updated);
    }
  }

  private void persistUpdates(Set<ReviewDto> updated) {
    if (!updated.isEmpty()) {
      reviewDao.update(updated);
    }
  }

  /**
   * Close reviews that relate to resources that have been deleted or renamed.
   */
  private void closeReviewsOnDeletedResources(Project project, int rootProjectId, int rootSnapshotId, Set<ReviewDto> updated) {
    Collection<ReviewDto> reviews = reviewDao.selectOnDeletedResources(rootProjectId, rootSnapshotId);
    for (ReviewDto review : reviews) {
      close(review, project, null);
      updated.add(review);
    }
  }

  private void updateReviewInformation(Collection<ReviewDto> openReviews, Collection<Violation> violations, Set<ReviewDto> updated) {
    Map<Integer, Violation> violationsByPermanentId = Maps.newHashMap();
    for (Violation violation : violations) {
      if (violation.getPermanentId()!=null) {
        violationsByPermanentId.put(violation.getPermanentId(), violation);
      }
    }

    for (ReviewDto review : openReviews) {
      Violation violation = violationsByPermanentId.get(review.getViolationPermanentId());
      if (violation != null && !hasUpToDateInformation(review, violation)) {
        review.setLine(violation.getLineId());
        review.setTitle(violation.getMessage());
        updated.add(review);
      }
    }
  }

  @VisibleForTesting
  static boolean hasUpToDateInformation(ReviewDto review, Violation violation) {
    return StringUtils.equals(review.getTitle(), violation.getMessage()) && ObjectUtils.equals(review.getLine(), violation.getLineId());
  }

  private void closeResolvedManualViolations(Collection<ReviewDto> openReviews, Project project, Resource resource, Set<ReviewDto> updated) {
    for (ReviewDto openReview : openReviews) {
      if (openReview.isManualViolation() && ReviewDto.STATUS_RESOLVED.equals(openReview.getStatus())) {
        close(openReview, project, resource);
        updated.add(openReview);
      }
    }
  }

  private void closeResolvedStandardViolations(Collection<ReviewDto> openReviews, List<Violation> violations, Project project, Resource resource, Set<ReviewDto> updated) {
    Set<Integer> violationIds = Sets.newHashSet(Collections2.transform(violations, new ViolationToPermanentIdFunction()));

    for (ReviewDto openReview : openReviews) {
      if (!openReview.isManualViolation() && !violationIds.contains(openReview.getViolationPermanentId())) {
        close(openReview, project, resource);
        updated.add(openReview);
      }
    }
  }

  private void reopenUnresolvedViolations(Collection<ReviewDto> openReviews, Project project, Resource resource, Set<ReviewDto> updated) {
    for (ReviewDto openReview : openReviews) {
      if (ReviewDto.STATUS_RESOLVED.equals(openReview.getStatus()) && !ReviewDto.RESOLUTION_FALSE_POSITIVE.equals(openReview.getResolution())
          && !openReview.isManualViolation()) {
        reopen(openReview, project, resource);
        updated.add(openReview);
      }
    }
  }

  private void close(ReviewDto review, Project project, Resource resource) {
    notifications.notifyClosed(review, project, resource);
    review.setStatus(ReviewDto.STATUS_CLOSED);
    review.setUpdatedAt(new Date());
  }

  private void reopen(ReviewDto review, Project project, Resource resource) {
    notifications.notifyReopened(review, project, resource);
    review.setStatus(ReviewDto.STATUS_REOPENED);
    review.setResolution(null);
    review.setUpdatedAt(new Date());
  }

  private static final class ViolationToPermanentIdFunction implements Function<Violation, Integer> {
    public Integer apply(@Nullable Violation violation) {
      return (violation != null ? violation.getPermanentId() : null);
    }
  }
}
