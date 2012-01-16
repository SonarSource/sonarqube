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

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.core.review.ReviewDao;
import org.sonar.core.review.ReviewDto;
import org.sonar.core.review.ReviewQuery;
import org.sonar.plugins.core.timemachine.ViolationTrackingDecorator;

/**
 * Decorator that creates measures related to reviews.
 *
 * @since 2.14
 */
@DependsUpon(CloseReviewsDecorator.REVIEW_LIFECYCLE_BARRIER)
public class ReviewsMeasuresDecorator implements Decorator {

  private ReviewDao reviewDao;

  public ReviewsMeasuresDecorator(ReviewDao reviewDao) {
    this.reviewDao = reviewDao;
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

    // Open reviews
    ReviewQuery openReviewQuery = ReviewQuery.create().setResourceId(resource.getId()).addStatus(ReviewDto.STATUS_OPEN)
        .addStatus(ReviewDto.STATUS_REOPENED);
    Double resourceOpenReviewsCount = reviewDao.countByQuery(openReviewQuery).doubleValue();
    Double totalOpenReviewsCount = resourceOpenReviewsCount + getChildrenSum(resource, context, CoreMetrics.ACTIVE_REVIEWS);
    context.saveMeasure(CoreMetrics.ACTIVE_REVIEWS, totalOpenReviewsCount);

    // Unassigned reviews
    ReviewQuery unassignedReviewQuery = ReviewQuery.copy(openReviewQuery).setNoAssignee();
    Double ressourceUnassignedReviewsCount = reviewDao.countByQuery(unassignedReviewQuery).doubleValue();
    Double totalUnassignedReviewsCount = ressourceUnassignedReviewsCount
      + getChildrenSum(resource, context, CoreMetrics.UNASSIGNED_REVIEWS);
    context.saveMeasure(CoreMetrics.UNASSIGNED_REVIEWS, totalUnassignedReviewsCount);

    // Unplanned reviews
    ReviewQuery plannedReviewQuery = ReviewQuery.copy(openReviewQuery).setPlanned();
    Double resourcePlannedReviewsCount = reviewDao.countByQuery(plannedReviewQuery).doubleValue();
    Double childrenUnplannedReviewsCount = getChildrenSum(resource, context, CoreMetrics.UNPLANNED_REVIEWS);
    context.saveMeasure(CoreMetrics.UNPLANNED_REVIEWS, (resourceOpenReviewsCount - resourcePlannedReviewsCount)
      + childrenUnplannedReviewsCount);

    // False positive reviews
    ReviewQuery falsePositiveReviewQuery = ReviewQuery.create().setResourceId(resource.getId())
        .addResolution(ReviewDto.RESOLUTION_FALSE_POSITIVE);
    Double resourceFalsePositiveReviewsCount = reviewDao.countByQuery(falsePositiveReviewQuery).doubleValue();
    Double totalFalsePositiveReviewsCount = resourceFalsePositiveReviewsCount
      + getChildrenSum(resource, context, CoreMetrics.FALSE_POSITIVE_REVIEWS);
    context.saveMeasure(CoreMetrics.FALSE_POSITIVE_REVIEWS, totalFalsePositiveReviewsCount);

    // Violations without a review
    Double violationsCount = MeasureUtils.getValue(context.getMeasure(CoreMetrics.VIOLATIONS), 0.0);
    context.saveMeasure(CoreMetrics.VIOLATIONS_WITHOUT_REVIEW, violationsCount - totalOpenReviewsCount);
  }

  private Double getChildrenSum(Resource<?> resource, DecoratorContext context, Metric metric) {
    Double sum = 0d;
    if (!ResourceUtils.isFile(resource)) {
      sum = MeasureUtils.sum(true, context.getChildrenMeasures(metric));
    }
    return sum;
  }

}
