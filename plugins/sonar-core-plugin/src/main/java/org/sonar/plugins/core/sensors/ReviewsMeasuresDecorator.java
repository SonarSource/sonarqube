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
package org.sonar.plugins.core.sensors;

import com.google.common.collect.Maps;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependedUpon;
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
import org.sonar.core.review.ReviewPredicates;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Decorator that creates measures related to reviews.
 *
 * @since 2.14
 */
@DependsUpon(ReviewWorkflowDecorator.END_OF_REVIEWS_UPDATES)
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

  @DependedUpon
  public Collection<Metric> generatesMetrics() {
    return Arrays.asList(CoreMetrics.ACTIVE_REVIEWS, CoreMetrics.UNASSIGNED_REVIEWS, CoreMetrics.UNPLANNED_REVIEWS, CoreMetrics.FALSE_POSITIVE_REVIEWS,
        CoreMetrics.UNREVIEWED_VIOLATIONS, CoreMetrics.NEW_UNREVIEWED_VIOLATIONS);
  }

  @SuppressWarnings({"rawtypes"})
  public void decorate(Resource resource, DecoratorContext context) {
    if (!ResourceUtils.isPersistable(resource) || resource.getId() == null) {
      return;
    }

    // Load open reviews (used for counting and also for tracking new violations without a review)
    Collection<ReviewDto> openReviews = reviewDao.selectOpenByResourceId(resource.getId(),
        ReviewPredicates.status(ReviewDto.STATUS_OPEN, ReviewDto.STATUS_REOPENED));

    Map<Integer, ReviewDto> openReviewsByViolationPermanentId = Maps.newHashMap();
    int countUnassigned = 0;
    int unplanned = 0;
    for (ReviewDto openReview : openReviews) {
      openReviewsByViolationPermanentId.put(openReview.getViolationPermanentId(), openReview);
      if (openReview.getAssigneeId() == null) {
        countUnassigned++;
      }
      if (openReview.getActionPlanId() == null) {
        unplanned++;
      }
    }

    int totalOpenReviews = openReviews.size() + sumChildren(resource, context, CoreMetrics.ACTIVE_REVIEWS);
    context.saveMeasure(CoreMetrics.ACTIVE_REVIEWS, (double) totalOpenReviews);
    context.saveMeasure(CoreMetrics.UNASSIGNED_REVIEWS, (double) (countUnassigned + sumChildren(resource, context, CoreMetrics.UNASSIGNED_REVIEWS)));
    context.saveMeasure(CoreMetrics.UNPLANNED_REVIEWS, (double) (unplanned + sumChildren(resource, context, CoreMetrics.UNPLANNED_REVIEWS)));

    Collection<ReviewDto> falsePositives = reviewDao.selectOpenByResourceId(resource.getId(),
        ReviewPredicates.resolution(ReviewDto.RESOLUTION_FALSE_POSITIVE));

    context.saveMeasure(CoreMetrics.FALSE_POSITIVE_REVIEWS, (double) (falsePositives.size() + sumChildren(resource, context, CoreMetrics.FALSE_POSITIVE_REVIEWS)));

    Double violationsCount = MeasureUtils.getValue(context.getMeasure(CoreMetrics.VIOLATIONS), 0.0);
    context.saveMeasure(CoreMetrics.UNREVIEWED_VIOLATIONS, violationsCount - totalOpenReviews);

    trackNewViolationsWithoutReview(context, openReviewsByViolationPermanentId);
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

  private int sumChildren(Resource<?> resource, DecoratorContext context, Metric metric) {
    int sum = 0;
    if (!ResourceUtils.isFile(resource)) {
      sum = MeasureUtils.sum(true, context.getChildrenMeasures(metric)).intValue();
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
