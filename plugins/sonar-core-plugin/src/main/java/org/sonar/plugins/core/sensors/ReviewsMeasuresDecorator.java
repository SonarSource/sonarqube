/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rules.Violation;
import org.sonar.batch.components.PastSnapshot;
import org.sonar.batch.components.TimeMachineConfiguration;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewQuery;
import org.sonar.plugins.core.timemachine.ViolationTrackingDecorator;

import com.google.common.collect.Maps;

/**
 * Decorator that creates measures related to reviews.
 *
 * @since 2.14
 */
@DependsUpon(CloseReviewsDecorator.REVIEW_LIFECYCLE_BARRIER)
public class ReviewsMeasuresDecorator implements Decorator {

  private ReviewDao reviewDao;
  private TimeMachineConfiguration timeMachineConfiguration;

  public ReviewsMeasuresDecorator(ReviewDao reviewDao, TimeMachineConfiguration timeMachineConfiguration) {
    this.reviewDao = reviewDao;
    this.timeMachineConfiguration = timeMachineConfiguration;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return project.isLatestAnalysis();
  }

  @SuppressWarnings("rawtypes")
  @DependsUpon
  public Class dependsUponViolationTracking() {
    // permanent ids of violations have been updated, so we can link them with reviews
    return ViolationTrackingDecorator.class;
  }

  @SuppressWarnings({"rawtypes"})
  public void decorate(Resource resource, DecoratorContext context) {
    if (!ResourceUtils.isPersistable(resource) || ResourceUtils.isUnitTestClass(resource)) {
      return;
    }

    // Load open reviews (used for counting and also for tracking new violations without a review)
    ReviewQuery openReviewQuery = ReviewQuery.create().setResourceId(resource.getId()).addStatus(ReviewDto.STATUS_OPEN)
        .addStatus(ReviewDto.STATUS_REOPENED);
    List<ReviewDto> openReviews = reviewDao.selectByQuery(openReviewQuery);
    Map<Integer, ReviewDto> openReviewsByViolationPermanentIds = Maps.newHashMap();
    for (ReviewDto reviewDto : openReviews) {
      openReviewsByViolationPermanentIds.put(reviewDto.getViolationPermanentId(), reviewDto);
    }

    // Count open reviews
    Double resourceOpenReviewsCount = (double) openReviewsByViolationPermanentIds.size();
    Double totalOpenReviewsCount = resourceOpenReviewsCount + getChildrenSum(resource, context, CoreMetrics.ACTIVE_REVIEWS);
    context.saveMeasure(CoreMetrics.ACTIVE_REVIEWS, totalOpenReviewsCount);

    // Count unassigned reviews
    ReviewQuery unassignedReviewQuery = ReviewQuery.copy(openReviewQuery).setNoAssignee();
    Double ressourceUnassignedReviewsCount = reviewDao.countByQuery(unassignedReviewQuery).doubleValue();
    Double totalUnassignedReviewsCount = ressourceUnassignedReviewsCount
      + getChildrenSum(resource, context, CoreMetrics.UNASSIGNED_REVIEWS);
    context.saveMeasure(CoreMetrics.UNASSIGNED_REVIEWS, totalUnassignedReviewsCount);

    // Count unplanned reviews
    ReviewQuery plannedReviewQuery = ReviewQuery.copy(openReviewQuery).setPlanned();
    Double resourcePlannedReviewsCount = reviewDao.countByQuery(plannedReviewQuery).doubleValue();
    Double childrenUnplannedReviewsCount = getChildrenSum(resource, context, CoreMetrics.UNPLANNED_REVIEWS);
    context.saveMeasure(CoreMetrics.UNPLANNED_REVIEWS, (resourceOpenReviewsCount - resourcePlannedReviewsCount)
      + childrenUnplannedReviewsCount);

    // Count false positive reviews
    ReviewQuery falsePositiveReviewQuery = ReviewQuery.create().setResourceId(resource.getId())
        .addResolution(ReviewDto.RESOLUTION_FALSE_POSITIVE);
    Double resourceFalsePositiveReviewsCount = reviewDao.countByQuery(falsePositiveReviewQuery).doubleValue();
    Double totalFalsePositiveReviewsCount = resourceFalsePositiveReviewsCount
      + getChildrenSum(resource, context, CoreMetrics.FALSE_POSITIVE_REVIEWS);
    context.saveMeasure(CoreMetrics.FALSE_POSITIVE_REVIEWS, totalFalsePositiveReviewsCount);

    // Count violations without a review
    Double violationsCount = MeasureUtils.getValue(context.getMeasure(CoreMetrics.VIOLATIONS), 0.0);
    context.saveMeasure(CoreMetrics.UNREVIEWED_VIOLATIONS, violationsCount - totalOpenReviewsCount);

    // And finally track new violations without a review
    trackNewViolationsWithoutReview(context, openReviewsByViolationPermanentIds);
  }

  protected void trackNewViolationsWithoutReview(DecoratorContext context, Map<Integer, ReviewDto> openReviewsByViolationPermanentIds) {
    List<Violation> violations = context.getViolations();
    Measure measure = new Measure(CoreMetrics.NEW_UNREVIEWED_VIOLATIONS);
    for (PastSnapshot pastSnapshot : timeMachineConfiguration.getProjectPastSnapshots()) {
      int newUnreviewedViolations = countNewUnreviewedViolationsForSnapshot(pastSnapshot, violations, openReviewsByViolationPermanentIds);
      int variationIndex = pastSnapshot.getIndex();
      Collection<Measure> children = context.getChildrenMeasures(CoreMetrics.NEW_UNREVIEWED_VIOLATIONS);
      double sumNewUnreviewedViolations = MeasureUtils.sumOnVariation(true, variationIndex, children) + newUnreviewedViolations;
      measure.setVariation(variationIndex, sumNewUnreviewedViolations);
    }
    context.saveMeasure(measure);
  }

  protected int countNewUnreviewedViolationsForSnapshot(PastSnapshot pastSnapshot, List<Violation> violations,
      Map<Integer, ReviewDto> openReviewsByViolationPermanentIds) {
    Date targetDate = pastSnapshot.getTargetDate();
    int newViolationCount = 0;
    int newReviewedViolationCount = 0;
    for (Violation violation : violations) {
      if (isAfter(violation, targetDate)) {
        newViolationCount += 1;
        if (openReviewsByViolationPermanentIds.get(violation.getPermanentId()) != null) {
          newReviewedViolationCount += 1;
        }
      }
    }
    return newViolationCount - newReviewedViolationCount;
  }

  private Double getChildrenSum(Resource<?> resource, DecoratorContext context, Metric metric) {
    Double sum = 0d;
    if (!ResourceUtils.isFile(resource)) {
      sum = MeasureUtils.sum(true, context.getChildrenMeasures(metric));
    }
    return sum;
  }

  private boolean isAfter(Violation violation, Date date) {
    if (date == null) {
      return true;
    }
    return violation.getCreatedAt() != null && violation.getCreatedAt().after(date);
  }

}
